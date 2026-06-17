package com.arthenica.ffmpegkit

data class Statistics(
    val sessionId: Long = 0,
    val videoFrameNumber: Int = 0,
    val videoFps: Float = 0f,
    val videoQuality: Float = 0f,
    val size: Long = 0,
    val time: Int = 0,
    val bitrate: Double = 0.0,
    val speed: Double = 0.0
)
