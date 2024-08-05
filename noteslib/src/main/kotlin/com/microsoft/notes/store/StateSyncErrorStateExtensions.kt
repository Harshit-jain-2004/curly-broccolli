package com.microsoft.notes.store

import com.microsoft.notes.ui.noteslist.FutureNoteUserNotification
import com.microsoft.notes.ui.noteslist.SyncErrorUserNotification
import com.microsoft.notes.ui.noteslist.UserNotificationType

fun State.withSyncErrorStateForUser(userID: String, syncErrorState: SyncErrorState): State {
    if (SyncErrorState.None.equals(syncErrorState)) {
        return this
    }
    val userState = getUserStateForUserID(userID)
    val updatedUserNotifications = when (syncErrorState) {
        is SyncErrorState.Unauthenticated -> userState.userNotifications
        else -> userState.userNotifications.with(SyncErrorUserNotification(syncErrorType = syncErrorState))
    }
    val updatedUserState = userState.copy(
        currentSyncErrorState = syncErrorState,
        userNotifications = updatedUserNotifications
    )
    return newState(userID, updatedUserState)
}

fun State.clearSyncErrorState(userID: String): State {
    val userState = getUserStateForUserID(userID)
    val updatedUserNotification = userState.userNotifications.remove(UserNotificationType.SyncError)
    val updatedUserState = userState.copy(
        currentSyncErrorState = SyncErrorState.None,
        userNotifications = updatedUserNotification
    )
    return newState(userID, updatedUserState)
}

fun State.withFutureNoteStateForUser(userID: String, futureNoteCount: Int): State {
    val userState = getUserStateForUserID(userID)
    val userNotification = FutureNoteUserNotification(futureNoteCount)
    val updatedUserNotification = userState.userNotifications.with(userNotification)
    val updatedUserState = userState.copy(userNotifications = updatedUserNotification)
    return newState(userID, updatedUserState)
}

fun State.clearFutureNoteState(userID: String): State {
    val userState = getUserStateForUserID(userID)
    if (userState.userNotifications.containsType(UserNotificationType.FutureNote)) {
        val updatedUserNotification = userState.userNotifications.remove(UserNotificationType.FutureNote)
        val updatedUserState = userState.copy(userNotifications = updatedUserNotification)
        return newState(userID, updatedUserState)
    }
    return this
}
