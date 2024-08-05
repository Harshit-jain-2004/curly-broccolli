package com.microsoft.notes.ui.noteslist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.store.SyncErrorState

fun toUserNotificationUiStringsForSync(
    syncErrorState: SyncErrorState,
    context: Context,
    userID: String,
    showEmailInfo: Boolean
): UserNotificationUiRes? {
    val syncErrorResIds = toSyncErrorStringIds(syncErrorState, showEmailInfo)
    if (syncErrorResIds != null) {
        return toUserNotificationUiStrings(
            syncErrorResIds, context, showEmailInfo,
            NotesLibrary.getInstance().getEmailForUserID(userID)
        )
            .copy(buttonInfo = getClickEventInfo(syncErrorState, context, userID))
    }
    return null
}

private fun toUserNotificationUiStrings(
    syncErrorResIds: SyncErrorResIds,
    context: Context,
    showEmailInfo: Boolean,
    email: String
): UserNotificationUiRes =
    UserNotificationUiRes(
        title = context.getString(syncErrorResIds.titleId),
        description = getDescriptionString(context, syncErrorResIds.descriptionId, showEmailInfo, email),
        iconResId = syncErrorResIds.errorIconOverrideResId,
        type = UserNotificationType.SyncError
    )

fun toUserNotificationUiStringsForFutureNote(futureNoteCount: Int, context: Context): UserNotificationUiRes? {
    if (futureNoteCount == 0)
        return null
    val title = when (futureNoteCount) {
        1 -> context.getString(R.string.sn_user_notification_single_future_note_title)
        else -> context.getString(R.string.sn_user_notification_multiple_future_note_title, futureNoteCount)
    }

    return UserNotificationUiRes(
        title = title,
        description = "",
        iconResId = defaultSyncErrorIcon,
        type = UserNotificationType.FutureNote
    )
}

fun toUserNotificationUiStringsForAuth(context: Context, userID: String, showEmailInfo: Boolean):
    UserNotificationUiRes? {
    val mappedSyncErrorState = SyncErrorState.Unauthenticated
    val syncErrorResIds = toSyncErrorStringIds(mappedSyncErrorState, showEmailInfo)
    if (syncErrorResIds != null) {
        return toUserNotificationUiStrings(
            syncErrorResIds, context, showEmailInfo,
            NotesLibrary.getInstance().getEmailForUserID(userID)
        )
            .copy(buttonInfo = getClickEventInfo(mappedSyncErrorState, context, userID))
    }
    return null
}

private fun getClickEventInfo(syncErrorState: SyncErrorState, context: Context, userID: String): NotificationButtonInfo? =
    when (syncErrorState) {
        SyncErrorState.Unauthenticated -> NotificationButtonInfo(
            title = context.getString(R.string.sn_not_authenticated_action_title),
            event = { NotesLibrary.getInstance().sendRequestClientAuthAction(userID) }
        )
        SyncErrorState.NoMailbox,
        SyncErrorState.GenericError -> {
            getStickyNotesHelpUrl(syncErrorState)?.let {
                return@let when (isValidWebUrl(it)) {
                    true -> NotificationButtonInfo(
                        title = context.getString(R.string.sn_learn_more_action_title),
                        event = { gotoStickyNotesHelpLink(context, it) }
                    )
                    false -> null
                }
            }
        }

        else -> null
    }

private fun gotoStickyNotesHelpLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    context.startActivity(intent)
}

private fun getStickyNotesHelpUrl(syncErrorState: SyncErrorState) =
    when (syncErrorState) {
        is SyncErrorState.NoMailbox -> "https://aka.ms/stickynotessupport"
        is SyncErrorState.GenericError -> "https://support.microsoft.com/en-us/office/troubleshoot-sticky-notes-89b1bb37-ef52-4e56-a066-418d7ea0f112?ui=en-us&rs=en-us&ad=us"
        else -> null
    }

private fun isValidWebUrl(url: String): Boolean = Patterns.WEB_URL.matcher(url).matches()

private fun getDescriptionString(
    context: Context,
    descriptionId: Int?,
    showEmailInfo: Boolean,
    email: String
): String {
    return descriptionId?.let {
        if (showEmailInfo) {
            context.getString(descriptionId, email)
        } else {
            context.getString(descriptionId)
        }
    } ?: ""
}
