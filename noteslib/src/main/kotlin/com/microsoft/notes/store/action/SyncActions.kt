package com.microsoft.notes.store.action

import com.microsoft.notes.models.AccountType
import com.microsoft.notes.models.Changes
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.RemoteData
import com.microsoft.notes.sync.NotesClientHost
import com.microsoft.notes.utils.utils.UserInfo
import com.microsoft.notes.utils.utils.toNullabilityIdentifierString
import java.net.URL

sealed class AuthAction(val userID: String) : Action {
    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is NewAuthTokenAction -> "NewAuthTokenAction"
            is ClientAuthFailedAction -> "ClientAuthFailedAction"
            is LogoutAction -> "LogoutAction"
            is AccountInfoForIntuneProtection -> "AccountInfoForIntuneProtection"
        }

        return "AuthAction.$actionType"
    }

    class NewAuthTokenAction(val userInfo: UserInfo) : AuthAction(userInfo.userID) {
        override fun toPIIFreeString(): String =
            "${toLoggingIdentifier()}: accountType = ${userInfo.accountType.name}"
    }

    class ClientAuthFailedAction(userID: String) : AuthAction(userID)

    class LogoutAction(userID: String) : AuthAction(userID)

    class AccountInfoForIntuneProtection(val databaseName: String, userID: String, val accountType: AccountType) :
        AuthAction(userID)
}

sealed class AuthenticatedSyncRequestAction(val userID: String) : Action {

    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is RemoteChangedDetected -> "RemoteChangedDetected"
            is ManualSyncRequestAction -> "ManualSyncRequestAction"
            is ForceFullSyncRequestAction -> "ForceFullSyncRequestAction"
            is ManualNoteReferencesSyncRequestAction -> "ManualNoteReferencesSyncRequestAction"
            is ManualMeetingNotesSyncRequestAction -> "ManualMeetingNotesSyncRequestAction"
            is RemoteNoteReferencesChangedDetected -> "RemoteNoteReferenceChangeDetected"
            is ManualSamsungNotesSyncRequestAction -> "ManualSamsungNotesSyncRequestAction"
            is SamsungNotesChangedDetected -> "SamsungNotesChangedDetected"
            is MeetingNotesChangedDetected -> "MeetingNotesChangedDetected"
        }

        return "AuthenticatedSyncRequestAction.$actionType"
    }

    class RemoteChangedDetected(userID: String) : AuthenticatedSyncRequestAction(userID)

    class RemoteNoteReferencesChangedDetected(userID: String) : AuthenticatedSyncRequestAction(userID)

    class SamsungNotesChangedDetected(userID: String) : AuthenticatedSyncRequestAction(userID)

    class ManualSyncRequestAction(userID: String) : AuthenticatedSyncRequestAction(userID)

    class ForceFullSyncRequestAction(userID: String) : AuthenticatedSyncRequestAction(userID)

    class ManualSamsungNotesSyncRequestAction(userID: String) : AuthenticatedSyncRequestAction(userID)

    class ManualNoteReferencesSyncRequestAction(userID: String) : AuthenticatedSyncRequestAction(userID)

    class ManualMeetingNotesSyncRequestAction(userID: String) : AuthenticatedSyncRequestAction(userID)

    class MeetingNotesChangedDetected(userID: String) : AuthenticatedSyncRequestAction(userID)
}

sealed class SyncRequestAction(val userID: String) : Action {

    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is CreateNote -> "CreateNote"
            is UpdateNote -> "UpdateNote"
            is DeleteNote -> "DeleteNote"
            is DeleteNoteReference -> "DeleteNoteReference"
            is DeleteSamsungNote -> "DeleteSamsungNote"
            is UploadMedia -> "UploadMedia"
            is DeleteMedia -> "DeleteMedia"
            is UpdateMediaAltText -> "UpdateMediaAltText"
        }

        return "SyncRequestAction.$actionType"
    }

    class CreateNote(val note: Note, userID: String) : SyncRequestAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: noteId = ${note.localId}"
    }

    class UpdateNote(val note: Note, val uiRevision: Long, userID: String) : SyncRequestAction(userID) {
        override fun toPIIFreeString(): String =
            "${toLoggingIdentifier()}: noteId = ${note.localId}, uiRevision = $uiRevision"
    }

    class DeleteNote(val localId: String, val remoteId: String?, userID: String) :
        SyncRequestAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: noteId = $localId"
    }

    class DeleteNoteReference(val localId: String, val remoteId: String, userID: String) :
        SyncRequestAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: noteId = $localId"
    }

    class DeleteSamsungNote(val localId: String, val remoteId: String, userID: String) :
        SyncRequestAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: noteId = $localId"
    }

    class UploadMedia(
        val note: Note,
        val mediaLocalId: String,
        val localUrl: String,
        val mimeType: String,
        userID: String
    ) :
        SyncRequestAction(userID) {
        override fun toPIIFreeString(): String =
            "${toLoggingIdentifier()}: noteId = ${note.localId}, LocalMediaId = $mediaLocalId"
    }

    class DeleteMedia(
        val localNoteId: String,
        val remoteNoteId: String?,
        val mediaLocalId: String,
        val mediaRemoteId: String?,
        userID: String
    ) : SyncRequestAction(userID) {
        override fun toPIIFreeString(): String =
            "${toLoggingIdentifier()}: noteId = $localNoteId, mediaId = $mediaLocalId"
    }

    class UpdateMediaAltText(
        val note: Note,
        val localNoteId: String,
        val remoteNoteId: String?,
        val mediaLocalId: String,
        val mediaRemoteId: String?,
        val altText: String?,
        userID: String
    ) : SyncRequestAction(userID) {
        override fun toPIIFreeString(): String =
            "${toLoggingIdentifier()}: noteId = $localNoteId, mediaId = $mediaLocalId"
    }
}

sealed class SyncResponseAction(val userID: String) : Action {
    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is ApplyChanges -> "ApplyChanges"
            is PermanentlyDeleteNote -> "PermanentlyDeleteNote"
            is PermanentlyDeleteNoteReference -> "PermanentlyDeleteNoteReference"
            is PermanentlyDeleteSamsungNote -> "PermanentlyDeleteSamsungNote"
            is RemoteDataUpdated -> "RemoteDataUpdated"
            is ApplyConflictResolution -> "ApplyConflictResolution"
            is MediaUploaded -> "MediaUploaded"
            is MediaDownloaded -> "MediaDownloaded"
            is NotAuthorized -> "NotAuthorized"
            is ForbiddenSyncError -> "ForbiddenSyncError"
            is InvalidateClientCache -> "InvalidateClientCache"
            is InvalidateNoteReferencesClientCache -> "InvalidateClientCacheForNoteReferences"
            is InvalidateSamsungNotesClientCache -> "InvalidateSamsungNotesClientCache"
            is ServiceUpgradeRequired -> "ServiceUpgradeRequired"
            is MediaDeleted -> "MediaDeleted"
            is MediaAltTextUpdated -> "MediaAltTextUpdated"
        }

        return "SyncResponseAction.$actionType"
    }

    class ApplyChanges(val changes: Changes, val deltaToken: String?, userID: String) : SyncResponseAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: changes = $changes" +
            ", deltaToken = ${toNullabilityIdentifierString(deltaToken)}"
    }

    class PermanentlyDeleteNote(val noteLocalId: String, userID: String) : SyncResponseAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: noteId = $noteLocalId"
    }

    class PermanentlyDeleteNoteReference(val noteLocalId: String, userID: String) : SyncResponseAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: noteId = $noteLocalId"
    }

    class PermanentlyDeleteSamsungNote(val noteLocalId: String, userID: String) : SyncResponseAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: noteId = $noteLocalId"
    }

    class RemoteDataUpdated(val noteLocalId: String, val remoteData: RemoteData, userID: String) :
        SyncResponseAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: noteId = $noteLocalId"
    }

    class ApplyConflictResolution(
        val noteLocalId: String,
        val remoteNote: Note,
        val uiBaseRevision: Long,
        userID: String
    ) :
        SyncResponseAction(userID) {
        override fun toPIIFreeString(): String =
            "${toLoggingIdentifier()}: noteId = $noteLocalId, uiBaseRevision = $uiBaseRevision"
    }

    class MediaUploaded(
        val noteId: String,
        val mediaLocalId: String,
        val localUrl: String,
        val mediaRemoteId: String,
        userID: String
    ) :
        SyncResponseAction(userID) {
        override fun toPIIFreeString(): String =
            "${toLoggingIdentifier()}: noteId = $noteId, mediaRemoteId = $mediaRemoteId"
    }

    class MediaDownloaded(
        val noteId: String,
        val mediaRemoteId: String,
        val localUrl: String,
        val mimeType: String,
        userID: String
    ) : SyncResponseAction(userID) {
        override fun toPIIFreeString(): String =
            "${toLoggingIdentifier()}: noteId = $noteId, mediaRemoteId = $mediaRemoteId"
    }

    class MediaDeleted(
        val noteId: String,
        val mediaLocalId: String,
        val mediaRemoteId: String,
        userID: String
    ) : SyncResponseAction(userID) {
        override fun toPIIFreeString(): String =
            "${toLoggingIdentifier()}: noteId = $noteId, mediaRemoteId = $mediaRemoteId"
    }

    class MediaAltTextUpdated(
        val noteId: String,
        val media: Media,
        val changeKey: String,
        userID: String
    ) : SyncResponseAction(userID) {
        override fun toPIIFreeString(): String =
            "${toLoggingIdentifier()}: noteId = $noteId, mediaRemoteId = ${media.remoteId}"
    }

    class NotAuthorized(userID: String) : SyncResponseAction(userID)
    sealed class ForbiddenSyncError(userID: String) : SyncResponseAction(userID) {
        class NoMailbox(val errorMessage: Int, val supportArticle: URL?, userID: String) :
            ForbiddenSyncError(userID)

        class QuotaExceeded(val errorMessage: Int, val supportArticle: URL?, userID: String) :
            ForbiddenSyncError(userID)

        class GenericSyncError(val errorMessage: Int, val supportArticle: URL?, userID: String) :
            ForbiddenSyncError(userID)
    }

    class InvalidateClientCache(userID: String) : SyncResponseAction(userID)
    class InvalidateNoteReferencesClientCache(userID: String) : SyncResponseAction(userID)
    class InvalidateSamsungNotesClientCache(userID: String) : SyncResponseAction(userID)
    class ServiceUpgradeRequired(userID: String) : SyncResponseAction(userID)
}

sealed class PollingAction(val userID: String) : Action {
    class Start(userID: String) : PollingAction(userID)
    class Stop(userID: String) : PollingAction(userID)

    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is Start -> "Start"
            is Stop -> "Stop"
        }
        return "PollingAction.$actionType"
    }
}

sealed class AutoDiscoverAction(val userID: String) : Action {
    class CacheHostUrl(val host: NotesClientHost.ExpirableHost, userID: String) :
        AutoDiscoverAction(userID)

    class SetHostUrl(val host: NotesClientHost, userID: String) :
        AutoDiscoverAction(userID)

    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is CacheHostUrl -> "CacheHostUrl"
            is SetHostUrl -> "SetHostUrl"
        }
        return "AutoDiscoverAction.$actionType"
    }
}
