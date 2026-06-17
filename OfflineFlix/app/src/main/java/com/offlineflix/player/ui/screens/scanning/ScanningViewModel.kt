package com.offlineflix.player.ui.screens.scanning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlineflix.player.data.repository.AudioRepository
import com.offlineflix.player.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanningUiState(
    val progress: Int = 0,
    val phase: String = "جاري التهيئة...",
    val currentFolder: String = "",
    val videosFound: Int = 0,
    val audiosFound: Int = 0,
    val pdfsFound: Int = 0,
    val foldersScanned: Int = 0,
    val isComplete: Boolean = false
)

/**
 * ViewModel لشاشة المسح
 */
@HiltViewModel
class ScanningViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanningUiState())
    val uiState: StateFlow<ScanningUiState> = _uiState

    fun startScan() = viewModelScope.launch {
        try {
            // مرحلة 1: مسح الفيديوهات
            _uiState.value = _uiState.value.copy(
                progress = 10,
                phase = "مسح الفيديوهات...",
                currentFolder = "جاري البحث في MediaStore..."
            )

            val videoCount = videoRepository.scanAllMedia()
            _uiState.value = _uiState.value.copy(
                progress = 50,
                videosFound = videoCount,
                phase = "مسح الأغاني...",
                currentFolder = "جاري فهرسة الموسيقى..."
            )

            // مرحلة 2: مسح الأغاني
            val audioCount = audioRepository.scanAllAudio()
            _uiState.value = _uiState.value.copy(
                progress = 80,
                audiosFound = audioCount,
                phase = "إنهاء المسح...",
                currentFolder = "جاري ترتيب الملفات..."
            )

            kotlinx.coroutines.delay(500)

            _uiState.value = _uiState.value.copy(
                progress = 100,
                phase = "اكتمل المسح!",
                currentFolder = "تم فهرسة كل الوسائط",
                isComplete = true
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                progress = 100,
                phase = "اكتمل المسح",
                isComplete = true
            )
        }
    }
}
