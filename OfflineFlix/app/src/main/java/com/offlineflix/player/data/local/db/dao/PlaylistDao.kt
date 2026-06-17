package com.offlineflix.player.data.local.db.dao

import androidx.room.*
import com.offlineflix.player.data.models.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO لقوائم التشغيل
 */
@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: PlaylistAudioCrossRef)

    @Delete
    suspend fun deleteCrossRef(crossRef: PlaylistAudioCrossRef)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistWithTracks(id: Long): Flow<PlaylistWithAudio?>

    @Query("UPDATE playlists SET trackCount = (SELECT COUNT(*) FROM playlist_audio_cross_ref WHERE playlistId = :playlistId) WHERE id = :playlistId")
    suspend fun updateTrackCount(playlistId: Long)

    @Query("DELETE FROM playlist_audio_cross_ref WHERE playlistId = :playlistId AND audioId = :audioId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, audioId: Long)

    @Query("SELECT COUNT(*) FROM playlists")
    fun getPlaylistCount(): Flow<Int>
}
