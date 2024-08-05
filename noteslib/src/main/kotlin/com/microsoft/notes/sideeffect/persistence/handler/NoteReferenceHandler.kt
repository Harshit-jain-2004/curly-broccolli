package com.microsoft.notes.sideeffect.persistence.handler

import com.microsoft.notes.models.Note
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.sideeffect.persistence.Preference
import com.microsoft.notes.sideeffect.persistence.PreferenceKeys
import com.microsoft.notes.sideeffect.persistence.extensions.toNoteReferenceMediaJson
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceNoteReference
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.NoteReferenceAction
import com.microsoft.notes.utils.logging.NotesLogger

object NoteReferenceHandler : ActionHandler<NoteReferenceAction> {

    override fun handle(
        action: NoteReferenceAction,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?,
        findNote: (String) -> Note?,
        actionDispatcher: (Action) -> Unit
    ) {
        when (action) {
            is NoteReferenceAction.ApplyChanges -> {
                notesDB.noteReferenceDao().insert(action.changes.toCreate.map { it.toPersistenceNoteReference() })
                notesDB.noteReferenceDao().update(action.changes.toReplace.map { it.remoteNote.toPersistenceNoteReference() })
                notesDB.noteReferenceDao().delete(action.changes.toDelete.map { it.toPersistenceNoteReference() })
                if (!action.isLocalChange) {
                    notesDB.preferencesDao().insertOrUpdate(
                        Preference(
                            id = PreferenceKeys.noteReferencesDeltaToken,
                            value = action.deltaToken
                        )
                    )
                }
            }

            is NoteReferenceAction.MarkAsDeleted -> {
                action.toMarkAsDeleted.forEach {
                    notesDB.noteReferenceDao().markAsDeleted(it.localId, isDeleted = true, isLocalOnlyPage = false)
                }
            }

            is NoteReferenceAction.PinNoteReference -> {
                action.toPinNoteReferences.forEach {
                    notesDB.noteReferenceDao().setPinnedProperties(it.localId, isPinned = true, pinnedAt = System.currentTimeMillis())
                }
            }

            is NoteReferenceAction.UpdateNoteReferenceMedia -> {
                action.media?.toNoteReferenceMediaJson()?.let { notesDB.noteReferenceDao().updateMedia(action.noteReference.localId, it) }
            }

            is NoteReferenceAction.UnpinNoteReference -> {
                action.toUnpinNoteReferences.forEach {
                    notesDB.noteReferenceDao().setPinnedProperties(it.localId, isPinned = false, pinnedAt = null)
                }
            }
        }
    }
}
