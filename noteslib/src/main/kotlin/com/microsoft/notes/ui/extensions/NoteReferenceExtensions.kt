package com.microsoft.notes.ui.extensions

import com.microsoft.notes.models.NoteReference

fun List<NoteReference>.filterDeletedNoteReferences(): List<NoteReference> = filter { !it.isDeleted }

fun List<NoteReference>.search(query: String): List<NoteReference> = filter {
    return filter { note ->
        if (query.isEmpty()) {
            true
        } else {
            query.parseSearchQuery().all { term ->
                note.title?.contains(term, ignoreCase = true) ?: false || note.previewText.contains(term, ignoreCase = true)
            }
        }
    }
}
