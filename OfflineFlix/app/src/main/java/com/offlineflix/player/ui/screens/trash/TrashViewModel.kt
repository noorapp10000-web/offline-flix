package com.offlineflix.player.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlineflix.player.data.local.db.dao.TrashDao
import com.offlineflix.player.data.models.TrashEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TrashUiState(val items: List<TrashEntity> = emptyList())

@HiltViewModel
class TrashViewModel @Inject constructor(private val trashDao: TrashDao) : ViewModel() {
    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            trashDao.getAllTrashItems().collect { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
        cleanExpired()
    }

    private fun cleanExpired() = viewModelScope.launch {
        trashDao.deleteExpiredItems(System.currentTimeMillis())
    }

    fun restoreItem(item: TrashEntity) = viewModelScope.launch {
        trashDao.deleteTrash(item)
        // إعادة الملف (إن كان موجوداً في مجلد مؤقت)
    }

    fun permanentlyDelete(item: TrashEntity) = viewModelScope.launch {
        try { File(item.originalPath).delete() } catch (e: Exception) {}
        trashDao.deleteTrash(item)
    }

    fun clearAll() = viewModelScope.launch {
        _uiState.value.items.forEach { item ->
            try { File(item.originalPath).delete() } catch (e: Exception) {}
        }
        trashDao.clearAll()
    }
}
