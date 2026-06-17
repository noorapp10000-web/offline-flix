package com.offlineflix.player.data.local.db.dao

import androidx.room.*
import com.offlineflix.player.data.models.AudioEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO للتعامل مع الأغاني في قاعدة البيانات
 */
@Dao
interface AudioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audio: AudioEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(audios: List<AudioEntity>)

    @Update
    suspend fun update(audio: AudioEntity)

    @Delete
    suspend fun delete(audio: AudioEntity)

    @Query("SELECT * FROM audio_tracks WHERE isDeleted = 0 ORDER BY title ASC")
    fun getAllTracks(): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio_tracks WHERE isDeleted = 0 AND id = :id")
    suspend fun getById(id: Long): AudioEntity?

    @Query("SELECT * FROM audio_tracks WHERE isDeleted = 0 AND path = :path")
    suspend fun getByPath(path: String): AudioEntity?

    @Query("SELECT DISTINCT album FROM audio_tracks WHERE isDeleted = 0 ORDER BY album ASC")
    fun getAllAlbums(): Flow<List<String>>

    @Query("SELECT DISTINCT artist FROM audio_tracks WHERE isDeleted = 0 ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>

    @Query("SELECT * FROM audio_tracks WHERE isDeleted = 0 AND album = :album ORDER BY trackNumber ASC")
    fun getTracksByAlbum(album: String): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio_tracks WHERE isDeleted = 0 AND artist = :artist ORDER BY title ASC")
    fun getTracksByArtist(artist: String): Flow<List<AudioEntity>>

    @Query("SELECT DISTINCT folderPath FROM audio_tracks WHERE isDeleted = 0 ORDER BY folderPath ASC")
    fun getAllFolders(): Flow<List<String>>

    @Query("SELECT * FROM audio_tracks WHERE isDeleted = 0 AND folderPath = :folder ORDER BY title ASC")
    fun getTracksByFolder(folder: String): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio_tracks WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteTracks(): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio_tracks WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%') ORDER BY title ASC")
    fun searchTracks(query: String): Flow<List<AudioEntity>>

    @Query("UPDATE audio_tracks SET lastPlayed = :timestamp, playCount = playCount + 1 WHERE id = :id")
    suspend fun updateLastPlayed(id: Long, timestamp: Long)

    @Query("UPDATE audio_tracks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE audio_tracks SET albumArtPath = :path WHERE albumId = :albumId")
    suspend fun updateAlbumArt(albumId: Long, path: String)

    @Query("UPDATE audio_tracks SET lrcPath = :lrcPath WHERE id = :id")
    suspend fun updateLrcPath(id: Long, lrcPath: String)

    @Query("SELECT COUNT(*) FROM audio_tracks WHERE isDeleted = 0")
    fun getTrackCount(): Flow<Int>

    @Query("SELECT * FROM audio_tracks WHERE isDeleted = 0 ORDER BY lastPlayed DESC LIMIT 50")
    fun getRecentlyPlayed(): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio_tracks WHERE isDeleted = 0 ORDER BY playCount DESC LIMIT 50")
    fun getMostPlayed(): Flow<List<AudioEntity>>

    @Query("SELECT path FROM audio_tracks WHERE isDeleted = 0")
    suspend fun getAllPaths(): List<String>
}
