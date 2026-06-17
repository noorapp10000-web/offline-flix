package com.offlineflix.player.data.local.db.dao

import androidx.room.*
import com.offlineflix.player.data.models.VideoEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO للتعامل مع الفيديوهات في قاعدة البيانات
 */
@Dao
interface VideoDao {

    // ==================== إدراج / تحديث ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)

    @Update
    suspend fun update(video: VideoEntity)

    @Delete
    suspend fun delete(video: VideoEntity)

    // ==================== استعلامات أساسية ====================

    @Query("SELECT * FROM videos WHERE isDeleted = 0 ORDER BY dateAdded DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getById(id: Long): VideoEntity?

    @Query("SELECT * FROM videos WHERE path = :path")
    suspend fun getByPath(path: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE isDeleted = 0 AND folderId = :folderId ORDER BY name ASC")
    fun getVideosByFolder(folderId: Long): Flow<List<VideoEntity>>

    // ==================== الفرز ====================

    @Query("SELECT * FROM videos WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllSortedByName(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 ORDER BY size DESC")
    fun getAllSortedBySize(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 ORDER BY duration DESC")
    fun getAllSortedByDuration(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 ORDER BY lastWatched DESC")
    fun getAllSortedByLastWatched(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 ORDER BY dateModified DESC")
    fun getAllSortedByDate(): Flow<List<VideoEntity>>

    // ==================== المجلدات الذكية ====================

    @Query("SELECT * FROM videos WHERE isDeleted = 0 AND watchProgress > 0 AND watchProgress < 95 ORDER BY lastWatched DESC")
    fun getIncompleteVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 AND (isWatched = 1 OR watchProgress >= 95) ORDER BY lastWatched DESC")
    fun getWatchedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 AND lastWatched = 0 ORDER BY dateAdded DESC")
    fun getUnwatchedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 AND watchProgress >= 90 AND watchProgress < 100 ORDER BY lastWatched DESC")
    fun getAlmostFinishedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 AND size > 2147483648 ORDER BY size DESC")
    fun getLargeVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 AND (width >= 3840 OR is4K = 1) ORDER BY dateAdded DESC")
    fun get4KVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 ORDER BY size DESC LIMIT 100")
    fun getTop100BySize(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 ORDER BY dateAdded ASC LIMIT 50")
    fun getOldestVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoriteVideos(): Flow<List<VideoEntity>>

    // ==================== البحث ====================

    @Query("SELECT * FROM videos WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR displayName LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchVideos(query: String): Flow<List<VideoEntity>>

    // ==================== التحديثات ====================

    @Query("UPDATE videos SET lastPosition = :position, lastWatched = :timestamp, watchProgress = :progress, viewCount = viewCount + 1 WHERE id = :id")
    suspend fun updatePlaybackProgress(id: Long, position: Long, progress: Int, timestamp: Long)

    @Query("UPDATE videos SET isWatched = 1, watchProgress = 100, lastWatched = :timestamp WHERE id = :id")
    suspend fun markAsWatched(id: Long, timestamp: Long)

    @Query("UPDATE videos SET rating = :rating WHERE id = :id")
    suspend fun updateRating(id: Long, rating: Int)

    @Query("UPDATE videos SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)

    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE videos SET displayName = :name WHERE id = :id")
    suspend fun updateDisplayName(id: Long, name: String)

    @Query("UPDATE videos SET thumbnailPath = :path WHERE id = :id")
    suspend fun updateThumbnailPath(id: Long, path: String)

    @Query("UPDATE videos SET isDeleted = 1, deletedAt = :timestamp WHERE id = :id")
    suspend fun moveToTrash(id: Long, timestamp: Long)

    @Query("UPDATE videos SET isDeleted = 0, deletedAt = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    // ==================== الإحصائيات ====================

    @Query("SELECT COUNT(*) FROM videos WHERE isDeleted = 0")
    fun getVideoCount(): Flow<Int>

    @Query("SELECT SUM(size) FROM videos WHERE isDeleted = 0")
    fun getTotalSize(): Flow<Long?>

    @Query("SELECT SUM(duration) FROM videos WHERE isDeleted = 0")
    fun getTotalDuration(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM videos WHERE isDeleted = 0 AND lastWatched > 0")
    fun getWatchedCount(): Flow<Int>

    // ==================== فحص التكرار ====================

    @Query("SELECT * FROM videos WHERE isDeleted = 0 AND size = :size AND duration = :duration")
    suspend fun findDuplicatesBySize(size: Long, duration: Long): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE isDeleted = 0 AND name = :name")
    suspend fun findByName(name: String): List<VideoEntity>

    // ==================== MediaStore Sync ====================

    @Query("SELECT path FROM videos WHERE isDeleted = 0")
    suspend fun getAllPaths(): List<String>

    @Query("DELETE FROM videos WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("SELECT * FROM videos WHERE isDeleted = 0 ORDER BY dateAdded DESC LIMIT 10")
    fun getRecentVideos(): Flow<List<VideoEntity>>

    /** استعلام لمرة واحدة (suspend) للمسح الكامل - يستخدمه كاشف التكرار */
    @Query("SELECT * FROM videos WHERE isDeleted = 0 ORDER BY size DESC")
    suspend fun getAllVideosOnce(): List<VideoEntity>
}
