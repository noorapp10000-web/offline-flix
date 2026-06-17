package com.offlineflix.player

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * كلاس التطبيق الرئيسي - نقطة البداية
 * يُهيئ Hilt و WorkManager و قنوات الإشعارات
 */
@HiltAndroidApp
class OfflineFlixApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /** تهيئة WorkManager مع Hilt */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    /** إنشاء قنوات الإشعارات */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // قناة تشغيل الوسائط
            NotificationChannel(
                CHANNEL_MEDIA_PLAYBACK,
                "تشغيل الوسائط",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "التحكم في تشغيل الفيديو والموسيقى"
                setShowBadge(false)
                notificationManager.createNotificationChannel(this)
            }

            // قناة تحويل الملفات
            NotificationChannel(
                CHANNEL_CONVERSION,
                "تحويل الملفات",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تقدم عمليات تحويل الصيغ والضغط"
                setShowBadge(true)
                notificationManager.createNotificationChannel(this)
            }

            // قناة مسح الملفات
            NotificationChannel(
                CHANNEL_SCANNING,
                "مسح الملفات",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "مسح وفهرسة الوسائط على الجهاز"
                setShowBadge(false)
                notificationManager.createNotificationChannel(this)
            }

            // قناة التنبيهات العامة
            NotificationChannel(
                CHANNEL_GENERAL,
                "إشعارات عامة",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات التطبيق العامة"
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    companion object {
        const val CHANNEL_MEDIA_PLAYBACK = "media_playback"
        const val CHANNEL_CONVERSION = "file_conversion"
        const val CHANNEL_SCANNING = "file_scanning"
        const val CHANNEL_GENERAL = "general"
    }
}
