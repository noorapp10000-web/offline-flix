package com.offlineflix.player.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlineflix.player.data.models.VideoEntity
import com.offlineflix.player.data.repository.AudioRepository
import com.offlineflix.player.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val heroVideo: VideoEntity? = null,
    val continueWatching: List<VideoEntity> = emptyList(),
    val recentVideos: List<VideoEntity> = emptyList(),
    val videos4K: List<VideoEntity> = emptyList(),
    val favoriteVideos: List<VideoEntity> = emptyList(),
    val videoCount: Int = 0,
    val audioCount: Int = 0,
    val totalSize: Long = 0
)

/**
 * ViewModel للشاشة الرئيسية
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                videoRepository.getRecentVideos(),
                videoRepository.getIncompleteVideos(),
                videoRepository.get4KVideos(),
                videoRepository.getFavoriteVideos(),
                videoRepository.getVideoCount(),
                videoRepository.getTotalSize(),
                audioRepository.getTrackCount()
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                val recentVideos = args[0] as List<VideoEntity>
                val continueWatching = args[1] as List<VideoEntity>
                val videos4K = args[2] as List<VideoEntity>
                val favoriteVideos = args[3] as List<VideoEntity>
                val videoCount = args[4] as Int
                val totalSize = (args[5] as Long?) ?: 0L
                val audioCount = args[6] as Int

                HomeUiState(
                    heroVideo = recentVideos.firstOrNull(),
                    continueWatching = continueWatching.take(15),
                    recentVideos = recentVideos.take(15),
                    videos4K = videos4K.take(15),
                    favoriteVideos = favoriteVideos.take(15),
                    videoCount = videoCount,
                    audioCount = audioCount,
                    totalSize = totalSize
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
