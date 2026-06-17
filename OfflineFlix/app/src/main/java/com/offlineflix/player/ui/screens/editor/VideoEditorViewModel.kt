package com.offlineflix.player.ui.screens.editor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Statistics
import com.offlineflix.player.data.models.VideoEntity
import com.offlineflix.player.data.repository.VideoRepository
import com.offlineflix.player.worker.FfmpegWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VideoEditorUiState(
    val videoName: String = "",
    val videoInfo: String = "",
    val videoPath: String = "",
    val duration: Long = 0,
    val isProcessing: Boolean = false,
    val processingProgress: Int = 0,
    val processingStatus: String = "",
    val remainingTime: String = "",
    val selectedFilter: String = "",
    val outputPath: String = ""
)

/**
 * ViewModel محرر الفيديو مع FFmpegKit
 */
@HiltViewModel
class VideoEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRepository: VideoRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoEditorUiState())
    val uiState: StateFlow<VideoEditorUiState> = _uiState.asStateFlow()

    private var currentVideo: VideoEntity? = null
    private var currentSessionId: Long = 0

    fun loadVideo(videoId: Long) {
        viewModelScope.launch {
            val video = videoRepository.getById(videoId) ?: return@launch
            currentVideo = video
            _uiState.update {
                it.copy(
                    videoName = video.displayName,
                    videoPath = video.path,
                    duration = video.duration,
                    videoInfo = "${video.width}x${video.height} • ${video.videoCodec} • ${formatBitrate(video.bitrate)}"
                )
            }
        }
    }

    private fun formatBitrate(bps: Long): String {
        return if (bps > 1_000_000) "${bps / 1_000_000}Mbps" else "${bps / 1000}Kbps"
    }

    /** تنفيذ أمر FFmpeg مع تتبع التقدم */
    private fun executeFFmpeg(command: String, onComplete: (Boolean) -> Unit = {}) {
        _uiState.update { it.copy(isProcessing = true, processingProgress = 0) }
        val startTime = System.currentTimeMillis()

        FFmpegKit.executeAsync(command, { session ->
            val success = ReturnCode.isSuccess(session.returnCode)
            _uiState.update { it.copy(isProcessing = false, processingProgress = 100) }
            onComplete(success)
        }, { log ->
            // سجل FFmpeg
        }, { stats: Statistics ->
            // حساب التقدم
            val dur = currentVideo?.duration ?: 1
            if (dur > 0 && stats.time > 0) {
                val progress = ((stats.time.toFloat() / dur) * 100).toInt().coerceIn(0, 99)
                val elapsed = System.currentTimeMillis() - startTime
                val estimatedTotal = if (progress > 0) elapsed * 100 / progress else 0
                val remaining = ((estimatedTotal - elapsed) / 1000).coerceAtLeast(0)
                _uiState.update {
                    it.copy(
                        processingProgress = progress,
                        processingStatus = "جاري المعالجة...",
                        remainingTime = "${remaining}s"
                    )
                }
            }
        })
    }

    /** الحصول على مسار الإخراج */
    private fun getOutputPath(suffix: String, ext: String = "mp4"): String {
        val dir = File(context.getExternalFilesDir(null), "OfflineFlix/Edited")
        dir.mkdirs()
        val name = currentVideo?.name?.substringBeforeLast(".") ?: "output"
        return "${dir.absolutePath}/${name}_$suffix.$ext"
    }

    fun openTrimmer() {
        // سيُفتح حوار القص مع محدد الوقت
    }

    fun losslessCut() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("cut")
        // قص بدون إعادة ترميز من الموضع الحالي
        executeFFmpeg("-i \"$input\" -ss 0 -to 60 -c copy \"$output\"")
    }

    fun openMerger() { /* فتح حوار الدمج */ }
    fun appendVideo() { /* إضافة فيديو للنهاية */ }

    fun extractAudio() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("audio", "mp3")
        _uiState.update { it.copy(processingStatus = "استخراج الصوت...") }
        executeFFmpeg("-i \"$input\" -vn -acodec libmp3lame -q:a 0 \"$output\"") { success ->
            if (success) android.util.Log.d("FFmpeg", "Audio extracted: $output")
        }
    }

    fun addBackgroundMusic() { /* إضافة موسيقى */ }
    fun muteOriginalAudio() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("muted")
        executeFFmpeg("-i \"$input\" -an -c:v copy \"$output\"")
    }

    fun boostVolume(percent: Int) {
        val input = currentVideo?.path ?: return
        val factor = percent / 100f
        val output = getOutputPath("boosted_${percent}pct")
        executeFFmpeg("-i \"$input\" -af \"volume=${factor}\" -c:v copy \"$output\"")
    }

    fun smartCompress() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("compressed")
        _uiState.update { it.copy(processingStatus = "ضغط الفيديو...") }
        // CRF 28 = ~70-80% تقليل
        executeFFmpeg("-i \"$input\" -vcodec libx264 -crf 28 -preset medium -acodec aac -b:a 128k \"$output\"")
    }

    fun changeBitrate() { /* حوار تغيير الـ Bitrate */ }
    fun changeResolution() { /* حوار تغيير الدقة */ }

    fun applySlowMotion() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("slow_0.5x")
        executeFFmpeg("-i \"$input\" -vf \"setpts=2.0*PTS\" -af \"atempo=0.5\" \"$output\"")
    }

    fun applyFastMotion() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("fast_2x")
        executeFFmpeg("-i \"$input\" -vf \"setpts=0.5*PTS\" -af \"atempo=2.0\" \"$output\"")
    }

    fun reverseVideo() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("reversed")
        executeFFmpeg("-i \"$input\" -vf reverse -af areverse \"$output\"")
    }

    fun rotateVideo() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("rotated")
        executeFFmpeg("-i \"$input\" -vf \"transpose=1\" -c:a copy \"$output\"")
    }

    fun flipVideo() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("flipped")
        executeFFmpeg("-i \"$input\" -vf hflip -c:a copy \"$output\"")
    }

    fun applyFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        val input = currentVideo?.path ?: return
        val output = getOutputPath("filter_${filter}")
        val ffFilter = when (filter) {
            "أبيض وأسود" -> "colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3"
            "Sepia" -> "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131"
            "تباين عالي" -> "eq=contrast=2"
            "تشبع" -> "hue=s=2"
            "Blur" -> "boxblur=5:1"
            "Vignette" -> "vignette"
            "Pixelate" -> "scale=iw/8:-1,scale=8*iw:-1:flags=neighbor"
            "تمييل" -> "geq=r='p(X,Y)':g='p(X,Y)':b='p(X,Y)'"
            "صورة نيغاتيف" -> "negate"
            "دفء" -> "curves=r='0/0 0.5/0.6 1/1':g='0/0 0.5/0.5 1/1':b='0/0 0.5/0.4 1/0.9'"
            "برودة" -> "curves=b='0/0 0.5/0.6 1/1':r='0/0 0.5/0.4 1/0.9'"
            "HDR وهمي" -> "eq=saturation=1.5:contrast=1.2:brightness=0.05"
            else -> "null"
        }
        executeFFmpeg("-i \"$input\" -vf \"$ffFilter\" -c:a copy \"$output\"")
    }

    fun addText() { /* حوار إضافة نص */ }
    fun addSticker() { /* حوار إضافة ستيكر */ }

    fun exportAsGif() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("animated", "gif")
        executeFFmpeg("-i \"$input\" -vf \"fps=15,scale=480:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse\" \"$output\"")
    }

    fun openMemeMaker() { /* فتح أداة الميم */ }

    fun extractFrames() {
        val input = currentVideo?.path ?: return
        val dir = File(context.getExternalFilesDir(null), "OfflineFlix/Frames/${currentVideo?.name}")
        dir.mkdirs()
        executeFFmpeg("-i \"$input\" -vf fps=1 \"${dir.absolutePath}/frame_%04d.jpg\"")
    }

    fun changeFps() { /* حوار تغيير FPS */ }
    fun changeCodec() { /* حوار تغيير Codec */ }

    fun repairVideo() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("repaired")
        // إعادة ترميز لإصلاح الهيدر التالف
        executeFFmpeg("-i \"$input\" -c copy -fflags +genpts \"$output\"")
    }

    fun cancelProcessing() {
        FFmpegKit.cancel()
        _uiState.update { it.copy(isProcessing = false, processingProgress = 0) }
    }
}
