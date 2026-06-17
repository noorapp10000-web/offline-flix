package com.offlineflix.player.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.offlineflix.player.data.local.db.dao.TrashDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

/**
 * Worker لتنظيف سلة المحذوفات تلقائياً
 * يُنفَّذ يومياً ويحذف الملفات المنتهية (30 يوم)
 */
@HiltWorker
class TrashCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val trashDao: TrashDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val expiredItems = trashDao.getAllTrashItems()
            // حذف الملفات المنتهية الصلاحية
            trashDao.deleteExpiredItems(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
