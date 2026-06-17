package com.offlineflix.player.ui.screens.pdf

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlineflix.player.data.local.db.dao.PdfDao
import com.offlineflix.player.data.models.PdfBookmarkEntity
import com.offlineflix.player.data.models.PdfEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class PdfUiState(
    val pdfs: List<PdfEntity> = emptyList(),
    val currentPdf: PdfEntity? = null,
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val bookmarks: List<PdfBookmarkEntity> = emptyList(),
    val searchQuery: String = "",
    val showThumbnails: Boolean = false
)

/**
 * ViewModel لعارض PDF
 */
@HiltViewModel
class PdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfDao: PdfDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfUiState())
    val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()

    init {
        loadPdfs()
    }

    private fun loadPdfs() {
        viewModelScope.launch {
            pdfDao.getAllPdfs().collect { pdfs ->
                _uiState.update { it.copy(pdfs = pdfs) }
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            pdfDao.searchPdfs(query).collect { pdfs ->
                _uiState.update { it.copy(pdfs = pdfs) }
            }
        }
    }

    fun loadPdf(pdfId: Long) {
        viewModelScope.launch {
            val pdf = pdfDao.getById(pdfId) ?: return@launch
            _uiState.update { it.copy(currentPdf = pdf, currentPage = pdf.lastOpenedPage) }

            pdfDao.getBookmarks(pdfId).collect { bookmarks ->
                _uiState.update { it.copy(bookmarks = bookmarks) }
            }
        }
    }

    fun onPdfLoaded(pageCount: Int) {
        _uiState.update { it.copy(pageCount = pageCount) }
    }

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
        viewModelScope.launch {
            _uiState.value.currentPdf?.let { pdf ->
                pdfDao.updateLastPage(pdf.id, page, System.currentTimeMillis())
            }
        }
    }

    fun addBookmark(page: Int) {
        viewModelScope.launch {
            val pdf = _uiState.value.currentPdf ?: return@launch
            pdfDao.insertBookmark(PdfBookmarkEntity(pdfId = pdf.id, pageNumber = page))
        }
    }

    fun searchInPdf(query: String) = _uiState.update { it.copy(searchQuery = query) }

    /** مسح كل ملفات PDF */
    suspend fun scanAllPdfs(): Int = withContext(Dispatchers.IO) {
        var count = 0

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
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)) ?: continue
                    if (!File(path).exists()) continue

                    pdfs.add(PdfEntity(
                        path = path,
                        name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)) ?: File(path).name,
                        size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)),
                        mediaStoreId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)),
                        folderPath = File(path).parent ?: "",
                        dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)) * 1000
                    ))
                    count++
                } catch (e: Exception) { }
            }
            pdfDao.insertAll(pdfs)
        }

        // مسح مجلدات Telegram/WhatsApp
        val extraDirs = listOf(
            "/sdcard/Telegram/Telegram Documents",
            "/sdcard/WhatsApp/Media/WhatsApp Documents",
            "/sdcard/Download"
        )
        extraDirs.forEach { dirPath ->
            File(dirPath).walkTopDown().forEach { file ->
                if (file.extension.equals("pdf", ignoreCase = true)) {
                    viewModelScope.launch {
                        if (pdfDao.getByPath(file.absolutePath) == null) {
                            pdfDao.insert(PdfEntity(path = file.absolutePath, name = file.name, size = file.length(), folderPath = file.parent ?: ""))
                            count++
                        }
                    }
                }
            }
        }

        count
    }
}
