package com.offlineflix.player.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.offlineflix.player.data.repository.AudioRepository
import com.offlineflix.player.data.repository.VideoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker لمسح الوسائط في الخلفية
 */
@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val videoRepository: VideoRepository,
    private val audioRepository: AudioRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            setProgress(workDataOf("progress" to 10, "phase" to "مسح الفيديوهات..."))
            val videoCount = videoRepository.scanAllMedia()

            setProgress(workDataOf("progress" to 60, "phase" to "مسح الأغاني..."))
            val audioCount = audioRepository.scanAllAudio()

            setProgress(workDataOf("progress" to 100, "phase" to "اكتمل"))
            Result.success(workDataOf("videos" to videoCount, "audio" to audioCount))
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
