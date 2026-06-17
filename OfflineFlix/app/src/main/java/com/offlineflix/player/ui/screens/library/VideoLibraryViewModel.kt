package com.offlineflix.player.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlineflix.player.data.models.VideoEntity
import com.offlineflix.player.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoLibraryUiState(
    val videos: List<VideoEntity> = emptyList(),
    val activeFilter: SmartFilter = SmartFilter.ALL,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val isGridView: Boolean = true,
    val isMultiSelectMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val isScanning: Boolean = false
)

/**
 * ViewModel لمكتبة الفيديوهات
 */
@HiltViewModel
class VideoLibraryViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoLibraryUiState())
    val uiState: StateFlow<VideoLibraryUiState> = _uiState.asStateFlow()

    private var currentFilter: SmartFilter = SmartFilter.ALL
    private var currentSort: SortOption = SortOption.DATE_DESC

    init {
        loadVideos()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            getVideoFlow(currentFilter, currentSort).collect { videos ->
                _uiState.update { it.copy(videos = videos) }
            }
        }
    }

    private fun getVideoFlow(filter: SmartFilter, sort: SortOption): Flow<List<VideoEntity>> {
        return when (filter) {
            SmartFilter.ALL -> getSortedFlow(sort)
            SmartFilter.CONTINUE -> videoRepository.getIncompleteVideos()
            SmartFilter.UNWATCHED -> videoRepository.getUnwatchedVideos()
            SmartFilter.ALMOST_DONE -> videoRepository.getAlmostFinishedVideos()
            SmartFilter.WATCHED -> videoRepository.getWatchedVideos()
            SmartFilter.FAVORITES -> videoRepository.getFavoriteVideos()
            SmartFilter.FOUR_K -> videoRepository.get4KVideos()
            SmartFilter.LARGE -> videoRepository.getLargeVideos()
            SmartFilter.OLDEST -> videoRepository.getOldestVideos()
        }
    }

    private fun getSortedFlow(sort: SortOption): Flow<List<VideoEntity>> {
        return when (sort) {
            SortOption.DATE_DESC -> videoRepository.getAllVideos()
            SortOption.DATE_ASC -> videoRepository.getAllVideos().map { it.sortedBy { v -> v.dateAdded } }
            SortOption.NAME_ASC -> videoRepository.getAllSortedByName()
            SortOption.NAME_DESC -> videoRepository.getAllSortedByName().map { it.reversed() }
            SortOption.SIZE_DESC -> videoRepository.getAllSortedBySize()
            SortOption.DURATION_DESC -> videoRepository.getAllSortedByDuration()
            SortOption.LAST_WATCHED -> videoRepository.getAllSortedByLastWatched()
        }
    }

    fun setFilter(filter: SmartFilter) {
        currentFilter = filter
        _uiState.update { it.copy(activeFilter = filter) }
        loadVideos()
    }

    fun setSort(sort: SortOption) {
        currentSort = sort
        _uiState.update { it.copy(sortOption = sort) }
        loadVideos()
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isEmpty()) {
                loadVideos()
            } else {
                videoRepository.searchVideos(query).collect { videos ->
                    _uiState.update { it.copy(videos = videos) }
                }
            }
        }
    }

    fun toggleViewMode() = _uiState.update { it.copy(isGridView = !it.isGridView) }

    fun toggleMultiSelect() {
        _uiState.update {
            it.copy(
                isMultiSelectMode = !it.isMultiSelectMode,
                selectedIds = emptySet()
            )
        }
    }

    fun toggleSelect(id: Long) {
        val current = _uiState.value.selectedIds.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _uiState.update { it.copy(selectedIds = current) }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _uiState.value.selectedIds.forEach { id ->
                videoRepository.moveToTrash(id)
            }
            _uiState.update { it.copy(selectedIds = emptySet(), isMultiSelectMode = false) }
        }
    }

    fun rescan() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isScanning = true) }
            try {
                videoRepository.scanAllMedia()
            } finally {
                _uiState.update { it.copy(isScanning = false) }
                loadVideos()
            }
        }
    }
}
