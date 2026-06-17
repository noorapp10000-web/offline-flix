package com.offlineflix.player.data.local.db.dao

import androidx.room.*
import com.offlineflix.player.data.models.FolderEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO للمجلدات
 */
@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<FolderEntity>)

    @Update
    suspend fun update(folder: FolderEntity)

    @Query("SELECT * FROM folders ORDER BY videoCount DESC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE path = :path")
    suspend fun getByPath(path: String): FolderEntity?

    @Query("UPDATE folders SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: Long, hidden: Boolean)

    @Query("UPDATE folders SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("SELECT COUNT(*) FROM folders")
    fun getFolderCount(): Flow<Int>
}
