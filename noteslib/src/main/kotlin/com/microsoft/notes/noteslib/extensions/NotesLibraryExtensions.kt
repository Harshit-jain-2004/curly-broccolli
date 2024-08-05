package com.microsoft.notes.noteslib.extensions

import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary

fun NotesLibrary.markAsDeleteAndDeleteIfEmpty(note: Note) {
    if (!note.isEmpty)
        return
    markAsDeleteAndDelete(note.localId, note.remoteData?.id)
}

// UI options
internal fun NotesLibrary.showFeedbackButton(): Boolean = !uiOptionFlags.hideNoteOptionsFeedbackButton && !disableNoteOptionsFeedbackButton

internal fun NotesLibrary.showShareButton(): Boolean = !uiOptionFlags.hideNoteOptionsShareButton

internal fun NotesLibrary.showClearCanvasButton(): Boolean = uiOptionFlags.showClearCanvasButtonForInkNotes

internal fun NotesLibrary.showDeleteButton(): Boolean = !uiOptionFlags.hideNoteOptionsDeleteButton

internal fun NotesLibrary.showSearchInNoteButton(): Boolean = uiOptionFlags.showSearchInNoteOption
