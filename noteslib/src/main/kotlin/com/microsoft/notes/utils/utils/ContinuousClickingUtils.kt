package com.microsoft.notes.utils.utils

class ContinuousClickingUtils {

    companion object {
        const val timeIntervalMs: Long = 300
        var lastTimeMs: Long = -1
    }
}

fun isContinuousClicking(): Boolean {
    val currentTime = System.currentTimeMillis()
    if (Math.abs(currentTime - ContinuousClickingUtils.lastTimeMs) <= ContinuousClickingUtils.timeIntervalMs) {
        return true
    }
    ContinuousClickingUtils.lastTimeMs = currentTime
    return false
}
