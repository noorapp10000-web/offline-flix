package com.arthenica.ffmpegkit

class FFmpegSession(
    val returnCode: ReturnCode = ReturnCode(1),
    val output: String = ""
) {
    companion object {
        fun failed()  = FFmpegSession(ReturnCode(1))
        fun success() = FFmpegSession(ReturnCode(0))
    }
}
