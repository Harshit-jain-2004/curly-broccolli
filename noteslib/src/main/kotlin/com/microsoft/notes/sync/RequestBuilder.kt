package com.microsoft.notes.sync

import okhttp3.MediaType
import okhttp3.Request
import java.net.URL

// TODO (machiam) Add additionalHeaders to other functions
class RequestBuilder(private val host: String) {
    fun get(
        path: String,
        additionalHeaders: Map<String, String> = emptyMap(),
        queryParams: Map<String, String>
    ): Request {
        val url = buildUrl(path, queryParams)
        val requestBuilder = Request.Builder()
        requestBuilder.get()
        requestBuilder.url(url)
        additionalHeaders.forEach { requestBuilder.addHeader(it.key, it.value) }
        return requestBuilder.build()
    }

    fun post(
        path: String,
        body: JSON
    ): Request {
        val url = buildUrl(path)
        val requestBody = okhttp3.RequestBody.create(MediaType.parse("application/json"), body.toString())
        val requestBuilder = Request.Builder()
        requestBuilder.post(requestBody)
        requestBuilder.url(url)
        return requestBuilder.build()
    }

    fun post(
        path: String,
        body: ByteArray,
        additionalHeaders: Map<String, String> = emptyMap(),
        mimeType: String
    ): Request {
        val url = buildUrl(path)
        val requestBody = okhttp3.RequestBody.create(MediaType.parse(mimeType), body)
        val requestBuilder = Request.Builder()
        requestBuilder.url(url)
        requestBuilder.post(requestBody)
        additionalHeaders.forEach { requestBuilder.addHeader(it.key, it.value) }
        return requestBuilder.build()
    }

    fun patch(
        path: String,
        body: JSON
    ): Request {
        val url = buildUrl(path)
        val requestBody = okhttp3.RequestBody.create(MediaType.parse("application/json"), body.toString())
        val requestBuilder = Request.Builder()
        requestBuilder.patch(requestBody)
        requestBuilder.url(url)
        return requestBuilder.build()
    }

    fun delete(path: String): Request {
        val url = buildUrl(path)
        val requestBuilder = Request.Builder()
        requestBuilder.delete()
        requestBuilder.url(url)
        return requestBuilder.build()
    }

    private fun buildUrl(path: String, queryParams: Map<String, String> = emptyMap()): URL {
        var queryString = queryParams.map { (key, value) -> "$key=$value" }.joinToString("&")
        queryString = if (queryString.isNotEmpty()) {
            "?$queryString"
        } else {
            ""
        }
        return URL(this.host + path + queryString)
    }
}
