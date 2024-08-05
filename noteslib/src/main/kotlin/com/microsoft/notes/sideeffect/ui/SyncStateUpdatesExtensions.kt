package com.microsoft.notes.sideeffect.ui

import com.microsoft.notes.utils.logging.ManualSyncRequestStatus

fun SyncStateUpdates.SyncErrorType.toTelemetryErrorType(): ManualSyncRequestStatus {
    return when (this) {
        SyncStateUpdates.SyncErrorType.NetworkUnavailable -> ManualSyncRequestStatus.NetworkUnavailable
        SyncStateUpdates.SyncErrorType.Unauthenticated -> ManualSyncRequestStatus.Unauthenticated
        SyncStateUpdates.SyncErrorType.AutoDiscoverGenericFailure -> ManualSyncRequestStatus.AutoDiscoverGenericFailure
        SyncStateUpdates.SyncErrorType.EnvironmentNotSupported -> ManualSyncRequestStatus.EnvironmentNotSupported
        SyncStateUpdates.SyncErrorType.UserNotFoundInAutoDiscover -> ManualSyncRequestStatus.UserNotFoundInAutoDiscover
        SyncStateUpdates.SyncErrorType.SyncPaused -> ManualSyncRequestStatus.SyncPaused
        SyncStateUpdates.SyncErrorType.SyncFailure -> ManualSyncRequestStatus.SyncFailure
    }
}
