package com.microsoft.notes.store.action

sealed class SyncStateAction(val userID: String) : Action {

    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is RemoteNotesSyncStartedAction -> "RemoteNotesSyncStartedAction"
            is RemoteNoteReferencesSyncStartedAction -> "RemoteNoteReferencesSyncStartedAction"
            is RemoteSamsungNotesSyncStartedAction -> "RemoteSamsungNotesSyncStartedAction"
            is RemoteMeetingNotesSyncStartedAction -> "RemoteMeetingNotesSyncStartedAction"
            is RemoteNotesSyncErrorAction -> "RemoteNotesSyncErrorAction.{$errorType.name}"
            is RemoteNotesSyncFailedAction -> "RemoteNotesSyncFailedAction"
            is RemoteNoteReferencesSyncFailedAction -> "RemoteNoteReferencesSyncFailedAction"
            is RemoteSamsungNotesSyncFailedAction -> "RemoteSamsungNotesSyncFailedAction"
            is RemoteMeetingNotesSyncFailedAction -> "RemoteMeetingNotesSyncFailedAction"
            is RemoteNotesSyncSucceededAction -> "RemoteNotesSyncSucceededAction"
            is RemoteNoteReferencesSyncSucceededAction -> "RemoteNoteReferencesSyncSucceededAction"
            is RemoteSamsungNotesSyncSucceededAction -> "RemoteSamsungNotesSyncSucceededAction"
            is RemoteMeetingNotesSyncSucceededAction -> "RemoteMeetingNotesSyncSucceededAction"
        }

        return "SyncStateAction.$actionType"
    }

    class RemoteNotesSyncStartedAction(userID: String) : SyncStateAction(userID)
    class RemoteNoteReferencesSyncStartedAction(userID: String) : SyncStateAction(userID)
    class RemoteSamsungNotesSyncStartedAction(userID: String) : SyncStateAction(userID)
    class RemoteMeetingNotesSyncStartedAction(userID: String) : SyncStateAction(userID)

    class RemoteNotesSyncErrorAction(val errorType: SyncErrorType, userID: String) : SyncStateAction(userID) {
        enum class SyncErrorType {
            NetworkUnavailable,
            Unauthenticated,
            AutoDiscoverGenericFailure,
            EnvironmentNotSupported,
            UserNotFoundInAutoDiscover,
            SyncPaused,
            SyncFailure
        }
    }

    class RemoteNotesSyncFailedAction(userID: String) : SyncStateAction(userID)
    class RemoteNoteReferencesSyncFailedAction(userID: String) : SyncStateAction(userID)
    class RemoteSamsungNotesSyncFailedAction(userID: String) : SyncStateAction(userID)
    class RemoteMeetingNotesSyncFailedAction(userID: String) : SyncStateAction(userID)

    class RemoteNotesSyncSucceededAction(userID: String) : SyncStateAction(userID)
    class RemoteNoteReferencesSyncSucceededAction(userID: String) : SyncStateAction(userID)
    class RemoteSamsungNotesSyncSucceededAction(userID: String) : SyncStateAction(userID)
    class RemoteMeetingNotesSyncSucceededAction(userID: String) : SyncStateAction(userID)
}
