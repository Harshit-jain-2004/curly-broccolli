package com.microsoft.notes.store.reducer

import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.utils.logging.NotesLogger

interface Reducer<in T : Action> {
    fun reduce(action: T, currentState: State, notesLogger: NotesLogger? = null, isDebugMode: Boolean): State
}
