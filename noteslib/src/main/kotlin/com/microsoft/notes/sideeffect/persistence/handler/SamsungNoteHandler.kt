package com.microsoft.notes.sideeffect.persistence.handler

import com.microsoft.notes.models.Note
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.sideeffect.persistence.Preference
import com.microsoft.notes.sideeffect.persistence.PreferenceKeys
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceNote
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.SamsungNotesResponseAction
import com.microsoft.notes.utils.logging.NotesLogger

object SamsungNoteHandler : ActionHandler<SamsungNotesResponseAction> {

    override fun handle(
        action: SamsungNotesResponseAction,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?,
        findNote: (String) -> Note?,
        actionDispatcher: (Action) -> Unit
    ) {
        when (action) {
            is SamsungNotesResponseAction.ApplyChanges -> {
                notesDB.noteDao().insert(action.changes.toCreate.map { it.toPersistenceNote() })
                notesDB.noteDao().update(action.changes.toReplace.map { it.noteFromServer.toPersistenceNote() })
                notesDB.noteDao().delete(action.changes.toDelete.map { it.toPersistenceNote() })
                notesDB.preferencesDao().insertOrUpdate(
                    Preference(id = PreferenceKeys.samsungNotesDeltaToken, value = action.deltaToken)
                )
            }
        }
    }
}
