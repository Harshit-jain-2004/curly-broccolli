package com.microsoft.notes.ui.noteslist

import com.microsoft.notes.noteslib.R
import com.microsoft.notes.sideeffect.ui.Notifications
import com.microsoft.notes.sideeffect.ui.SyncStateUpdates
import com.microsoft.notes.store.SyncErrorState
import com.microsoft.notes.store.state.toSyncResponseError

internal val defaultSyncErrorIcon = R.drawable.sn_sync_error_icon

fun toSyncErrorStringIds(errorType: Notifications.SyncError): SyncErrorResIds? =
    toSyncErrorStringIds(toSyncResponseError(errorType))

fun toSyncErrorStringIds(errorType: SyncStateUpdates.SyncErrorType): SyncErrorResIds? =
    toSyncErrorStringIds(toSyncResponseError(errorType))

fun toSyncErrorStringIds(errorType: SyncErrorState, showEmailInfo: Boolean = false): SyncErrorResIds? {
    return when (errorType) {
        SyncErrorState.None -> null
        SyncErrorState.NoMailbox -> SyncErrorResIds(
            R.string.sn_sync_status_mailbox_error_title,
            if (showEmailInfo) R.string.sn_sync_status_mailbox_error_multi_account_description else
                R.string.sn_sync_status_mailbox_error_description,
            defaultSyncErrorIcon
        )
        SyncErrorState.QuotaExceeded -> null
        SyncErrorState.GenericError -> SyncErrorResIds(
            R.string.sn_sync_status_generic_error_title,
            if (showEmailInfo) R.string.sn_sync_status_generic_error_multi_account_description else
                R.string.sn_sync_status_generic_error_description,
            defaultSyncErrorIcon
        )
        SyncErrorState.NetworkUnavailable -> null
        SyncErrorState.Unauthenticated -> SyncErrorResIds(
            R.string.sn_sync_status_unauthenticated_title,
            if (showEmailInfo) R.string.sn_sync_status_unauthenticated_multi_account_description else
                R.string.sn_sync_status_unauthenticated_description,
            defaultSyncErrorIcon
        )
        SyncErrorState.AutoDiscoverGenericFailure -> SyncErrorResIds(
            R.string.sn_sync_status_generic_error_title,
            if (showEmailInfo) R.string.sn_sync_status_generic_error_multi_account_description else
                R.string.sn_sync_status_generic_error_description,
            defaultSyncErrorIcon
        )
        SyncErrorState.EnvironmentNotSupported -> SyncErrorResIds(
            R.string.sn_sync_status_environment_not_supported_title,
            if (showEmailInfo) R.string.sn_sync_status_environment_not_supported_multi_account_description else
                R.string.sn_sync_status_environment_not_supported_description,
            defaultSyncErrorIcon
        )
        SyncErrorState.UserNotFoundInAutoDiscover -> SyncErrorResIds(
            R.string.sn_sync_status_user_not_found_title,
            if (showEmailInfo) R.string.sn_sync_status_user_not_found_multi_account_description else
                R.string.sn_sync_status_user_not_found_description,
            defaultSyncErrorIcon
        )
        SyncErrorState.UpgradeRequired -> null
    }
}
