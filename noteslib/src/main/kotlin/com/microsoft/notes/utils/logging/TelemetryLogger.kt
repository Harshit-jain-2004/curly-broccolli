package com.microsoft.notes.utils.logging

interface TelemetryLogger {
    fun logEvent(telemetryData: TelemetryData)
    fun logEventSyncScore(telemetryData: TelemetryData)
}
