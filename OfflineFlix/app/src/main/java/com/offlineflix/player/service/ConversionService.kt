package com.offlineflix.player.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.offlineflix.player.OfflineFlixApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * خدمة تحويل الصيغ في الخلفية
 * تسمح بالخروج من التطبيق أثناء التحويل
 */
@AndroidEntryPoint
class ConversionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = intent?.getStringExtra("command") ?: return START_NOT_STICKY
        val title = intent.getStringExtra("title") ?: "جاري التحويل..."

        startForeground(NOTIFICATION_ID, buildNotification(title, 0))

        Thread {
            FFmpegKit.execute(command)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }.start()

        return START_STICKY
    }

    private fun buildNotification(title: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, OfflineFlixApp.CHANNEL_CONVERSION)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1002
    }
}
