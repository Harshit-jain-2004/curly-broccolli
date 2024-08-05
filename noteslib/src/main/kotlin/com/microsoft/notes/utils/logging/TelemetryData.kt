package com.microsoft.notes.utils.logging

import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.SyncProperty

private fun createHeaderMap(
    eventName: EventMarkers,
    severityLevel: SeverityLevel,
    isSyncScore: Boolean
): TelemetryEventHeaders {
    val event = if (eventName.category != Categories.None) {
        eventName.category.mCategory() + "." + eventName.name
    } else {
        eventName.name
    }

    val policy = if (isSyncScore) SamplingPolicy.Critical else eventName.samplingPolicy
    val expiration = if (isSyncScore) ExpirationDate.Perpetual else ExpirationDate.HostDefined
    return TelemetryEventHeaders(
        eventName = event,
        samplingPolicy = policy,
        expirationDate = expiration,
        severityLevel = severityLevel,
        costPriority = eventName.costPriority,
        persistencePriority = eventName.persistencePriority,
        diagnosticLevel = eventName.diagnosticLevel,
        dataCategory = eventName.dataCategory
    )
}

private fun createPropertyMap(
    eventName: EventMarkers,
    vararg keyValuePairs: Pair<String, String>,
    isSyncScore: Boolean
): HashMap<String, String> {
    val payload = HashMap<String, String>()

    for (keyValuePair in keyValuePairs) {
        payload[keyValuePair.first] = keyValuePair.second
    }

    if (isSyncScore) {
        payload[SyncProperty.IS_SYNC_SCORE] = true.toString()
    }

    if (eventName.isExportable) {
        payload[SyncProperty.IS_EXPORTABLE] = true.toString()
    }

    return payload
}

fun createTelemetryData(
    eventName: EventMarkers,
    vararg keyValuePairs: Pair<String, String>,
    severityLevel: SeverityLevel,
    isSyncScore: Boolean
): TelemetryData {
    val headers = createHeaderMap(
        eventName = eventName,
        severityLevel = severityLevel,
        isSyncScore = isSyncScore
    )
    val data = createPropertyMap(
        eventName = eventName,
        keyValuePairs = *keyValuePairs,
        isSyncScore = isSyncScore
    )
    return TelemetryData(headers, data)
}

data class TelemetryData(
    @JvmField val eventHeaders: TelemetryEventHeaders,
    @JvmField val eventProperties: HashMap<String, String>
)
