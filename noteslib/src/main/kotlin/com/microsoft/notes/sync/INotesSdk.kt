package com.microsoft.notes.sync

import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.sync.extensions.parseAsMediaAltTextUpdate
import com.microsoft.notes.sync.extensions.parseAsRemoteNote
import com.microsoft.notes.sync.extensions.parseAsUpload
import com.microsoft.notes.sync.models.MediaAltTextUpdate
import com.microsoft.notes.sync.models.MediaUpload
import com.microsoft.notes.sync.models.RemoteNote
import com.microsoft.notes.sync.models.SyncResponse
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.sync.models.localOnly.Note
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource
import kotlin.reflect.KFunction1

const val REALTIME_API_VERSION_V1 = "v1.1"
const val REALTIME_API_VERSION_V2 = "v2.0"

interface INotesSdk {
    var host: NotesClientHost
    val token: String
    val apiVersion: String

    val hostRelativeApiRealtimeUrl: String
        get() {
            return "/api/$apiVersion/me/realtime"
        }

    fun fetch(
        requestId: String,
        realTimeSessionId: String,
        request: Request,
        isFileRelated: Boolean
    ): ApiPromise<Response>

    fun fetchAsJSON(
        requestId: String,
        realTimeSessionId: String,
        request: Request,
        isFileRelated: Boolean = false,
        withHtml: Boolean = false
    ): ApiPromise<JSON>

    fun fetchAsBufferedSource(
        requestId: String,
        realTimeSessionId: String,
        request: Request,
        isFileRelated: Boolean = false
    ): ApiPromise<BufferedSource>

    fun longPoll(path: String, sessionId: String, onNewData: (char: Char) -> Unit): ApiPromise<Unit>

    fun <NoteT> fullSync(
        requestId: String,
        realTimeSessionId: String,
        skipToken: Token.Skip? = null,
        url: String = HOST_RELATIVE_API_BASE_URL,
        parser: KFunction1<JSON, NoteT?>,
        withHtml: Boolean = false
    ): ApiPromise<SyncResponse<NoteT>> {
        val queryParams = if (skipToken != null) {
            hashMapOf("skipToken" to skipToken.token)
        } else {
            emptyMap<String, String>()
        }
        return fetchNotes(requestId, realTimeSessionId, queryParams, url, withHtml, parser = parser)
    }

    fun <NoteT> deltaSync(
        requestId: String,
        realTimeSessionId: String,
        deltaToken: Token.Delta,
        url: String = HOST_RELATIVE_API_BASE_URL,
        parser: KFunction1<JSON, NoteT?>,
        withHtml: Boolean = false
    ): ApiPromise<SyncResponse<NoteT>> {
        val queryParams = hashMapOf("deltaToken" to deltaToken.token)
        return fetchNotes(requestId, realTimeSessionId, queryParams, url, withHtml, parser = parser)
    }

    fun <NoteT> pageSync(
        requestId: String,
        realTimeSessionId: String,
        skipToken: Token.Skip,
        url: String = HOST_RELATIVE_API_BASE_URL,
        parser: KFunction1<JSON, NoteT?>,
        withHtml: Boolean = false
    ): ApiPromise<SyncResponse<NoteT>> {
        val queryParams = hashMapOf("skipToken" to skipToken.token)
        return fetchNotes(requestId, realTimeSessionId, queryParams, url, withHtml, parser = parser)
    }

    private fun <T> fetchNotes(
        requestId: String,
        realTimeSessionId: String,
        queryParams: Map<String, String>,
        url: String = HOST_RELATIVE_API_BASE_URL,
        withHtml: Boolean = false,
        parser: (JSON) -> T?
    ): ApiPromise<SyncResponse<T>> {
        val request = RequestBuilder(host.url).get(
            url, additionalHeaders = emptyMap(),
            queryParams = queryParams
        )
        return this.fetchAsJSON(requestId, realTimeSessionId, request, withHtml = withHtml) andTry { json: JSON ->
            val syncResponse = SyncResponse.fromJSON(json, parser, (Token)::fromJSON)

            if (syncResponse != null) {
                ApiResult.Success(syncResponse)
            } else {
                ApiResult.Failure<SyncResponse<T>>(
                    ApiError.InvalidJSONError(json)
                )
            }
        }
    }

    fun createNote(
        requestId: String,
        realTimeSessionId: String,
        note: Note
    ): ApiPromise<RemoteNote> {
        val body = RequestBody.forCreateNote(note)
        val request = RequestBuilder(host.url).post(HOST_RELATIVE_API_BASE_URL, body)
        return fetchAsJSON(requestId, realTimeSessionId, request) andTry { json -> json.parseAsRemoteNote() }
    }

    fun updateNote(
        requestId: String,
        realTimeSessionId: String,
        note: Note
    ): ApiPromise<RemoteNote> {
        if (note.remoteData == null) {
            throw IllegalArgumentException("remoteData should be present on the note")
        }
        val body = RequestBody.forUpdateNote(note)
            ?: return ApiPromise.of(
                ApiError.FatalError("Could not form update note body")
            )
        val request = RequestBuilder(host.url).patch("$HOST_RELATIVE_API_BASE_URL/${note.remoteData.id}", body)
        return fetchAsJSON(requestId, realTimeSessionId, request) andTry { json -> json.parseAsRemoteNote() }
    }

    fun getNote(
        requestId: String,
        realTimeSessionId: String,
        note: Note
    ): ApiPromise<RemoteNote> {
        val request = RequestBuilder(host.url).get(
            "$HOST_RELATIVE_API_BASE_URL/${note.remoteData?.id}",
            additionalHeaders = emptyMap(), queryParams = emptyMap()
        )
        return fetchAsJSON(requestId, realTimeSessionId, request) andTry { json -> json.parseAsRemoteNote() }
    }

    fun deleteNote(
        requestId: String,
        realTimeSessionId: String,
        remoteId: String,
        urlPath: String
    ): ApiPromise<Unit> {
        val request = RequestBuilder(host.url).delete("$urlPath/$remoteId")
        return fetchAsBufferedSource(requestId, realTimeSessionId, request) map { it.close() }
    }

    fun uploadMedia(
        requestId: String,
        realTimeSessionId: String,
        remoteNoteId: String,
        mediaLocalId: String,
        fileName: String?,
        data: ByteArray,
        mimeType: String
    ): ApiPromise<MediaUpload> {
        val headers = mapOf("X-Created-With-Local-Id" to mediaLocalId)
        val request = RequestBuilder(host.url).post(
            path = "$HOST_RELATIVE_API_BASE_URL/$remoteNoteId/media", body = data,
            additionalHeaders = headers, mimeType = mimeType
        )
        return fetchAsJSON(
            requestId,
            realTimeSessionId,
            request,
            isFileRelated = true
        ) andTry { json ->
            json
                .parseAsUpload()
        }
    }

    fun downloadMedia(
        requestId: String,
        realTimeSessionId: String,
        remoteId: String,
        mediaRemoteId: String,
        isSamsungNoteDocument: Boolean = false
    ): ApiPromise<BufferedSource> {
        //test
        val baseUrl =
            if (NotesLibrary.getInstance().experimentFeatureFlags.samsungNotesSyncEnabled &&
                isSamsungNoteDocument
            ) {
                HOST_RELATIVE_API_SAMSUNG_NOTES_BASE_URL
            } else {
                HOST_RELATIVE_API_BASE_URL
            }
        val request = RequestBuilder(host.url).get(
            path = "$baseUrl/$remoteId/media/$mediaRemoteId",
            queryParams = emptyMap()
        )
        return fetchAsBufferedSource(requestId, realTimeSessionId, request, isFileRelated = true)
    }

    fun deleteMedia(
        requestId: String,
        realTimeSessionId: String,
        noteRemoteId: String,
        mediaRemoteId: String
    ): ApiPromise<Unit> {
        val request = RequestBuilder(host.url).delete("$HOST_RELATIVE_API_BASE_URL/$noteRemoteId/media/$mediaRemoteId")
        return fetchAsBufferedSource(requestId, realTimeSessionId, request) map { it.close() }
    }

    fun updateMediaAltText(
        requestId: String,
        realTimeSessionId: String,
        note: Note,
        mediaRemoteId: String,
        altText: String?
    ): ApiPromise<MediaAltTextUpdate> {
        val body = RequestBody.forUpdateMediaAltText(note, altText)
            ?: return ApiPromise.of(
                ApiError.FatalError("Could not form update media alt text body")
            )
        val request = RequestBuilder(host.url).patch(
            "$HOST_RELATIVE_API_BASE_URL/${note.remoteData?.id}/media/$mediaRemoteId", body
        )
        return fetchAsJSON(requestId, realTimeSessionId, request) andTry { json -> json.parseAsMediaAltTextUpdate() }
    }
}

fun <T> List<T?>.sequence(): List<T>? {
    @Suppress("UNCHECKED_CAST", "UnsafeCast")
    return if (this.any { it == null }) null else this as List<T>
}

fun <T> List<T?>.filterOutNulls(): List<T>? {
    @Suppress("UNCHECKED_CAST", "UnsafeCast")
    return this.filter { it != null } as List<T>
}

fun Request.Builder.addHeaderIfNotNull(key: String, value: String?): Request.Builder {
    var builder = this
    if (value != null) {
        builder = builder.addHeader(key, value)
    }
    return builder
}
