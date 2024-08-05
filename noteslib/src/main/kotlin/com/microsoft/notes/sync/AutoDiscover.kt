package com.microsoft.notes.sync

import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.utils.UserInfo
import com.microsoft.notes.utils.utils.getRoutingKey

interface AutoDiscover {
    fun getNotesClientHostUrl(userInfo: UserInfo): ApiPromise<NotesClientHost.ExpirableHost>
}

class NetworkedAutoDiscover(
    private val autoDiscoverHost: String,
    private val userAgent: String,
    private val notesLogger: NotesLogger?
) : AutoDiscover {

    companion object {
        private const val TIMEOUT_IN_SECONDS = 80L
        const val DEFAULT_AUTODISCOVER_HOST = "https://outlook.office365.com"
        private val defaultQueryParams = mapOf(Pair("Protocol", "NotesClient"))
    }

    override fun getNotesClientHostUrl(userInfo: UserInfo): ApiPromise<NotesClientHost.ExpirableHost> {
        val builder = RequestBuilder(autoDiscoverHost)
        val routingKey = userInfo.getRoutingKey()
        val request = builder.get(
            "/autodiscover/autodiscover.json/v1.0/$routingKey",
            additionalHeaders = mapOf(Pair("User-Agent", userAgent), Pair("X-AnchorMailbox", routingKey)),
            queryParams = defaultQueryParams
        )
        val httpClient = HttpClient(notesLogger, userInfo.userID)
        return httpClient.fetchAsJSON(request, TIMEOUT_IN_SECONDS) andTry { json ->
            val obj = json as? JSON.JObject
            val url = obj?.get<JSON.JString>("Url")?.string
            val apiResult: ApiResult<NotesClientHost.ExpirableHost> = if (url != null) {
                ApiResult.Success(NotesClientHost.ExpirableHost(url, System.currentTimeMillis()))
            } else {
                ApiResult.Failure(
                    ApiError.Exception(
                        Exception(
                            "AutoDiscover responded with an unexpected body " +
                                "format"
                        )
                    )
                )
            }
            apiResult
        }
    }
}
