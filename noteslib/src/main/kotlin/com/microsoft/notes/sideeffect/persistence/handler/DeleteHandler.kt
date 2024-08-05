package com.microsoft.notes.sideeffect.persistence.handler

import com.microsoft.notes.models.Note
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.DeleteAction
import com.microsoft.notes.store.action.DeleteAction.CleanupNotesMarkedAsDeletedAction
import com.microsoft.notes.store.action.DeleteAction.DeleteAllNotesAction
import com.microsoft.notes.store.action.DeleteAction.MarkNoteAsDeletedAction
import com.microsoft.notes.store.action.DeleteAction.MarkNoteReferenceAsDeletedAction
import com.microsoft.notes.store.action.DeleteAction.MarkSamsungNoteAsDeletedAction
import com.microsoft.notes.store.action.DeleteAction.UnmarkNoteAsDeletedAction
import com.microsoft.notes.utils.logging.NotesLogger

object DeleteHandler : ActionHandler<DeleteAction> {

    override fun handle(
        action: DeleteAction,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?,
        findNote: (String) -> Note?,
        actionDispatcher: (Action) -> Unit
    ) =
        when (action) {
            is DeleteAllNotesAction -> deleteAllNotes(notesDB)
            is MarkNoteAsDeletedAction -> markNoteAsDeleted(action.localId, notesDB)
            is MarkNoteReferenceAsDeletedAction -> markNoteReferenceAsDeleted(action.localId, notesDB)
            is MarkSamsungNoteAsDeletedAction -> markNoteAsDeleted(action.localId, notesDB)
            is UnmarkNoteAsDeletedAction -> unmarkNoteAsDeleted(action, notesDB)
            is CleanupNotesMarkedAsDeletedAction -> {
                // left blank, marked as deleted notes will be retried
                // if they 404 they will be permanently deleted see
                // SyncSideEffect handleCleanupNotesMarkedAsDeletedAction
            }
        }

    private fun deleteAllNotes(notesDB: NotesDatabase) {
        notesDB.noteDao().deleteAll()
    }

    private fun markNoteAsDeleted(localId: String, notesDB: NotesDatabase) =
        notesDB.noteDao().markNoteAsDeleted(localId, isDeleted = true)

    private fun markNoteReferenceAsDeleted(localId: String, notesDB: NotesDatabase) =
        notesDB.noteReferenceDao().markAsDeleted(localId, isDeleted = true, isLocalOnlyPage = false)

    private fun unmarkNoteAsDeleted(action: UnmarkNoteAsDeletedAction, notesDB: NotesDatabase) {
        notesDB.noteDao().markNoteAsDeleted(action.localId, isDeleted = false)
    }
}
