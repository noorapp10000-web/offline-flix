package com.offlineflix.player.data.local.db

import androidx.room.TypeConverter
import com.offlineflix.player.data.models.PlaylistType

/** محولات أنواع البيانات لـ Room */
class Converters {

    @TypeConverter
    fun fromPlaylistType(type: PlaylistType): String = type.name

    @TypeConverter
    fun toPlaylistType(value: String): PlaylistType = PlaylistType.valueOf(value)
}
