package com.microsoft.notes.sync

import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.CreateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.GetNoteForMerge
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.SamsungNotesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.Sync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateNote
import com.microsoft.notes.sync.models.DeltaSyncPayload
import com.microsoft.notes.sync.models.Document.RichTextDocument
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.sync.models.localOnly.Note
import com.microsoft.notes.sync.models.localOnly.RemoteData
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.utils.UserInfo
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

internal val apiVersion = "v2.0"
internal val hostRelativeBaseUrl = "/api/$apiVersion/me/notes"
internal const val hostRelativeApiSamsungNotesBaseUrl = "/api/v2.0/me/connectednotes"

class OutboundQueueHandlerSyncTest {
    @Test
    fun should_handle_full_sync_which_returns_delta_token() {
        fun call1(request: Request): ResponseDescription {
            assertRequest(
                request, method = "GET",
                path = hostRelativeBaseUrl
            )
            return ResponseDescription(
                200,
                syncResponseWithDelta(
                    "someDeltaToken",
                    listOf(remoteRichTextNoteJSON("123"))
                ).toString()
            )
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall> { call1(it) }).getMockNotesSdk()
        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = Sync(null)
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleSync(operation).get()
        val expected = ApiResult.Success(
            ApiResponseEvent.FullSync(
                Token.Delta("someDeltaToken"),
                listOf(remoteRichTextNote("123"))
            )
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert((sdkManager.notes as MockNotesSdk).isExhausted)
    }

    @Test
    fun should_handle_full_sync_which_returns_skip_token() {
        fun call1(request: Request): ResponseDescription {
            assertRequest(
                request, method = "GET",
                path = hostRelativeBaseUrl
            )
            return ResponseDescription(
                200,
                syncResponseWithSkip(
                    "someSkipToken",
                    listOf(remoteRichTextNoteJSON("123"))
                ).toString()
            )
        }

        fun call2(request: Request): ResponseDescription {
            assertRequest(
                request, method = "GET",
                path = hostRelativeBaseUrl,
                query = "skipToken=someSkipToken"
            )
            return ResponseDescription(
                200,
                syncResponseWithDelta(
                    "someDeltaToken",
                    listOf(remoteRichTextNoteJSON("456"))
                ).toString()
            )
        }

        val sdkManager = SdkManager()
        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1(it) }, { call2(it) })).getMockNotesSdk()

        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = Sync(null)
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleSync(operation).get()
        val expected = ApiResult.Success(
            ApiResponseEvent.FullSync(
                Token.Delta("someDeltaToken"),
                listOf(
                    remoteRichTextNote("123"),
                    remoteRichTextNote("456")
                )
            )
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }

    @Test
    fun should_handle_delta_sync_which_returns_delta_token() {
        fun call1(request: Request): ResponseDescription {
            assertRequest(
                request, method = "GET",
                path = hostRelativeBaseUrl,
                query = "deltaToken=originalToken"
            )
            return ResponseDescription(
                200,
                syncResponseWithDelta(
                    "someDeltaToken",
                    listOf(remoteRichTextNoteJSON("123"))
                ).toString()
            )
        }

        val sdkManager = SdkManager()
        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1(it) })).getMockNotesSdk()

        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = Sync(Token.Delta("originalToken"))
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleSync(operation).get()
        val expected = ApiResult.Success(
            ApiResponseEvent.DeltaSync(
                Token.Delta("someDeltaToken"),
                listOf(DeltaSyncPayload.NonDeleted(remoteRichTextNote("123")))
            )
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }

    @Test
    fun should_handle_delta_sync_which_returns_skip_token() {
        fun call1(request: Request): ResponseDescription {
            assertRequest(
                request, method = "GET",
                path = hostRelativeBaseUrl,
                query = "deltaToken=originalToken"
            )
            return ResponseDescription(
                200,
                syncResponseWithSkip(
                    "someSkipToken",
                    listOf(remoteRichTextNoteJSON("123"))
                ).toString()
            )
        }

        fun call2(request: Request): ResponseDescription {
            assertRequest(
                request, method = "GET",
                path = hostRelativeBaseUrl,
                query = "skipToken=someSkipToken"
            )
            val json = JSON.JObject(
                hashMapOf(
                    "reason" to JSON.JString("deleted"),
                    "id" to JSON.JString("456")
                )
            )
            return ResponseDescription(
                200,
                syncResponseWithDelta("someDeltaToken", listOf(json)).toString()
            )
        }

        val sdkManager = SdkManager()
        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1(it) }, { call2(it) })).getMockNotesSdk()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)
        val operation = Sync(Token.Delta("originalToken"))
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleSync(operation).get()
        val expected = ApiResult.Success(
            ApiResponseEvent.DeltaSync(
                Token.Delta("someDeltaToken"),
                listOf(
                    DeltaSyncPayload.NonDeleted(remoteRichTextNote("123")),
                    DeltaSyncPayload.Deleted("456")
                )
            )
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }
}

class OutboundQueueHandlerCreateNoteTest {
    private val CREATED_BY_APP = "Test"

    @Test
    fun should_handle_create_note() {
        val note = note("123")

        fun call1(request: Request): ResponseDescription {
            val expectedBody = JSON.JObject(
                hashMapOf(
                    "document" to note.document.toJSON(),
                    "color" to JSON.JNumber(note.color.value.toDouble()),
                    "createdWithLocalId" to JSON.JString(note.id),
                    "createdByApp" to JSON.JString(CREATED_BY_APP)
                )
            )
            assertRequest(
                request, method = "POST",
                path = hostRelativeBaseUrl,
                bodyJson = expectedBody
            )
            return ResponseDescription(
                201,
                remoteRichTextNoteJSON("123").toString()
            )
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1(it) })).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = CreateNote(note)
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleCreate(operation).get()
        val expected = ApiResult.Success(
            ApiResponseEvent.NoteCreated(
                note.id,
                remoteRichTextNote("123")
            )
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }

    @Test
    fun should_be_failure_when_json_is_malformed() {
        fun call1(): ResponseDescription {
            return ResponseDescription(201, "{")
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1() })).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = CreateNote(note("123"))
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleCreate(operation).get()
        val expected = ApiResult.Failure<ApiResponseEvent>(
            ApiError.NonJSONError("End of input")
        )
        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }

    @Test
    fun should_be_failure_when_response_is_not_2xx() {
        fun call1(): ResponseDescription {
            return ResponseDescription(404, "{}")
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1() })).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = CreateNote(note("123"))
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleCreate(operation).get()
        val expected = ApiResult.Failure<ApiResponseEvent>(
            HttpError404.UnknownHttp404Error(emptyMap())
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }
}

class OutboundQueueHandlerUpdateNoteTest {
    @Test
    fun should_handle_update_note() {
        val note = note("123", "remoteId")
        fun call1(request: Request): ResponseDescription {
            val expectedBody = JSON.JObject(
                hashMapOf(
                    "changeKey" to JSON.JString("changeKey"),
                    "document" to note.document.toJSON(),
                    "color" to JSON.JNumber(note.color.value.toDouble()),
                    "documentModifiedAt" to JSON.JString("2018-01-31T16:50:31.0000000Z")
                )
            )
            assertRequest(
                request, method = "PATCH",
                path = "$hostRelativeBaseUrl/remoteId",
                bodyJson = expectedBody
            )
            return ResponseDescription(
                201,
                remoteRichTextNoteJSON("remoteId").toString()
            )
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1(it) })).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = UpdateNote(note, uiBaseRevision = 0L)
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleUpdate(operation).get()
        val expected = ApiResult.Success(
            ApiResponseEvent.NoteUpdated(
                "123",
                remoteRichTextNote("remoteId"), uiBaseRevision = 0L
            )
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }

    @Test
    fun should_be_failure_when_json_is_malformed() {
        fun call1(): ResponseDescription {
            return ResponseDescription(201, "{")
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1() })).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = UpdateNote(note("123", "remoteId"), uiBaseRevision = 0L)
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleUpdate(operation).get()
        val expected = ApiResult.Failure<ApiResponseEvent>(
            ApiError.NonJSONError("End of input")
        )
        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }

    @Test
    fun should_be_failure_when_response_is_not_2xx() {
        fun call1(): ResponseDescription {
            return ResponseDescription(404, "{}")
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1() })).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = UpdateNote(note("123", "remoteId"), uiBaseRevision = 0L)
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleUpdate(operation).get()
        val expected = ApiResult.Failure<ApiResponseEvent>(
            HttpError404.UnknownHttp404Error(emptyMap())
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }

    @Test
    fun should_be_fatal_error_when_local_note_does_not_have_remote_data() {

        val mockSdk = MockNotesSdkWithApiCalls(emptyList()).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = UpdateNote(note("123"), uiBaseRevision = 0L)
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleUpdate(operation).get()
        val expected = ApiResult.Failure<ApiResponseEvent>(
            ApiError.FatalError("Missing remote id localId: 123")
        )
        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }
}

class OutboundQueueHandlerDeleteNoteTest {
    @Test
    fun should_handle_delete_note() {
        fun call1(request: Request): ResponseDescription {
            assertRequest(
                request, method = "DELETE",
                path = "$hostRelativeBaseUrl/remoteId"
            )
            return ResponseDescription(204, "")
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1(it) })).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)
        val operation = DeleteNote("123", "remoteId")
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleDelete(operation).get()
        val expected = ApiResult.Success(
            ApiResponseEvent.NoteDeleted("123", "remoteId")
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }

    @Test
    fun should_be_failure_when_response_is_not_2xx() {
        fun call1(): ResponseDescription {
            return ResponseDescription(404, "{}")
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1() })).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = DeleteNote("123", "remoteId")
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleDelete(operation).get()
        val expected = ApiResult.Failure<ApiResponseEvent>(
            HttpError404.UnknownHttp404Error(emptyMap())
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }
}

class OutboundQueueHandlerMergeNoteTest {
    @Test
    fun should_handle_get_note_for_merge() {
        val REMOTE_ID = "thisIsRemoteId"
        fun call1(request: Request): ResponseDescription {
            assertThat(request.method(), iz("GET"))
            assertThat(request.url().encodedPath(), iz("$hostRelativeBaseUrl/$REMOTE_ID"))
            // TODO: Test body is correct
            return ResponseDescription(
                200,
                remoteRichTextNoteJSON(REMOTE_ID).toString()
            )
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1(it) })).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)
        val operation = GetNoteForMerge(note("123", REMOTE_ID), uiBaseRevision = 0L)
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleGetForMerge(operation).get()
        val expected = ApiResult.Success(
            ApiResponseEvent.NoteFetchedForMerge(
                "123",
                remoteRichTextNote(REMOTE_ID), uiBaseRevision = 0L
            )
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }

    @Test
    fun should_be_failure_when_response_is_not_2xx() {
        fun call1(): ResponseDescription {
            return ResponseDescription(404, "{}")
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ call1() })).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = GetNoteForMerge(note("123", "remoteId"), uiBaseRevision = 0L)
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleGetForMerge(operation).get()
        val expected = ApiResult.Failure<ApiResponseEvent>(
            HttpError404.UnknownHttp404Error(emptyMap())
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }
}

class SamsungNotesSyncHandler {
    @Test
    fun `Samsung Notes Sync request with delta token in the response`() {
        fun networkCall(request: Request): ResponseDescription {
            assertRequest(
                request, method = "GET",
                path = hostRelativeApiSamsungNotesBaseUrl
            )
            return ResponseDescription(
                200,
                syncResponseWithDelta(
                    "someDeltaToken",
                    listOf(remoteRichTextNoteJSON("123"))
                ).toString()
            )
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall> { networkCall(it) }).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)
        val operation = SamsungNotesSync(null)
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleSamsungNoteSync(operation).get()
        val expected = ApiResult.Success(
            ApiResponseEvent.SamsungNoteFullSync(
                Token.Delta("someDeltaToken"),
                listOf(remoteRichTextNote("123"))
            )
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }

    @Test
    fun `Samsung Notes Sync request with skip token in the response`() {
        fun networkCall1(request: Request): ResponseDescription {
            assertRequest(
                request, method = "GET",
                path = hostRelativeApiSamsungNotesBaseUrl
            )
            return ResponseDescription(
                200,
                syncResponseWithSkip(
                    "someSkipToken",
                    listOf(remoteRichTextNoteJSON("123"))
                ).toString()
            )
        }

        fun networkCall2(request: Request): ResponseDescription {
            assertRequest(
                request, method = "GET",
                path = hostRelativeApiSamsungNotesBaseUrl,
                query = "skipToken=someSkipToken"
            )
            return ResponseDescription(
                200,
                syncResponseWithDelta(
                    "someDeltaToken",
                    listOf(remoteRichTextNoteJSON("456"))
                ).toString()
            )
        }

        val mockSdk = MockNotesSdkWithApiCalls(listOf<ApiCall>({ networkCall1(it) }, { networkCall2(it) })).getMockNotesSdk()

        val sdkManager = SdkManager()
        sdkManager.notes = mockSdk
        val handler = handler(sdkManager)

        val operation = SamsungNotesSync(null)
        operation.requestId = "correlationVectorBase1.1"
        operation.realTimeSessionId = "realTimeSessionId"
        val actual = handler.handleSamsungNoteSync(operation).get()
        val expected = ApiResult.Success(
            ApiResponseEvent.SamsungNoteFullSync(
                Token.Delta("someDeltaToken"),
                listOf(
                    remoteRichTextNote("123"),
                    remoteRichTextNote("456")
                )
            )
        )

        assertThat<ApiResult<ApiResponseEvent>>(actual, iz(expected))
        assert(mockSdk.isExhausted)
    }
}

data class ResponseDescription(val status: Int, val body: String)
typealias ApiCall = (Request) -> ResponseDescription

fun handler(sdkManager: SdkManager): OutboundQueueApiRequestOperationHandler {
    return OutboundQueueApiRequestOperationHandler(sdkManager)
}

fun syncResponseWithDelta(deltaToken: String, payloads: List<JSON>): JSON {
    return JSON.JObject(
        hashMapOf(
            "deltaToken" to JSON.JString(deltaToken),
            "value" to JSON.JArray(payloads)
        )
    )
}

fun syncResponseWithSkip(skipToken: String, payloads: List<JSON>): JSON {
    return JSON.JObject(
        hashMapOf(
            "skipToken" to JSON.JString(skipToken),
            "value" to JSON.JArray(payloads)
        )
    )
}

fun note(id: String, remoteId: String? = null): Note {
    val remoteData = if (remoteId != null) {
        RemoteData(
            id = remoteId, changeKey = "changeKey",
            lastServerVersion = remoteRichTextNote(
                remoteId
            ),
            createdAt = "2018-01-31T16:45:05.0000000Z", lastModifiedAt = "2018-01-31T16:50:31.0000000Z"
        )
    } else {
        null
    }
    return Note(
        id = id, remoteData = remoteData, color = Note.Color.BLUE,
        document = RichTextDocument(emptyList()), createdByApp = "Test",
        documentModifiedAt = "2018-01-31T16:50:31.0000000Z",
        // todo implement
        media = listOf(), metadata = testRemoteNoteMetadata()
    )
}

fun assertRequest(actual: Request, method: String, path: String, query: String? = null, bodyJson: JSON? = null) {
    assertThat(actual.method(), iz(method))
    assertThat(actual.url().encodedPath(), iz(path))
    if (query == null) {
        assertNull(actual.url().query())
    } else {
        assertThat(actual.url().query(), iz(query))
    }
    if (bodyJson == null) {
        if (actual.body() != null) {
            val buffer = Buffer()
            val body = actual.body()
            assertThat(body, iz(not(nullValue())))
            body!!.writeTo(buffer)
            assertThat(buffer.readByteString().utf8(), iz(""))
        } else {
            assertNull(actual.body())
        }
    } else {
        val buffer = Buffer()
        val body = actual.body()
        assertThat(body, iz(not(nullValue())))
        body!!.writeTo(buffer)
        assertThat(buffer.readByteString().utf8(), iz(bodyJson.toString()))
    }
}

class MockNotesSdkWithApiCalls(val apiCalls: List<ApiCall>) {
    var host: NotesClientHost = NotesClientHost.StaticHost("http://www.example.com")
    val token = ""
    val userInfo = UserInfo.EMPTY_USER_INFO
    val userAgent = ""
    val notesLogger: NotesLogger? = null
    fun getMockNotesSdk(): MockNotesSdk {
        return MockNotesSdk(apiCalls, host, token, userInfo, userAgent, notesLogger)
    }
}

open class MockNotesSdk(
    apiCalls: List<ApiCall>,
    host: NotesClientHost,
    token: String,
    userInfo: UserInfo,
    userAgent: String,
    notesLogger: NotesLogger?
) :
    NetworkedSdk.NotesSdk(host, token, userInfo, userAgent, notesLogger, gsonParserEnabled = true, inkEnabled = true, requestPriority = { RequestPriority.background }, feedRealTimeSyncEnabled = true) {
    override fun longPoll(path: String, sessionId: String, onNewData: (char: Char) -> Unit): ApiPromise<Unit> {
        TODO("not implemented")
    }

    override val apiVersion = "v1.1"
    private val mockHttp: MockHttpClient = MockHttpClient(apiCalls)

    override fun fetch(
        requestId: String,
        realTimeSessionId: String,
        request: Request,
        isFileRelated: Boolean
    ): ApiPromise<Response> {
        return mockHttp.fetch(request, 10000L)
    }

    override fun fetchAsJSON(
        requestId: String,
        realTimeSessionId: String,
        request: Request,
        isFileRelated: Boolean,
        withHtml: Boolean
    ): ApiPromise<JSON> {
        return mockHttp.fetchAsJSON(request, 10000L)
    }

    override fun fetchAsBufferedSource(
        requestId: String,
        realTimeSessionId: String,
        request: Request,
        isFileRelated: Boolean
    ): ApiPromise<BufferedSource> {
        return mockHttp.fetchAsBufferedSource(request, 10000L)
    }

    var isExhausted = false
        get() = this.mockHttp.apiCalls.size == this.mockHttp.responseIndex
}

class MockHttpClient(val apiCalls: List<ApiCall>) : HttpClient(null) {
    var responseIndex = 0

    override fun fetch(request: Request, timeoutInSecs: Long): ApiPromise<Response> {
        val responseDescription = this.apiCalls[this.responseIndex](request)
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(responseDescription.status)
            .message("")
            .body(ResponseBody.create(MediaType.parse("application/json"), responseDescription.body))
            .build()

        this.responseIndex = this.responseIndex + 1

        return ApiPromise.of(response)
    }
}
