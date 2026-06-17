package com.offlineflix.player.data.repository

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.offlineflix.player.data.local.db.dao.AudioDao
import com.offlineflix.player.data.local.db.dao.PlaylistDao
import com.offlineflix.player.data.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مستودع الأغاني والموسيقى
 */
@Singleton
class AudioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioDao: AudioDao,
    private val playlistDao: PlaylistDao
) {
    fun getAllTracks(): Flow<List<AudioEntity>> = audioDao.getAllTracks()
    fun getAllAlbums(): Flow<List<String>> = audioDao.getAllAlbums()
    fun getAllArtists(): Flow<List<String>> = audioDao.getAllArtists()
    fun getTracksByAlbum(album: String): Flow<List<AudioEntity>> = audioDao.getTracksByAlbum(album)
    fun getTracksByArtist(artist: String): Flow<List<AudioEntity>> = audioDao.getTracksByArtist(artist)
    fun getTracksByFolder(folder: String): Flow<List<AudioEntity>> = audioDao.getTracksByFolder(folder)
    fun getAllAudioFolders(): Flow<List<String>> = audioDao.getAllFolders()
    fun getFavoriteTracks(): Flow<List<AudioEntity>> = audioDao.getFavoriteTracks()
    fun searchTracks(query: String): Flow<List<AudioEntity>> = audioDao.searchTracks(query)
    fun getRecentlyPlayed(): Flow<List<AudioEntity>> = audioDao.getRecentlyPlayed()
    fun getMostPlayed(): Flow<List<AudioEntity>> = audioDao.getMostPlayed()
    fun getTrackCount(): Flow<Int> = audioDao.getTrackCount()
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    fun getPlaylistWithTracks(id: Long): Flow<PlaylistWithAudio?> = playlistDao.getPlaylistWithTracks(id)

    suspend fun getById(id: Long): AudioEntity? = audioDao.getById(id)

    /**
     * مسح كل الأغاني من MediaStore
     */
    suspend fun scanAllAudio(): Int = withContext(Dispatchers.IO) {
        var count = 0
        val supportedExts = setOf("mp3", "flac", "wav", "aac", "m4a", "ogg", "opus", "wma", "ape", "alac", "aiff", "mid", "midi", "ac3", "dts")

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.BITRATE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.BUCKET_RELATIVE_PATH
        )

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        context.contentResolver.query(uri, projection, null, null, "${MediaStore.Audio.Media.TITLE} ASC")
            ?.use { cursor ->
                val tracks = mutableListOf<AudioEntity>()

                while (cursor.moveToNext()) {
                    try {
                        val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)) ?: continue
                        if (!File(path).exists()) continue

                        val lrcPath = path.substringBeforeLast(".") + ".lrc"
                        val albumArtPath = getAlbumArtPath(
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                        )

                        tracks.add(AudioEntity(
                            path = path,
                            name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)) ?: "",
                            title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)) ?: "",
                            artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)) ?: "فنان غير معروف",
                            album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)) ?: "ألبوم غير معروف",
                            albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)),
                            artistId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)),
                            duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
                            size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)),
                            bitrate = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)),
                            dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)) * 1000,
                            trackNumber = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)),
                            year = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)),
                            mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)) ?: "",
                            composer = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER)) ?: "",
                            mediaStoreId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)),
                            folderPath = File(path).parent ?: "",
                            albumArtPath = albumArtPath,
                            lrcPath = if (File(lrcPath).exists()) lrcPath else ""
                        ))
                        count++
                    } catch (e: Exception) { }
                }

                audioDao.insertAll(tracks)
            }

        count
    }

    private fun getAlbumArtPath(albumId: Long): String {
        return try {
            val uri = android.net.Uri.parse("content://media/external/audio/albumart/$albumId")
            uri.toString()
        } catch (e: Exception) { "" }
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = audioDao.updateFavorite(id, isFavorite)
    suspend fun updateLastPlayed(id: Long) = audioDao.updateLastPlayed(id, System.currentTimeMillis())

    // قوائم التشغيل
    suspend fun createPlaylist(name: String): Long = playlistDao.insertPlaylist(PlaylistEntity(name = name))
    suspend fun deletePlaylist(playlist: PlaylistEntity) = playlistDao.deletePlaylist(playlist)
    suspend fun addTrackToPlaylist(playlistId: Long, audioId: Long) {
        playlistDao.insertCrossRef(PlaylistAudioCrossRef(playlistId, audioId))
        playlistDao.updateTrackCount(playlistId)
    }
    suspend fun removeTrackFromPlaylist(playlistId: Long, audioId: Long) {
        playlistDao.removeTrackFromPlaylist(playlistId, audioId)
        playlistDao.updateTrackCount(playlistId)
    }
}
