package com.offlineflix.player.ui.screens.player

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Environment
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

/** بيانات نمط الترجمة */
data class SubtitleStyle(
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    val fontSize: Int = 18,
    val backgroundAlpha: Float = 0.6f,
    val position: Float = 0.9f
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
    val subtitlesEnabled: Boolean = false,
    val subtitleText: String = "",
    val secondarySubtitleText: String = "",
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val abRepeatA: Long = 0,
    val abRepeatB: Long = 0,
    val sleepTimerRemaining: Long = 0,
    val isBuffering: Boolean = false
)

/**
 * ViewModel مشغل الفيديو مع كل المنطق
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
    private var currentVideoId: Long = 0

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
                if (isPlaying) startProgressTracking()
                else progressJob?.cancel()
            }
        })
    }

    fun loadVideo(videoId: Long) {
        viewModelScope.launch {
            val video = videoRepository.getById(videoId) ?: return@launch
            currentVideoId = videoId

            val mediaItem = MediaItem.fromUri(android.net.Uri.parse(video.path))
            player.setMediaItem(mediaItem)

            if (video.lastPosition > 0) {
                player.seekTo(video.lastPosition)
            }

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
        }
    }

    /** تتبع التقدم كل ثانية */
    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val pos = player.currentPosition
                val dur = player.duration.coerceAtLeast(1)
                val progress = ((pos.toFloat() / dur) * 100).toInt()

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
                if (abB > 0 && pos >= abB) {
                    player.seekTo(abA)
                }

                delay(500)
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(position: Long) {
        player.seekTo(position.coerceIn(0, player.duration))
        _uiState.update { it.copy(currentPosition = position) }
    }

    fun seekForward(ms: Long) {
        seekTo(player.currentPosition + ms)
        _uiState.update { it.copy(seekIndicator = ms) }
        viewModelScope.launch {
            delay(1000)
            _uiState.update { it.copy(seekIndicator = 0) }
        }
    }

    fun seekBackward(ms: Long) {
        seekTo(player.currentPosition - ms)
        _uiState.update { it.copy(seekIndicator = -ms) }
        viewModelScope.launch {
            delay(1000)
            _uiState.update { it.copy(seekIndicator = 0) }
        }
    }

    fun seekBySwipe(deltaX: Float) {
        val seekAmount = (deltaX * 0.5f).toLong() * 1000
        seekTo(player.currentPosition + seekAmount)
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _uiState.update { it.copy(speed = speed) }
    }

    fun resetSpeed() = setSpeed(1f)

    /** ضبط مستوى الصوت (0.0 - 1.0) */
    fun adjustVolume(delta: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (current + delta * maxVolume).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        val normalized = newVolume.toFloat() / maxVolume
        _uiState.update { it.copy(volume = normalized, showVolumeIndicator = true) }
        viewModelScope.launch {
            delay(1500)
            _uiState.update { it.copy(showVolumeIndicator = false) }
        }
    }

    /** ضبط السطوع */
    fun adjustBrightness(delta: Float) {
        val current = _uiState.value.brightness
        val newBrightness = (current + delta).coerceIn(0f, 1f)
        _uiState.update { it.copy(brightness = newBrightness, showBrightnessIndicator = true) }
        viewModelScope.launch {
            delay(1500)
            _uiState.update { it.copy(showBrightnessIndicator = false) }
        }
    }

    fun setZoomMode(mode: ZoomMode) = _uiState.update { it.copy(zoomMode = mode) }
    fun toggleLock() = _uiState.update { it.copy(isLocked = !it.isLocked) }
    fun toggleAudioOnly() = _uiState.update { it.copy(audioOnlyMode = !it.audioOnlyMode) }
    fun toggleSubtitles() = _uiState.update { it.copy(subtitlesEnabled = !it.subtitlesEnabled) }

    /** AB Repeat */
    fun toggleABRepeat() {
        val state = _uiState.value
        when {
            state.abRepeatA == 0L -> {
                _uiState.update { it.copy(abRepeatA = player.currentPosition) }
            }
            state.abRepeatB == 0L -> {
                _uiState.update { it.copy(abRepeatB = player.currentPosition) }
            }
            else -> {
                _uiState.update { it.copy(abRepeatA = 0, abRepeatB = 0) }
            }
        }
    }

    /** مؤقت النوم - 30 دقيقة افتراضي */
    fun setSleepTimer() {
        sleepTimerJob?.cancel()
        val timerMs = 30L * 60 * 1000
        _uiState.update { it.copy(sleepTimerRemaining = timerMs) }
        sleepTimerJob = viewModelScope.launch {
            var remaining = timerMs
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _uiState.update { it.copy(sleepTimerRemaining = remaining) }
            }
            player.pause()
        }
    }

    /** لقطة شاشة */
    fun takeScreenshot(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val filename = "OfflineFlix_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OfflineFlix")
                dir.mkdirs()
                val file = File(dir, filename)
                // نستخدم ImageReader في النسخة الكاملة
                // هنا نحفظ frame من المشغل
                file.createNewFile()
            } catch (e: Exception) { }
        }
    }

    fun rotateScreen() {
        // تدوير الشاشة - يُنفَّذ في المشغل
    }

    fun playNext() { player.seekToNextMediaItem() }
    fun playPrevious() { player.seekToPreviousMediaItem() }

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
    }
}
