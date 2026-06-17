package com.offlineflix.player.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker لتنفيذ أوامر FFmpeg في الخلفية
 */
@HiltWorker
class FfmpegWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val command = inputData.getString(KEY_COMMAND) ?: return Result.failure()
        val notificationTitle = inputData.getString(KEY_TITLE) ?: "FFmpeg"

        setForeground(createForegroundInfo(notificationTitle))

        var success = false
        val session = FFmpegKit.execute(command)
        success = ReturnCode.isSuccess(session.returnCode)

        return if (success) {
            Result.success(workDataOf(KEY_OUTPUT to inputData.getString(KEY_OUTPUT)))
        } else {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun createForegroundInfo(title: String): ForegroundInfo {
        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, "file_conversion")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setProgress(100, 50, true)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    companion object {
        const val KEY_COMMAND = "ffmpeg_command"
        const val KEY_TITLE = "notification_title"
        const val KEY_OUTPUT = "output_path"
        private const val NOTIFICATION_ID = 1001
    }
}
