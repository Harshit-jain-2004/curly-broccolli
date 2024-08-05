package com.microsoft.notes.sideeffect.persistence.handler

import com.microsoft.notes.models.Note
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.CreationAction
import com.microsoft.notes.utils.logging.NotesLogger

object CreationHandler : ActionHandler<CreationAction> {

    override fun handle(
        action: CreationAction,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?,
        findNote: (String) -> Note?,
        actionDispatcher: (Action) -> Unit
    ) {
        when (action) {
            is CreationAction.AddNoteAction ->
                insertNoteIntoDB(storeNote = action.note, notesDB = notesDB, notesLogger = notesLogger)
        }
    }
}
