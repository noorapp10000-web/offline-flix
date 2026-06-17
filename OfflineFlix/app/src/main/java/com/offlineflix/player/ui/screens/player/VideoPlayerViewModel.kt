package com.offlineflix.player.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.offlineflix.player.data.models.VideoEntity
import com.offlineflix.player.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/** أوضاع الزوم */
enum class ZoomMode(val label: String) {
    FIT("ملاءمة الشاشة"),
    FILL("ملء الشاشة"),
    ZOOM("تكبير"),
    FIXED_WIDTH("عرض ثابت")
}

/** نمط الترجمة */
data class SubtitleStyle(
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    val fontSize: Int = 18,
    val backgroundAlpha: Float = 0.6f,
    val position: Float = 0.9f,
    val delayMs: Long = 0L
)

/** حالة واجهة المشغل */
data class PlayerUiState(
    val video: VideoEntity? = null,
    val title: String = "",
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val buffered: Long = 0,
    val speed: Float = 1f,
    val volume: Float = 1f,
    val brightness: Float = 0.5f,
    val zoomMode: ZoomMode = ZoomMode.FIT,
    val isLocked: Boolean = false,
    val audioOnlyMode: Boolean = false,
    val showVolumeIndicator: Boolean = false,
    val showBrightnessIndicator: Boolean = false,
    val seekIndicator: Long = 0L,

    // ==================== الترجمة ====================
    val subtitlesEnabled: Boolean = false,
    val subtitleText: String = "",
    val secondarySubtitleText: String = "",
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val availableSubtitleFiles: List<SubtitleFile> = emptyList(),
    val selectedSubtitleIndex: Int = -1,
    val secondarySubtitleIndex: Int = -1,
    val showSubtitleSettings: Boolean = false,

    // ==================== AB Repeat ====================
    val abRepeatA: Long = 0,
    val abRepeatB: Long = 0,

    // ==================== مؤقت النوم ====================
    val sleepTimerRemaining: Long = 0,
    val sleepTimerMode: SleepTimerMode = SleepTimerMode.OFF,

    // ==================== حالة البناء ====================
    val isBuffering: Boolean = false,
    val screenshotPath: String = "",
    val showScreenshotToast: Boolean = false,
    val rotationDegree: Int = 0
)

/** وضع مؤقت النوم */
enum class SleepTimerMode(val label: String, val minutes: Long) {
    OFF("إيقاف", 0),
    MIN_15("15 دقيقة", 15),
    MIN_30("30 دقيقة", 30),
    MIN_60("ساعة", 60),
    END_OF_EPISODE("نهاية الحلقة", -1)
}

/** ملف ترجمة */
data class SubtitleFile(
    val path: String,
    val name: String,
    val language: String = "unknown"
)

/**
 * ViewModel مشغل الفيديو مع كل المنطق المتقدم
 * يدعم: إيماءات، ترجمة SRT/ASS، لقطة شاشة، تدوير، AB Repeat، Sleep Timer
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRepository: VideoRepository
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var subtitleJob: Job? = null
    private var currentVideoId: Long = 0

    /** قائمة أسطر الترجمة الأولى: وقت → نص */
    private var primarySrtLines: List<SrtEntry> = emptyList()
    /** قائمة أسطر الترجمة الثانية */
    private var secondarySrtLines: List<SrtEntry> = emptyList()

    init {
        setupPlayerListener()
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _uiState.update { it.copy(isBuffering = state == Player.STATE_BUFFERING) }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressTracking() else progressJob?.cancel()
            }
        })
    }

    /** تحميل الفيديو وابحث تلقائياً عن ملفات الترجمة المجاورة */
    fun loadVideo(videoId: Long) {
        viewModelScope.launch {
            val video = videoRepository.getById(videoId) ?: return@launch
            currentVideoId = videoId

            val mediaItem = MediaItem.fromUri(android.net.Uri.parse(video.path))
            player.setMediaItem(mediaItem)

            if (video.lastPosition > 0) player.seekTo(video.lastPosition)
            player.prepare()
            player.play()

            _uiState.update {
                it.copy(
                    video = video,
                    title = video.displayName,
                    duration = video.duration,
                    currentPosition = video.lastPosition
                )
            }

            // مسح ملفات الترجمة المجاورة تلقائياً
            scanSubtitleFiles(video.path)
        }
    }

    // ==================== الترجمة SRT/ASS/SSA ====================

    /** مسح ملفات الترجمة بجوار الفيديو */
    private fun scanSubtitleFiles(videoPath: String) {
        val videoFile = File(videoPath)
        val videoNameNoExt = videoFile.nameWithoutExtension
        val dir = videoFile.parentFile ?: return
        val subtitleExts = setOf("srt", "ass", "ssa", "sub", "vtt")

        val files = dir.listFiles { file ->
            file.extension.lowercase() in subtitleExts &&
            file.nameWithoutExtension.startsWith(videoNameNoExt)
        }?.map { file ->
            val lang = detectLanguage(file.name)
            SubtitleFile(path = file.absolutePath, name = file.name, language = lang)
        } ?: emptyList()

        _uiState.update { it.copy(availableSubtitleFiles = files) }

        // تحميل تلقائي للترجمة الأولى لو موجودة
        if (files.isNotEmpty()) {
            selectSubtitle(0)
        }
    }

    /** تخمين اللغة من اسم الملف */
    private fun detectLanguage(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("arabic") || lower.contains("ar") || lower.contains("عربي") -> "عربي"
            lower.contains("english") || lower.contains("en") -> "إنجليزي"
            lower.contains("french") || lower.contains("fr") -> "فرنسي"
            lower.contains("spanish") || lower.contains("es") -> "إسباني"
            else -> "غير محدد"
        }
    }

    /** تحديد ترجمة رئيسية */
    fun selectSubtitle(index: Int) {
        val files = _uiState.value.availableSubtitleFiles
        if (index < 0 || index >= files.size) {
            primarySrtLines = emptyList()
            _uiState.update { it.copy(selectedSubtitleIndex = -1, subtitlesEnabled = false) }
            return
        }
        primarySrtLines = parseSrtFile(files[index].path)
        _uiState.update { it.copy(selectedSubtitleIndex = index, subtitlesEnabled = true) }
        startSubtitleTracking()
    }

    /** تحديد ترجمة ثانوية (مزدوجة) */
    fun selectSecondarySubtitle(index: Int) {
        val files = _uiState.value.availableSubtitleFiles
        if (index < 0 || index >= files.size) {
            secondarySrtLines = emptyList()
            _uiState.update { it.copy(secondarySubtitleIndex = -1) }
            return
        }
        secondarySrtLines = parseSrtFile(files[index].path)
        _uiState.update { it.copy(secondarySubtitleIndex = index) }
    }

    /** تحميل ملف SRT خارجي من مسار مخصص */
    fun loadSubtitleFromPath(path: String, isPrimary: Boolean = true) {
        val entries = parseSrtFile(path)
        val file = SubtitleFile(path = path, name = File(path).name, language = detectLanguage(path))
        val current = _uiState.value.availableSubtitleFiles.toMutableList()
        if (!current.any { it.path == path }) current.add(file)

        if (isPrimary) {
            primarySrtLines = entries
            _uiState.update { it.copy(availableSubtitleFiles = current, subtitlesEnabled = true) }
            startSubtitleTracking()
        } else {
            secondarySrtLines = entries
            _uiState.update { it.copy(availableSubtitleFiles = current) }
        }
    }

    /** تفعيل/إيقاف الترجمة */
    fun toggleSubtitles() {
        val enabled = !_uiState.value.subtitlesEnabled
        _uiState.update { it.copy(subtitlesEnabled = enabled) }
        if (enabled && primarySrtLines.isNotEmpty()) startSubtitleTracking()
        else subtitleJob?.cancel()
    }

    fun toggleSubtitleSettings() = _uiState.update { it.copy(showSubtitleSettings = !it.showSubtitleSettings) }

    /** ضبط حجم الترجمة */
    fun setSubtitleFontSize(size: Int) = _uiState.update {
        it.copy(subtitleStyle = it.subtitleStyle.copy(fontSize = size.coerceIn(10, 36)))
    }

    /** ضبط لون الترجمة */
    fun setSubtitleColor(color: androidx.compose.ui.graphics.Color) = _uiState.update {
        it.copy(subtitleStyle = it.subtitleStyle.copy(color = color))
    }

    /** ضبط تأخير الترجمة */
    fun setSubtitleDelay(delayMs: Long) = _uiState.update {
        it.copy(subtitleStyle = it.subtitleStyle.copy(delayMs = delayMs))
    }

    /** تتبع الترجمة لحظياً */
    private fun startSubtitleTracking() {
        subtitleJob?.cancel()
        subtitleJob = viewModelScope.launch {
            while (true) {
                if (_uiState.value.subtitlesEnabled) {
                    val pos = player.currentPosition + _uiState.value.subtitleStyle.delayMs
                    val primary = primarySrtLines.lastOrNull { it.startMs <= pos && pos <= it.endMs }?.text ?: ""
                    val secondary = secondarySrtLines.lastOrNull { it.startMs <= pos && pos <= it.endMs }?.text ?: ""
                    _uiState.update { it.copy(subtitleText = primary, secondarySubtitleText = secondary) }
                }
                delay(100)
            }
        }
    }

    /** تحليل ملف SRT */
    private fun parseSrtFile(path: String): List<SrtEntry> {
        return try {
            val text = File(path).readText(Charsets.UTF_8)
            val ext = File(path).extension.lowercase()
            when (ext) {
                "srt" -> parseSrt(text)
                "ass", "ssa" -> parseAss(text)
                "vtt" -> parseVtt(text)
                else -> parseSrt(text)
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun parseSrt(text: String): List<SrtEntry> {
        val entries = mutableListOf<SrtEntry>()
        val blocks = text.trim().split(Regex("\n\n+"))
        for (block in blocks) {
            val lines = block.trim().lines()
            if (lines.size < 3) continue
            val timeLine = lines.getOrNull(1) ?: continue
            val timeRegex = Regex("""(\d+):(\d+):(\d+)[,.](\d+)\s+-->\s+(\d+):(\d+):(\d+)[,.](\d+)""")
            val match = timeRegex.find(timeLine) ?: continue
            val g = match.groupValues
            val startMs = g[1].toLong()*3600000 + g[2].toLong()*60000 + g[3].toLong()*1000 + g[4].toLong()
            val endMs   = g[5].toLong()*3600000 + g[6].toLong()*60000 + g[7].toLong()*1000 + g[8].toLong()
            val subText = lines.drop(2).joinToString("\n").trim()
            entries.add(SrtEntry(startMs, endMs, subText))
        }
        return entries
    }

    private fun parseAss(text: String): List<SrtEntry> {
        val entries = mutableListOf<SrtEntry>()
        val dialogueRegex = Regex("""Dialogue:\s*\d+,(\d+:\d+:\d+\.\d+),(\d+:\d+:\d+\.\d+),[^,]*,[^,]*,\d+,\d+,\d+,[^,]*,(.*)""")
        for (match in dialogueRegex.findAll(text)) {
            val start = parseAssTime(match.groupValues[1])
            val end = parseAssTime(match.groupValues[2])
            val rawText = match.groupValues[3]
                .replace(Regex("\\{[^}]*}"), "")
                .replace("\\N", "\n").trim()
            entries.add(SrtEntry(start, end, rawText))
        }
        return entries.sortedBy { it.startMs }
    }

    private fun parseAssTime(t: String): Long {
        val parts = t.split(":", ".")
        if (parts.size < 4) return 0
        return parts[0].toLong()*3600000 + parts[1].toLong()*60000 + parts[2].toLong()*1000 + (parts[3].toLong()*10)
    }

    private fun parseVtt(text: String): List<SrtEntry> {
        val entries = mutableListOf<SrtEntry>()
        val blocks = text.removePrefix("WEBVTT").trim().split(Regex("\n\n+"))
        val timeRegex = Regex("""(\d+:\d+:\d+\.\d+|\d+:\d+\.\d+)\s+-->\s+(\d+:\d+:\d+\.\d+|\d+:\d+\.\d+)""")
        for (block in blocks) {
            val lines = block.trim().lines()
            val timeLine = lines.firstOrNull { timeRegex.containsMatchIn(it) } ?: continue
            val match = timeRegex.find(timeLine) ?: continue
            val startMs = parseVttTime(match.groupValues[1])
            val endMs = parseVttTime(match.groupValues[2])
            val subText = lines.dropWhile { !timeRegex.containsMatchIn(it) }.drop(1).joinToString("\n").trim()
            if (subText.isNotEmpty()) entries.add(SrtEntry(startMs, endMs, subText))
        }
        return entries
    }

    private fun parseVttTime(t: String): Long {
        val parts = t.split(":")
        return when (parts.size) {
            3 -> parts[0].toLong()*3600000 + parts[1].toLong()*60000 + (parts[2].replace(".", "").take(3).toLong())
            2 -> parts[0].toLong()*60000 + (parts[1].replace(".", "").take(3+3).let {
                val sec = it.take(2).toLong(); val ms = it.drop(3).take(3).padEnd(3,'0').toLong()
                sec*1000+ms
            })
            else -> 0
        }
    }

    // ==================== تتبع التقدم ====================

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val pos = player.currentPosition
                val dur = player.duration.coerceAtLeast(1)
                _uiState.update {
                    it.copy(
                        currentPosition = pos,
                        duration = dur,
                        buffered = player.bufferedPosition
                    )
                }
                // AB Repeat
                val abB = _uiState.value.abRepeatB
                val abA = _uiState.value.abRepeatA
                if (abB > 0 && pos >= abB) player.seekTo(abA)
                delay(500)
            }
        }
    }

    // ==================== التحكم الأساسي ====================

    fun togglePlayPause() { if (player.isPlaying) player.pause() else player.play() }

    fun seekTo(position: Long) {
        player.seekTo(position.coerceIn(0, player.duration))
        _uiState.update { it.copy(currentPosition = position) }
    }

    fun seekForward(ms: Long) {
        seekTo(player.currentPosition + ms)
        _uiState.update { it.copy(seekIndicator = ms) }
        viewModelScope.launch { delay(1000); _uiState.update { it.copy(seekIndicator = 0) } }
    }

    fun seekBackward(ms: Long) {
        seekTo(player.currentPosition - ms)
        _uiState.update { it.copy(seekIndicator = -ms) }
        viewModelScope.launch { delay(1000); _uiState.update { it.copy(seekIndicator = 0) } }
    }

    fun seekBySwipe(deltaX: Float) = seekTo(player.currentPosition + (deltaX * 0.5f).toLong() * 1000)

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _uiState.update { it.copy(speed = speed) }
    }

    fun resetSpeed() = setSpeed(1f)

    /** ضبط الصوت (سحب يمين) */
    fun adjustVolume(delta: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val newVol = (cur + delta * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        _uiState.update { it.copy(volume = newVol.toFloat() / max, showVolumeIndicator = true) }
        viewModelScope.launch { delay(1500); _uiState.update { it.copy(showVolumeIndicator = false) } }
    }

    /** ضبط السطوع (سحب يسار) */
    fun adjustBrightness(delta: Float, activity: Activity?) {
        val cur = _uiState.value.brightness
        val newBrightness = (cur + delta).coerceIn(0.01f, 1f)
        activity?.window?.attributes = activity?.window?.attributes?.apply {
            screenBrightness = newBrightness
        }
        _uiState.update { it.copy(brightness = newBrightness, showBrightnessIndicator = true) }
        viewModelScope.launch { delay(1500); _uiState.update { it.copy(showBrightnessIndicator = false) } }
    }

    fun setZoomMode(mode: ZoomMode) = _uiState.update { it.copy(zoomMode = mode) }
    fun toggleLock() = _uiState.update { it.copy(isLocked = !it.isLocked) }
    fun toggleAudioOnly() = _uiState.update { it.copy(audioOnlyMode = !it.audioOnlyMode) }

    /** تدوير الشاشة */
    fun rotateScreen(activity: Activity?) {
        val current = _uiState.value.rotationDegree
        val next = (current + 90) % 360
        _uiState.update { it.copy(rotationDegree = next) }
        activity?.requestedOrientation = when (next) {
            0   -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            90  -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // ==================== AB Repeat ====================

    fun toggleABRepeat() {
        val s = _uiState.value
        when {
            s.abRepeatA == 0L -> _uiState.update { it.copy(abRepeatA = player.currentPosition) }
            s.abRepeatB == 0L -> _uiState.update { it.copy(abRepeatB = player.currentPosition) }
            else -> _uiState.update { it.copy(abRepeatA = 0, abRepeatB = 0) }
        }
    }

    // ==================== مؤقت النوم ====================

    fun setSleepTimer(mode: SleepTimerMode) {
        sleepTimerJob?.cancel()
        if (mode == SleepTimerMode.OFF) {
            _uiState.update { it.copy(sleepTimerRemaining = 0, sleepTimerMode = SleepTimerMode.OFF) }
            return
        }
        if (mode == SleepTimerMode.END_OF_EPISODE) {
            _uiState.update { it.copy(sleepTimerMode = SleepTimerMode.END_OF_EPISODE) }
            sleepTimerJob = viewModelScope.launch {
                while (true) {
                    val remaining = (player.duration - player.currentPosition).coerceAtLeast(0)
                    _uiState.update { it.copy(sleepTimerRemaining = remaining) }
                    if (remaining <= 0) { player.pause(); break }
                    delay(1000)
                }
            }
            return
        }
        val timerMs = mode.minutes * 60 * 1000
        _uiState.update { it.copy(sleepTimerRemaining = timerMs, sleepTimerMode = mode) }
        sleepTimerJob = viewModelScope.launch {
            var remaining = timerMs
            while (remaining > 0) {
                delay(1000); remaining -= 1000
                _uiState.update { it.copy(sleepTimerRemaining = remaining) }
            }
            player.pause()
            _uiState.update { it.copy(sleepTimerMode = SleepTimerMode.OFF) }
        }
    }

    // ==================== لقطة الشاشة الحقيقية ====================

    /**
     * التقاط لقطة شاشة حقيقية من ExoPlayer عبر PixelCopy أو Bitmap extraction
     */
    fun takeScreenshot(playerViewRef: android.view.View?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "OfflineFlix"
                )
                dir.mkdirs()
                val filename = "OfflineFlix_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
                val file = File(dir, filename)

                if (playerViewRef != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // PixelCopy على Android 8+
                    val bitmap = Bitmap.createBitmap(
                        playerViewRef.width.coerceAtLeast(1),
                        playerViewRef.height.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    val latch = java.util.concurrent.CountDownLatch(1)
                    var success = false
                    android.view.PixelCopy.request(
                        (playerViewRef.context as? Activity)?.window ?: return@launch,
                        android.graphics.Rect(
                            playerViewRef.left, playerViewRef.top,
                            playerViewRef.right, playerViewRef.bottom
                        ),
                        bitmap,
                        { result ->
                            success = (result == android.view.PixelCopy.SUCCESS)
                            latch.countDown()
                        },
                        Handler(Looper.getMainLooper())
                    )
                    latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
                    if (success) {
                        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                        bitmap.recycle()
                        _uiState.update { it.copy(screenshotPath = file.absolutePath, showScreenshotToast = true) }
                        delay(2000)
                        _uiState.update { it.copy(showScreenshotToast = false) }
                    }
                } else {
                    // Fallback: FFmpeg استخراج Frame من الموضع الحالي
                    val input = _uiState.value.video?.path ?: return@launch
                    val posSec = player.currentPosition / 1000.0
                    com.arthenica.ffmpegkit.FFmpegKit.execute(
                        "-ss $posSec -i \"$input\" -vframes 1 -q:v 2 \"${file.absolutePath}\""
                    )
                    if (file.exists() && file.length() > 0) {
                        _uiState.update { it.copy(screenshotPath = file.absolutePath, showScreenshotToast = true) }
                        delay(2000)
                        _uiState.update { it.copy(showScreenshotToast = false) }
                    }
                }

                // تسجيل الصورة في المعرض
                try {
                    android.media.MediaScannerConnection.scanFile(
                        context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null
                    )
                } catch (_: Exception) {}

            } catch (e: Exception) { android.util.Log.e("Screenshot", "فشل: ${e.message}") }
        }
    }

    fun playNext() = player.seekToNextMediaItem()
    fun playPrevious() = player.seekToPreviousMediaItem()

    fun saveProgress() {
        viewModelScope.launch {
            val pos = player.currentPosition
            val dur = player.duration.coerceAtLeast(1)
            val progress = ((pos.toFloat() / dur) * 100).toInt()
            videoRepository.updatePlaybackProgress(currentVideoId, pos, progress)
            if (progress >= 95) videoRepository.markAsWatched(currentVideoId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveProgress()
        player.release()
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        subtitleJob?.cancel()
    }
}

/** سطر ترجمة واحد */
data class SrtEntry(val startMs: Long, val endMs: Long, val text: String)
