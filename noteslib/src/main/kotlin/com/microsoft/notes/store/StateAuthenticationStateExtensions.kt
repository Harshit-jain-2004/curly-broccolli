package com.microsoft.notes.store

import com.microsoft.notes.ui.noteslist.AuthErrorUserNotification
import com.microsoft.notes.ui.noteslist.UserNotificationType
import com.microsoft.notes.utils.utils.Constants

fun State.withAuthenticationStateForUser(authState: AuthenticationState, userID: String): State {
    val newUserID = when (authState.authState) {
        /**
         * We don't automatically sign in a user if a new token is given to us, as it might be a
         * token from a refresh (rather than a new sign-in). We want the host to explicitly decide
         * when a currentUserID should be switched.
         */
        AuthState.AUTHENTICATED -> currentUserID

        /**
         * When user's access token expires, we will still show notes for current
         * primary account.
         */
        AuthState.NOT_AUTHORIZED -> currentUserID

        /**
         * If a user signs out, we will remove notes for that account. If this is the primary account,
         * the host app will need to switch to another account if they wish to show another account's
         * notes. If this account is not the primary account, the current account selection will
         * stay the same.
         */
        AuthState.UNAUTHENTICATED -> {
            if (userID == currentUserID) Constants.EMPTY_USER_ID else currentUserID
        }
    }

    if (authState.authState == AuthState.AUTHENTICATED && currentUserID.isEmpty()) {
        val newState = migrateNotesFromEmptyUserID(userID)
        val updatedUserState = newState.getUserStateForUserID(userID).copy(authenticationState = authState)
        return newState.newState(userID = userID, updatedUserState = updatedUserState, newSelectedUserID = newUserID)
    }
    val updatedUserState = getUserStateForUserID(userID).copy(authenticationState = authState)
    return newState(userID = userID, updatedUserState = updatedUserState, newSelectedUserID = newUserID)
}

fun State.updateUserNotifications(userID: String): State {
    val userState = getUserStateForUserID(userID)
    val updatedUserNotifications = when (userState.authenticationState.authState) {
        AuthState.AUTHENTICATED, AuthState.UNAUTHENTICATED ->
            userState.userNotifications.remove(UserNotificationType.AuthError)
        AuthState.NOT_AUTHORIZED -> userState.userNotifications.with(AuthErrorUserNotification())
    }
    val updatedUserState = userState.copy(userNotifications = updatedUserNotifications)
    return newState(userID = userID, updatedUserState = updatedUserState)
}

private fun State.migrateNotesFromEmptyUserID(userID: String): State {
    val userStateForEmptyUserId = getUserStateForUserID(Constants.EMPTY_USER_ID)
    return addDistinctNotes(userStateForEmptyUserId.notesList.notesCollection, userID)
        .withNotesLoaded(userStateForEmptyUserId.notesList.notesLoaded, userID)
        .deleteAllNotesForUserID(Constants.EMPTY_USER_ID)
}
