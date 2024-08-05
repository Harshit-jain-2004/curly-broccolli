package com.microsoft.notes.ui.noteslist

import android.content.Context

internal fun UserNotification.toUserNotificationResIDsLocal(context: Context, userID: String, showEmailInfo: Boolean):
    UserNotificationUiRes? =
    when (this) {
        is FutureNoteUserNotification -> toUserNotificationUiStringsForFutureNote(futureNoteCount, context)
        is SyncErrorUserNotification -> toUserNotificationUiStringsForSync(
            syncErrorType,
            context = context,
            userID = userID,
            showEmailInfo = showEmailInfo
        )
        is AuthErrorUserNotification -> toUserNotificationUiStringsForAuth(
            context = context,
            userID = userID,
            showEmailInfo = showEmailInfo
        )
        else -> null
    }
