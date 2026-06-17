package com.offlineflix.player.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * كيان الأغنية في قاعدة البيانات
 */
@Entity(
    tableName = "audio_tracks",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["albumId"]),
        Index(value = ["artistId"])
    ]
)
data class AudioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val path: String,
    val name: String,
    val title: String = name,
    val artist: String = "فنان غير معروف",
    val album: String = "ألبوم غير معروف",
    val albumId: Long = 0,
    val artistId: Long = 0,
    val duration: Long = 0,
    val size: Long = 0,
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val mimeType: String = "",
    val trackNumber: Int = 0,
    val year: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayed: Long = 0,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val mediaStoreId: Long = 0,
    val albumArtPath: String = "",
    val genre: String = "",
    val composer: String = "",
    val lyrics: String = "",
    val lrcPath: String = "",
    val folderPath: String = ""
)
