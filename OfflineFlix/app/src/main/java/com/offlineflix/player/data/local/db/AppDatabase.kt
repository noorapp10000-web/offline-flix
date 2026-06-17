package com.offlineflix.player.data.local.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.offlineflix.player.data.models.*
import com.offlineflix.player.data.local.db.dao.*

/**
 * قاعدة بيانات OfflineFlix الرئيسية
 * تستخدم Room لتخزين كل الوسائط والبيانات محلياً
 */
@Database(
    entities = [
        VideoEntity::class,
        AudioEntity::class,
        PlaylistEntity::class,
        PlaylistAudioCrossRef::class,
        PdfEntity::class,
        PdfBookmarkEntity::class,
        FolderEntity::class,
        ScheduledDeletionEntity::class,
        TrashEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoDao
    abstract fun audioDao(): AudioDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun pdfDao(): PdfDao
    abstract fun folderDao(): FolderDao
    abstract fun trashDao(): TrashDao

    companion object {
        const val DATABASE_NAME = "offlineflix.db"
    }
}
