package com.microsoft.notes.richtext.scheme

import com.microsoft.notes.models.ReminderWrapper
import java.io.Serializable

data class NoteMetadata(
    val context: NoteContext? = null,
    val reminder: ReminderWrapper? = null
) : Serializable

data class NoteContext(
    val host: String,
    val hostIcon: String?,
    val displayName: String,
    val url: String?
) : Serializable
