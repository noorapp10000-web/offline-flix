package com.offlineflix.player.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.offlineflix.player.data.local.db.AppDatabase
import com.offlineflix.player.data.local.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * وحدة Hilt الرئيسية - توفير التبعيات
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** توفير قاعدة البيانات */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideVideoDao(db: AppDatabase): VideoDao = db.videoDao()
    @Provides fun provideAudioDao(db: AppDatabase): AudioDao = db.audioDao()
    @Provides fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()
    @Provides fun providePdfDao(db: AppDatabase): PdfDao = db.pdfDao()
    @Provides fun provideFolderDao(db: AppDatabase): FolderDao = db.folderDao()
    @Provides fun provideTrashDao(db: AppDatabase): TrashDao = db.trashDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
