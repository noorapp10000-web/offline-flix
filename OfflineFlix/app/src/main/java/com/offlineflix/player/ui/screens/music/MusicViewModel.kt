package com.offlineflix.player.ui.screens.music

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.offlineflix.player.data.models.AudioEntity
import com.offlineflix.player.data.models.PlaylistEntity
import com.offlineflix.player.data.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject

data class MusicUiState(
    val tracks: List<AudioEntity> = emptyList(),
    val albums: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val folders: List<String> = emptyList(),
    val currentTrack: AudioEntity? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentPosition: Long = 0,
    val selectedTab: MusicTab = MusicTab.SONGS,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val eqBands: List<Int> = List(10) { 0 },
    val eqPreset: String = "عادي",
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val showEqualizer: Boolean = false,
    val currentLyricLine: String = ""
)

/**
 * ViewModel للموسيقى مع ExoPlayer + Equalizer
 */
@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRepository: AudioRepository
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private var equalizer: Equalizer? = null
    private var bassBoostEffect: BassBoost? = null
    private var virtualizerEffect: Virtualizer? = null

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var lrcJob: Job? = null
    private var currentPlaylist: List<AudioEntity> = emptyList()
    private var currentIndex: Int = 0
    private var lrcLines: List<Pair<Long, String>> = emptyList()

    init {
        loadData()
        setupPlayer()
        setupEqualizer()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                audioRepository.getAllTracks(),
                audioRepository.getAllAlbums(),
                audioRepository.getAllArtists(),
                audioRepository.getAllPlaylists(),
                audioRepository.getAllAudioFolders()
            ) { tracks, albums, artists, playlists, folders ->
                _uiState.update {
                    it.copy(
                        tracks = tracks,
                        albums = albums,
                        artists = artists,
                        playlists = playlists,
                        folders = folders
                    )
                }
                currentPlaylist = tracks
            }.collect()
        }
    }

    private fun setupPlayer() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressTracking() else progressJob?.cancel()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    when (_uiState.value.repeatMode) {
                        1 -> player.seekTo(0)
                        2 -> player.seekTo(0)
                        else -> playNext()
                    }
                }
            }
        })
    }

    private fun setupEqualizer() {
        try {
            equalizer = Equalizer(0, player.audioSessionId).apply { enabled = true }
            bassBoostEffect = BassBoost(0, player.audioSessionId).apply { enabled = true }
            virtualizerEffect = Virtualizer(0, player.audioSessionId).apply { enabled = true }
        } catch (e: Exception) { }
    }

    fun playTrack(track: AudioEntity) {
        viewModelScope.launch {
            val index = currentPlaylist.indexOfFirst { it.id == track.id }
            currentIndex = if (index >= 0) index else 0

            val mediaItem = MediaItem.fromUri(android.net.Uri.parse(track.path))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            _uiState.update { it.copy(currentTrack = track) }
            audioRepository.updateLastPlayed(track.id)
            loadLrcFile(track)
        }
    }

    private fun loadLrcFile(track: AudioEntity) {
        lrcJob?.cancel()
        lrcLines = emptyList()
        if (track.lrcPath.isNotEmpty()) {
            try {
                val lines = File(track.lrcPath).readLines()
                val parsed = mutableListOf<Pair<Long, String>>()
                val timeRegex = Regex("""\[(\d+):(\d+\.\d+)\](.*)""")
                lines.forEach { line ->
                    timeRegex.find(line)?.let { match ->
                        val min = match.groupValues[1].toLong()
                        val sec = match.groupValues[2].toDouble()
                        val ms = min * 60_000 + (sec * 1000).toLong()
                        val text = match.groupValues[3].trim()
                        parsed.add(Pair(ms, text))
                    }
                }
                lrcLines = parsed.sortedBy { it.first }

                lrcJob = viewModelScope.launch {
                    while (true) {
                        val pos = player.currentPosition
                        val current = lrcLines.lastOrNull { it.first <= pos }?.second ?: ""
                        _uiState.update { it.copy(currentLyricLine = current) }
                        delay(200)
                    }
                }
            } catch (e: Exception) { }
        } else {
            _uiState.update { it.copy(currentLyricLine = "") }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val pos = player.currentPosition
                val dur = player.duration.coerceAtLeast(1)
                _uiState.update {
                    it.copy(
                        progress = pos.toFloat() / dur,
                        currentPosition = pos
                    )
                }
                delay(500)
            }
        }
    }

    fun togglePlayPause() = if (player.isPlaying) player.pause() else player.play()

    fun playNext() {
        if (_uiState.value.shuffleEnabled) {
            val nextIndex = currentPlaylist.indices.random()
            playTrack(currentPlaylist[nextIndex])
        } else {
            currentIndex = (currentIndex + 1) % currentPlaylist.size
            playTrack(currentPlaylist[currentIndex])
        }
    }

    fun playPrevious() {
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        } else {
            currentIndex = (currentIndex - 1 + currentPlaylist.size) % currentPlaylist.size
            playTrack(currentPlaylist[currentIndex])
        }
    }

    fun seekTo(progress: Float) {
        val pos = (progress * player.duration).toLong()
        player.seekTo(pos)
    }

    fun toggleShuffle() = _uiState.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    fun toggleRepeat() = _uiState.update { it.copy(repeatMode = (it.repeatMode + 1) % 3) }
    fun selectTab(tab: MusicTab) = _uiState.update { it.copy(selectedTab = tab) }
    fun openEqualizer() = _uiState.update { it.copy(showEqualizer = true) }
    fun closeEqualizer() = _uiState.update { it.copy(showEqualizer = false) }
    /** تصفية وتشغيل أغاني ألبوم معين */
    fun openAlbum(album: String) {
        viewModelScope.launch {
            audioRepository.getTracksByAlbum(album).first().let { tracks ->
                if (tracks.isNotEmpty()) {
                    currentPlaylist = tracks
                    currentIndex = 0
                    playTrack(tracks.first())
                }
            }
        }
    }

    /** تصفية وتشغيل أغاني فنان معين */
    fun openArtist(artist: String) {
        viewModelScope.launch {
            audioRepository.getTracksByArtist(artist).first().let { tracks ->
                if (tracks.isNotEmpty()) {
                    currentPlaylist = tracks
                    currentIndex = 0
                    playTrack(tracks.first())
                }
            }
        }
    }

    /** تصفية وتشغيل أغاني مجلد معين */
    fun openFolder(folder: String) {
        viewModelScope.launch {
            audioRepository.getTracksByFolder(folder).first().let { tracks ->
                if (tracks.isNotEmpty()) {
                    currentPlaylist = tracks
                    currentIndex = 0
                    playTrack(tracks.first())
                }
            }
        }
    }

    /** فتح وتشغيل قائمة تشغيل بالـ ID */
    fun openPlaylist(id: Long) {
        viewModelScope.launch {
            audioRepository.getPlaylistWithTracks(id).first()?.tracks?.let { tracks ->
                if (tracks.isNotEmpty()) {
                    currentPlaylist = tracks
                    currentIndex = 0
                    playTrack(tracks.first())
                }
            }
        }
    }

    fun shuffleAll() {
        if (currentPlaylist.isNotEmpty()) {
            _uiState.update { it.copy(shuffleEnabled = true) }
            playTrack(currentPlaylist.random())
        }
    }

    fun toggleFavorite(id: Long) = viewModelScope.launch {
        val track = _uiState.value.currentTrack ?: return@launch
        audioRepository.toggleFavorite(id, !track.isFavorite)
    }

    /** فتح نافذة إضافة الأغنية لقائمة تشغيل - يُضاف للقائمة الأولى المتاحة */
    fun addToPlaylist() = viewModelScope.launch {
        val track = _uiState.value.currentTrack ?: return@launch
        val playlists = _uiState.value.playlists
        if (playlists.isNotEmpty()) {
            audioRepository.addTrackToPlaylist(playlists.first().id, track.id)
        } else {
            val newId = audioRepository.createPlaylist("المفضلة")
            audioRepository.addTrackToPlaylist(newId, track.id)
        }
    }

    /** إضافة الأغنية لقائمة تشغيل محددة بالـ ID */
    fun addToPlaylistById(playlistId: Long) = viewModelScope.launch {
        val track = _uiState.value.currentTrack ?: return@launch
        audioRepository.addTrackToPlaylist(playlistId, track.id)
    }

    fun createPlaylist() = viewModelScope.launch {
        audioRepository.createPlaylist("قائمة ${System.currentTimeMillis()}")
    }

    // ==================== الإيكولايزر ====================
    fun setEqBand(band: Int, value: Int) {
        val bands = _uiState.value.eqBands.toMutableList()
        bands[band] = value
        _uiState.update { it.copy(eqBands = bands) }
        try {
            val millibell = (value * 100).toShort()
            equalizer?.setBandLevel(band.toShort(), millibell)
        } catch (e: Exception) { }
    }

    fun setBassBoost(value: Int) {
        _uiState.update { it.copy(bassBoost = value) }
        try {
            bassBoostEffect?.setStrength((value * 10).coerceIn(0, 1000).toShort())
        } catch (e: Exception) { }
    }

    fun setVirtualizer(value: Int) {
        _uiState.update { it.copy(virtualizer = value) }
        try {
            virtualizerEffect?.setStrength((value * 10).coerceIn(0, 1000).toShort())
        } catch (e: Exception) { }
    }

    fun applyEqPreset(preset: String) {
        val bands = EQ_PRESETS[preset] ?: return
        bands.forEachIndexed { index, value -> setEqBand(index, value) }
        _uiState.update { it.copy(eqPreset = preset) }
    }

    companion object {
        val EQ_PRESETS = mapOf(
            "عادي" to listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            "كلاسيك" to listOf(4, 3, 2, 1, 0, 0, -1, -2, -3, -4),
            "باص" to listOf(6, 5, 4, 2, 0, 0, 0, 0, 0, 0),
            "روك" to listOf(5, 4, 3, -1, -2, 0, 3, 4, 5, 5),
            "بوب" to listOf(-1, -1, 0, 2, 4, 4, 2, 0, -1, -1),
            "جاز" to listOf(3, 2, 0, 2, -2, -2, 0, 1, 2, 3),
            "صوت بشري" to listOf(-3, -2, 0, 3, 4, 4, 3, 0, -2, -3),
            "هيب هوب" to listOf(5, 4, 1, 3, -1, 0, -1, 0, 1, 2),
            "إلكترونية" to listOf(5, 4, 1, 0, -3, -3, 0, 1, 4, 5),
            "بودكاست" to listOf(-2, 0, 0, 2, 3, 3, 2, 0, 0, -2)
        )
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
        equalizer?.release()
        bassBoostEffect?.release()
        virtualizerEffect?.release()
        progressJob?.cancel()
        lrcJob?.cancel()
    }
}
