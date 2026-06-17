package com.offlineflix.player.ui.screens.converter

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Statistics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ConverterUiState(
    val inputFile: String = "",
    val inputFileName: String = "",
    val detectedType: String = "",
    val detectedFormat: String = "",
    val outputFormat: String = "",
    val selectedCodec: String = "",
    val availableCodecs: List<String> = emptyList(),
    val bitrateIndex: Int = 5,
    val selectedBitrate: String = "2Mbps",
    val selectedResolution: String = "نفس الأصلي",
    val isVideoFormat: Boolean = true,
    val estimatedSize: Long = 0,
    val isConverting: Boolean = false,
    val progress: Int = 0,
    val conversionSpeed: String = "",
    val remainingTime: String = "",
    val isComplete: Boolean = false,
    val outputPath: String = "",
    val error: String = ""
)

val BITRATES = listOf("128Kbps", "256Kbps", "512Kbps", "1Mbps", "2Mbps", "4Mbps", "6Mbps", "8Mbps", "12Mbps", "16Mbps", "24Mbps")

/**
 * ViewModel محول الصيغ الشامل
 */
@HiltViewModel
class FormatConverterViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    private var inputUri: Uri? = null
    private var inputDuration: Long = 0

    fun setInputFile(uri: Uri) {
        inputUri = uri
        val fileName = getFileName(uri)
        val ext = fileName.substringAfterLast(".").lowercase()
        val type = getFileType(ext)
        val codecs = getAvailableCodecs(ext)

        _uiState.update {
            it.copy(
                inputFile = uri.toString(),
                inputFileName = fileName,
                detectedType = type,
                detectedFormat = ext.uppercase(),
                availableCodecs = codecs,
                selectedCodec = codecs.firstOrNull() ?: "",
                isComplete = false
            )
        }
    }

    fun setOutputFormat(format: String) {
        val isVideo = isVideoFormat(format)
        val codecs = getCodecsForFormat(format)
        _uiState.update {
            it.copy(
                outputFormat = format,
                isVideoFormat = isVideo,
                availableCodecs = codecs,
                selectedCodec = codecs.firstOrNull() ?: "",
                isComplete = false
            )
        }
        estimateOutputSize()
    }

    fun setCodec(codec: String) = _uiState.update { it.copy(selectedCodec = codec) }
    fun setBitrateIndex(index: Int) {
        _uiState.update {
            it.copy(bitrateIndex = index, selectedBitrate = BITRATES.getOrElse(index) { "2Mbps" })
        }
        estimateOutputSize()
    }
    fun setResolution(res: String) = _uiState.update { it.copy(selectedResolution = res) }

    private fun estimateOutputSize() {
        val state = _uiState.value
        if (inputDuration > 0 && state.selectedBitrate.isNotEmpty()) {
            val bitrateKbps = parseBitrate(state.selectedBitrate)
            val estimatedBytes = (bitrateKbps * 1000L / 8L) * (inputDuration / 1000)
            _uiState.update { it.copy(estimatedSize = estimatedBytes) }
        }
    }

    private fun parseBitrate(bitrate: String): Int {
        return when {
            bitrate.contains("Mbps") -> bitrate.replace("Mbps", "").toIntOrNull()?.times(1000) ?: 2000
            bitrate.contains("Kbps") -> bitrate.replace("Kbps", "").toIntOrNull() ?: 2000
            else -> 2000
        }
    }

    fun startConversion() {
        val state = _uiState.value
        if (state.inputFile.isEmpty() || state.outputFormat.isEmpty()) return

        val inputPath = getRealPath(inputUri!!) ?: return
        val outputDir = File(context.getExternalFilesDir(null), "OfflineFlix/Converted")
        outputDir.mkdirs()

        val outputName = "${state.inputFileName.substringBeforeLast(".")}_converted.${state.outputFormat.lowercase()}"
        val outputPath = "${outputDir.absolutePath}/$outputName"

        val command = buildFFmpegCommand(inputPath, outputPath, state)
        _uiState.update { it.copy(isConverting = true, progress = 0, isComplete = false) }

        val startTime = System.currentTimeMillis()

        FFmpegKit.executeAsync(command, { session ->
            val success = ReturnCode.isSuccess(session.returnCode)
            _uiState.update {
                it.copy(
                    isConverting = false,
                    progress = if (success) 100 else it.progress,
                    isComplete = success,
                    outputPath = if (success) outputPath else "",
                    error = if (!success) "فشل التحويل" else ""
                )
            }
        }, null) { stats: Statistics ->
            if (inputDuration > 0 && stats.time > 0) {
                val progress = ((stats.time.toFloat() / inputDuration) * 100).toInt().coerceIn(0, 99)
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = if (progress > 0) ((elapsed * (100 - progress)) / progress / 1000) else 0
                _uiState.update {
                    it.copy(
                        progress = progress,
                        conversionSpeed = "${String.format("%.1f", stats.speed)}x",
                        remainingTime = "${remaining}s"
                    )
                }
            }
        }
    }

    private fun buildFFmpegCommand(input: String, output: String, state: ConverterUiState): String {
        val sb = StringBuilder("-i \"$input\"")

        // تطبيق الكودك
        if (state.selectedCodec.isNotEmpty()) {
            when {
                state.isVideoFormat -> sb.append(" -vcodec ${state.selectedCodec}")
                else -> sb.append(" -acodec ${state.selectedCodec}")
            }
        }

        // تطبيق الدقة
        val resolution = when (state.selectedResolution) {
            "480p" -> " -vf scale=-2:480"
            "720p" -> " -vf scale=-2:720"
            "1080p" -> " -vf scale=-2:1080"
            "4K" -> " -vf scale=-2:2160"
            else -> ""
        }
        sb.append(resolution)

        // تطبيق Bitrate
        val bitrate = parseBitrate(state.selectedBitrate)
        if (state.isVideoFormat) {
            sb.append(" -b:v ${bitrate}k")
        } else {
            sb.append(" -b:a ${bitrate}k")
        }

        sb.append(" -y \"$output\"")
        return sb.toString()
    }

    fun cancelConversion() {
        FFmpegKit.cancel()
        _uiState.update { it.copy(isConverting = false, progress = 0) }
    }

    private fun getFileName(uri: Uri): String {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            cursor.getString(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
        } ?: uri.lastPathSegment ?: "file"
    }

    private fun getRealPath(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("ffmpeg_input", ".tmp", context.cacheDir)
            tempFile.outputStream().use { out -> inputStream.copyTo(out) }
            tempFile.absolutePath
        } catch (e: Exception) { null }
    }

    private fun getFileType(ext: String): String = when (ext) {
        in listOf("mp4", "mkv", "avi", "mov", "flv", "wmv", "ts", "webm", "3gp", "m4v", "vob", "rmvb") -> "فيديو"
        in listOf("mp3", "flac", "wav", "aac", "m4a", "ogg", "wma", "opus") -> "صوت"
        in listOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "tiff") -> "صورة"
        in listOf("pdf") -> "PDF"
        else -> "ملف"
    }

    private fun getAvailableCodecs(ext: String): List<String> = when (ext) {
        in listOf("mp4", "m4v") -> listOf("libx264", "libx265", "libvpx-vp9", "mpeg4")
        "mkv" -> listOf("libx264", "libx265", "libvpx-vp9", "av1")
        "webm" -> listOf("libvpx-vp9", "libvpx")
        "mp3" -> listOf("libmp3lame")
        "aac" -> listOf("aac", "libfdk_aac")
        "flac" -> listOf("flac")
        "wav" -> listOf("pcm_s16le", "pcm_s24le")
        else -> listOf("copy", "libx264", "aac")
    }

    private fun getCodecsForFormat(format: String): List<String> = when (format.uppercase()) {
        "MP4" -> listOf("libx264", "libx265", "libvpx-vp9", "mpeg4")
        "MKV" -> listOf("libx264", "libx265", "libvpx-vp9", "copy")
        "WEBM" -> listOf("libvpx-vp9", "libvpx")
        "AVI" -> listOf("mpeg4", "libxvid", "libx264")
        "MP3" -> listOf("libmp3lame")
        "AAC" -> listOf("aac")
        "FLAC" -> listOf("flac")
        "WAV" -> listOf("pcm_s16le")
        "OGG" -> listOf("libvorbis")
        "OPUS" -> listOf("libopus")
        else -> listOf("libx264", "aac", "copy")
    }

    private fun isVideoFormat(format: String): Boolean {
        return format.uppercase() in listOf("MP4", "MKV", "AVI", "MOV", "WMV", "FLV", "WEBM", "TS", "3GP", "VOB", "M4V", "OGV", "MPEG")
    }
}
