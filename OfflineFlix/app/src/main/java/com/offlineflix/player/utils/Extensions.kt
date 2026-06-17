package com.offlineflix.player.utils

import kotlin.math.abs

/**
 * دوال مساعدة لتنسيق البيانات
 */

/** تنسيق المدة من مللي ثانية إلى نص مقروء */
fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

/** تنسيق الحجم من بايت إلى نص مقروء */
fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    return when {
        bytes >= 1_073_741_824L -> String.format("%.1f GB", bytes / 1_073_741_824f)
        bytes >= 1_048_576L -> String.format("%.1f MB", bytes / 1_048_576f)
        bytes >= 1_024L -> String.format("%.1f KB", bytes / 1_024f)
        else -> "$bytes B"
    }
}

/** تنسيق معدل البت */
fun formatBitrate(bps: Long): String {
    return when {
        bps >= 1_000_000L -> String.format("%.1f Mbps", bps / 1_000_000f)
        bps >= 1_000L -> String.format("%.0f Kbps", bps / 1_000f)
        else -> "$bps bps"
    }
}

/** حساب نسبة مئوية */
fun Float.toPercent(): Int = (this * 100).toInt()

/** تنسيق تاريخ إضافي */
fun Long.toRelativeTime(): String {
    val diff = System.currentTimeMillis() - this
    return when {
        diff < 60_000 -> "الآن"
        diff < 3_600_000 -> "${diff / 60_000} دقيقة"
        diff < 86_400_000 -> "${diff / 3_600_000} ساعة"
        diff < 604_800_000 -> "${diff / 86_400_000} يوم"
        diff < 2_592_000_000 -> "${diff / 604_800_000} أسبوع"
        else -> "${diff / 2_592_000_000} شهر"
    }
}
