package com.offlineflix.player.data.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * كيان قائمة التشغيل
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val coverPath: String = "",
    val trackCount: Int = 0,
    val isDefault: Boolean = false,
    val type: PlaylistType = PlaylistType.AUDIO
)

enum class PlaylistType { AUDIO, VIDEO }

/**
 * كيان علاقة قائمة التشغيل والأغاني
 */
@Entity(
    tableName = "playlist_audio_cross_ref",
    primaryKeys = ["playlistId", "audioId"]
)
data class PlaylistAudioCrossRef(
    val playlistId: Long,
    val audioId: Long,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * كيان قائمة تشغيل مع الأغاني
 */
data class PlaylistWithAudio(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistAudioCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "audioId"
        )
    )
    val tracks: List<AudioEntity>
)
