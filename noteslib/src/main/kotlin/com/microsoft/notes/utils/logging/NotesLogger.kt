package com.microsoft.notes.utils.logging

private const val DEFAULT_TAG: String = "NotesSDK"

data class NotesLogger(private val logger: Logger? = null, private val telemetryLogger: TelemetryLogger? = null) {
    private val throttledTelemetryLogger = ThrottledTelemetryLogger(
        telemetryLogger
    )

    init {
        throttledTelemetryLogger.add(EventMarkers.NoteContentUpdated, 1 * 60 * 1000 /*ms*/)
    }

    fun d(tag: String = DEFAULT_TAG, message: String, exception: Throwable? = null) {
        logger?.d(tag, message, exception)
    }

    fun v(tag: String = DEFAULT_TAG, message: String, exception: Throwable? = null) {
        logger?.v(tag, message, exception)
    }

    fun e(tag: String = DEFAULT_TAG, message: String, exception: Throwable? = null) {
        logger?.e(tag, message, exception)
    }

    fun i(tag: String = DEFAULT_TAG, message: String, exception: Throwable? = null) {
        logger?.i(tag, message, exception)
    }

    fun w(tag: String = DEFAULT_TAG, message: String, exception: Throwable? = null) {
        logger?.w(tag, message, exception)
    }

    fun recordTelemetry(
        eventMarker: EventMarkers,
        vararg keyValuePairs: Pair<String, String>,
        severityLevel: SeverityLevel = SeverityLevel.Info,
        isSyncScore: Boolean = false
    ) {
        throttledTelemetryLogger.recordTelemetry(
            eventMarker = eventMarker,
            keyValuePairs = *keyValuePairs,
            severityLevel = severityLevel,
            isSyncScore = isSyncScore
        )
    }
}
