package com.microsoft.notes.store.state

import com.microsoft.notes.sideeffect.ui.Notifications
import com.microsoft.notes.sideeffect.ui.SyncStateUpdates
import com.microsoft.notes.store.SyncErrorState
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.action.SyncStateAction

fun toSyncResponseError(errorType: SyncStateAction.RemoteNotesSyncErrorAction.SyncErrorType): SyncErrorState? {
    val intermediaryErrorTypeMapping = when (errorType) {
        SyncStateAction.RemoteNotesSyncErrorAction.SyncErrorType.NetworkUnavailable ->
            SyncStateUpdates.SyncErrorType.NetworkUnavailable
        SyncStateAction.RemoteNotesSyncErrorAction.SyncErrorType.Unauthenticated ->
            SyncStateUpdates.SyncErrorType.Unauthenticated
        SyncStateAction.RemoteNotesSyncErrorAction.SyncErrorType.AutoDiscoverGenericFailure ->
            SyncStateUpdates.SyncErrorType.AutoDiscoverGenericFailure
        SyncStateAction.RemoteNotesSyncErrorAction.SyncErrorType.EnvironmentNotSupported ->
            SyncStateUpdates.SyncErrorType.EnvironmentNotSupported
        SyncStateAction.RemoteNotesSyncErrorAction.SyncErrorType.UserNotFoundInAutoDiscover ->
            SyncStateUpdates.SyncErrorType.UserNotFoundInAutoDiscover
        SyncStateAction.RemoteNotesSyncErrorAction.SyncErrorType.SyncPaused ->
            SyncStateUpdates.SyncErrorType.SyncPaused
        SyncStateAction.RemoteNotesSyncErrorAction.SyncErrorType.SyncFailure ->
            SyncStateUpdates.SyncErrorType.SyncFailure
    }
    return toSyncResponseError(intermediaryErrorTypeMapping)
}

fun toSyncResponseError(errorType: SyncResponseAction.ForbiddenSyncError): SyncErrorState? {
    return when (errorType) {
        is SyncResponseAction.ForbiddenSyncError.NoMailbox -> SyncErrorState.NoMailbox
        is SyncResponseAction.ForbiddenSyncError.QuotaExceeded -> SyncErrorState.QuotaExceeded
        is SyncResponseAction.ForbiddenSyncError.GenericSyncError -> SyncErrorState.GenericError
    }
}

fun toSyncResponseError(errorType: SyncResponseAction.ServiceUpgradeRequired): SyncErrorState? =
    SyncErrorState.UpgradeRequired

fun toSyncResponseError(errorType: Notifications.SyncError): SyncErrorState {
    return when (errorType) {
        is Notifications.SyncError.NoMailbox -> SyncErrorState.NoMailbox
        is Notifications.SyncError.QuotaExceeded -> SyncErrorState.QuotaExceeded
        is Notifications.SyncError.GenericError -> SyncErrorState.GenericError
    }
}

fun toSyncResponseError(errorType: SyncStateUpdates.SyncErrorType): SyncErrorState {
    return when (errorType) {
        // We do not want to surface all error types as shown errors
        SyncStateUpdates.SyncErrorType.SyncFailure -> SyncErrorState.None
        SyncStateUpdates.SyncErrorType.SyncPaused -> SyncErrorState.None
        SyncStateUpdates.SyncErrorType.Unauthenticated -> SyncErrorState.Unauthenticated
        SyncStateUpdates.SyncErrorType.AutoDiscoverGenericFailure -> SyncErrorState.AutoDiscoverGenericFailure
        SyncStateUpdates.SyncErrorType.EnvironmentNotSupported -> SyncErrorState.EnvironmentNotSupported
        SyncStateUpdates.SyncErrorType.UserNotFoundInAutoDiscover -> SyncErrorState.UserNotFoundInAutoDiscover
        SyncStateUpdates.SyncErrorType.NetworkUnavailable -> SyncErrorState.NetworkUnavailable
    }
}
