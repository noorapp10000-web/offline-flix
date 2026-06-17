package com.arthenica.ffmpegkit

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

typealias ExecuteCallback     = (FFmpegSession) -> Unit
typealias LogCallback         = (Any?) -> Unit
typealias StatisticsCallback  = (Statistics) -> Unit

object FFmpegKit {

    private const val TAG = "FFmpegKit-Stub"

    @Volatile private var cancelled = false

    @JvmStatic
    fun execute(command: String): FFmpegSession {
        Log.w(TAG, "FFmpegKit stub — command not executed: $command")
        return FFmpegSession.failed()
    }

    @JvmStatic
    fun executeAsync(
        command: String,
        completeCallback: ExecuteCallback?,
        logCallback: LogCallback? = null,
        statisticsCallback: StatisticsCallback? = null
    ): FFmpegSession {
        Log.w(TAG, "FFmpegKit stub (async) — command not executed: $command")
        cancelled = false
        CoroutineScope(Dispatchers.IO).launch {
            val session = FFmpegSession.failed()
            completeCallback?.invoke(session)
        }
        return FFmpegSession.failed()
    }

    @JvmStatic
    fun cancel() {
        cancelled = true
        Log.w(TAG, "FFmpegKit stub — cancel called")
    }

    @JvmStatic
    fun cancelSession(sessionId: Long) = cancel()
}
