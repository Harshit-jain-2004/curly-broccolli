package com.microsoft.notes.richtext.editor.extensions

import com.microsoft.notes.models.Color
import org.junit.Assert.assertThat
import org.junit.Test
import com.microsoft.notes.utils.logging.NoteColor as TelemetryColor
import org.hamcrest.CoreMatchers.`is` as iz

class ModelExtensionsTest {

    @Test
    fun `should parse GREY store Color to telemetry Grey Color`() {
        val storeColor: Color = Color.GREY
        assertThat(storeColor.toTelemetryColor(), iz(TelemetryColor.Grey))
        assertThat(storeColor.toTelemetryColorAsString(), iz("Grey"))
        assertThat(storeColor.toTelemetryColorAsString(), iz(TelemetryColor.Grey.name))
    }

    @Test
    fun `should parse YELLOW store Color to telemetry Yellow Color`() {
        val storeColor: Color = Color.YELLOW
        assertThat(storeColor.toTelemetryColor(), iz(TelemetryColor.Yellow))
        assertThat(storeColor.toTelemetryColorAsString(), iz("Yellow"))
        assertThat(storeColor.toTelemetryColorAsString(), iz(TelemetryColor.Yellow.name))
    }

    @Test
    fun `should parse GREEN store Color to telemetry Green Color`() {
        val storeColor: Color = Color.GREEN
        assertThat(storeColor.toTelemetryColor(), iz(TelemetryColor.Green))
        assertThat(storeColor.toTelemetryColorAsString(), iz("Green"))
        assertThat(storeColor.toTelemetryColorAsString(), iz(TelemetryColor.Green.name))
    }

    @Test
    fun `should parse PINK store Color to telemetry Pink Color`() {
        val storeColor: Color = Color.PINK
        assertThat(storeColor.toTelemetryColor(), iz(TelemetryColor.Pink))
        assertThat(storeColor.toTelemetryColorAsString(), iz("Pink"))
        assertThat(storeColor.toTelemetryColorAsString(), iz(TelemetryColor.Pink.name))
    }

    @Test
    fun `should parse PURPLE store Color to telemetry Purple Color`() {
        val storeColor: Color = Color.PURPLE
        assertThat(storeColor.toTelemetryColor(), iz(TelemetryColor.Purple))
        assertThat(storeColor.toTelemetryColorAsString(), iz("Purple"))
        assertThat(storeColor.toTelemetryColorAsString(), iz(TelemetryColor.Purple.name))
    }

    @Test
    fun `should parse BLUE store Color to telemetry Blue Color`() {
        val storeColor: Color = Color.BLUE
        assertThat(storeColor.toTelemetryColor(), iz(TelemetryColor.Blue))
        assertThat(storeColor.toTelemetryColorAsString(), iz("Blue"))
        assertThat(storeColor.toTelemetryColorAsString(), iz(TelemetryColor.Blue.name))
    }

    @Test
    fun `should parse CHARCOAL store Color to telemetry Charcoal Color`() {
        val storeColor: Color = Color.CHARCOAL
        assertThat(storeColor.toTelemetryColor(), iz(TelemetryColor.Charcoal))
        assertThat(storeColor.toTelemetryColorAsString(), iz("Charcoal"))
        assertThat(storeColor.toTelemetryColorAsString(), iz(TelemetryColor.Charcoal.name))
    }
}
