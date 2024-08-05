package com.microsoft.notes.store.reducer

import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.UIAction
import com.microsoft.notes.store.clearFutureNoteState
import com.microsoft.notes.store.withFutureNoteStateForUser
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys

object UIReducer : Reducer<UIAction> {
    override fun reduce(
        action: UIAction,
        currentState: State,
        notesLogger: NotesLogger?,
        isDebugMode: Boolean
    ): State {
        when (action) {
            is UIAction.AccountChanged -> {
                notesLogger?.i(message = "AccountChanged")
                return currentState.copy(currentUserID = action.userID)
            }
            is UIAction.UpdateCurrentUserID -> {
                notesLogger?.i(message = "UpdateCurrentUserID")
                return currentState.copy(currentUserID = action.userID)
            }
            is UIAction.UpdateFutureNoteUserNotification -> {
                val futureNotesCount = action.notes.count { it.isFutureNote }

                if (futureNotesCount > 0) {
                    notesLogger?.recordTelemetry(
                        EventMarkers.FutureNoteEncountered,
                        Pair(NotesSDKTelemetryKeys.NoteProperty.COUNT, futureNotesCount.toString())
                    )
                    return currentState.withFutureNoteStateForUser(
                        userID = action.userID,
                        futureNoteCount = futureNotesCount
                    )
                }
                return currentState.clearFutureNoteState(userID = action.userID)
            }
        }
        return currentState
    }
}
