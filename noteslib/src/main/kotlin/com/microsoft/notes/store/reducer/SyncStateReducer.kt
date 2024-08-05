package com.microsoft.notes.store.reducer

import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.SyncStateAction
import com.microsoft.notes.store.clearSyncErrorState
import com.microsoft.notes.store.state.toSyncResponseError
import com.microsoft.notes.store.withSyncErrorStateForUser
import com.microsoft.notes.utils.logging.NotesLogger

object SyncStateReducer : Reducer<SyncStateAction> {
    override fun reduce(
        action: SyncStateAction,
        currentState: State,
        notesLogger: NotesLogger?,
        isDebugMode: Boolean
    ): State {
        return when (action) {
            is SyncStateAction.RemoteNotesSyncErrorAction -> {
                toSyncResponseError(action.errorType)?.let {
                    currentState.withSyncErrorStateForUser(
                        userID = action
                            .userID,
                        syncErrorState = it
                    )
                }
                    ?: currentState
            }
            is SyncStateAction.RemoteNotesSyncSucceededAction -> {
                currentState.clearSyncErrorState(userID = action.userID)
            }
            else -> currentState
        }
    }
}
