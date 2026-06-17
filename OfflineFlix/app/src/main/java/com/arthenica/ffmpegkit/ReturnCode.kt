package com.arthenica.ffmpegkit

class ReturnCode(val value: Int) {
    companion object {
        private const val SUCCESS = 0
        private const val CANCEL  = 255

        @JvmStatic fun isSuccess(rc: ReturnCode?) = rc?.value == SUCCESS
        @JvmStatic fun isCancel(rc: ReturnCode?)  = rc?.value == CANCEL
    }
    fun getValue() = value
}
