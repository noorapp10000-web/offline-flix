package com.offlineflix.player.ui.screens.scanning

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlineflix.player.data.local.db.dao.PdfDao
import com.offlineflix.player.data.models.PdfEntity
import com.offlineflix.player.data.repository.AudioRepository
import com.offlineflix.player.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
 * ViewModel لشاشة المسح - يمسح الفيديوهات والأغاني وملفات PDF
 */
@HiltViewModel
class ScanningViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val audioRepository: AudioRepository,
    private val pdfDao: PdfDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanningUiState())
    val uiState: StateFlow<ScanningUiState> = _uiState

    fun startScan() = viewModelScope.launch {
        try {
            // مرحلة 1: مسح الفيديوهات (0→40%)
            _uiState.value = _uiState.value.copy(
                progress = 10,
                phase = "مسح الفيديوهات...",
                currentFolder = "جاري البحث في MediaStore..."
            )

            val videoCount = videoRepository.scanAllMedia()
            _uiState.value = _uiState.value.copy(
                progress = 40,
                videosFound = videoCount,
                phase = "مسح الأغاني...",
                currentFolder = "جاري فهرسة الموسيقى..."
            )

            // مرحلة 2: مسح الأغاني (40→70%)
            val audioCount = audioRepository.scanAllAudio()
            _uiState.value = _uiState.value.copy(
                progress = 70,
                audiosFound = audioCount,
                phase = "مسح ملفات PDF...",
                currentFolder = "جاري البحث عن ملفات PDF..."
            )

            // مرحلة 3: مسح PDF (70→95%)
            val pdfCount = scanPdfs()
            _uiState.value = _uiState.value.copy(
                progress = 95,
                pdfsFound = pdfCount,
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

    @Suppress("DEPRECATION")
    private suspend fun scanPdfs(): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED
            )
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
            val selectionArgs = arrayOf("application/pdf")
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val pdfs = mutableListOf<PdfEntity>()
                while (cursor.moveToNext()) {
                    try {
                        val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
                            ?: continue
                        if (!File(path).exists()) continue

                        pdfs.add(PdfEntity(
                            path = path,
                            name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
                                ?: File(path).name,
                            size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)),
                            mediaStoreId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)),
                            folderPath = File(path).parent ?: "",
                            dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)) * 1000
                        ))
                        count++
                    } catch (_: Exception) { }
                }
                pdfDao.insertAll(pdfs)
            }
        } catch (_: Exception) { }
        count
    }
}
