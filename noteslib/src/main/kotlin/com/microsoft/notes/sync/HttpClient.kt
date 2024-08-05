package com.microsoft.notes.sync

import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.utils.logging.NotesLogger
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource
import java.io.IOException
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

open class HttpClient(val notesLogger: NotesLogger? = null, val userID: String? = null) {

    fun fetchAsJSON(
        request: Request,
        timeoutInSecs: Long
    ): ApiPromise<JSON> {
        return fetchAsBufferedSource(request, timeoutInSecs) andTry { bufferedSource: BufferedSource ->
            val json = JSON.read(bufferedSource)
            bufferedSource.close()
            json
        }
    }

    fun fetchAsBufferedSource(
        request: Request,
        timeoutInSecs: Long
    ): ApiPromise<BufferedSource> {
        return fetch(request, timeoutInSecs) mapError {
            when (it) {
                is ApiError.Exception -> ApiError.NetworkError(it.exception)
                else -> it
            }
        } andTry { response ->
            if (response.isSuccessful) {
                notesLogger?.d(message = "Response successful")
                handleSuccessfulResponse(response)
            } else {
                notesLogger?.d(message = "Response failed message: ${response.message()}")
                notesLogger?.d(message = "Response failed code: ${response.code()}")
                handleFailedResponse(response)
            }
        }
    }

    open fun fetch(request: Request, timeoutInSecs: Long): ApiPromise<Response> {
        return ApiPromise.execute { done, fail ->
            var sslObject: SSLSocketFactory? = null
            var trustManager: X509TrustManager? = null
            if (userID != null) {
                val secureSocketDetails = NotesLibrary.getInstance().getSSLConfigurationFromIntune?.invoke(userID, request.url().toString())
                sslObject = secureSocketDetails?.first
                trustManager = secureSocketDetails?.second
            }
            defaultHttpClient(timeoutInSecs, sslObject, trustManager).newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) { done(response) }
                override fun onFailure(call: Call, e: IOException) { fail(e) }
            })
        }
    }

    private fun handleSuccessfulResponse(response: Response): ApiResult<BufferedSource> {
        val body = response.body()
        return if (body != null) {
            ApiResult.Success(body.source())
        } else {
            ApiResult.Failure(ApiError.FatalError("Body was null"))
        }
    }

    private fun handleFailedResponse(response: Response): ApiResult.Failure<BufferedSource> {
        val body = response.body()
        return if (body != null) {
            val bodyString = body.string()
            val headers = response.headers().toMultimap().toMap().mapValues {
                it.component2().joinToString(",")
            }
            ApiResult.Failure(
                toHttpError(
                    statusCode = response.code(),
                    body = bodyString,
                    headers = headers,
                    notesLogger = notesLogger
                )
            )
        } else {
            ApiResult.Failure(ApiError.FatalError("Body was null"))
        }
    }

    private fun defaultHttpClient(timeoutInSecs: Long, sslSignedObject: SSLSocketFactory?, trustManager: X509TrustManager?) = OkHttpClientProvider.defaultHttpClient(timeoutInSecs, sslSignedObject, trustManager)
}
