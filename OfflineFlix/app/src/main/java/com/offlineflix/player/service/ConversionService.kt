package com.offlineflix.player.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Statistics
import com.offlineflix.player.OfflineFlixApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * خدمة تحويل الصيغ في الخلفية
 * تتابع تقدم FFmpeg وتعرضه في الإشعار
 * تسمح بالخروج من التطبيق أثناء التحويل
 */
@AndroidEntryPoint
class ConversionService : Service() {

    private var notificationManager: NotificationManager? = null
    private var totalDurationMs: Long = 0

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = intent?.getStringExtra("command") ?: return START_NOT_STICKY
        val title = intent.getStringExtra("title") ?: "جاري التحويل..."
        totalDurationMs = intent.getLongExtra("durationMs", 0L)

        startForeground(NOTIFICATION_ID, buildNotification(title, 0, false))

        // تنفيذ FFmpeg مع تتبع التقدم عبر الإحصائيات
        FFmpegKit.executeAsync(
            command,
            { session ->
                // انتهاء التحويل
                val success = ReturnCode.isSuccess(session.returnCode)
                val resultTitle = if (success) "✅ $title - اكتمل" else "❌ $title - فشل"
                notificationManager?.notify(NOTIFICATION_ID, buildNotification(resultTitle, 100, false))
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            },
            { _ ->
                // سجل FFmpeg - تجاهل
            },
            { stats: Statistics ->
                // حساب نسبة التقدم من وقت FFmpeg الحالي
                val progress = if (totalDurationMs > 0 && stats.time > 0) {
                    ((stats.time.toFloat() / totalDurationMs) * 100).toInt().coerceIn(0, 99)
                } else {
                    -1 // غير محدد
                }
                val speedText = if (stats.speed > 0) " • ${String.format("%.1f", stats.speed)}x" else ""
                val sizeText = if (stats.size > 0) " • ${stats.size / 1024}KB" else ""
                val progressTitle = "$title$speedText$sizeText"
                notificationManager?.notify(
                    NOTIFICATION_ID,
                    buildNotification(progressTitle, progress, progress < 0)
                )
            }
        )

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        FFmpegKit.cancel()
    }

    /**
     * بناء إشعار مع شريط التقدم
     * @param title عنوان الإشعار
     * @param progress نسبة التقدم (0-100)، أو -1 للغير محدد
     * @param indeterminate هل التقدم غير محدد
     */
    private fun buildNotification(title: String, progress: Int, indeterminate: Boolean): Notification {
        return NotificationCompat.Builder(this, OfflineFlixApp.CHANNEL_CONVERSION)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(if (progress in 0..99) "$progress%" else "جاري التحويل...")
            .setProgress(100, progress.coerceAtLeast(0), indeterminate || progress < 0)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1002

        /** إنشاء Intent لبدء الخدمة */
        fun buildIntent(
            context: android.content.Context,
            command: String,
            title: String,
            durationMs: Long = 0L
        ) = Intent(context, ConversionService::class.java).apply {
            putExtra("command", command)
            putExtra("title", title)
            putExtra("durationMs", durationMs)
        }
    }
}
