package com.microsoft.notes.store.reducer

import com.microsoft.notes.models.Note
import com.microsoft.notes.models.ReminderWrapper
import com.microsoft.notes.richtext.scheme.NoteMetadata
import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.UpdateAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateDocumentRange
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithAddedMediaAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithColorAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithDocumentAction
import com.microsoft.notes.store.getNoteForNoteLocalId
import com.microsoft.notes.store.pinOrUnpinNotes
import com.microsoft.notes.store.replaceNote
import com.microsoft.notes.threeWayMerge.canThreeWayMerge
import com.microsoft.notes.threeWayMerge.noteWithNewType
import com.microsoft.notes.threeWayMerge.threeWayMerge
import com.microsoft.notes.utils.logging.NotesLogger

object UpdateReducer : Reducer<UpdateAction> {
    var notesLogger: NotesLogger? = null

    override fun reduce(
        action: UpdateAction,
        currentState: State,
        notesLogger: NotesLogger?,
        isDebugMode: Boolean
    ): State {
        this.notesLogger = notesLogger
        return reduceNotesList(action, currentState)
    }

    private fun reduceNotesList(action: UpdateAction, state: State): State =
        when (action) {
            is UpdateAction.PinNotes -> {
                state.pinOrUnpinNotes(action.toPinNotes, action.userID, true)
            }
            is UpdateAction.UnpinNotes -> {
                state.pinOrUnpinNotes(action.toUnpinNotes, action.userID, false)
            }
            is UpdateActionWithId -> {
                val note = state.getNoteForNoteLocalId(action.noteLocalId)
                if (note != null) state.replaceNote(changeNoteFields(action, note)) else state
            }
        }

    private fun changeNoteFields(action: UpdateActionWithId, note: Note): Note =
        when (action) {
            is UpdateDocumentRange -> {
                note.copy(document = note.document.copy(range = action.newRange))
            }
            is UpdateNoteWithDocumentAction -> {
                val updatedNote = note.copy(
                    document = action.updatedDocument,
                    documentModifiedAt = action.documentModifiedAt,
                    uiRevision = action.uiRevision
                )
                mergeNote(note, updatedNote, action.uiRevision)
            }
            is UpdateNoteWithColorAction -> {
                val updatedNote = note.copy(
                    color = action.color,
                    uiRevision = action.uiRevision
                )
                mergeNote(note, updatedNote, action.uiRevision)
            }
            is UpdateNoteWithAddedMediaAction -> {
                val updatedNote = note.copy(
                    media = listOf(action.media) + note.media,
                    documentModifiedAt = System.currentTimeMillis(),
                    uiRevision = action.uiRevision
                )
                mergeNote(note, updatedNote, action.uiRevision)
            }
            is UpdateActionWithId.UpdateNoteWithRemovedMediaAction -> {
                val updatedNote = note.copy(
                    media = note.media - action.media,
                    documentModifiedAt = System.currentTimeMillis(),
                    uiRevision = action.uiRevision
                )
                mergeNote(note, updatedNote, action.uiRevision)
            }
            is UpdateActionWithId.UpdateNoteWithUpdateMediaAltTextAction -> {
                val mediaWithUpdatedAltText =
                    note.media.find { action.mediaLocalId == it.localId }
                mediaWithUpdatedAltText?.let {
                    val updatedMedia =
                        note.media - mediaWithUpdatedAltText + mediaWithUpdatedAltText.copy(
                            altText = action.altText
                        )

                    val updatedNote = note.copy(
                        media = updatedMedia,
                        documentModifiedAt = System.currentTimeMillis(),
                        uiRevision = action.uiRevision
                    )
                    mergeNote(note, updatedNote, action.uiRevision)
                } ?: note
            }
            is UpdateActionWithId.UpdateTimeReminderAction -> {
                val reminder = note.metadata.reminder ?: ReminderWrapper()
                reminder.timeReminder = action.timeReminder
                val updatedNode = note.copy(metadata = NoteMetadata(context = note.metadata.context, reminder = reminder))
                mergeNote(note, updatedNode, action.uiRevision)
            }
        }

    private fun mergeNote(currentNote: Note, updatedNote: Note, revision: Long): Note {
        val finalNote = if (currentNote != updatedNote && currentNote.uiShadow != null) {
            doMerge(currentNote.uiShadow, currentNote, updatedNote)
        } else if (updatedNote.uiShadow == null) {
            updatedNote.copy(
                uiShadow = updatedNote.copy(uiShadow = null),
                remoteData = currentNote.remoteData
            )
        } else {
            updatedNote
        }
        return finalNote.copy(
            uiRevision = revision,
            documentModifiedAt = updatedNote.documentModifiedAt
        )
    }

    private fun doMerge(uiShadow: Note, currentNote: Note, updatedNote: Note): Note {
        return if (canThreeWayMerge(
                base = uiShadow, primary = updatedNote,
                secondary = currentNote
            )
        ) {
            val merged = threeWayMerge(
                base = uiShadow, primary = updatedNote,
                secondary = currentNote
            )
            merged.copy(
                uiShadow = updatedNote.copy(uiShadow = null),
                remoteData = currentNote.remoteData
            )
        } else {
            noteWithNewType(uiShadow, updatedNote, currentNote)
        }
    }
}
