package com.microsoft.notes.store.reducer

import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.DeleteAction
import com.microsoft.notes.store.action.DeleteAction.CleanupNotesMarkedAsDeletedAction
import com.microsoft.notes.store.action.DeleteAction.DeleteAllNotesAction
import com.microsoft.notes.store.action.DeleteAction.MarkNoteAsDeletedAction
import com.microsoft.notes.store.action.DeleteAction.MarkNoteReferenceAsDeletedAction
import com.microsoft.notes.store.action.DeleteAction.MarkSamsungNoteAsDeletedAction
import com.microsoft.notes.store.action.DeleteAction.UnmarkNoteAsDeletedAction
import com.microsoft.notes.store.deleteAllNotesForUserID
import com.microsoft.notes.store.markNoteReferenceWithLocalIdAsDeleted
import com.microsoft.notes.store.markNoteWithLocalIdAsDeleted
import com.microsoft.notes.store.markSamsungNoteWithLocalIdAsDeleted
import com.microsoft.notes.store.unmarkNoteWithLocalIdAsDeleted
import com.microsoft.notes.utils.logging.NotesLogger

object DeleteReducer : Reducer<DeleteAction> {

    override fun reduce(
        action: DeleteAction,
        currentState: State,
        notesLogger: NotesLogger?,
        isDebugMode: Boolean
    ): State = reduceNotesList(action, currentState)

    private fun reduceNotesList(action: DeleteAction, state: State): State =
        when (action) {
            is DeleteAllNotesAction -> state.deleteAllNotesForUserID(action.userID)
            is MarkNoteAsDeletedAction -> state.markNoteWithLocalIdAsDeleted(action.localId)
            is MarkNoteReferenceAsDeletedAction -> state.markNoteReferenceWithLocalIdAsDeleted(action.localId)
            is MarkSamsungNoteAsDeletedAction -> state.markSamsungNoteWithLocalIdAsDeleted(action.localId)
            is UnmarkNoteAsDeletedAction -> state.unmarkNoteWithLocalIdAsDeleted(action.localId)
            // We get a separate call for these to Delete individual note.
            // Hence we don't need to do any extra work here.
            is CleanupNotesMarkedAsDeletedAction -> state
        }
}
