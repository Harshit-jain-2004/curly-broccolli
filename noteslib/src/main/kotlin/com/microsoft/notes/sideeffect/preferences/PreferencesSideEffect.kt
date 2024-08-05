package com.microsoft.notes.sideeffect.preferences

import android.content.Context
import com.microsoft.notes.store.SideEffect
import com.microsoft.notes.store.State
import com.microsoft.notes.store.Store
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.AuthAction
import com.microsoft.notes.store.action.CompoundAction
import com.microsoft.notes.store.action.PreferencesAction
import com.microsoft.notes.store.action.ReadAction
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.threading.ExecutorServices
import com.microsoft.notes.utils.threading.ThreadExecutor
import com.microsoft.notes.utils.threading.ThreadExecutorService
import com.microsoft.notes.utils.utils.UserInfoUtils

class PreferencesThreadService : ThreadExecutorService(
    ExecutorServices.preferencesSideEffect
)

class PreferencesSideEffect(
    val context: Context,
    val store: Store,
    val preferencesThread: ThreadExecutor? = null,
    val notesLogger: NotesLogger? = null
) : SideEffect(preferencesThread) {
    override fun handle(action: Action, state: State) {
        when (action) {
            is CompoundAction -> action.actions.forEach { handle(it, state) }
            is ReadAction.FetchAllNotesAction -> handleFetchAllNotesAction(action)
            is AuthAction.NewAuthTokenAction -> handleNewAuthTokenAction(action)
            is AuthAction.LogoutAction -> handleLogoutAction(action)
            is PreferencesAction.UpdateStoredEmailForUserIDAction -> handleUpdateStoredEmailForUserIDAction(action)
        }
    }

    private fun handleFetchAllNotesAction(action: ReadAction.FetchAllNotesAction) {
        val emailID = UserInfoUtils.getEmailIDFromUserID(action.userID, context)
        store.dispatch(PreferencesAction.UpdateEmailForUserIDAction(emailID, action.userID))
    }

    private fun handleNewAuthTokenAction(action: AuthAction.NewAuthTokenAction) {
        UserInfoUtils.addUserIDToSharedPreferences(action.userID, action.userInfo.email, context)
        store.dispatch(PreferencesAction.UpdateEmailForUserIDAction(action.userInfo.email, action.userID))
    }

    private fun handleLogoutAction(action: AuthAction.LogoutAction) {
        UserInfoUtils.removeUserIDFromSharedPreferences(action.userID, context)
    }

    private fun handleUpdateStoredEmailForUserIDAction(action: PreferencesAction.UpdateStoredEmailForUserIDAction) {
        UserInfoUtils.updateUserIDToSharedPreferences(context, action.userID, action.emailID)
    }
}
