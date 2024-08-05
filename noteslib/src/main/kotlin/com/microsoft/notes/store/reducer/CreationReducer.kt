package com.microsoft.notes.store.reducer

import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.CreationAction
import com.microsoft.notes.store.action.CreationAction.AddNoteAction
import com.microsoft.notes.store.addNotes
import com.microsoft.notes.utils.logging.NotesLogger

object CreationReducer : Reducer<CreationAction> {
    override fun reduce(
        action: CreationAction,
        currentState: State,
        notesLogger: NotesLogger?,
        isDebugMode: Boolean
    ): State = reduceNotesList(action, currentState)

    private fun reduceNotesList(action: CreationAction, state: State): State =
        when (action) {
            is AddNoteAction -> {
                state.addNotes(listOf(action.note), action.userID)
            }
        }
}
