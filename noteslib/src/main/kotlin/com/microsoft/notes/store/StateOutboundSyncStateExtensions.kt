package com.microsoft.notes.store

fun State.withOutboundSyncStateForUser(outboundSyncState: OutboundSyncState, userID: String): State {
    val updatedUserState = getUserStateForUserID(userID).copy(outboundSyncState = outboundSyncState)
    return newState(userID, updatedUserState)
}
