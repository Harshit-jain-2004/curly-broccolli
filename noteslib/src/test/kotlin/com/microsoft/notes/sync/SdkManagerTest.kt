package com.microsoft.notes.sync

import com.microsoft.notes.sync.models.DeltaSyncPayload
import com.microsoft.notes.sync.models.Document
import com.microsoft.notes.sync.models.RemoteNote
import com.microsoft.notes.sync.models.SyncResponse
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.utils.UserInfo
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class SdkManagerTest {
    @Test
    fun should_return_success_for_full_sync_on_200() {
        val fullSyncResponse = """
        {
            "deltaToken":"delta-token",
            "value":[
                ${remoteRichTextNoteJSON("123")}
            ]
        }
        """

        val sdk = MockNotesSdkWithValidators(
            listOf(
                { request -> response200(request, fullSyncResponse) }
            )
        ).getMockNotesSdk()
        val result = sdk.fullSync("1", "realTimeSessionId", parser = (RemoteNote)::fromJSON).get()
        val syncResponse = SyncResponse(
            token = Token.Delta("delta-token"),
            value = listOf(remoteRichTextNote("123"))
        )
        val expected = ApiResult.Success(syncResponse)
        assertThat<ApiResult<SyncResponse<RemoteNote>>>(result, iz(expected))
    }

    @Test
    fun should_return_success_for_delta_sync_on_200() {
        val deltaSyncResponse = """
        {
            "deltaToken":"new-delta-token",
            "value":[
                ${remoteRichTextNoteJSON("123")},
                {
                    "reason":"deleted",
                    "id": "456"
                }
            ]
        }
        """

        val sdk = MockNotesSdkWithValidators(
            listOf(
                { request -> response200(request, deltaSyncResponse) }
            )
        ).getMockNotesSdk()

        val result = sdk.deltaSync(
            "1", "realTimeSessionId",
            Token.Delta("old-delta-token"), parser = (DeltaSyncPayload)::fromJSON
        ).get()
        val syncResponse = SyncResponse(
            token = Token.Delta("new-delta-token"),
            value = listOf(
                DeltaSyncPayload.NonDeleted(remoteRichTextNote("123")),
                DeltaSyncPayload.Deleted(id = "456")
            )
        )
        val expected = ApiResult.Success(syncResponse)
        assertThat<ApiResult<SyncResponse<DeltaSyncPayload>>>(result, iz(expected))
    }

    @Test
    fun should_return_failure_for_full_sync_on_500() {
        val sdk = MockNotesSdkWithValidators(
            listOf(
                { request -> response500(request, "") }
            )
        ).getMockNotesSdk()

        val result = sdk.fullSync("1", "realTimeSessionId", parser = (RemoteNote)::fromJSON).get()
        val expected = ApiResult.Failure<SyncResponse<RemoteNote>>(
            HttpError500(emptyMap())
        )

        assertThat<ApiResult<SyncResponse<RemoteNote>>>(result, iz(expected))
    }

    @Test
    fun should_return_failure_for_delta_sync_on_500() {
        val sdk = MockNotesSdkWithValidators(
            listOf(
                { request -> response500(request, "") }
            )
        ).getMockNotesSdk()

        val result = sdk.deltaSync(
            "1", "realTimeSessionId",
            Token.Delta("old-delta-token"), parser = (DeltaSyncPayload)::fromJSON
        ).get()
        val expected = ApiResult.Failure<SyncResponse<RemoteNote>>(
            HttpError500(emptyMap())
        )

        assertThat<ApiResult<SyncResponse<DeltaSyncPayload>>>(result, iz(expected))
    }

    @Test
    fun should_use_correct_body_for_update() {
        var body = ""
        var called = false
        val sdk = MockNotesSdkWithValidators(
            listOf(
                { request ->
                    called = true
                    val buffer = Buffer()
                    request.newBuilder().build().body()!!.writeTo(buffer)
                    body = buffer.readUtf8()
                    response200(request, "")
                }
            )
        ).getMockNotesSdk()

        val note = note("123", "remoteId").copy(document = Document.RenderedInkDocument("", ""))
        sdk.updateNote("1", "reaTimeSessionId", note).get()
        assert(called)
        assert(!body.contains("\"document\":"))
    }
}

typealias Validator = (Request) -> Response

class MockNotesSdkWithValidators(val validators: List<Validator>) {
    var host: NotesClientHost = NotesClientHost.StaticHost("http://www.example.com")
    val token = ""
    val userInfo = UserInfo.EMPTY_USER_INFO
    val userAgent = ""
    val notesLogger: NotesLogger? = null
    fun getMockNotesSdk(): TestSdk {
        return TestSdk(validators, host, token, userInfo, userAgent, notesLogger)
    }
}

class TestSdk(
    validators: List<Validator>,
    host: NotesClientHost,
    token: String,
    userInfo: UserInfo,
    userAgent: String,
    notesLogger: NotesLogger?
) :
    NetworkedSdk.NotesSdk(host, token, userInfo, userAgent, notesLogger, gsonParserEnabled = true, inkEnabled = true, requestPriority = { RequestPriority.background }, feedRealTimeSyncEnabled = true) {

    override val apiVersion: String = "v1.1"
    private val mockHttp: TestHttpClient = TestHttpClient(validators)

    override fun longPoll(path: String, sessionId: String, onNewData: (char: Char) -> Unit): ApiPromise<Unit> {
        TODO("not implemented")
    }

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
}

class TestHttpClient(private val validators: List<Validator>) : HttpClient(null) {
    private val index = 0

    override fun fetch(request: Request, timeoutInSecs: Long): ApiPromise<Response> {
        val validator = this.validators.getOrNull(this.index)
            ?: throw AssertionError("Not enough validators")
        val response = validator(request)
        return ApiPromise.of(response)
    }
}

fun response200(request: Request, body: String): Response {
    return response(request, 200, body)
}

fun response500(request: Request, body: String): Response {
    return response(request, 500, body)
}

fun response(request: Request, responseCode: Int, body: String): Response {
    val responseBuilder = Response.Builder()
    responseBuilder.request(request)
    responseBuilder.code(responseCode)
    responseBuilder.protocol(Protocol.HTTP_1_1)
    responseBuilder.message("")
    responseBuilder.body(ResponseBody.create(MediaType.parse("application/json"), body))
    return responseBuilder.build()
}
