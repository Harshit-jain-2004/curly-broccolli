package com.microsoft.notes.sync

import com.microsoft.notes.models.TimeDuration
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.sync.models.MeetingsResponse
import com.microsoft.notes.sync.models.RemoteMeeting
import com.microsoft.notes.sync.models.RemoteMeetingNoteFromCollab
import com.microsoft.notes.sync.models.SyncResponse
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.sync.models.getListOfRemoteMeetingNoteFromCollab
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.utils.UserInfo
import com.microsoft.notes.utils.utils.getRoutingKey
import com.microsoft.notes.utils.utils.parseMillisToISO8601String
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import kotlin.collections.LinkedHashMap
import kotlin.reflect.KFunction1

private const val FILE_TIMEOUT: Long = 120
private const val DEFAULT_TIMEOUT: Long = 60

sealed class NetworkedSdk(
    val token: String,
    val userInfo: UserInfo,
    val userAgent: String,
    val notesLogger: NotesLogger?,
    gsonParserEnabled: Boolean,
    val requestPriority: () -> RequestPriority
) {
    init {
        JSON.isGsonEnabled = gsonParserEnabled
        JSON.notesLogger = notesLogger
    }

    fun defaultHttpClient(
        timeoutInSecs: Long,
        sslSocketFactory: SSLSocketFactory?,
        trustManager: X509TrustManager?
    ) = OkHttpClientProvider.defaultHttpClient(timeoutInSecs, sslSocketFactory, trustManager)

    open class NotesSdk(
        override var host: NotesClientHost,
        token: String,
        userInfo: UserInfo,
        userAgent: String,
        notesLogger: NotesLogger?,
        val inkEnabled: Boolean,
        gsonParserEnabled: Boolean,
        requestPriority: () -> RequestPriority,
        feedRealTimeSyncEnabled: Boolean
    ) : NetworkedSdk(token, userInfo, userAgent, notesLogger, gsonParserEnabled, requestPriority), INotesSdk {

        override val apiVersion: String =
            if (feedRealTimeSyncEnabled) REALTIME_API_VERSION_V2 else REALTIME_API_VERSION_V1
        val http = HttpClient(notesLogger, userInfo.userID)
        private val xAnchorMailbox = userInfo.getRoutingKey()

        override fun fetch(
            requestId: String,
            realTimeSessionId: String,
            request: Request,
            isFileRelated: Boolean
        ): ApiPromise<Response> {
            val requestWithHeaders = addHeadersToRequest(request, requestId, realTimeSessionId)
            val timeoutInSecs: Long = getTimeoutInSeconds(isFileRelated)
            return http.fetch(requestWithHeaders, timeoutInSecs)
        }

        override fun fetchAsJSON(
            requestId: String,
            realTimeSessionId: String,
            request: Request,
            isFileRelated: Boolean,
            withHtml: Boolean
        ): ApiPromise<JSON> {
            val requestWithHeaders =
                addHeadersToRequest(request, requestId, realTimeSessionId, withHtml)
            val timeoutInSecs: Long = getTimeoutInSeconds(isFileRelated)
            return http.fetchAsJSON(requestWithHeaders, timeoutInSecs)
        }

        override fun fetchAsBufferedSource(
            requestId: String,
            realTimeSessionId: String,
            request: Request,
            isFileRelated: Boolean
        ): ApiPromise<BufferedSource> {
            val requestWithHeaders = addHeadersToRequest(request, requestId, realTimeSessionId)
            val timeoutInSecs: Long = getTimeoutInSeconds(isFileRelated)
            return http.fetchAsBufferedSource(requestWithHeaders, timeoutInSecs)
        }

        private fun getTimeoutInSeconds(isFileRelated: Boolean): Long =
            if (isFileRelated) FILE_TIMEOUT else DEFAULT_TIMEOUT

        override fun longPoll(
            path: String,
            sessionId: String,
            onNewData: (char: Char) -> Unit
        ): ApiPromise<Unit> {
            return ApiPromise.tryTask {
                notesLogger?.i(message = "Realtime longPoll starting")
                val request = RequestBuilder(host.url).get(
                    path, additionalHeaders = emptyMap(),
                    queryParams = emptyMap()
                )
                val requestWithHeaders = addHeadersToRequest(
                    request,
                    realTimeSessionId = sessionId
                )

                val timeoutInSecs: Long = DEFAULT_TIMEOUT

                val (sslObject, trustManager) = NotesLibrary.getInstance().getSSLConfigurationFromIntune?.invoke(
                    userInfo.userID,
                    request.url().toString()
                ) ?: Pair(null, null)

                val client = defaultHttpClient(timeoutInSecs, sslObject, trustManager)
                val response = client.newCall(requestWithHeaders).execute()
                if (!response.isSuccessful) {
                    notesLogger?.e(message = "Realtime longPoll not successful")
                    val headers = response.headers().toMultimap().toMap().mapValues {
                        it.component2().joinToString(",")
                    }
                    val httpError =
                        toHttpError(response.code(), response.body()?.string() ?: "", headers)
                    response.close()
                    ApiResult.Failure<Unit>(httpError)
                } else {
                    val source = response.body()?.source()
                    val stream = source?.inputStream()?.buffered()

                    var data = stream?.read()
                    while (data != null) {
                        onNewData(data.toChar())
                        data = stream?.read()
                    }
                    notesLogger?.e(message = "Realtime longPoll connection ended")
                    ApiResult.Success(response.close())
                }
            }
        }

        private fun addHeadersToRequest(
            request: Request,
            requestId: String? = null,
            realTimeSessionId: String? = null,
            addHtmlHeader: Boolean = false
        ): Request {
            val inkFormat = if (inkEnabled) "ink" else "png"

            val header = request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("X-AnchorMailBox", xAnchorMailbox)
                .addHeader("Prefer", "inkFormat=\"$inkFormat\"")
                .addHeader("User-Agent", userAgent)
                .addHeader("Request-Priority", requestPriority().name)
                .addHeaderIfNotNull("MS-CV", requestId)
                .addHeaderIfNotNull("realtime-session-id", realTimeSessionId)
            if (addHtmlHeader) {
                header.addHeader("html", "true")
            }
            if (NotesLibrary.getInstance().experimentFeatureFlags.enableContextInNotes) {
                header.addHeader("useMetadataSchema", "true")
            }
            return header.build()
        }
    }

    open class MeetingNotesSdk(
        val workingSetAPIHost: MeetingNotesClientHost,
        val eventsAPIHost: MeetingNotesClientHost,
        val collabAPIHost: MeetingNotesClientHost,
        token: String,
        userInfo: UserInfo,
        userAgent: String,
        notesLogger: NotesLogger?,
        gsonParserEnabled: Boolean,
        requestPriority: () -> RequestPriority
    ) : NetworkedSdk(token, userInfo, userAgent, notesLogger, gsonParserEnabled, requestPriority) {
        private enum class ApiType {
            WORKING_SET_API,
            EVENTS_API,
            COLLAB_API
        }

        val http = HttpClient(notesLogger, userInfo.userID)
        fun <NoteT> fullSync(
            requestId: String,
            skipToken: Token.Skip? = null,
            parser: KFunction1<JSON, NoteT?>
        ): ApiPromise<SyncResponse<NoteT>> {
            val queryParams = getWorkingSetAPIQueryParams(skipToken?.token)
            return fetchNotesFromWorkingSet(requestId, queryParams, parser = parser)
        }

        private fun <T> fetchNotesFromWorkingSet(
            requestId: String,
            queryParams: Map<String, String>,
            parser: (JSON) -> T?
        ): ApiPromise<SyncResponse<T>> {
            val request = RequestBuilder(host = workingSetAPIHost.url).get(
                "", additionalHeaders = emptyMap(),
                queryParams = queryParams
            )
            return fetchResultAsJSON(requestId, request, ApiType.WORKING_SET_API) andTry { json: JSON ->
                val syncResponse = SyncResponse.fromJSON(
                    json = json,
                    valueParser = parser,
                    tokenParser = (Token)::fromWsetResponseJSON
                )
                if (syncResponse != null) {
                    ApiResult.Success(syncResponse)
                } else {
                    ApiResult.Failure<SyncResponse<T>>(
                        ApiError.InvalidJSONError(json)
                    )
                }
            }
        }

        /*
        * oId is in this format oid:${userOId}@${tenantId} eg. 123@456.
        * The first part is the logged in user's oid and the second is the tenant id they belong to.
        */
        private fun fetchNotesForMeetingUsingCollab(
            requestId: String,
            userOId: String,
            organizerEmail: String,
            iCalUId: String
        ): ApiPromise<List<RemoteMeetingNoteFromCollab>> {
            val request = RequestBuilder(host = collabAPIHost.url).get(
                "/oid:$userOId/collabs",
                additionalHeaders = emptyMap(),
                queryParams = getCollabAPIQueryParams(organizerEmail, iCalUId)
            )
            return fetchResultAsJSON(
                requestId,
                request,
                ApiType.COLLAB_API
            ) andTry { json: JSON ->
                val remoteMeetingNoteFromCollabList: List<RemoteMeetingNoteFromCollab>? = getListOfRemoteMeetingNoteFromCollab(json)
                if (remoteMeetingNoteFromCollabList != null) {
                    ApiResult.Success(remoteMeetingNoteFromCollabList)
                } else {
                    ApiResult.Failure<List<RemoteMeetingNoteFromCollab>>(
                        ApiError.InvalidJSONError(json)
                    )
                }
            }
        }

        private fun getCollabAPIQueryParams(
            organizerEmail: String,
            iCalUId: String
        ): Map<String, String> {
            val map = HashMap<String, String>()
            map["collab_id"] = "2smtp:$organizerEmail" + "externalentitykey:$iCalUId"
            return map
        }

        // Function to fetch events using calendar ApiType for a given time duration
        // Time duration is marked by TimeDuration(startTimeInEpochMillis, endTimeInEpochMillis)
        private fun fetchMeetings(
            requestId: String,
            timeDuration: TimeDuration
        ): ApiPromise<MeetingsResponse<RemoteMeeting>> {
            val request = RequestBuilder(host = eventsAPIHost.url).get(
                "", additionalHeaders = emptyMap(),
                queryParams = getEventsAPIQueryParams(
                    timeDuration
                )
            )
            return fetchResultAsJSON(
                requestId,
                request,
                ApiType.EVENTS_API
            ) andTry { json: JSON ->
                val meetingsResponse = MeetingsResponse.fromJSON(
                    json = json
                )
                if (meetingsResponse != null) {
                    ApiResult.Success(meetingsResponse)
                } else {
                    ApiResult.Failure<MeetingsResponse<RemoteMeeting>>(
                        ApiError.InvalidJSONError(json)
                    )
                }
            }
        }

        private fun getEventsAPIQueryParams(timeDuration: TimeDuration): Map<String, String> {
            val map = HashMap<String, String>()
            map["startDateTime"] =
                parseMillisToISO8601String(timeDuration.startTimeInEpochMillis)
            map["endDateTime"] =
                parseMillisToISO8601String(timeDuration.endTimeInEpochMillis)
            return map
        }

        private fun getWorkingSetAPIQueryParams(skipToken: String?): Map<String, String> {
            val map = LinkedHashMap<String, String>()
            map["\$select"] =
                "FileName,FileCreatedTime,FileModifiedTime,SharePointItem/FileUrl,Visualization"
            map["\$filter"] = "(Visualization/Type eq 'FluidNote' and FileExtension eq 'note')"
            map["\$top"] = "5"
            if (!skipToken.isNullOrEmpty())
                map["\$skiptoken"] = skipToken
            return map
        }

        private fun addHeadersToRequest(
            request: Request,
            requestId: String? = null,
            api: ApiType
        ): Request {
            val header = request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("User-Agent", userAgent)
                .addHeaderIfNotNull("client-request-id", requestId)
            if (api == ApiType.WORKING_SET_API) {
                header.addHeader(
                    "Prefer",
                    "outlook.data-source=\"Substrate\", exchange.behavior=\"SubstrateFiles\""
                )
            }
            return header.build()
        }

        private fun fetchResultAsJSON(
            requestId: String,
            request: Request,
            api: ApiType
        ): ApiPromise<JSON> {
            val requestWithHeaders =
                addHeadersToRequest(request, requestId, api)
            return http.fetchAsJSON(requestWithHeaders, DEFAULT_TIMEOUT)
        }
    }
}
