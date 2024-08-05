package com.microsoft.notes.sideeffect.persistence.extensions

import com.microsoft.notes.models.Note
import com.microsoft.notes.models.toTelemetryColor
import com.microsoft.notes.models.toTelemetryNoteType
import com.microsoft.notes.richtext.scheme.paragraphListCount
import com.microsoft.notes.utils.logging.NoteColor
import com.microsoft.notes.utils.logging.NoteType

data class StoredNoteTelemetryAttributes(
    val noteType: NoteType,
    val noteColor: NoteColor,
    val paragraphCount: Int,
    val imageCount: Int
)

fun Note.getNoteTelemetryAttributes(): StoredNoteTelemetryAttributes =
    StoredNoteTelemetryAttributes(
        noteType = toTelemetryNoteType(),
        noteColor = color.toTelemetryColor(),
        paragraphCount = document.paragraphListCount(),
        imageCount = mediaCountWithoutInlineMedia
    )
