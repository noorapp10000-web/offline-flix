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
    val outputPath: String = "",

    // ==================== أعلام الحوارات ====================
    /** هل يظهر حوار القص */
    val showTrimDialog: Boolean = false,
    /** هل يظهر حوار الدمج */
    val showMergeDialog: Boolean = false,
    /** هل يظهر حوار الإلحاق */
    val showAppendDialog: Boolean = false,
    /** هل يظهر حوار الموسيقى */
    val showMusicPickerDialog: Boolean = false,
    /** هل يظهر حوار معدل البت */
    val showBitrateDialog: Boolean = false,
    /** هل يظهر حوار الدقة */
    val showResolutionDialog: Boolean = false,
    /** هل يظهر حوار النص */
    val showTextDialog: Boolean = false,
    /** هل يظهر حوار الستيكرز */
    val showStickerDialog: Boolean = false,
    /** هل يظهر حوار مولد الميمز */
    val showMemeMakerDialog: Boolean = false,
    /** هل يظهر حوار معدل الإطارات */
    val showFpsDialog: Boolean = false,
    /** هل يظهر حوار الكودك */
    val showCodecDialog: Boolean = false,

    // ==================== قيم الحوارات ====================
    /** نقطة البداية للقص (ms) */
    val trimStart: Long = 0,
    /** نقطة النهاية للقص (ms) */
    val trimEnd: Long = 0,
    /** معدل البت المختار (kbps) */
    val selectedBitrate: Int = 2000,
    /** الدقة المختارة */
    val selectedResolution: String = "1920x1080",
    /** معدل الإطارات المختار */
    val selectedFps: Int = 30,
    /** الكودك المختار */
    val selectedCodec: String = "libx264",
    /** نص يُضاف للفيديو */
    val overlayText: String = "",
    /** قائمة الفيديوهات للدمج */
    val mergeList: List<String> = emptyList()
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

    /** فتح حوار القص - يُعيّن trimEnd للمدة الكاملة كنقطة بداية */
    fun openTrimmer() {
        val dur = currentVideo?.duration ?: 0L
        _uiState.update {
            it.copy(
                showTrimDialog = true,
                trimStart = 0L,
                trimEnd = if (dur > 0) dur else 60_000L
            )
        }
    }

    /** إغلاق حوار القص */
    fun dismissTrimDialog() = _uiState.update { it.copy(showTrimDialog = false) }

    /** تحديث نقطة البداية */
    fun setTrimStart(ms: Long) = _uiState.update { it.copy(trimStart = ms) }

    /** تحديث نقطة النهاية */
    fun setTrimEnd(ms: Long) = _uiState.update { it.copy(trimEnd = ms) }

    /** تأكيد القص */
    fun confirmTrim() {
        val input = currentVideo?.path ?: return
        val state = _uiState.value
        val startSec = state.trimStart / 1000.0
        val durationSec = (state.trimEnd - state.trimStart) / 1000.0
        val output = getOutputPath("cut_${state.trimStart / 1000}s-${state.trimEnd / 1000}s")
        _uiState.update { it.copy(showTrimDialog = false, processingStatus = "قص الفيديو...") }
        executeFFmpeg("-ss $startSec -i \"$input\" -t $durationSec -c copy \"$output\"")
    }

    fun losslessCut() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("cut")
        // قص بدون إعادة ترميز من الموضع الحالي
        executeFFmpeg("-i \"$input\" -ss 0 -to 60 -c copy \"$output\"")
    }

    /** فتح حوار دمج الفيديوهات */
    fun openMerger() = _uiState.update { it.copy(showMergeDialog = true) }
    fun dismissMergeDialog() = _uiState.update { it.copy(showMergeDialog = false) }

    /** إضافة فيديو للقائمة */
    fun addToMergeList(path: String) = _uiState.update {
        it.copy(mergeList = it.mergeList + path)
    }

    /** تأكيد دمج قائمة الفيديوهات */
    fun confirmMerge() {
        val input = currentVideo?.path ?: return
        val state = _uiState.value
        if (state.mergeList.isEmpty()) return
        val listFile = File(context.cacheDir, "merge_list.txt")
        val allPaths = listOf(input) + state.mergeList
        listFile.writeText(allPaths.joinToString("\n") { "file '${it.replace("'", "\\'")}'" })
        val output = getOutputPath("merged")
        _uiState.update { it.copy(showMergeDialog = false, processingStatus = "دمج الفيديوهات...") }
        executeFFmpeg("-f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy \"$output\"") { success ->
            listFile.delete()
        }
    }

    /** فتح حوار إلحاق فيديو للنهاية */
    fun appendVideo() = _uiState.update { it.copy(showAppendDialog = true) }
    fun dismissAppendDialog() = _uiState.update { it.copy(showAppendDialog = false) }

    /** تأكيد إلحاق فيديو بالنهاية */
    fun confirmAppend(appendPath: String) {
        val input = currentVideo?.path ?: return
        val listFile = File(context.cacheDir, "append_list.txt")
        listFile.writeText("file '${input.replace("'", "\\'")}'\nfile '${appendPath.replace("'", "\\'")}'")
        val output = getOutputPath("appended")
        _uiState.update { it.copy(showAppendDialog = false, processingStatus = "إلحاق الفيديو...") }
        executeFFmpeg("-f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy \"$output\"") { success ->
            listFile.delete()
        }
    }

    fun extractAudio() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("audio", "mp3")
        _uiState.update { it.copy(processingStatus = "استخراج الصوت...") }
        executeFFmpeg("-i \"$input\" -vn -acodec libmp3lame -q:a 0 \"$output\"") { success ->
            if (success) android.util.Log.d("FFmpeg", "Audio extracted: $output")
        }
    }

    /** فتح حوار اختيار موسيقى خلفية */
    fun addBackgroundMusic() = _uiState.update { it.copy(showMusicPickerDialog = true) }
    fun dismissMusicPickerDialog() = _uiState.update { it.copy(showMusicPickerDialog = false) }

    /** دمج موسيقى خلفية مع الفيديو */
    fun confirmAddMusic(musicPath: String, originalVolume: Float = 0.5f, musicVolume: Float = 0.5f) {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("with_music")
        _uiState.update { it.copy(showMusicPickerDialog = false, processingStatus = "دمج الموسيقى...") }
        executeFFmpeg(
            "-i \"$input\" -i \"$musicPath\" " +
            "-filter_complex \"[0:a]volume=$originalVolume[a0];[1:a]volume=$musicVolume[a1];[a0][a1]amix=inputs=2[aout]\" " +
            "-map 0:v -map \"[aout]\" -c:v copy -shortest \"$output\""
        )
    }
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

    /** فتح حوار تغيير معدل البت */
    fun changeBitrate() = _uiState.update { it.copy(showBitrateDialog = true) }
    fun dismissBitrateDialog() = _uiState.update { it.copy(showBitrateDialog = false) }
    fun setSelectedBitrate(kbps: Int) = _uiState.update { it.copy(selectedBitrate = kbps) }
    fun confirmChangeBitrate() {
        val input = currentVideo?.path ?: return
        val state = _uiState.value
        val output = getOutputPath("${state.selectedBitrate}kbps")
        _uiState.update { it.copy(showBitrateDialog = false, processingStatus = "تغيير معدل البت...") }
        executeFFmpeg("-i \"$input\" -b:v ${state.selectedBitrate}k -bufsize ${state.selectedBitrate * 2}k -c:a copy \"$output\"")
    }

    /** فتح حوار تغيير الدقة */
    fun changeResolution() = _uiState.update { it.copy(showResolutionDialog = true) }
    fun dismissResolutionDialog() = _uiState.update { it.copy(showResolutionDialog = false) }
    fun setSelectedResolution(resolution: String) = _uiState.update { it.copy(selectedResolution = resolution) }
    fun confirmChangeResolution() {
        val input = currentVideo?.path ?: return
        val state = _uiState.value
        val (w, h) = state.selectedResolution.split("x").let { Pair(it[0], it[1]) }
        val output = getOutputPath("${w}x${h}")
        _uiState.update { it.copy(showResolutionDialog = false, processingStatus = "تغيير الدقة...") }
        executeFFmpeg("-i \"$input\" -vf scale=$w:$h -c:a copy \"$output\"")
    }

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

    /** فتح حوار إضافة نص متحرك */
    fun addText() = _uiState.update { it.copy(showTextDialog = true) }
    fun dismissTextDialog() = _uiState.update { it.copy(showTextDialog = false) }
    fun setOverlayText(text: String) = _uiState.update { it.copy(overlayText = text) }
    fun confirmAddText(text: String, fontColor: String = "white", fontSize: Int = 36, x: Int = 10, y: Int = 10) {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("text")
        val safeText = text.replace(":", "\\:").replace("'", "\\'")
        _uiState.update { it.copy(showTextDialog = false, processingStatus = "إضافة النص...") }
        executeFFmpeg(
            "-i \"$input\" -vf \"drawtext=text='$safeText':fontcolor=$fontColor:fontsize=$fontSize:x=$x:y=$y:box=1:boxcolor=black@0.5:boxborderw=5\" -c:a copy \"$output\""
        )
    }

    /** فتح حوار إضافة ستيكر/صورة فوق الفيديو */
    fun addSticker() = _uiState.update { it.copy(showStickerDialog = true) }
    fun dismissStickerDialog() = _uiState.update { it.copy(showStickerDialog = false) }
    fun confirmAddSticker(stickerPath: String, x: Int = 10, y: Int = 10, scale: Float = 0.2f) {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("sticker")
        _uiState.update { it.copy(showStickerDialog = false, processingStatus = "إضافة الستيكر...") }
        executeFFmpeg(
            "-i \"$input\" -i \"$stickerPath\" " +
            "-filter_complex \"[1:v]scale=iw*$scale:-1[sticker];[0:v][sticker]overlay=$x:$y\" " +
            "-c:a copy \"$output\""
        )
    }

    fun exportAsGif() {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("animated", "gif")
        executeFFmpeg("-i \"$input\" -vf \"fps=15,scale=480:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse\" \"$output\"")
    }

    /** فتح أداة صانع الميمز */
    fun openMemeMaker() = _uiState.update { it.copy(showMemeMakerDialog = true) }
    fun dismissMemeMakerDialog() = _uiState.update { it.copy(showMemeMakerDialog = false) }

    /** إنشاء ميم من الصورة الحالية مع نص */
    fun createMeme(captureAtMs: Long, topText: String, bottomText: String) {
        val input = currentVideo?.path ?: return
        val output = getOutputPath("meme_${captureAtMs / 1000}s", "jpg")
        val startSec = captureAtMs / 1000.0
        val safeTop = topText.replace(":", "\\:").replace("'", "\\'")
        val safeBot = bottomText.replace(":", "\\:").replace("'", "\\'")
        _uiState.update { it.copy(showMemeMakerDialog = false, processingStatus = "إنشاء الميم...") }
        executeFFmpeg(
            "-ss $startSec -i \"$input\" -vframes 1 " +
            "-vf \"drawtext=text='$safeTop':fontcolor=white:fontsize=48:x=(w-text_w)/2:y=20:borderw=3:bordercolor=black," +
            "drawtext=text='$safeBot':fontcolor=white:fontsize=48:x=(w-text_w)/2:y=h-th-20:borderw=3:bordercolor=black\" " +
            "\"$output\""
        )
    }

    fun extractFrames() {
        val input = currentVideo?.path ?: return
        val dir = File(context.getExternalFilesDir(null), "OfflineFlix/Frames/${currentVideo?.name}")
        dir.mkdirs()
        executeFFmpeg("-i \"$input\" -vf fps=1 \"${dir.absolutePath}/frame_%04d.jpg\"")
    }

    /** فتح حوار تغيير معدل الإطارات */
    fun changeFps() = _uiState.update { it.copy(showFpsDialog = true) }
    fun dismissFpsDialog() = _uiState.update { it.copy(showFpsDialog = false) }
    fun setSelectedFps(fps: Int) = _uiState.update { it.copy(selectedFps = fps) }
    fun confirmChangeFps() {
        val input = currentVideo?.path ?: return
        val state = _uiState.value
        val output = getOutputPath("${state.selectedFps}fps")
        _uiState.update { it.copy(showFpsDialog = false, processingStatus = "تغيير معدل الإطارات...") }
        executeFFmpeg("-i \"$input\" -vf fps=${state.selectedFps} -c:a copy \"$output\"")
    }

    /** فتح حوار تغيير الكودك */
    fun changeCodec() = _uiState.update { it.copy(showCodecDialog = true) }
    fun dismissCodecDialog() = _uiState.update { it.copy(showCodecDialog = false) }
    fun setSelectedCodec(codec: String) = _uiState.update { it.copy(selectedCodec = codec) }
    fun confirmChangeCodec() {
        val input = currentVideo?.path ?: return
        val state = _uiState.value
        val ext = when (state.selectedCodec) {
            "libvpx-vp9" -> "webm"
            "libaom-av1" -> "mkv"
            else -> "mp4"
        }
        val output = getOutputPath(state.selectedCodec, ext)
        _uiState.update { it.copy(showCodecDialog = false, processingStatus = "تغيير الكودك...") }
        executeFFmpeg("-i \"$input\" -vcodec ${state.selectedCodec} -crf 23 -c:a aac \"$output\"")
    }

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
