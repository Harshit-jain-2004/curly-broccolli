package com.microsoft.notes.sync

import com.microsoft.notes.sync.models.DeltaSyncPayload
import com.microsoft.notes.sync.models.MediaAltTextUpdate
import com.microsoft.notes.sync.models.NoteReferencesDeltaSyncPayload
import com.microsoft.notes.sync.models.RemoteMeetingNote
import com.microsoft.notes.sync.models.RemoteNote
import com.microsoft.notes.sync.models.RemoteNoteReference
import com.microsoft.notes.sync.models.Token
import okio.BufferedSource
import java.net.URL

sealed class ApiResponseEvent {
    data class DeltaSync(val deltaToken: Token.Delta, val payloads: List<DeltaSyncPayload>) : ApiResponseEvent()
    data class FullSync(val deltaToken: Token.Delta?, val remoteNotes: List<RemoteNote>) : ApiResponseEvent()
    data class NoteCreated(val localId: String, val remoteNote: RemoteNote) : ApiResponseEvent()
    data class NoteReferenceFullSync(val deltaToken: Token.Delta?, val remoteNoteReferences: List<RemoteNoteReference>) : ApiResponseEvent()
    data class MeetingNoteFullSync(val remoteMeetingNotes: List<RemoteMeetingNote>) : ApiResponseEvent()
    data class NoteReferenceDeltaSync(val deltaToken: Token.Delta, val payloads: List<NoteReferencesDeltaSyncPayload>) :
        ApiResponseEvent()
    data class SamsungNoteFullSync(val deltaToken: Token.Delta?, val remoteSamsungNotes: List<RemoteNote>) : ApiResponseEvent()
    data class SamsungNoteDeltaSync(val deltaToken: Token.Delta, val payloads: List<DeltaSyncPayload>) : ApiResponseEvent()

    data class NoteUpdated(
        val localId: String,
        val remoteNote: RemoteNote,
        val uiBaseRevision: Long
    ) : ApiResponseEvent()

    data class NoteFetchedForMerge(
        val localId: String,
        val remoteNote: RemoteNote,
        val uiBaseRevision: Long
    ) : ApiResponseEvent()

    data class NoteDeleted(val localId: String, val remoteId: String) : ApiResponseEvent()
    data class NoteReferenceDeleted(val localId: String, val remoteId: String) : ApiResponseEvent()
    data class SamsungNoteDeleted(val localId: String, val remoteId: String) : ApiResponseEvent()
    data class MediaUploaded(
        val noteId: String,
        val mediaLocalId: String,
        val localUrl: String,
        val mediaRemoteId: String
    ) : ApiResponseEvent()

    data class MediaDownloaded(
        val noteId: String,
        val mediaRemoteId: String,
        val mimeType: String,
        val data: BufferedSource
    ) : ApiResponseEvent()

    data class MediaDeleted(val noteId: String, val mediaLocalId: String, val mediaRemoteId: String) :
        ApiResponseEvent()

    data class MediaAltTextUpdated(val noteId: String, val mediaAltTextUpdate: MediaAltTextUpdate) :
        ApiResponseEvent()

    // 401 error
    data class NotAuthorized(val userID: String) : ApiResponseEvent()

    // 403 error
    data class ForbiddenError(val error: ErrorType) : ApiResponseEvent() {
        sealed class ErrorType {
            object NoMailbox : ErrorType()
            object QuotaExceeded : ErrorType()
            data class GenericSyncError(val supportUrl: URL?) : ErrorType()
        }
    }

    // 404 error
    data class Gone(val operation: ApiRequestOperation) : ApiResponseEvent()

    // 410 error
    class InvalidateClientCache : ApiResponseEvent()

    // 410 error for NoteReferences
    class InvalidateNoteReferencesClientCache : ApiResponseEvent()

    class InvalidateSamsungNotesClientCache : ApiResponseEvent()

    // 426 error
    class UpgradeRequired : ApiResponseEvent()

    // Sync State responses
    class RemoteNotesSyncStarted : ApiResponseEvent()

    class RemoteNotesSyncError(val error: SyncErrorType) : ApiResponseEvent() {
        enum class SyncErrorType {
            NetworkUnavailable, Unauthenticated, SyncPaused, SyncFailure
        }
    }

    class RemoteNotesSyncFailed : ApiResponseEvent()

    class RemoteNotesSyncSucceeded : ApiResponseEvent()

    // Note Reference Sync State responses
    class RemoteNoteReferencesSyncStarted : ApiResponseEvent()

    class RemoteNoteReferencesSyncSucceeded : ApiResponseEvent()

    class RemoteNoteReferencesSyncFailed : ApiResponseEvent()

    // Meeting Notes Sync State responses
    class RemoteMeetingNotesSyncStarted : ApiResponseEvent()

    class RemoteMeetingNotesSyncSucceeded : ApiResponseEvent()

    class RemoteMeetingNotesSyncFailed : ApiResponseEvent()

    // Samsung Notes Sync State responses
    class RemoteSamsungNotesSyncStarted : ApiResponseEvent()

    class RemoteSamsungNotesSyncSucceeded : ApiResponseEvent()

    class RemoteSamsungNotesSyncFailed : ApiResponseEvent()

    // Sync Status
    class OutboundQueueSyncActive : ApiResponseEvent()

    class OutboundQueueSyncInactive : ApiResponseEvent()
}
