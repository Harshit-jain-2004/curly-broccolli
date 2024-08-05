package com.microsoft.notes.store.reducer

import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.PreferencesAction
import com.microsoft.notes.store.withEmailForUser
import com.microsoft.notes.utils.logging.NotesLogger

object PreferencesReducer : Reducer<PreferencesAction> {
    override fun reduce(
        action: PreferencesAction,
        currentState: State,
        notesLogger: NotesLogger?,
        isDebugMode: Boolean
    ): State {
        return when (action) {
            is PreferencesAction.UpdateEmailForUserIDAction -> reduceUpdateEmailForUserIDAction(
                action,
                currentState
            )
            is PreferencesAction.UpdateStoredEmailForUserIDAction -> currentState
        }
    }

    private fun reduceUpdateEmailForUserIDAction(
        action: PreferencesAction.UpdateEmailForUserIDAction,
        currentState: State
    ): State =
        currentState.withEmailForUser(action.emailID, action.userID)
}
