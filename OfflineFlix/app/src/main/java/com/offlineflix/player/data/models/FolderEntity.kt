package com.offlineflix.player.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * كيان المجلد
 */
@Entity(
    tableName = "folders",
    indices = [Index(value = ["path"], unique = true)]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val name: String,
    val videoCount: Int = 0,
    val audioCount: Int = 0,
    val totalSize: Long = 0,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val lastScanned: Long = System.currentTimeMillis(),
    val thumbnailPath: String = ""
)

/**
 * كيان عمليات الجدولة
 */
@Entity(tableName = "scheduled_deletions")
data class ScheduledDeletionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val fileType: String,
    val deleteAt: Long,
    val scheduledAt: Long = System.currentTimeMillis(),
    val reason: String = "جدولة يدوية"
)

/**
 * كيان الملف المحذوف (سلة المحذوفات)
 */
@Entity(tableName = "trash_bin")
data class TrashEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalPath: String,
    val name: String,
    val size: Long,
    val type: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
    val thumbnailPath: String = ""
)
