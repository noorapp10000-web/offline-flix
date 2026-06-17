package com.offlineflix.player.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * كيان ملف PDF في قاعدة البيانات
 */
@Entity(
    tableName = "pdf_files",
    indices = [Index(value = ["path"], unique = true)]
)
data class PdfEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int = 0,
    val lastOpenedPage: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastOpened: Long = 0,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val bookmarks: String = "[]",
    val notes: String = "",
    val mediaStoreId: Long = 0,
    val folderPath: String = ""
)

/**
 * كيان علامة مرجعية PDF
 */
@Entity(
    tableName = "pdf_bookmarks",
    indices = [Index(value = ["pdfId", "pageNumber"])]
)
data class PdfBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pdfId: Long,
    val pageNumber: Int,
    val title: String = "صفحة $pageNumber",
    val createdAt: Long = System.currentTimeMillis(),
    val note: String = ""
)
