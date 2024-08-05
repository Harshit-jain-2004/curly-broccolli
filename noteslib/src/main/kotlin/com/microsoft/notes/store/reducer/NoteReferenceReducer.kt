package com.microsoft.notes.store.reducer

import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.NoteReferenceAction
import com.microsoft.notes.store.addNoteReferences
import com.microsoft.notes.store.deleteNoteReferences
import com.microsoft.notes.store.markNoteReferencesAsDeleted
import com.microsoft.notes.store.pinOrUnpinNoteReferences
import com.microsoft.notes.store.replaceNoteReference
import com.microsoft.notes.store.replaceNoteReferences
import com.microsoft.notes.utils.logging.NotesLogger

object NoteReferenceReducer : Reducer<NoteReferenceAction> {
    override fun reduce(
        action: NoteReferenceAction,
        currentState: State,
        notesLogger: NotesLogger?,
        isDebugMode: Boolean
    ): State = reduceNotesList(action, currentState, notesLogger)

    private fun reduceNotesList(action: NoteReferenceAction, state: State, notesLogger: NotesLogger?): State =
        when (action) {
            is NoteReferenceAction.ApplyChanges -> {
                with(action) {
                    notesLogger?.i(
                        message = "applyChanges: toCreate: ${changes.toCreate.size}, " +
                            "toDelete: ${changes.toDelete.size}, toReplace: ${changes.toReplace.size}"
                    )

                    return state.addNoteReferences(changes.toCreate, action.userID)
                        .replaceNoteReferences(changes.toReplace.map { it.remoteNote }, action.userID)
                        .deleteNoteReferences(changes.toDelete, action.userID)
                }
            }
            is NoteReferenceAction.MarkAsDeleted -> {
                with(action) {
                    notesLogger?.i(message = "markAsDeleted: toMarkAsDeleted: ${toMarkAsDeleted.size}")
                    return state.markNoteReferencesAsDeleted(toMarkAsDeleted, action.userID)
                }
            }
            is NoteReferenceAction.PinNoteReference -> {
                with(action) {
                    notesLogger?.i(message = "pinNoteReferences: toPinNoteReferences: ${toPinNoteReferences.size}")
                    return state.pinOrUnpinNoteReferences(toPinNoteReferences, userID, true)
                }
            }
            is NoteReferenceAction.UnpinNoteReference -> {
                with(action) {
                    notesLogger?.i(message = "unpinNoteReferences: toUnpinNoteReferences: ${toUnpinNoteReferences.size}")
                    return state.pinOrUnpinNoteReferences(toUnpinNoteReferences, action.userID, false)
                }
            }

            is NoteReferenceAction.UpdateNoteReferenceMedia -> {
                with(action) {
                    val updatedNoteReference = action.noteReference.copy(media = action.media)
                    return state.replaceNoteReference(updatedNoteReference, action.userID)
                }
            }
        }
}
