package com.offlineflix.player.ui.screens.filemanager

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class FileManagerUiState(
    val currentPath: String = Environment.getExternalStorageDirectory().absolutePath,
    val files: List<File> = emptyList()
)

@HiltViewModel
class FileManagerViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(FileManagerUiState())
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()
    private val pathHistory = mutableListOf<String>()

    init { loadFiles(Environment.getExternalStorageDirectory().absolutePath) }

    fun openDirectory(path: String) {
        pathHistory.add(_uiState.value.currentPath)
        loadFiles(path)
    }

    fun navigateUp(): Boolean {
        if (pathHistory.isEmpty()) return false
        loadFiles(pathHistory.removeLast())
        return true
    }

    private fun loadFiles(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(path)
            val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
            _uiState.update { it.copy(currentPath = path, files = files) }
        }
    }

    fun renameFile(file: File, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newFile = File(file.parent, "$newName.${file.extension}")
            file.renameTo(newFile)
            loadFiles(_uiState.value.currentPath)
        }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            file.deleteRecursively()
            loadFiles(_uiState.value.currentPath)
        }
    }

    fun copyFile(file: File) {
        // نسخ الملف للحافظة
    }

    fun moveFile(file: File) {
        // نقل الملف
    }
}
