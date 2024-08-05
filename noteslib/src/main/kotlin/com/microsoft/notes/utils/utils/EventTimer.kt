package com.microsoft.notes.utils.utils

class EventTimer {
    private var startTime = 0L

    fun startTimer() {
        startTime = System.currentTimeMillis()
    }

    fun endTimer(): Long {
        val currentTime = System.currentTimeMillis()
        if (startTime <= 0L || currentTime < startTime) {
            return -1
        }

        val timeTaken = currentTime - startTime
        startTime = 0L
        return timeTaken
    }

    fun hasStarted(): Boolean = startTime != 0L
}
