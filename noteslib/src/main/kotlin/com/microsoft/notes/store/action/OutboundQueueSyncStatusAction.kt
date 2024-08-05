package com.microsoft.notes.store.action

sealed class OutboundQueueSyncStatusAction(val userID: String) : Action {

    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is SyncActiveAction -> "SyncActiveAction"
            is SyncInactiveAction -> "SyncInactiveAction"
        }

        return "OutboundQueueSyncStatusAction.$actionType"
    }

    class SyncActiveAction(userID: String) : OutboundQueueSyncStatusAction(userID = userID)
    class SyncInactiveAction(userID: String) : OutboundQueueSyncStatusAction(userID = userID)
}
