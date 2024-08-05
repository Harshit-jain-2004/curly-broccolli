package com.microsoft.notes.store.reducer

import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.SamsungNotesResponseAction
import com.microsoft.notes.store.addSamsungNotes
import com.microsoft.notes.store.deleteSamsungNotes
import com.microsoft.notes.store.replaceSamsungNotes
import com.microsoft.notes.utils.logging.NotesLogger

object SamsungNotesReducer : Reducer<SamsungNotesResponseAction> {
    override fun reduce(
        action: SamsungNotesResponseAction,
        currentState: State,
        notesLogger: NotesLogger?,
        isDebugMode: Boolean
    ): State = reduceSamsungNotesList(action, currentState, notesLogger)

    private fun reduceSamsungNotesList(
        responseAction: SamsungNotesResponseAction,
        state: State,
        notesLogger: NotesLogger?
    ): State =
        when (responseAction) {
            is SamsungNotesResponseAction.ApplyChanges -> {
                with(responseAction) {
                    notesLogger?.i(
                        message = "SamsungNotes applyChanges: toCreate: ${changes.toCreate.size}, " +
                            "toDelete: ${changes.toDelete.size}, toReplace: ${changes.toReplace.size}"
                    )

                    return state.addSamsungNotes(changes.toCreate, responseAction.userID)
                        .replaceSamsungNotes(changes.toReplace.map { it.noteFromServer }, responseAction.userID)
                        .deleteSamsungNotes(changes.toDelete, responseAction.userID)
                }
            }
        }
}
