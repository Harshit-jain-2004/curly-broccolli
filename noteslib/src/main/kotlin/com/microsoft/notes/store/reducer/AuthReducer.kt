package com.microsoft.notes.store.reducer

import com.microsoft.notes.store.AuthState
import com.microsoft.notes.store.AuthenticationState
import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.AuthAction
import com.microsoft.notes.store.action.AuthAction.LogoutAction
import com.microsoft.notes.store.action.AuthAction.NewAuthTokenAction
import com.microsoft.notes.store.deleteAllNotesForUserID
import com.microsoft.notes.store.updateUserNotifications
import com.microsoft.notes.store.withAuthenticationStateForUser
import com.microsoft.notes.utils.logging.NotesLogger

object AuthReducer : Reducer<AuthAction> {

    override fun reduce(
        action: AuthAction,
        currentState: State,
        notesLogger: NotesLogger?,
        isDebugMode: Boolean
    ): State {
        when (action) {
            is NewAuthTokenAction -> {
                with(action) {
                    notesLogger?.i(message = "NewAuthTokenAction")
                    val newAuthenticationState = AuthenticationState(AuthState.AUTHENTICATED)
                    return currentState.withAuthenticationStateForUser(
                        authState = newAuthenticationState,
                        userID = userID
                    ).updateUserNotifications(userID = userID)
                }
            }

            is AuthAction.ClientAuthFailedAction -> {
                with(action) {
                    notesLogger?.i(message = "ClientAuthFailedAction")
                    val newAuthenticationState = AuthenticationState(AuthState.NOT_AUTHORIZED)
                    return currentState.withAuthenticationStateForUser(
                        authState = newAuthenticationState,
                        userID = userID
                    ).updateUserNotifications(userID = userID)
                }
            }

            is LogoutAction -> {
                with(action) {
                    notesLogger?.i(message = "LogoutAction")
                    val newAuthenticationState = AuthenticationState(AuthState.UNAUTHENTICATED)
                    return currentState.withAuthenticationStateForUser(
                        authState = newAuthenticationState,
                        userID = userID
                    ).deleteAllNotesForUserID(userID).updateUserNotifications(userID = userID)
                }
            }
            else -> return currentState
        }
    }
}
