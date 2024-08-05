package com.microsoft.notes.store.reducer

import com.microsoft.notes.store.OutboundSyncState
import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.OutboundQueueSyncStatusAction
import com.microsoft.notes.store.action.OutboundQueueSyncStatusAction.SyncActiveAction
import com.microsoft.notes.store.action.OutboundQueueSyncStatusAction.SyncInactiveAction
import com.microsoft.notes.store.withOutboundSyncStateForUser
import com.microsoft.notes.utils.logging.NotesLogger

object OutboundQueueSyncStatusReducer : Reducer<OutboundQueueSyncStatusAction> {
    override fun reduce(
        action: OutboundQueueSyncStatusAction,
        currentState: State,
        notesLogger: NotesLogger?,
        isDebugMode: Boolean
    ): State {
        return when (action) {
            is SyncActiveAction -> currentState.withOutboundSyncStateForUser(
                outboundSyncState =
                OutboundSyncState.Active,
                userID = action.userID
            )
            is SyncInactiveAction -> currentState.withOutboundSyncStateForUser(
                outboundSyncState =
                OutboundSyncState.Inactive,
                userID = action.userID
            )
        }
    }
}
