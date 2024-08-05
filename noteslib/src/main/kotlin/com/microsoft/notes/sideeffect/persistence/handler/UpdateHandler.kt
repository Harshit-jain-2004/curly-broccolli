package com.microsoft.notes.sideeffect.persistence.handler

import com.microsoft.notes.models.Note
import com.microsoft.notes.models.ReminderWrapper
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.sideeffect.persistence.extensions.toJson
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceColor
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.UpdateAction
import com.microsoft.notes.store.action.UpdateAction.PinNotes
import com.microsoft.notes.store.action.UpdateAction.UnpinNotes
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateDocumentRange
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithAddedMediaAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithColorAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithDocumentAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithRemovedMediaAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithUpdateMediaAltTextAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateTimeReminderAction
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.utils.exhaustive

object UpdateHandler : ActionHandler<UpdateAction> {

    override fun handle(
        action: UpdateAction,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?,
        findNote: (String) -> Note?,
        actionDispatcher: (Action) -> Unit
    ) {
        when (action) {
            is PinNotes -> action.toPinNotes.forEach {
                notesDB.noteDao().setPinnedProperties(it.localId, isPinned = true, pinnedAt = System.currentTimeMillis())
            }
            is UnpinNotes -> action.toUnpinNotes.forEach {
                notesDB.noteDao().setPinnedProperties(it.localId, isPinned = false, pinnedAt = null)
            }
            is UpdateAction.UpdateActionWithId -> when (action) {
                is UpdateNoteWithDocumentAction -> findNote(action.noteLocalId)
                    ?.let { updateNoteWithDocument(notesDB, notesLogger, it) }
                is UpdateNoteWithColorAction -> findNote(action.noteLocalId)
                    ?.let { updateNoteWithColor(notesDB, it) }
                is UpdateNoteWithAddedMediaAction -> findNote(action.noteLocalId)
                    ?.let { updateNoteWithAddedMedia(notesDB, notesLogger, it) }
                is UpdateNoteWithRemovedMediaAction -> findNote(action.noteLocalId)
                    ?.let { updateNoteWithRemovedMedia(action, notesDB, notesLogger, it) }
                is UpdateNoteWithUpdateMediaAltTextAction -> findNote(action.noteLocalId)
                    ?.let { updateNoteWithUpdateMediaAltText(action, notesDB, notesLogger, it) }
                is UpdateDocumentRange -> Unit
                is UpdateTimeReminderAction -> findNote(action.noteLocalId)
                    ?.let { updateNoteWithReminder(action, notesDB, it) }
            }
        }.exhaustive
    }

    private fun updateNoteWithDocument(
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?,
        note: Note
    ) {
        with(note) {
            notesDB.noteDao().updateDocumentModifiedAt(localId, documentModifiedAt)
            updateDocument(localId, document, notesDB, notesLogger)
        }
    }

    private fun updateNoteWithAddedMedia(
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?,
        note: Note
    ) {
        with(note) {
            updateMedia(localId, media, notesDB, notesLogger)
        }
    }

    private fun updateNoteWithColor(
        notesDB: NotesDatabase,
        note: Note
    ) {
        with(note) {
            notesDB.noteDao().updateColor(localId, color.toPersistenceColor())
        }
    }

    private fun updateNoteWithReminder(
        action: UpdateTimeReminderAction,
        notesDB: NotesDatabase,
        note: Note
    ) {
        with(note) {
            val updatedReminder = (
                note.metadata.reminder
                    ?: ReminderWrapper()
                ).copy(timeReminder = action.timeReminder)
            notesDB.noteDao().updateReminder(localId, updatedReminder.toJson())
        }
    }

    private fun updateNoteWithRemovedMedia(
        action: UpdateNoteWithRemovedMediaAction,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?,
        note: Note
    ) {
        with(note) {
            val mediaIdToRemove = action.media.localId
            val updatedMedia = media.filter { media -> media.localId != mediaIdToRemove }
            updateMedia(localId, updatedMedia, notesDB, notesLogger)
        }
    }

    private fun updateNoteWithUpdateMediaAltText(
        action: UpdateNoteWithUpdateMediaAltTextAction,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?,
        note: Note
    ) {
        with(note) {
            val updatedMedia = media.map { media ->
                if (media.localId == action.mediaLocalId) media.copy(altText = action.altText) else media
            }
            updateMedia(localId, updatedMedia, notesDB, notesLogger)
        }
    }
}
