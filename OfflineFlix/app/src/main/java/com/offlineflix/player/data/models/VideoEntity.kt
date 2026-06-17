package com.offlineflix.player.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * كيان الفيديو في قاعدة البيانات
 * يحتوي على كل معلومات الفيديو المحلي
 */
@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["folderId"]),
        Index(value = ["dateAdded"]),
        Index(value = ["lastWatched"])
    ]
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** مسار الملف الكامل */
    val path: String,

    /** اسم الملف */
    val name: String,

    /** اسم العرض (قابل للتعديل) */
    val displayName: String = name,

    /** حجم الملف بالبايت */
    val size: Long,

    /** مدة الفيديو بالمللي ثانية */
    val duration: Long,

    /** عرض الفيديو بالبكسل */
    val width: Int = 0,

    /** ارتفاع الفيديو بالبكسل */
    val height: Int = 0,

    /** معدل البت */
    val bitrate: Long = 0,

    /** ترميز الفيديو (H.264, HEVC, etc.) */
    val videoCodec: String = "",

    /** ترميز الصوت */
    val audioCodec: String = "",

    /** معدل الإطارات */
    val frameRate: Float = 0f,

    /** معرف المجلد */
    val folderId: Long = 0,

    /** مسار المجلد */
    val folderPath: String = "",

    /** تاريخ الإضافة (Unix timestamp) */
    val dateAdded: Long = System.currentTimeMillis(),

    /** تاريخ التعديل */
    val dateModified: Long = System.currentTimeMillis(),

    /** آخر وقت مشاهدة */
    val lastWatched: Long = 0,

    /** آخر موضع تشغيل (للاستكمال) */
    val lastPosition: Long = 0,

    /** هل تمت المشاهدة بالكامل */
    val isWatched: Boolean = false,

    /** نسبة المشاهدة (0-100) */
    val watchProgress: Int = 0,

    /** التقييم بالنجوم (0-5) */
    val rating: Int = 0,

    /** ملاحظات المستخدم */
    val notes: String = "",

    /** هل في المفضلة */
    val isFavorite: Boolean = false,

    /** هل في سلة المحذوفات */
    val isDeleted: Boolean = false,

    /** تاريخ الحذف (للسلة) */
    val deletedAt: Long = 0,

    /** MediaStore ID */
    val mediaStoreId: Long = 0,

    /** نوع الصيغة */
    val mimeType: String = "",

    /** هل فيديو 4K */
    val is4K: Boolean = width >= 3840,

    /** هل فيديو HDR */
    val isHdr: Boolean = false,

    /** عدد مرات المشاهدة */
    val viewCount: Int = 0,

    /** مسار الصورة المصغرة المخزنة */
    val thumbnailPath: String = "",

    /** البيانات الإضافية (JSON) */
    val metadata: String = ""
)
