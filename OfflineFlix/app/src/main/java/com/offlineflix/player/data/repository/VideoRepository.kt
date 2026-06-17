package com.offlineflix.player.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.offlineflix.player.data.local.db.dao.VideoDao
import com.offlineflix.player.data.local.db.dao.FolderDao
import com.offlineflix.player.data.models.FolderEntity
import com.offlineflix.player.data.models.VideoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مستودع الفيديوهات - يدير الوصول لقاعدة البيانات و MediaStore
 */
@Singleton
class VideoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoDao: VideoDao,
    private val folderDao: FolderDao
) {

    // ==================== قراءة البيانات ====================

    fun getAllVideos(): Flow<List<VideoEntity>> = videoDao.getAllVideos()
    fun getAllSortedByName(): Flow<List<VideoEntity>> = videoDao.getAllSortedByName()
    fun getAllSortedBySize(): Flow<List<VideoEntity>> = videoDao.getAllSortedBySize()
    fun getAllSortedByDuration(): Flow<List<VideoEntity>> = videoDao.getAllSortedByDuration()
    fun getAllSortedByLastWatched(): Flow<List<VideoEntity>> = videoDao.getAllSortedByLastWatched()
    fun getVideosByFolder(folderId: Long): Flow<List<VideoEntity>> = videoDao.getVideosByFolder(folderId)
    fun getIncompleteVideos(): Flow<List<VideoEntity>> = videoDao.getIncompleteVideos()
    fun getWatchedVideos(): Flow<List<VideoEntity>> = videoDao.getWatchedVideos()
    fun getUnwatchedVideos(): Flow<List<VideoEntity>> = videoDao.getUnwatchedVideos()
    fun getAlmostFinishedVideos(): Flow<List<VideoEntity>> = videoDao.getAlmostFinishedVideos()
    fun getLargeVideos(): Flow<List<VideoEntity>> = videoDao.getLargeVideos()
    fun get4KVideos(): Flow<List<VideoEntity>> = videoDao.get4KVideos()
    fun getTop100BySize(): Flow<List<VideoEntity>> = videoDao.getTop100BySize()
    fun getOldestVideos(): Flow<List<VideoEntity>> = videoDao.getOldestVideos()
    fun getFavoriteVideos(): Flow<List<VideoEntity>> = videoDao.getFavoriteVideos()
    fun searchVideos(query: String): Flow<List<VideoEntity>> = videoDao.searchVideos(query)
    fun getRecentVideos(): Flow<List<VideoEntity>> = videoDao.getRecentVideos()
    fun getVideoCount(): Flow<Int> = videoDao.getVideoCount()
    fun getTotalSize(): Flow<Long?> = videoDao.getTotalSize()
    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()

    suspend fun getById(id: Long): VideoEntity? = videoDao.getById(id)

    // ==================== مسح MediaStore ====================

    /**
     * مسح كامل لكل الجهاز والبطاقة والـ OTG
     * يبحث في: Downloads, DCIM, Movies, WhatsApp, Telegram, وكل المجلدات
     */
    suspend fun scanAllMedia(): Int = withContext(Dispatchers.IO) {
        var count = 0

        // الصيغ المدعومة
        val supportedExtensions = setOf(
            "mp4", "mkv", "avi", "mov", "flv", "wmv", "ts", "m2ts", "3gp", "webm",
            "vob", "rmvb", "divx", "xvid", "m4v", "ogv", "f4v", "mpg", "mpeg",
            "asf", "rm", "m2v", "mts", "tp", "trp", "mod", "tod", "rec",
            "mp2", "m1v", "m2p", "ps", "pva", "svi", "amv", "nsv", "nuv",
            "m4b", "m4r", "h264", "h265", "hevc", "264", "265", "iso", "img",
            "bdmv", "ssif", "m3u8"
        )

        // مسح MediaStore
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BITRATE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.RESOLUTION
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val videos = mutableListOf<VideoEntity>()
            val folderMap = mutableMapOf<Long, FolderEntity>()

            while (cursor.moveToNext()) {
                try {
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                        ?: continue
                    val file = File(path)
                    if (!file.exists()) continue

                    val ext = file.extension.lowercase()
                    // قبول كل الصيغ + الصيغ الإضافية
                    val mediaStoreId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)) ?: file.name
                    val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                    val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                    val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH))
                    val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT))
                    val bitrate = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BITRATE))
                    val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)) * 1000
                    val dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)) * 1000
                    val folderId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
                    val folderName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)) ?: ""
                    val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)) ?: ""

                    val video = VideoEntity(
                        path = path,
                        name = name,
                        displayName = name.substringBeforeLast("."),
                        size = size,
                        duration = duration,
                        width = width,
                        height = height,
                        bitrate = bitrate,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        folderId = folderId,
                        folderPath = file.parent ?: "",
                        mediaStoreId = mediaStoreId,
                        mimeType = mimeType,
                        is4K = width >= 3840,
                        isHdr = mimeType.contains("hdr", true)
                    )

                    videos.add(video)
                    count++

                    // تجميع المجلدات
                    if (!folderMap.containsKey(folderId)) {
                        folderMap[folderId] = FolderEntity(
                            path = file.parent ?: "",
                            name = folderName,
                            videoCount = 0
                        )
                    }
                } catch (e: Exception) {
                    // تجاهل الملفات التالفة
                }
            }

            // حفظ دفعي في قاعدة البيانات
            videoDao.insertAll(videos)
            folderDao.insertAll(folderMap.values.toList())
        }

        // مسح المجلدات الإضافية يدوياً (WhatsApp, Telegram, etc.)
        count += scanExtraFolders(supportedExtensions)

        count
    }

    /**
     * مسح المجلدات الخاصة يدوياً
     */
    private suspend fun scanExtraFolders(extensions: Set<String>): Int {
        var count = 0
        val extraPaths = listOf(
            "/sdcard/WhatsApp/Media/WhatsApp Video",
            "/sdcard/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video",
            "/sdcard/Telegram",
            "/sdcard/DCIM",
            "/sdcard/Movies",
            "/sdcard/Downloads",
            "/sdcard/Pictures",
            "/storage/emulated/0/WhatsApp/Media/WhatsApp Video",
            "/storage/emulated/0/Telegram",
            "/storage/emulated/0/Download",
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath,
        )

        for (path in extraPaths) {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                count += scanDirectory(dir, extensions)
            }
        }
        return count
    }

    private suspend fun scanDirectory(dir: File, extensions: Set<String>): Int {
        var count = 0
        try {
            dir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension.lowercase() in extensions) {
                    val existing = videoDao.getByPath(file.absolutePath)
                    if (existing == null) {
                        val video = VideoEntity(
                            path = file.absolutePath,
                            name = file.name,
                            displayName = file.nameWithoutExtension,
                            size = file.length(),
                            duration = getVideoDuration(file.absolutePath),
                            folderPath = file.parent ?: "",
                            dateAdded = file.lastModified()
                        )
                        videoDao.insert(video)
                        count++
                    }
                }
            }
        } catch (e: Exception) { }
        return count
    }

    private fun getVideoDuration(path: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            retriever.release()
            duration
        } catch (e: Exception) { 0L }
    }

    // ==================== تحديثات ====================

    suspend fun updatePlaybackProgress(id: Long, position: Long, progress: Int) {
        videoDao.updatePlaybackProgress(id, position, progress, System.currentTimeMillis())
    }

    suspend fun markAsWatched(id: Long) {
        videoDao.markAsWatched(id, System.currentTimeMillis())
    }

    suspend fun updateRating(id: Long, rating: Int) = videoDao.updateRating(id, rating)
    suspend fun updateNotes(id: Long, notes: String) = videoDao.updateNotes(id, notes)
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = videoDao.updateFavorite(id, isFavorite)
    suspend fun renameVideo(id: Long, newName: String) = videoDao.updateDisplayName(id, newName)
    suspend fun moveToTrash(id: Long) = videoDao.moveToTrash(id, System.currentTimeMillis())
    suspend fun restoreFromTrash(id: Long) = videoDao.restoreFromTrash(id)

    /**
     * كشف الملفات المكررة بالحجم والاسم
     * يجمع الفيديوهات التي لها نفس الحجم أو نفس الاسم
     * ويُعيد مجموعات تحتوي على أكثر من نسخة واحدة
     */
    suspend fun findDuplicates(): Map<String, List<VideoEntity>> = withContext(Dispatchers.IO) {
        val allPaths = videoDao.getAllPaths()
        val duplicates = mutableMapOf<String, MutableList<VideoEntity>>()

        // جلب كل الفيديوهات النشطة
        val allVideos = mutableListOf<VideoEntity>()
        for (path in allPaths) {
            val video = videoDao.getByPath(path)
            if (video != null && !video.isDeleted) allVideos.add(video)
        }

        // المجموعة 1: تكرار بالحجم الدقيق (نفس الحجم = نفس الملف غالباً)
        val bySize = mutableMapOf<Long, MutableList<VideoEntity>>()
        for (video in allVideos) {
            if (video.size > 0) {
                bySize.getOrPut(video.size) { mutableListOf() }.add(video)
            }
        }
        for ((size, group) in bySize) {
            if (group.size > 1) {
                duplicates["حجم ${formatSizeKt(size)}"] = group
            }
        }

        // المجموعة 2: تكرار بالاسم (بدون امتداد)
        val byName = mutableMapOf<String, MutableList<VideoEntity>>()
        for (video in allVideos) {
            val nameKey = video.name.substringBeforeLast(".").lowercase().trim()
            if (nameKey.isNotBlank()) {
                byName.getOrPut(nameKey) { mutableListOf() }.add(video)
            }
        }
        for ((name, group) in byName) {
            // تجنب الإضافة المزدوجة مع مجموعة الحجم
            if (group.size > 1 && !duplicates.values.any { it.containsAll(group) }) {
                duplicates["اسم: $name"] = group
            }
        }

        duplicates
    }

    /** دالة مساعدة لتنسيق الحجم داخل Repository */
    private fun formatSizeKt(bytes: Long): String = when {
        bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824f)
        bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576f)
        else -> "%.1f KB".format(bytes / 1_024f)
    }
}
