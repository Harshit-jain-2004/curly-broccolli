package com.microsoft.notes.richtext.editor.extensions

import com.microsoft.notes.models.Color
import com.microsoft.notes.utils.logging.NoteColor as TelemetryColor

fun Color.toTelemetryColor(): TelemetryColor {
    return when (this) {
        Color.GREY -> TelemetryColor.Grey
        Color.YELLOW -> TelemetryColor.Yellow
        Color.GREEN -> TelemetryColor.Green
        Color.PINK -> TelemetryColor.Pink
        Color.PURPLE -> TelemetryColor.Purple
        Color.BLUE -> TelemetryColor.Blue
        Color.CHARCOAL -> TelemetryColor.Charcoal
    }
}

fun Color.toTelemetryColorAsString(): String = toTelemetryColor().name

fun Long.increaseRevision(): Long {
    return if (this < Long.MAX_VALUE) {
        this + 1
    } else {
        0
    }
}
