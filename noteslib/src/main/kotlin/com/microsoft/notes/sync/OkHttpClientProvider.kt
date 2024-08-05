package com.microsoft.notes.sync

import com.microsoft.notes.noteslib.NotesLibrary
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.internal.Util
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

class OkHttpClientProvider {
    companion object {
        private const val HTTP_CLIENT_THREAD_NAME = "OkHttp Dispatcher#StickyNotesSDK"

        private val okHttpClient by lazy { createDefaultOkHttpClient() }

        fun defaultHttpClient(timeoutInSecs: Long, sslSignedObject: SSLSocketFactory?, trustManager: X509TrustManager?): OkHttpClient {
            return if (sslSignedObject != null && trustManager != null) {
                okHttpClient.newBuilder()
                    .sslSocketFactory(sslSignedObject, trustManager)
                    .connectTimeout(timeoutInSecs, TimeUnit.SECONDS)
                    .readTimeout(timeoutInSecs, TimeUnit.SECONDS)
                    .writeTimeout(timeoutInSecs, TimeUnit.SECONDS)
                    .build()
            } else {
                okHttpClient.newBuilder()
                    .connectTimeout(timeoutInSecs, TimeUnit.SECONDS)
                    .readTimeout(timeoutInSecs, TimeUnit.SECONDS)
                    .writeTimeout(timeoutInSecs, TimeUnit.SECONDS)
                    .build()
            }
        }

        private fun createDefaultOkHttpClient(): OkHttpClient {
            // create a executor service which creates threads with a customized name
            // to make it easier to point out how many active threads are created by SDK
            val defaultExecutorService = ThreadPoolExecutor(
                0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
                SynchronousQueue(), Util.threadFactory(HTTP_CLIENT_THREAD_NAME, false)
            )
            val defaultDispatcher = Dispatcher(defaultExecutorService)

            // limit the max request count if configured
            NotesLibrary.getInstance().experimentFeatureFlags.httpMaxRequestCount?.let {
                defaultDispatcher.maxRequests = it
            }

            return OkHttpClient.Builder()
                .dispatcher(defaultDispatcher)
                .build()
        }
    }
}
