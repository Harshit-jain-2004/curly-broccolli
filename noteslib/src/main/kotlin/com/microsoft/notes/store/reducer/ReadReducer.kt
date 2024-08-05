package com.microsoft.notes.store.reducer

import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.ReadAction
import com.microsoft.notes.store.action.ReadAction.NotesLoadedAction
import com.microsoft.notes.store.addDistinctNoteReferences
import com.microsoft.notes.store.addDistinctNotes
import com.microsoft.notes.store.addDistinctSamsungNotes
import com.microsoft.notes.store.withNotesLoaded
import com.microsoft.notes.utils.logging.NotesLogger

object ReadReducer : Reducer<ReadAction> {

    override fun reduce(
        action: ReadAction,
        currentState: State,
        notesLogger: NotesLogger?,
        isDebugMode: Boolean
    ): State = reduceNotesList(action, currentState)

    private fun reduceNotesList(
        action: ReadAction,
        state: State
    ): State {
        return when (action) {
            is NotesLoadedAction -> handleNotesLoaded(action, state)
            else -> state
        }
    }

    private fun handleNotesLoaded(
        action: NotesLoadedAction,
        state: State
    ): State = state.withNotesLoaded(true, action.userID)
        .addDistinctNotes(action.notesCollection, action.userID)
        .addDistinctSamsungNotes(action.samsungNotesCollection, action.userID)
        .addDistinctNoteReferences(action.noteReferencesCollection, action.userID)
}
