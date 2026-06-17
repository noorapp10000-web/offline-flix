package com.offlineflix.player.data.local.db.dao

import androidx.room.*
import com.offlineflix.player.data.models.PdfEntity
import com.offlineflix.player.data.models.PdfBookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO لملفات PDF
 */
@Dao
interface PdfDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pdf: PdfEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pdfs: List<PdfEntity>)

    @Update
    suspend fun update(pdf: PdfEntity)

    @Query("SELECT * FROM pdf_files WHERE isDeleted = 0 ORDER BY lastOpened DESC")
    fun getAllPdfs(): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdf_files WHERE isDeleted = 0 AND id = :id")
    suspend fun getById(id: Long): PdfEntity?

    @Query("SELECT * FROM pdf_files WHERE isDeleted = 0 AND path = :path")
    suspend fun getByPath(path: String): PdfEntity?

    @Query("UPDATE pdf_files SET lastOpenedPage = :page, lastOpened = :timestamp WHERE id = :id")
    suspend fun updateLastPage(id: Long, page: Int, timestamp: Long)

    @Query("UPDATE pdf_files SET isFavorite = :fav WHERE id = :id")
    suspend fun updateFavorite(id: Long, fav: Boolean)

    @Query("SELECT * FROM pdf_files WHERE isDeleted = 0 AND (name LIKE '%' || :q || '%') ORDER BY name ASC")
    fun searchPdfs(q: String): Flow<List<PdfEntity>>

    // Bookmarks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: PdfBookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: PdfBookmarkEntity)

    @Query("SELECT * FROM pdf_bookmarks WHERE pdfId = :pdfId ORDER BY pageNumber ASC")
    fun getBookmarks(pdfId: Long): Flow<List<PdfBookmarkEntity>>

    @Query("SELECT path FROM pdf_files WHERE isDeleted = 0")
    suspend fun getAllPaths(): List<String>
}
