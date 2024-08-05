package com.microsoft.notes.sync

import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.CreateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteNoteReference
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteSamsungNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DownloadMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.GetNoteForMerge
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.MeetingNotesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.NoteReferencesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.SamsungNotesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.Sync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateMediaAltText
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UploadMedia
import com.microsoft.notes.sync.ApiResponseEvent.DeltaSync
import com.microsoft.notes.sync.ApiResponseEvent.FullSync
import com.microsoft.notes.sync.ApiResponseEvent.MediaAltTextUpdated
import com.microsoft.notes.sync.ApiResponseEvent.MediaDeleted
import com.microsoft.notes.sync.ApiResponseEvent.MediaDownloaded
import com.microsoft.notes.sync.ApiResponseEvent.MediaUploaded
import com.microsoft.notes.sync.ApiResponseEvent.NoteCreated
import com.microsoft.notes.sync.ApiResponseEvent.NoteDeleted
import com.microsoft.notes.sync.ApiResponseEvent.NoteFetchedForMerge
import com.microsoft.notes.sync.ApiResponseEvent.NoteReferenceDeleted
import com.microsoft.notes.sync.ApiResponseEvent.NoteReferenceFullSync
import com.microsoft.notes.sync.ApiResponseEvent.NoteUpdated
import com.microsoft.notes.sync.ApiResponseEvent.SamsungNoteDeleted
import com.microsoft.notes.sync.ApiResponseEvent.SamsungNoteDeltaSync
import com.microsoft.notes.sync.ApiResponseEvent.SamsungNoteFullSync
import com.microsoft.notes.sync.models.DeltaSyncPayload
import com.microsoft.notes.sync.models.Document
import com.microsoft.notes.sync.models.NoteReferencesDeltaSyncPayload
import com.microsoft.notes.sync.models.RemoteMeetingNote
import com.microsoft.notes.sync.models.RemoteNote
import com.microsoft.notes.sync.models.RemoteNoteReference
import com.microsoft.notes.sync.models.SyncResponse
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.sync.models.localOnly.RemoteData
import com.microsoft.notes.utils.threading.ExecutorServices
import com.microsoft.notes.utils.threading.ThreadExecutorService
import org.jdeferred.impl.DeferredObject
import java.io.File
import java.net.URI
import kotlin.reflect.KFunction1

class SyncThreadService : ThreadExecutorService(ExecutorServices.syncPool)

interface ApiRequestOperationHandler {
    fun setIsFRESyncing(value: Boolean)
    val remoteDataMap: Map<String, RemoteData>
    fun handleSync(operation: Sync): ApiPromise<ApiResponseEvent>
    fun handleNoteReferenceSync(operation: NoteReferencesSync): ApiPromise<ApiResponseEvent>
    fun handleMeetingNoteSync(operation: MeetingNotesSync): ApiPromise<ApiResponseEvent>
    fun handleSamsungNoteSync(operation: SamsungNotesSync): ApiPromise<ApiResponseEvent>
    fun handleCreate(operation: CreateNote): ApiPromise<NoteCreated>
    fun handleUpdate(operation: UpdateNote): ApiPromise<NoteUpdated>
    fun handleGetForMerge(operation: GetNoteForMerge): ApiPromise<NoteFetchedForMerge>
    fun handleDelete(operation: DeleteNote): ApiPromise<NoteDeleted>
    fun handleNoteReferenceDelete(operation: DeleteNoteReference): ApiPromise<NoteReferenceDeleted>
    fun handleSamsungNoteDelete(operation: DeleteSamsungNote): ApiPromise<SamsungNoteDeleted>
    fun handleUploadMedia(operation: UploadMedia): ApiPromise<MediaUploaded>
    fun handleDownloadMedia(operation: DownloadMedia): ApiPromise<MediaDownloaded>
    fun handleDeleteMedia(operation: DeleteMedia): ApiPromise<MediaDeleted>
    fun handleUpdateMediaAltText(operation: UpdateMediaAltText): ApiPromise<MediaAltTextUpdated>
}

// TODO: 09-Oct-20 Convert hard-coded apiVersion to a property
const val HOST_RELATIVE_API_BASE_URL: String = "/api/v2.0/me/notes"
const val HOST_RELATIVE_API_NOTE_REFERENCES_BASE_URL: String = "/api/v2.0/me/notereferences"
const val HOST_RELATIVE_API_SAMSUNG_NOTES_BASE_URL = "/api/v2.0/me/connectednotes"

class OutboundQueueApiRequestOperationHandler(
    var sdkManager: SdkManager
) : ApiRequestOperationHandler {

    private val syncThread = SyncThreadService()
    private var isFRESyncing = false

    override fun setIsFRESyncing(value: Boolean) {
        isFRESyncing = value
    }

    override val remoteDataMap: MutableMap<String, RemoteData> = mutableMapOf()

    fun reset() {
        remoteDataMap.clear()
    }

    override fun handleSync(operation: Sync): ApiPromise<ApiResponseEvent> {
        return if (operation.deltaToken == null) {
            fullSync(operation = operation, url = HOST_RELATIVE_API_BASE_URL, parser = (RemoteNote)::fromJSON, idMapper = RemoteNote::id)
        } else {
            deltaSync(
                operation, operation.deltaToken, HOST_RELATIVE_API_BASE_URL,
                parser = (DeltaSyncPayload)::fromJSON, idMapper = DeltaSyncPayload::noteId
            )
        }
    }

    override fun handleNoteReferenceSync(operation: NoteReferencesSync): ApiPromise<ApiResponseEvent> {
        return if (operation.deltaToken == null) {
            fullSync(operation, url = HOST_RELATIVE_API_NOTE_REFERENCES_BASE_URL, parser = (RemoteNoteReference)::fromJSON, idMapper = RemoteNoteReference::id)
        } else {
            deltaSync(operation, operation.deltaToken, HOST_RELATIVE_API_NOTE_REFERENCES_BASE_URL, parser = (NoteReferencesDeltaSyncPayload)::fromJSON, idMapper = NoteReferencesDeltaSyncPayload::noteId)
        }
    }

    override fun handleMeetingNoteSync(operation: MeetingNotesSync): ApiPromise<ApiResponseEvent> =
        fullSync(operation, url = "", parser = (RemoteMeetingNote)::fromJSON, idMapper = RemoteMeetingNote::id)

    override fun handleSamsungNoteSync(operation: SamsungNotesSync): ApiPromise<ApiResponseEvent> {
        return if (operation.deltaToken == null) {
            fullSync(operation, url = HOST_RELATIVE_API_SAMSUNG_NOTES_BASE_URL, parser = (RemoteNote)::fromJSON, idMapper = RemoteNote::id, withHtml = true)
        } else {
            deltaSync(
                operation, operation.deltaToken, HOST_RELATIVE_API_SAMSUNG_NOTES_BASE_URL,
                parser = (DeltaSyncPayload)::fromJSON, idMapper = DeltaSyncPayload::noteId, withHtml = true
            )
        }
    }

    private fun <T> handleSyncError(
        deferred: DeferredObject<ApiResult<T>, Exception, Any>,
        error: ApiError?
    ) {
        if (error != null) {
            deferred.resolve(ApiResult.Failure(error))
        } else {
            deferred.resolve(
                ApiResult.Failure(
                    ApiError.Exception(Exception("Sync hit an invalid state"))
                )
            )
        }
    }

    private inline fun <NoteT> collatePaginatedResponse(
        notes: Map<String, NoteT>,
        remoteNotes: List<NoteT>,
        idMapper: (NoteT) -> String
    ): Map<String, NoteT> = notes + remoteNotes.associateBy(idMapper)

    @Suppress("UnsafeCast")
    private fun <NoteT> handleFullSyncDeltaToken(
        operation: ValidApiRequestOperation,
        deferred: DeferredObject<ApiResult<ApiResponseEvent>, Exception, Any>,
        deltaToken: Token.Delta?,
        notes: Map<String, NoteT>,
        newNotes: List<NoteT>,
        idMapper: (NoteT) -> String
    ) {
        val remoteNotes = collatePaginatedResponse(notes, newNotes, idMapper).values.toList()
        when (operation) {
            is Sync -> {
                val fullSyncResponse = FullSync(deltaToken = deltaToken, remoteNotes = remoteNotes as List<RemoteNote>)
                updateRemoteDataFromFullSync(fullSyncResponse)
                deferred.resolve(ApiResult.Success(fullSyncResponse))
            }
            is NoteReferencesSync -> deferred.resolve(
                ApiResult.Success(
                    NoteReferenceFullSync(deltaToken = deltaToken, remoteNoteReferences = remoteNotes as List<RemoteNoteReference>)
                )
            )
            is SamsungNotesSync -> deferred.resolve(
                ApiResult.Success(
                    SamsungNoteFullSync(deltaToken = deltaToken, remoteSamsungNotes = remoteNotes as List<RemoteNote>)
                )
            )
            is MeetingNotesSync -> deferred.resolve(
                ApiResult.Success(
                    ApiResponseEvent.MeetingNoteFullSync(remoteMeetingNotes = remoteNotes as List<RemoteMeetingNote>)
                )
            )
            else -> throw IllegalArgumentException("Unsupported Operation $operation for full sync")
        }
    }

    @Suppress("UnsafeCallOnNullableType", "LoopWithTooManyJumpStatements", "LongMethod")
    private fun <NoteT> fullSync(
        operation: ValidApiRequestOperation,
        skipToken: Token.Skip? = null,
        url: String,
        parser: KFunction1<JSON, NoteT?>,
        idMapper: (NoteT) -> String,
        withHtml: Boolean = false
    ): ApiPromise<ApiResponseEvent> {
        val requestId = operation.requestId
        val realTimeSessionId = operation.realTimeSessionId
        val deferred = DeferredObject<ApiResult<ApiResponseEvent>, Exception, Any>()
        syncThread.execute {
            var notes: Map<String, NoteT> = emptyMap()
            var currentToken: Token.Skip? = skipToken
            // in order to prevent stack overflows from unbounded promise chains,
            // loop iteratively through pages of sync data rather than recursing
            loop@ while (true) {
                var response: SyncResponse<NoteT>? = null
                var error: ApiError? = null

                val request =
                    if (operation is MeetingNotesSync)
                        sdkManager.meetingNotes.fullSync(requestId, currentToken, parser)
                    else
                        sdkManager.notes.fullSync(requestId, realTimeSessionId, currentToken, url, parser, withHtml)

                request.onComplete {
                    response = it.unwrap()
                }
                request.mapError {
                    error = it
                    it
                }
                // within this task, the sdk call is treated as if a blocking call
                // after waiting here, result will be populated by the onComplete lambda
                try {
                    request.waitForPromise()
                } catch (e: InterruptedException) {
                    deferred.resolve(
                        ApiResult.Failure(
                            ApiError.Exception(e)
                        )
                    )
                    break@loop
                }

                when (response) {
                    is SyncResponse<NoteT> -> {
                        val token = response!!.token
                        val value = response!!.value
                        when (token) {
                            is Token.Delta -> {
                                handleFullSyncDeltaToken(operation, deferred, token, notes, value, idMapper)
                                break@loop
                            }
                            is Token.Skip -> {

                                if (isFRESyncing) {
                                    handleFullSyncDeltaToken(operation, deferred, null, notes, value, idMapper)
                                    break@loop
                                }

                                notes = collatePaginatedResponse(notes, value, idMapper)
                                currentToken = token
                            }
                        }
                    }
                    else -> {
                        handleSyncError(deferred, error)
                        break@loop
                    }
                }
            }
        }
        return ApiPromise(deferred.promise())
    }

    private fun <DeltaSyncPayloadT> handleDeltaSyncDeltaToken(
        operation: ValidApiRequestOperation,
        deferred: DeferredObject<ApiResult<ApiResponseEvent>, Exception, Any>,
        deltaToken: Token.Delta,
        payloads: Map<String, DeltaSyncPayloadT>,
        newPayloads: List<DeltaSyncPayloadT>,
        idMapper: (DeltaSyncPayloadT) -> String
    ) {
        val remotePayloads = collatePaginatedResponse(payloads, newPayloads, idMapper).values.toList()
        when (operation) {
            is Sync -> {
                val deltaSyncResponse = DeltaSync(deltaToken = deltaToken, payloads = remotePayloads as List<DeltaSyncPayload>)
                updateRemoteDataFromDeltaSync(deltaSyncResponse)
                deferred.resolve(ApiResult.Success(deltaSyncResponse))
            }
            is NoteReferencesSync -> deferred.resolve(
                ApiResult.Success(
                    ApiResponseEvent.NoteReferenceDeltaSync(
                        deltaToken = deltaToken,
                        payloads = remotePayloads as List<NoteReferencesDeltaSyncPayload>
                    )
                )
            )
            is SamsungNotesSync -> deferred.resolve(
                ApiResult.Success(
                    SamsungNoteDeltaSync(deltaToken = deltaToken, payloads = remotePayloads as List<DeltaSyncPayload>)
                )
            )
            else -> throw IllegalArgumentException("Unsupported Operation $operation for full sync")
        }
    }

    @Suppress("UnsafeCallOnNullableType", "LoopWithTooManyJumpStatements", "LongMethod")
    private fun <DeltaSyncPayloadT> deltaSync(
        operation: ValidApiRequestOperation,
        deltaToken: Token.Delta,
        url: String,
        parser: KFunction1<JSON, DeltaSyncPayloadT?>,
        idMapper: (DeltaSyncPayloadT) -> String,
        withHtml: Boolean = false
    ): ApiPromise<ApiResponseEvent> {
        val requestId = operation.requestId
        val realTimeSessionId = operation.realTimeSessionId
        val deferred = DeferredObject<ApiResult<ApiResponseEvent>, Exception, Any>()
        syncThread.execute {
            var payloads: Map<String, DeltaSyncPayloadT> = emptyMap()
            var skipToken: Token.Skip? = null
            // in order to prevent stack overflows from unbounded promise chains,
            // loop iteratively through pages of sync data rather than recursing
            loop@ while (true) {
                var response: SyncResponse<DeltaSyncPayloadT>? = null
                var error: ApiError? = null
                val request = if (skipToken == null) {
                    sdkManager.notes.deltaSync(
                        requestId, realTimeSessionId, deltaToken, url = url, parser = parser,
                        withHtml = withHtml
                    )
                } else {
                    sdkManager.notes.pageSync(
                        requestId, realTimeSessionId, skipToken, url = url, parser = parser,
                        withHtml = withHtml
                    )
                }
                request.onComplete {
                    response = it.unwrap()
                }
                request.mapError {
                    error = it
                    it
                }
                // within this task, the sdk call is treated as if a blocking call
                // after waiting here, result will be populated by the onComplete lambda
                try {
                    request.waitForPromise()
                } catch (e: InterruptedException) {
                    deferred.resolve(
                        ApiResult.Failure(
                            ApiError.Exception(e)
                        )
                    )
                    break@loop
                }
                when (response) {
                    is SyncResponse<DeltaSyncPayloadT> -> {
                        val token = response!!.token
                        val value = response!!.value
                        when (token) {
                            is Token.Delta -> {
                                handleDeltaSyncDeltaToken(operation, deferred, token, payloads = payloads, newPayloads = value, idMapper = idMapper)
                                break@loop
                            }
                            is Token.Skip -> {
                                payloads = collatePaginatedResponse(payloads, value, idMapper)
                                skipToken = token
                            }
                        }
                    }
                    else -> {
                        handleSyncError(deferred, error)
                        break@loop
                    }
                }
            }
        }
        return ApiPromise(deferred.promise())
    }

    private fun updateRemoteDataFromFullSync(fullSync: FullSync) {
        fullSync.remoteNotes.forEach { remoteNote -> updateRemoteDataMapFromRemoteNote(remoteDataMap, remoteNote) }
    }

    private fun updateRemoteDataFromDeltaSync(deltaSync: DeltaSync) {
        deltaSync.payloads.forEach { payload ->
            when (payload) {
                is DeltaSyncPayload.NonDeleted -> updateRemoteDataMapFromRemoteNote(remoteDataMap, payload.note)
            }
        }
    }

    private fun updateRemoteDataMapFromRemoteNote(
        remoteDataMap: MutableMap<String, RemoteData>,
        remoteNote: RemoteNote
    ) {
        remoteDataMap.forEach { item ->
            updateRemoteDataFromRemoteNote(remoteDataMap, item.key, remoteNote)
        }
    }

    private fun updateRemoteDataFromRemoteNote(
        remoteDataMap: MutableMap<String, RemoteData>,
        noteId: String,
        remoteNote: RemoteNote
    ) {
        remoteDataMap[noteId] = RemoteData(
            id = remoteNote.id,
            changeKey = remoteNote.changeKey,
            lastServerVersion = remoteNote,
            createdAt = remoteNote.createdAt,
            lastModifiedAt = remoteNote.lastModifiedAt
        )
    }

    private fun <T> remoteIdMissing(id: String): ApiPromise<T> =
        ApiPromise.of(
            ApiError.FatalError("Missing remote id localId: $id")
        )

    override fun handleCreate(operation: CreateNote): ApiPromise<NoteCreated> {
        val note = operation.note
        return sdkManager.notes.createNote(operation.requestId, operation.realTimeSessionId, note) map { remoteNote ->
            updateRemoteDataFromRemoteNote(remoteDataMap, note.id, remoteNote)
            NoteCreated(note.id, remoteNote)
        }
    }

    override fun handleUpdate(operation: UpdateNote): ApiPromise<NoteUpdated> {
        val note = operation.note
        if (note.remoteData == null) {
            return remoteIdMissing(note.id)
        }
        return sdkManager.notes.updateNote(operation.requestId, operation.realTimeSessionId, note) map { remoteNote ->
            updateRemoteDataFromRemoteNote(remoteDataMap, note.id, remoteNote)
            NoteUpdated(note.id, remoteNote, operation.uiBaseRevision)
        }
    }

    override fun handleGetForMerge(operation: GetNoteForMerge): ApiPromise<NoteFetchedForMerge> {
        val note = operation.note
        if (note.remoteData == null) {
            return remoteIdMissing(note.id)
        }
        return sdkManager.notes.getNote(operation.requestId, operation.realTimeSessionId, note) map { remoteNote ->
            NoteFetchedForMerge(note.id, remoteNote, operation.uiBaseRevision)
        }
    }

    override fun handleDelete(operation: DeleteNote): ApiPromise<NoteDeleted> =
        sdkManager.notes.deleteNote(
            operation.requestId,
            operation.realTimeSessionId,
            operation.remoteId,
            HOST_RELATIVE_API_BASE_URL
        ) map { _ ->
            NoteDeleted(operation.localId, operation.remoteId)
        }

    override fun handleNoteReferenceDelete(operation: DeleteNoteReference): ApiPromise<NoteReferenceDeleted> =
        sdkManager.notes.deleteNote(
            operation.requestId,
            operation.realTimeSessionId,
            operation.remoteId,
            HOST_RELATIVE_API_BASE_URL // Using the same API as Delete SN, https://office.visualstudio.com/OneNote/_workitems/edit/5796913
        ) map { _ ->
            NoteReferenceDeleted(operation.localId, operation.remoteId)
        }

    override fun handleSamsungNoteDelete(operation: DeleteSamsungNote): ApiPromise<SamsungNoteDeleted> =
        sdkManager.notes.deleteNote(
            operation.requestId,
            operation.realTimeSessionId,
            operation.remoteId,
            HOST_RELATIVE_API_SAMSUNG_NOTES_BASE_URL
        ) map { _ ->
            SamsungNoteDeleted(operation.localId, operation.remoteId)
        }

    override fun handleUploadMedia(operation: UploadMedia): ApiPromise<MediaUploaded> {
        fun readMediaFromDisk(localUrl: String): ByteArray {
            val uri = URI.create(localUrl)
            val file = File(uri)
            return file.readBytes()
        }
        if (operation.note.remoteData == null) {
            return remoteIdMissing(operation.note.id)
        }

        val remoteId = operation.note.remoteData.id
        // test
        val data = try {
            readMediaFromDisk(localUrl = operation.localUrl)
        } catch (e: IllegalArgumentException) {
            return ApiPromise.of(ApiError.Exception(e))
        }

        return sdkManager.notes.uploadMedia(
            requestId = operation.requestId,
            realTimeSessionId = operation.realTimeSessionId,
            remoteNoteId = remoteId,
            mediaLocalId = operation.mediaLocalId,
            fileName = operation.localUrl,
            data = data,
            mimeType = operation.mimeType
        ) map {
            MediaUploaded(
                noteId = operation.note.id, mediaLocalId = operation.mediaLocalId,
                localUrl = operation.localUrl, mediaRemoteId = it.remoteId
            )
        }
    }

    override fun handleDownloadMedia(operation: DownloadMedia): ApiPromise<MediaDownloaded> {
        var note = operation.note
        if (note.remoteData == null) {
            note = note.copy(remoteData = remoteDataMap[note.id])
        }
        val remoteId = note.remoteData?.id ?: return remoteIdMissing(note.id)
        // test
//        val mimeType = operation.mimeType
//        if (mimeType.contains("gif")) {
//            mimeType = ""
//        }
        return sdkManager.notes.downloadMedia(
            requestId = operation.requestId,
            realTimeSessionId = operation.realTimeSessionId,
            remoteId = remoteId,
            mediaRemoteId = operation.mediaRemoteId,
            isSamsungNoteDocument = note.document.type == Document.DocumentType.HTML
        ) map {
            MediaDownloaded(
                noteId = operation.note.id, mediaRemoteId = operation.mediaRemoteId,
                mimeType = operation.mimeType,
                data = it
            )
        }
    }

    override fun handleDeleteMedia(operation: DeleteMedia): ApiPromise<MediaDeleted> {
        return sdkManager.notes.deleteMedia(
            operation.requestId, operation.realTimeSessionId, operation.remoteNoteId,
            operation.remoteMediaId
        ) map { _ ->
            MediaDeleted(operation.localNoteId, operation.localMediaId, operation.remoteMediaId)
        }
    }

    override fun handleUpdateMediaAltText(operation: UpdateMediaAltText): ApiPromise<MediaAltTextUpdated> {
        return sdkManager.notes.updateMediaAltText(
            operation.requestId, operation.realTimeSessionId, operation.note,
            operation.remoteMediaId, operation.altText
        ) map {
            MediaAltTextUpdated(operation.note.id, mediaAltTextUpdate = it)
        }
    }
}
