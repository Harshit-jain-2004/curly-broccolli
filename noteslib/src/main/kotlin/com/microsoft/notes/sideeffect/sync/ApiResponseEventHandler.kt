package com.microsoft.notes.sideeffect.sync

import android.util.Base64
import android.widget.Toast
import com.microsoft.notes.models.Changes
import com.microsoft.notes.models.ImageDimensions
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReferenceChanges
import com.microsoft.notes.models.RemoteData
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.noteslib.extensions.calculateNoteReferencesChanges
import com.microsoft.notes.noteslib.extensions.calculateNoteReferencesChangesForHybrid
import com.microsoft.notes.noteslib.extensions.calculateNoteReferencesDeltaSyncChanges
import com.microsoft.notes.noteslib.extensions.calculateNoteReferencesDeltaSyncChangesForHybrid
import com.microsoft.notes.noteslib.extensions.calculateSamsungNoteChanges
import com.microsoft.notes.noteslib.extensions.calculateSamsungNotesDeltaSyncChanges
import com.microsoft.notes.sideeffect.sync.mapper.toLastServerVersion
import com.microsoft.notes.sideeffect.sync.mapper.toStoreNote
import com.microsoft.notes.sideeffect.sync.mapper.toStoreSyncErrorType
import com.microsoft.notes.store.Store
import com.microsoft.notes.store.action.NoteReferenceAction
import com.microsoft.notes.store.action.OutboundQueueSyncStatusAction
import com.microsoft.notes.store.action.SamsungNotesResponseAction
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.action.SyncStateAction
import com.microsoft.notes.store.getNoteForNoteLocalId
import com.microsoft.notes.store.getNoteReferencesCollectionForUser
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.store.getSamsungNotesCollectionForUser
import com.microsoft.notes.sync.ApiRequestOperation
import com.microsoft.notes.sync.ApiResponseEvent
import com.microsoft.notes.sync.ApiResponseEvent.DeltaSync
import com.microsoft.notes.sync.ApiResponseEvent.FullSync
import com.microsoft.notes.sync.ApiResponseEvent.InvalidateClientCache
import com.microsoft.notes.sync.ApiResponseEvent.InvalidateNoteReferencesClientCache
import com.microsoft.notes.sync.ApiResponseEvent.MediaAltTextUpdated
import com.microsoft.notes.sync.ApiResponseEvent.MediaDeleted
import com.microsoft.notes.sync.ApiResponseEvent.MediaDownloaded
import com.microsoft.notes.sync.ApiResponseEvent.MediaUploaded
import com.microsoft.notes.sync.ApiResponseEvent.NotAuthorized
import com.microsoft.notes.sync.ApiResponseEvent.NoteCreated
import com.microsoft.notes.sync.ApiResponseEvent.NoteDeleted
import com.microsoft.notes.sync.ApiResponseEvent.NoteFetchedForMerge
import com.microsoft.notes.sync.ApiResponseEvent.NoteReferenceDeleted
import com.microsoft.notes.sync.ApiResponseEvent.NoteReferenceDeltaSync
import com.microsoft.notes.sync.ApiResponseEvent.NoteReferenceFullSync
import com.microsoft.notes.sync.ApiResponseEvent.NoteUpdated
import com.microsoft.notes.sync.ApiResponseEvent.SamsungNoteDeleted
import com.microsoft.notes.sync.ApiResponseEvent.SamsungNoteDeltaSync
import com.microsoft.notes.sync.ApiResponseEvent.SamsungNoteFullSync
import com.microsoft.notes.sync.ApiResponseEvent.UpgradeRequired
import com.microsoft.notes.sync.ApiResponseEventHandler
import com.microsoft.notes.sync.models.DeltaSyncPayload
import com.microsoft.notes.sync.models.Document
import com.microsoft.notes.sync.models.RemoteNote
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.ui.extensions.isBelowSizeLimitForStorage
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.utils.utils.Constants
import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.UUID

class ApiResponseEventHandler(
    val store: Store,
    private val createFile: (String) -> File,
    val mimeTypeToFileExtension: (String) -> String,
    val decodeBase64: (String) -> ByteArray,
    var userID: String
) : ApiResponseEventHandler {

    internal var deltaToken: Token.Delta? = null
    internal var samsungDeltaToken: Token.Delta? = null
    internal var noteReferencesDeltaToken: Token.Delta? = null
    private val lastKnownUIRevisions = mutableMapOf<String, Long>()
    private val noExchangeMailboxSupportPageURL = "https://aka.ms/stickynotessupport"

    internal fun reset() {
        deltaToken = null
        samsungDeltaToken = null
        noteReferencesDeltaToken = null
        lastKnownUIRevisions.clear()
    }

    fun resetNotesDeltaToken() {
        deltaToken = null
        store.dispatch(SyncResponseAction.ApplyChanges(Changes(), null, userID))
    }

    private fun resetNotes() {
        deltaToken = null
        lastKnownUIRevisions.clear()
    }

    private fun resetNoteReferences() {
        noteReferencesDeltaToken = null
    }

    private fun resetSamsungNotes() {
        samsungDeltaToken = null
    }

    @Suppress("LongMethod")
    override fun handleEvent(apiResponseEvent: ApiResponseEvent) {
        when (apiResponseEvent) {
            is FullSync -> {
                val processedRemoteNotes = apiResponseEvent.remoteNotes.map { replaceMediaWithURL(it) }
                val changes = changesForFullSync(
                    localNotes = store.state.getNotesCollectionForUser(userID),
                    remoteNotes = processedRemoteNotes
                )
                handleChanges(changes, apiResponseEvent.deltaToken)
            }
            is DeltaSync -> {
                val processedPayloads = apiResponseEvent.payloads.map {
                    when (it) {
                        is DeltaSyncPayload.NonDeleted -> {
                            it.copy(note = replaceMediaWithURL(it.note))
                        }
                        is DeltaSyncPayload.Deleted -> it
                    }
                }
                val changes = changesForDeltaSync(
                    localNotes = store.state.getNotesCollectionForUser(userID),
                    deltaSyncPayloads = processedPayloads,
                    uiBaseRevisions = lastKnownUIRevisions
                )
                handleChanges(changes, apiResponseEvent.deltaToken)

                // Remove history
                val indexedNotes = getIndexedNotes(store.state.getNotesCollectionForUser(userID))
                val ids = getNoteIdsFromPayloads(indexedNotes, apiResponseEvent.payloads)
                ids.forEach { id -> lastKnownUIRevisions.remove(id) }
            }
            is NoteCreated -> {
                val action = updateRemoteDataAction(apiResponseEvent.localId, apiResponseEvent.remoteNote)
                store.dispatch(action)
            }
            is NoteUpdated -> {
                lastKnownUIRevisions[apiResponseEvent.localId] = apiResponseEvent.uiBaseRevision
                val action = updateRemoteDataAction(
                    apiResponseEvent.localId, apiResponseEvent.remoteNote,
                    apiResponseEvent.uiBaseRevision
                )
                store.dispatch(action)
            }
            is NoteFetchedForMerge -> {
                val (localId, remoteNote, uiBaseRevision) = apiResponseEvent

                val fetchedNote = convertToStoreNote(localId, remoteNote, uiBaseRevision)
                val action = SyncResponseAction.ApplyConflictResolution(
                    localId,
                    fetchedNote,
                    uiBaseRevision,
                    userID
                )
                store.dispatch(action)
            }
            is NoteDeleted -> {
                val action = SyncResponseAction.PermanentlyDeleteNote(apiResponseEvent.localId, userID)
                store.dispatch(action)
            }
            is NoteReferenceDeleted -> {
                val action = SyncResponseAction.PermanentlyDeleteNoteReference(apiResponseEvent.localId, userID)
                store.dispatch(action)
            }
            is SamsungNoteDeleted -> {
                val action = SyncResponseAction.PermanentlyDeleteSamsungNote(apiResponseEvent.localId, userID)
                store.dispatch(action)
            }
            is MediaUploaded -> {
                with(apiResponseEvent) {
                    val action = SyncResponseAction.MediaUploaded(
                        noteId,
                        mediaLocalId,
                        localUrl,
                        mediaRemoteId,
                        userID
                    )
                    store.dispatch(action)
                }
            }
            is MediaDownloaded -> {
                // test
                with(apiResponseEvent) {
                    val localUrl = saveImageMediaStreamToDisk(
                        data, noteId, mediaRemoteId, createFile,
                        mimeTypeToFileExtension, mimeType
                    )
                    data.close()
                    val action = SyncResponseAction.MediaDownloaded(noteId, mediaRemoteId, localUrl, mimeType, userID)
                    store.dispatch(action)
                }
            }
            is MediaDeleted -> {
                with(apiResponseEvent) {
                    val action = SyncResponseAction.MediaDeleted(noteId, mediaLocalId, mediaRemoteId, userID)
                    store.dispatch(action)
                }
            }
            is MediaAltTextUpdated -> {
                with(apiResponseEvent) {
                    val imageDimensions = mediaAltTextUpdate.imageDimensions?.let {
                        ImageDimensions(it.height.toLong(), it.width.toLong())
                    }

                    val media = Media(
                        localId = mediaAltTextUpdate.createdWithLocalId,
                        remoteId = mediaAltTextUpdate.id,
                        localUrl = "",
                        mimeType = mediaAltTextUpdate.mimeType,
                        altText = mediaAltTextUpdate.altText,
                        imageDimensions = imageDimensions,
                        lastModified = mediaAltTextUpdate.lastModified
                    )
                    val action = SyncResponseAction.MediaAltTextUpdated(
                        noteId, media, mediaAltTextUpdate.changeKey, userID
                    )
                    store.dispatch(action)
                }
            }
            is NoteReferenceFullSync -> {
                val changes = if (NotesLibrary.getInstance().experimentFeatureFlags.isHybridFeedEnabled) {
                    calculateNoteReferencesChangesForHybrid(
                        store.state.getNoteReferencesCollectionForUser(userID),
                        apiResponseEvent.remoteNoteReferences
                    )
                } else {
                    calculateNoteReferencesChanges(
                        store.state.getNoteReferencesCollectionForUser(userID),
                        apiResponseEvent.remoteNoteReferences
                    )
                }
                handleNoteReferencesSyncChanges(changes, apiResponseEvent.deltaToken)
            }
            is NoteReferenceDeltaSync -> {
                val changes = if (NotesLibrary.getInstance().experimentFeatureFlags.isHybridFeedEnabled) {
                    calculateNoteReferencesDeltaSyncChangesForHybrid(
                        store.state
                            .getNoteReferencesCollectionForUser(userID),
                        apiResponseEvent.payloads, store.notesLogger
                    )
                } else {
                    calculateNoteReferencesDeltaSyncChanges(
                        store.state
                            .getNoteReferencesCollectionForUser(userID),
                        apiResponseEvent.payloads
                    )
                }
                handleNoteReferencesSyncChanges(changes, apiResponseEvent.deltaToken)
            }
            is SamsungNoteFullSync -> {
                val changes = calculateSamsungNoteChanges(
                    store.state.getSamsungNotesCollectionForUser(userID),
                    apiResponseEvent.remoteSamsungNotes
                )
                handleSamsungNotesSyncChanges(changes, apiResponseEvent.deltaToken)
            }
            is SamsungNoteDeltaSync -> {
                val changes = calculateSamsungNotesDeltaSyncChanges(
                    store.state.getSamsungNotesCollectionForUser(userID),
                    apiResponseEvent.payloads
                )
                handleSamsungNotesSyncChanges(changes, apiResponseEvent.deltaToken)
            }
            is NotAuthorized -> {
                val action = SyncResponseAction.NotAuthorized(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.ForbiddenError -> {
                val error = apiResponseEvent.error
                val action = when (error) {
                    is ApiResponseEvent.ForbiddenError.ErrorType.NoMailbox -> {
                        val url = try {
                            URL(noExchangeMailboxSupportPageURL)
                        } catch (e: MalformedURLException) {
                            store.notesLogger?.recordTelemetry(
                                EventMarkers.SyncMalformedUrlException,
                                Pair(NotesSDKTelemetryKeys.SyncProperty.URL, noExchangeMailboxSupportPageURL)
                            )
                            null
                        }
                        SyncResponseAction.ForbiddenSyncError.NoMailbox(
                            R.string.sn_mailbox_creation_failed_message, url, userID
                        )
                    }
                    is ApiResponseEvent.ForbiddenError.ErrorType.QuotaExceeded -> {
                        SyncResponseAction.ForbiddenSyncError.QuotaExceeded(
                            R.string.sn_sync_failure_with_quota_exceeded_message, null, userID
                        )
                    }
                    is ApiResponseEvent.ForbiddenError.ErrorType.GenericSyncError -> {
                        val resId = if (error.supportUrl != null) {
                            R.string.sn_sync_failure_with_available_kb_article_message
                        } else {
                            R.string.sn_sync_failure_with_contact_support_message
                        }
                        SyncResponseAction.ForbiddenSyncError.GenericSyncError(resId, error.supportUrl, userID)
                    }
                }
                store.dispatch(action)
            }
            is InvalidateClientCache -> {
                resetNotes()
                val action = SyncResponseAction.InvalidateClientCache(userID)
                store.dispatch(action)
            }
            is InvalidateNoteReferencesClientCache -> {
                resetNoteReferences()
                val action = SyncResponseAction.InvalidateNoteReferencesClientCache(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.InvalidateSamsungNotesClientCache -> {
                resetSamsungNotes()
                val action = SyncResponseAction.InvalidateSamsungNotesClientCache(userID)
                store.dispatch(action)
            }
            is UpgradeRequired -> {
                val action = SyncResponseAction.ServiceUpgradeRequired(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.RemoteNotesSyncStarted -> {
                val action = SyncStateAction.RemoteNotesSyncStartedAction(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.RemoteNoteReferencesSyncStarted -> {
                val action = SyncStateAction.RemoteNoteReferencesSyncStartedAction(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.RemoteSamsungNotesSyncStarted -> {
                val action = SyncStateAction.RemoteSamsungNotesSyncStartedAction(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.RemoteMeetingNotesSyncStarted -> {
                val action = SyncStateAction.RemoteMeetingNotesSyncStartedAction(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.RemoteNotesSyncError -> {
                val action = SyncStateAction.RemoteNotesSyncErrorAction(
                    apiResponseEvent.error.toStoreSyncErrorType(), userID
                )
                store.dispatch(action)
            }
            is ApiResponseEvent.RemoteNotesSyncFailed -> {
                val action = SyncStateAction.RemoteNotesSyncFailedAction(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.RemoteNoteReferencesSyncFailed -> {
                val action = SyncStateAction.RemoteNoteReferencesSyncFailedAction(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.RemoteSamsungNotesSyncFailed -> {
                val action = SyncStateAction.RemoteSamsungNotesSyncFailedAction(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.RemoteNotesSyncSucceeded -> {
                val action = SyncStateAction.RemoteNotesSyncSucceededAction(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.RemoteNoteReferencesSyncSucceeded -> {
                val action = SyncStateAction.RemoteNoteReferencesSyncSucceededAction(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.RemoteSamsungNotesSyncSucceeded -> {
                val action = SyncStateAction.RemoteSamsungNotesSyncSucceededAction(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.OutboundQueueSyncActive -> {
                val action = OutboundQueueSyncStatusAction.SyncActiveAction(userID)
                store.dispatch(action)
            }
            is ApiResponseEvent.OutboundQueueSyncInactive -> {
                val action = OutboundQueueSyncStatusAction.SyncInactiveAction(userID)
                store.dispatch(action)
            }
            // handle ignored 404 operation corner cases
            //   e.g.   notes marked locally as deleted which have been deleted on another device, integration or
            //          client should be removed from the device
            is ApiResponseEvent.Gone -> {
                when (apiResponseEvent.operation) {
                    is ApiRequestOperation.ValidApiRequestOperation.DeleteNote -> {
                        val action = SyncResponseAction.PermanentlyDeleteNote(
                            apiResponseEvent.operation.localId,
                            userID
                        )
                        store.dispatch(action)
                    }
                    is ApiRequestOperation.ValidApiRequestOperation.DeleteNoteReference -> {
                        val action = SyncResponseAction.PermanentlyDeleteNoteReference(
                            apiResponseEvent.operation.localId,
                            userID
                        )
                        store.dispatch(action)
                    }
                    is ApiRequestOperation.ValidApiRequestOperation.DeleteSamsungNote -> {
                        val action = SyncResponseAction.PermanentlyDeleteSamsungNote(
                            apiResponseEvent.operation.localId,
                            userID
                        )
                        store.dispatch(action)
                    }
                }
            }
        }
    }

    private fun handleChanges(changesResult: ChangesResult, newDeltaToken: Token.Delta?) {
        val newToCreate = changesResult.changes.toCreate.filter { it.isBelowSizeLimitForStorage() }
        val newToReplace = changesResult.changes.toReplace.filter { updates ->
            val note = updates.noteFromServer
            note.isBelowSizeLimitForStorage()
        }
        val newChanges = changesResult.changes.copy(toCreate = newToCreate, toReplace = newToReplace)

        store.dispatch(SyncResponseAction.ApplyChanges(newChanges, newDeltaToken?.token, userID))
        deltaToken = newDeltaToken
    }

    private fun handleNoteReferencesSyncChanges(changes: NoteReferenceChanges, deltaToken: Token.Delta?) {
        store.dispatch(NoteReferenceAction.ApplyChanges(changes, userID, deltaToken?.token))
        noteReferencesDeltaToken = deltaToken
    }

    private fun handleSamsungNotesSyncChanges(changes: Changes, deltaToken: Token.Delta?) {
        store.dispatch(SamsungNotesResponseAction.ApplyChanges(changes, userID, deltaToken?.token))
        samsungDeltaToken = deltaToken
    }

    private fun updateRemoteDataAction(
        localId: String,
        remoteNote: RemoteNote,
        uiBaseRevision: Long? = null
    ): SyncResponseAction {
        val remoteData = RemoteData(
            id = remoteNote.id,
            changeKey = remoteNote.changeKey,
            lastServerVersion = convertToLastServerVersion(
                noteId = localId,
                remoteNote = remoteNote,
                revision = uiBaseRevision
            ),
            createdAt = remoteNote.createdAt,
            lastModifiedAt = remoteNote.lastModifiedAt
        )
        return SyncResponseAction.RemoteDataUpdated(localId, remoteData, userID)
    }

    private fun replaceMediaWithURL(note: RemoteNote): RemoteNote {
        val document = note.document
        return when (document) {
            is Document.RenderedInkDocument -> {
                val uuid = UUID.randomUUID().toString().replace("-".toRegex(), "")
                val localURL = saveInkMediaToDisk(document, id = note.id + "_" + uuid)
                note.copy(document = document.copy(image = localURL))
            }
            else -> note
        }
    }

    private fun saveImageMediaStreamToDisk(
        data: BufferedSource,
        noteId: String,
        mediaRemoteId: String,
        createFile: (String) -> File,
        mimeTypeToFileExtension: (String) -> String,
        mimeType: String
    ): String {
        // Samsung notes preview image always has the id "samsungpreviewimage".
        // This is done to refresh UI with a unique URI, whenever a new image is downloaded
        val newMediaRemoteId = if (mediaRemoteId == SAMSUNG_PREVIEW_IMAGE_LOCAL_ID) {
            "${mediaRemoteId}_${UUID.randomUUID()}"
        } else {
            mediaRemoteId
        }
        var fileName = "media_$newMediaRemoteId.${mimeTypeToFileExtension(mimeType)}"
        if (mimeType.contains("gif")) {
            fileName = "media_$newMediaRemoteId.mp3"
        }
        val file = createFile(fileName)
        val sink = file.sink().buffer()
        sink.writeAll(data)
        sink.close()
        return file.toURI().toString()
    }

//    private fun convertBase64ToMp3() {
//        val decodedBytes = Base64.decode(Constants.mp3Base64, Base64.DEFAULT)
//
//        try {
//            FileOutputStream(outputFile).use { outputStream ->
//                outputStream.write(decodedBytes)
//            }
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//    }


    private fun saveInkMediaToDisk(document: Document.RenderedInkDocument, id: String): String {
        val decodedStream = decodeBase64(document.image)
        val fileName = "renderedink_$id.png"
        val fileHandle = createFile(fileName)
        val stream = FileOutputStream(fileHandle)
        stream.use { it.write(decodedStream) }
        return fileHandle.toURI().toString()
    }

    private fun convertToStoreNote(noteId: String, remoteNote: RemoteNote, revision: Long? = null): Note {
        val localNote = store.state.getNoteForNoteLocalId(noteId)

        return if (localNote != null) {
            remoteNote.toStoreNote(localNote, revision ?: 0)
        } else {
            remoteNote.toStoreNote(localNoteId = noteId)
        }
    }

    private fun convertToLastServerVersion(noteId: String, remoteNote: RemoteNote, revision: Long? = null): Note {
        val localNote = store.state.getNoteForNoteLocalId(noteId)

        return if (localNote != null) {
            remoteNote.toLastServerVersion(localNote, revision ?: 0)
        } else {
            remoteNote.toLastServerVersion(localNoteId = noteId)
        }
    }
}

data class NotesByIds(val notesByLocalId: Map<String, Note>, val notesByRemoteId: Map<String, Note>)

fun getIndexedNotes(notes: List<Note>): NotesByIds {
    val notesByLocalId = mutableMapOf<String, Note>()
    val notesByRemoteId = mutableMapOf<String, Note>()
    notes.forEach { note ->
        notesByLocalId.put(note.localId, note)
        note.remoteData?.let {
            notesByRemoteId.put(it.id, note)
        }
    }
    return NotesByIds(notesByLocalId, notesByRemoteId)
}

fun getNoteIdsFromPayloads(
    noteIds: NotesByIds,
    payloads: List<DeltaSyncPayload>
): List<String> {
    val ids = mutableListOf<String>()
    payloads.forEach { payload ->
        val note = when (payload) {
            is DeltaSyncPayload.NonDeleted -> noteIds.notesByRemoteId[payload.note.id]
            is DeltaSyncPayload.Deleted -> noteIds.notesByRemoteId[payload.id]
        }
        note?.let { ids.add(note.localId) }
    }
    return ids
}
