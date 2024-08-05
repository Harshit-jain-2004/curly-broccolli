package com.microsoft.notes.sampleapp.logging

import android.util.Log
import com.microsoft.notes.utils.logging.TelemetryData
import com.microsoft.notes.utils.logging.TelemetryLogger

class StickyNotesTelemetryLogger : TelemetryLogger {
    override fun logEvent(telemetryData: TelemetryData) {
        log(telemetryData)
    }

    override fun logEventSyncScore(telemetryData: TelemetryData) {
        log(telemetryData)
    }

    private fun log(telemetryData: TelemetryData) {
        val telemetryStringBuilder = StringBuilder()
        val eventHeaders = telemetryData.eventHeaders
        val eventProperties = telemetryData.eventProperties

        telemetryStringBuilder.append("EventName: ").append(eventHeaders.eventName).append("\n")
        telemetryStringBuilder.append("Severity: ").append(eventHeaders.severityLevel).append("\n")
        telemetryStringBuilder.append("SamplingPolicy: ${eventHeaders.samplingPolicy}").append("\n")
        telemetryStringBuilder.append("ExpirationDate: ${eventHeaders.expirationDate}").append("\n")

        telemetryStringBuilder.append("EventProperties :{ ").append("\n")
        for (eventProperty in eventProperties) {
            telemetryStringBuilder.append(eventProperty.key).append("=").append(eventProperty.value).append(" \n")
        }
        telemetryStringBuilder.append("} \n")

        Log.i("NotesSDKTelemetry", telemetryStringBuilder.toString())
    }
}