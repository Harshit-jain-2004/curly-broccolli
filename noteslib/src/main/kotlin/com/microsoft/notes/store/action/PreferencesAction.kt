package com.microsoft.notes.store.action

sealed class PreferencesAction(val userID: String) : Action {
    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is UpdateEmailForUserIDAction -> "UpdateEmailForUserIDAction"
            is UpdateStoredEmailForUserIDAction -> "UpdateStoredEmailForUserIDAction"
        }

        return "PreferencesAction $actionType"
    }

    class UpdateEmailForUserIDAction(val emailID: String, userID: String) : PreferencesAction(userID)
    class UpdateStoredEmailForUserIDAction(val emailID: String, userID: String) : PreferencesAction(userID)
}
