package com.offlineflix.player.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlineflix.player.data.local.db.dao.AudioDao
import com.offlineflix.player.data.local.db.dao.TrashDao
import com.offlineflix.player.data.local.db.dao.VideoDao
import com.offlineflix.player.data.models.TrashEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TrashUiState(
    val items: List<TrashEntity> = emptyList(),
    val restoredCount: Int = 0,
    val errorMessage: String = ""
)

/** أنواع الوسائط المعروفة */
private val VIDEO_EXTENSIONS = setOf(
    "mp4","mkv","avi","mov","flv","wmv","ts","3gp","webm","vob","rmvb","m4v","ogv","mpg","mpeg"
)
private val AUDIO_EXTENSIONS = setOf(
    "mp3","flac","wav","aac","m4a","ogg","opus","wma","alac","ape","mka"
)

/**
 * ViewModel سلة المحذوفات
 * يدعم استعادة الفيديوهات والصوتيات من سلة المحذوفات
 * والحذف النهائي بعد 30 يوم تلقائياً
 */
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trashDao: TrashDao,
    private val videoDao: VideoDao,
    private val audioDao: AudioDao
) : ViewModel() {

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

    /** حذف العناصر المنتهية الصلاحية (أكثر من 30 يوم) تلقائياً */
    private fun cleanExpired() = viewModelScope.launch {
        trashDao.deleteExpiredItems(System.currentTimeMillis())
    }

    /**
     * استعادة عنصر من السلة إلى المكتبة
     * يُحدد نوع الملف من الامتداد ثم يُزيل علامة الحذف من DB
     */
    fun restoreItem(item: TrashEntity) = viewModelScope.launch {
        try {
            val ext = item.type.lowercase().trimStart('.')
            when {
                ext in VIDEO_EXTENSIONS -> {
                    // بحث عن الفيديو في قاعدة البيانات بالمسار ثم استعادته
                    val video = videoDao.getByPath(item.originalPath)
                    if (video != null) {
                        videoDao.restoreFromTrash(video.id)
                    }
                }
                ext in AUDIO_EXTENSIONS -> {
                    // بحث عن الصوت في قاعدة البيانات بالمسار ثم استعادته (بغض النظر عن isDeleted)
                    val audio = audioDao.getByPathAny(item.originalPath)
                    if (audio != null) {
                        audioDao.restoreFromTrash(audio.id)
                    }
                }
                else -> {
                    // نوع غير معروف (PDF, صورة, إلخ) - الملف لا يزال موجوداً على القرص
                }
            }
            // إزالة من سلة المحذوفات بعد الاستعادة
            trashDao.deleteTrash(item)
            _uiState.update { it.copy(restoredCount = it.restoredCount + 1) }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "فشل في استعادة ${item.name}: ${e.message}") }
        }
    }

    /** حذف نهائي فوري مع حذف الملف من التخزين */
    fun permanentlyDelete(item: TrashEntity) = viewModelScope.launch {
        try {
            // حذف الملف الفعلي من القرص
            val file = File(item.originalPath)
            if (file.exists()) file.delete()
            // حذف من DB بالمسار
            val ext = item.type.lowercase().trimStart('.')
            when {
                ext in VIDEO_EXTENSIONS -> {
                    val video = videoDao.getByPath(item.originalPath)
                    if (video != null) videoDao.delete(video)
                }
                ext in AUDIO_EXTENSIONS -> {
                    val audio = audioDao.getByPathAny(item.originalPath)
                    if (audio != null) audioDao.delete(audio)
                }
            }
            trashDao.deleteTrash(item)
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "فشل في الحذف: ${e.message}") }
        }
    }

    /** مسح كل السلة نهائياً */
    fun clearAll() = viewModelScope.launch {
        _uiState.value.items.forEach { item ->
            try { File(item.originalPath).delete() } catch (_: Exception) {}
        }
        trashDao.clearAll()
    }

    /** مسح رسالة الخطأ */
    fun clearError() = _uiState.update { it.copy(errorMessage = "") }
}
