package com.microsoft.notes.sideeffect.persistence.handler

import com.microsoft.notes.models.Note
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.utils.logging.NotesLogger

internal interface ActionHandler<in T : Action> {
    fun handle(
        action: T,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger? = null,
        findNote: (String) -> Note?,
        actionDispatcher: (Action) -> Unit
    )
}
