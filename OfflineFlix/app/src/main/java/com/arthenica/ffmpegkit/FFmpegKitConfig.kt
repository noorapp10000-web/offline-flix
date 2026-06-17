package com.arthenica.ffmpegkit

object FFmpegKitConfig {
    fun enableLogCallback(callback: ((Any?) -> Unit)?) {}
    fun enableStatisticsCallback(callback: ((Statistics?) -> Unit)?) {}
    fun setLogLevel(level: Int) {}
}
