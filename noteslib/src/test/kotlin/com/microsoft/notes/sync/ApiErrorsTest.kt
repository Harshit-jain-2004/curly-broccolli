package com.microsoft.notes.sync

import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ApiErrorsTest {

    @Test
    fun `should parse correctly a json error well formed`() {
        val STATUS_CODE = 400
        val jsonError = "{\n" +
            "    \"error\": {\n" +
            "        \"code\": \"An error code string for the error that occured\",\n" +
            "        \"message\": \"A developer ready message about the error that occured. This should not be displayed to the user directly.\",\n" +
            "        \"innerError\": {}\n" +
            "    }\n" +
            "}"

        val httpError = toHttpError(STATUS_CODE, jsonError, emptyMap(), null)

        val httpErrorExpected = ErrorDetails(
            error = Error(
                code = "An error code string for the error that occured",
                message = "A developer ready message about the error that occured. This should not be displayed " +
                    "to the user directly.",
                innerError = emptyMap()
            )
        )

        assertThat(httpError, instanceOf(HttpError400::class.java))
        assertThat(httpError.statusCode, iz(STATUS_CODE))
        assertThat(httpError.errorDetails, iz(not(nullValue())))
        assertThat(httpError.errorDetails, iz(httpErrorExpected))
    }

    @Test
    fun `should parse correctly a json error with not all fields`() {
        val STATUS_CODE = 409
        val jsonError = "{\"error\":{\"code\":\"Conflict\"}}"

        val httpError = toHttpError(STATUS_CODE, jsonError, emptyMap(), null)

        val httpErrorExpected = ErrorDetails(
            error = Error(
                code = "Conflict",
                message = null,
                innerError = null
            )
        )

        assertThat(httpError, instanceOf(HttpError409::class.java))
        assertThat(httpError.statusCode, iz(STATUS_CODE))
        assertThat(httpError.errorDetails, iz(not(nullValue())))
        assertThat(httpError.errorDetails, iz(httpErrorExpected))
    }

    @Test
    fun `should parse error details correctly`() {
        val STATUS_CODE = 409
        val jsonError = "{\"error\":{\"code\":\"Conflict\", \"innerError\":{\"foo\":null}}}"

        val httpError = toHttpError(STATUS_CODE, jsonError, emptyMap(), null)

        val httpErrorExpected = ErrorDetails(
            error = Error(
                code = "Conflict",
                message = null,
                innerError = mapOf("foo" to null)
            )
        )

        assertThat(httpError, instanceOf(HttpError409::class.java))
        assertThat(httpError.statusCode, iz(STATUS_CODE))
        assertThat(httpError.errorDetails, iz(not(nullValue())))
        assertThat(httpError.errorDetails, iz(httpErrorExpected))
    }

    @Test
    fun `should parse correctly a json error with a code that we don't know`() {
        val STATUS_CODE = 409
        val CODE = "dont know"
        val jsonError = "{\"error\":{\"code\":\"$CODE\"}}"

        val httpError = toHttpError(STATUS_CODE, jsonError, emptyMap(), null)

        val httpErrorExpected = ErrorDetails(
            error = Error(
                code = CODE,
                message = null,
                innerError = null
            )
        )

        assertThat(httpError, instanceOf(HttpError409::class.java))
        assertThat(httpError.statusCode, iz(STATUS_CODE))
        assertThat(httpError.errorDetails, iz(not(nullValue())))
        assertThat(httpError.errorDetails, iz(httpErrorExpected))
    }

    @Test
    fun `should create specific UnknownError when parsing wrong json error message`() {
        val CODE_STATUS = 409
        val jsonError = "{\"cesar\":{\"code\":\"error\"}}"

        val httpError = toHttpError(CODE_STATUS, jsonError, emptyMap(), null)

        assertThat(httpError, instanceOf(HttpError409::class.java))
        assertThat(httpError.statusCode, iz(CODE_STATUS))
        assertThat(httpError.errorDetails, iz(nullValue()))
    }

    @Test
    fun `should create general UnknownError when parsing unknown code status`() {
        val CODE_STATUS = 505
        val jsonError = "{\"error\":{\"code\":\"UnknownCode\"}}"

        val httpError = toHttpError(CODE_STATUS, jsonError, emptyMap(), null)

        val httpErrorExpected = ErrorDetails(
            error = Error(
                code = "UnknownCode",
                message = null,
                innerError = null
            )
        )

        assertThat(httpError, instanceOf(UnknownHttpError::class.java))
        assertThat(httpError.statusCode, iz(CODE_STATUS))
        assertThat(httpError.errorDetails, iz(httpErrorExpected))
    }

    @Test
    fun `should create a specific HttpError when parsing HttpError with known specific codes`() {
        val CODE_STATUS = 410
        val jsonError = "{\n" +
            "    \"error\": {\n" +
            "        \"code\": \"InvalidClientCache\",\n" +
            "        \"message\": \"You have to invalidate the client cache\",\n" +
            "        \"innerError\": {}\n" +
            "    }\n" +
            "}"

        val httpError = toHttpError(CODE_STATUS, jsonError, emptyMap(), null)

        val httpErrorExpected = ErrorDetails(
            error = Error(
                code = "InvalidClientCache",
                message = "You have to invalidate the client cache",
                innerError = emptyMap()
            )
        )

        assertThat(httpError, instanceOf(HttpError410.InvalidateClientCache::class.java))
        assertThat(httpError.statusCode, iz(CODE_STATUS))
        assertThat(httpError.errorDetails, iz(httpErrorExpected))
    }

    @Test
    fun `should create an unknown HttpError when parsing HttpError with known specific codes`() {
        val CODE_STATUS = 410
        val jsonError = "{\n" +
            "    \"error\": {\n" +
            "        \"code\": \"Unknown\",\n" +
            "        \"message\": \"You have to invalidate the client cache\",\n" +
            "        \"innerError\": {}\n" +
            "    }\n" +
            "}"

        val httpError = toHttpError(CODE_STATUS, jsonError, emptyMap(), null)

        val httpErrorExpected = ErrorDetails(
            error = Error(
                code = "Unknown",
                message = "You have to invalidate the client cache",
                innerError = emptyMap()
            )
        )

        assertThat(httpError, instanceOf(HttpError410.UnknownHttpError410::class.java))
        assertThat(httpError.statusCode, iz(CODE_STATUS))
        assertThat(httpError.errorDetails, iz(httpErrorExpected))
    }

    @Test
    fun `should create a specific 404 HttpError when parsing HttpError with known specific codes`() {
        val CODE_STATUS = 404
        val jsonError = "{\n" +
            "    \"error\": {\n" +
            "        \"code\": \"RestApiNotFound\",\n" +
            "        \"message\": \"Rest Api Not Found. No rest now. Enjoy :P.\",\n" +
            "        \"innerError\": {}\n" +
            "    }\n" +
            "}"

        val httpError = toHttpError(CODE_STATUS, jsonError, emptyMap(), null)

        val httpErrorExpected = ErrorDetails(
            error = Error(
                code = "RestApiNotFound",
                message = "Rest Api Not Found. No rest now. Enjoy :P.",
                innerError = emptyMap()
            )
        )

        assertThat(httpError, instanceOf(HttpError404.Http404RestApiNotFound::class.java))
        assertThat(httpError.statusCode, iz(CODE_STATUS))
        assertThat(httpError.errorDetails, iz(httpErrorExpected))
    }

    @Test
    fun `should create an unknown Http404Error when parsing HttpError with known specific codes`() {
        val CODE_STATUS = 404
        val jsonError = "{\n" +
            "    \"error\": {\n" +
            "        \"code\": \"NotFound\",\n" +
            "        \"message\": \"Again not found.\",\n" +
            "        \"innerError\": {}\n" +
            "    }\n" +
            "}"

        val httpError = toHttpError(CODE_STATUS, jsonError, emptyMap(), null)

        val httpErrorExpected = ErrorDetails(
            error = Error(
                code = "NotFound",
                message = "Again not found.",
                innerError = emptyMap()
            )
        )

        assertThat(httpError, instanceOf(HttpError404.UnknownHttp404Error::class.java))
        assertThat(httpError.statusCode, iz(CODE_STATUS))
        assertThat(httpError.errorDetails, iz(httpErrorExpected))
    }

    @Test
    fun `should create a 503 with retry`() {
        val CODE_STATUS = 503
        val jsonError = "{}"

        val httpError = toHttpError(
            CODE_STATUS, jsonError, mapOf("Retry-After" to "1000"),
            null
        ) as HttpError503

        assertThat(httpError.statusCode, iz(CODE_STATUS))
        assertThat(httpError.headers["Retry-After"], iz(not(nullValue())))
        assertThat(httpError.headers["Retry-After"]!!, iz("1000"))
        assertNull(httpError.errorDetails)
    }
}
