package com.microsoft.notes.utils.logging

import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class TelemetryDataTest {

    private fun getEventNameString(eventName: EventMarkers): String {
        return if (eventName.category == Categories.None) {
            eventName.name
        } else {
            eventName.category.mCategory() + "." + eventName.name
        }
    }

    @Test
    fun should_check_header_properties() {
        val severityLevel: SeverityLevel = SeverityLevel.Info
        val eventName = EventMarkers.NoteCreated

        val telemetryData = createTelemetryData(
            eventName = eventName,
            severityLevel = severityLevel,
            isSyncScore = false
        )

        assertThat(telemetryData.eventHeaders.eventName, iz(getEventNameString(eventName)))
        assertThat(telemetryData.eventHeaders.severityLevel, iz(severityLevel))
    }
}
