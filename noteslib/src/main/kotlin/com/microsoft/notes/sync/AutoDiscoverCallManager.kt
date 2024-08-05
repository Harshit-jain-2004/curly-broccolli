package com.microsoft.notes.sync

import android.content.Context
import com.microsoft.notes.platform.extensions.isNetworkConnected
import com.microsoft.notes.platform.extensions.registerOnNetworkAvailableCompat
import com.microsoft.notes.utils.logging.AutoDiscoverApiHostRequestResult
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.HostTelemetryKeys
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.AutoDiscoverProperty
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.RequestProperty
import com.microsoft.notes.utils.network.NetworkAvailableObservable
import com.microsoft.notes.utils.network.NetworkAvailableObserver
import com.microsoft.notes.utils.utils.UserInfo

class AutoDiscoverCallManager(
    private val context: Context,
    private val autoDiscover: AutoDiscover,
    private val autoDiscoverCache: AutoDiscoverCache,
    private val notesLogger: NotesLogger? = null
) {
    companion object {
        private val autoDiscoverBackoff = ExponentialBackoff(
            retryTimes = 3,
            initialDelayInMs = 500,
            maxDelayInMs = 2000,
            factor = 2.0
        )
        private const val AUTODISCOVER_SERVICE_ISSUE_RETRY_DELAY: Long = 60 * 60 * 1000 // 1 hour
        private val pendingNetworkCallUserIDs = mutableSetOf<String>()
    }

    private val networkAvailableObservable = NetworkAvailableObservable(
        { context.registerOnNetworkAvailableCompat(it) },
        { context.isNetworkConnected() }
    )

    /**
     * Gets the user's NotesClient API host url with:
     * - Caching
     * - Outbound rate limiting
     * - Waiting on network availability if disconnected (with max 1 network listener per user)
     */
    fun getNotesClientHostUrl(
        userInfo: UserInfo,
        successObserver: (h: NotesClientHost.ExpirableHost, fromCache: Boolean) -> Unit,
        errorObserver: (failure: ApiResult.Failure<NotesClientHost.ExpirableHost>) -> Unit
    ) {
        val hostInCache = autoDiscoverCache.getExpirableHost(userInfo.userID)
        hostInCache?.let {
            successObserver(it, true)
            if (!it.isExpired()) {
                return
            }
        }

        if (pendingNetworkCallUserIDs.contains(userInfo.userID)) {
            // Do nothing as a network listener has already been registered
            return
        }
        getWhenNetworkOnline(userInfo, hostInCache != null, successObserver, errorObserver)
    }

    private fun getWhenNetworkOnline(
        userInfo: UserInfo,
        existsInCache: Boolean,
        successCallback: (h: NotesClientHost.ExpirableHost, fromCache: Boolean) -> Unit,
        errorObserver: (failure: ApiResult.Failure<NotesClientHost.ExpirableHost>) -> Unit
    ) {
        pendingNetworkCallUserIDs.add(userInfo.userID)

        networkAvailableObservable.addObserver(
            NetworkAvailableObserver({
                getHostWithBackoff(userInfo, existsInCache, successCallback = successCallback, failureCallback = {
                    if (!existsInCache) {
                        errorObserver(it)
                    }
                    ApiPromise.delay(AUTODISCOVER_SERVICE_ISSUE_RETRY_DELAY).andThen {
                        getWhenNetworkOnline(
                            userInfo = userInfo,
                            existsInCache = existsInCache,
                            successCallback = successCallback,
                            errorObserver = errorObserver
                        )
                    }
                })
            }, removeWhenCalled = true)
        )
    }

    private fun getHostWithBackoff(
        userInfo: UserInfo,
        existsInCache: Boolean,
        successCallback: (h: NotesClientHost.ExpirableHost, fromCache: Boolean) -> Unit,
        failureCallback: (failure: ApiResult.Failure<NotesClientHost.ExpirableHost>) -> Unit
    ) {
        var startTime: Long = 0
        (
            {
                startTime = System.currentTimeMillis()

                autoDiscover.getNotesClientHostUrl(userInfo)
            }
            ).withBackoff(autoDiscoverBackoff, shouldRetryFailure).onComplete {
            when (it) {
                is ApiResult.Success -> {
                    notesLogger?.recordTelemetry(
                        EventMarkers.AutoDiscoverApiHostRequest,
                        Pair(AutoDiscoverProperty.EXISTS_IN_CACHE, "$existsInCache"),
                        Pair(
                            RequestProperty.DURATION_IN_MS,
                            "${System
                                .currentTimeMillis() - startTime}"
                        ),
                        Pair(HostTelemetryKeys.RESULT, AutoDiscoverApiHostRequestResult.SUCCESS)
                    )

                    pendingNetworkCallUserIDs.remove(userInfo.userID)
                    successCallback(it.value, false)
                }
                is ApiResult.Failure -> {
                    var statusCode = "0"
                    var errorCode: String? = null
                    var errorMessage: String? = null
                    if (it.error is HttpError) {
                        statusCode = "${it.error.statusCode}"
                        errorCode = it.error.errorDetails?.error?.code
                        errorMessage = it.error.errorDetails?.error?.message
                    }
                    notesLogger?.recordTelemetry(
                        EventMarkers.AutoDiscoverApiHostRequest,
                        Pair(RequestProperty.HTTP_STATUS, statusCode),
                        Pair(AutoDiscoverProperty.EXISTS_IN_CACHE, "$existsInCache"),
                        Pair(
                            RequestProperty.DURATION_IN_MS,
                            "${
                            System
                                .currentTimeMillis() - startTime
                            }"
                        ),
                        Pair(HostTelemetryKeys.RESULT, AutoDiscoverApiHostRequestResult.FAILURE)
                    )
                    Pair(AutoDiscoverProperty.ERROR_CODE, errorCode)
                    Pair(AutoDiscoverProperty.ERROR_MESSAGE, errorMessage)
                    failureCallback(it)
                }
            }
        }
    }
}

val shouldRetryFailure = { failure: ApiResult.Failure<NotesClientHost.ExpirableHost> ->
    failure.error.let {
        when (it) {
            is HttpError -> {
                when (it.errorDetails?.error?.code) {
                    AutoDiscoverErrorCode.PROTOCOL_NOT_SUPPORTED.errorCode -> false
                    AutoDiscoverErrorCode.USER_NOT_FOUND.errorCode -> false
                    else -> true
                }
            }
            else -> true
        }
    }
}
