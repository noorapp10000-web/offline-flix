package com.offlineflix.player.data.local.db.dao

import androidx.room.*
import com.offlineflix.player.data.models.TrashEntity
import com.offlineflix.player.data.models.ScheduledDeletionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO لسلة المحذوفات والجدولة
 */
@Dao
interface TrashDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrash(item: TrashEntity): Long

    @Delete
    suspend fun deleteTrash(item: TrashEntity)

    @Query("SELECT * FROM trash_bin ORDER BY deletedAt DESC")
    fun getAllTrashItems(): Flow<List<TrashEntity>>

    @Query("DELETE FROM trash_bin WHERE expiresAt < :now")
    suspend fun deleteExpiredItems(now: Long)

    @Query("SELECT * FROM trash_bin WHERE id = :id")
    suspend fun getTrashById(id: Long): TrashEntity?

    @Query("DELETE FROM trash_bin")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM trash_bin")
    fun getTrashCount(): Flow<Int>

    // Scheduled deletions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduled(item: ScheduledDeletionEntity): Long

    @Delete
    suspend fun deleteScheduled(item: ScheduledDeletionEntity)

    @Query("SELECT * FROM scheduled_deletions WHERE deleteAt <= :now")
    suspend fun getItemsDueForDeletion(now: Long): List<ScheduledDeletionEntity>

    @Query("SELECT * FROM scheduled_deletions ORDER BY deleteAt ASC")
    fun getAllScheduled(): Flow<List<ScheduledDeletionEntity>>
}
