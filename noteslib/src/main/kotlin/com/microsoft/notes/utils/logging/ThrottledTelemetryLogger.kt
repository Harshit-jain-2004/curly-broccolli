package com.microsoft.notes.utils.logging

data class ThrottledMarker(
    val marker: EventMarkers,
    val throttlingDurationMillis: Long,
    var throttleTillMillis: Long
)

class ThrottledTelemetryLogger(private val telemetryLogger: TelemetryLogger?) {

    private val throttledMarkers = mutableMapOf<EventMarkers, ThrottledMarker>()

    private fun currentTime() = System.currentTimeMillis()

    internal fun isThrottled(marker: EventMarkers) = !throttledMarkers.containsKey(marker)

    internal fun add(marker: EventMarkers, duration: Long): Boolean {
        if (!throttledMarkers.containsKey(marker) && duration > 0) {
            throttledMarkers.put(
                marker,
                ThrottledMarker(marker, duration, currentTime())
            )
            return true
        }

        return false
    }

    private fun throttledTransmission(marker: EventMarkers, isSyncScore: Boolean, transmit: () -> Unit) {
        val throttledMarker = throttledMarkers[marker]
        if (!isSyncScore && throttledMarker != null) {
            if (throttledMarker.throttleTillMillis <= currentTime()) {
                transmit()
                throttledMarker.throttleTillMillis = currentTime() + throttledMarker.throttlingDurationMillis
            }
        } else {
            transmit()
        }
    }

    internal fun recordTelemetry(
        eventMarker: EventMarkers,
        vararg keyValuePairs: Pair<String, String>,
        severityLevel: SeverityLevel,
        isSyncScore: Boolean
    ) {
        throttledTransmission(eventMarker, isSyncScore) {
            val telemetryData = createTelemetryData(
                eventName = eventMarker,
                keyValuePairs = *keyValuePairs,
                severityLevel = severityLevel,
                isSyncScore = isSyncScore
            )

            if (isSyncScore) {
                telemetryLogger?.logEventSyncScore(telemetryData)
            } else {
                telemetryLogger?.logEvent(telemetryData)
            }
        }
    }
}
