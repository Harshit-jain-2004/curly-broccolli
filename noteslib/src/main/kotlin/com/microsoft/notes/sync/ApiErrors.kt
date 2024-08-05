package com.microsoft.notes.sync

import com.microsoft.notes.utils.logging.NotesLogger
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException

sealed class ApiError {
    data class NetworkError(val error: Throwable) : ApiError()
    data class NonJSONError(val error: String) : ApiError()
    data class InvalidJSONError(val json: JSON) : ApiError()
    data class FatalError(val message: String) : ApiError()
    data class Exception(val exception: kotlin.Exception) : ApiError()
}

data class Error(val code: String? = null, val message: String? = null, val innerError: Map<String, Any?>? = null)
@JsonClass(generateAdapter = true)
data class ErrorDetails(val error: Error)

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming")
data class AutoDiscoverErrorDetails(val ErrorCode: String, val ErrorMessage: String)

sealed class HttpError : ApiError() {
    abstract val statusCode: Int
    abstract val errorDetails: ErrorDetails?
    abstract val headers: Map<String, String>

    final override fun toString(): String {
        val errorDetailsSafe = errorDetails
        if (errorDetailsSafe != null) {
            with(errorDetailsSafe.error) {
                return "Status code: $statusCode. " +
                    "\nError details: " +
                    "\n\tcode: $code" +
                    "\n\tmessage: $message" +
                    "\n\tinnerError: $innerError\n"
            }
        } else {
            return "Status code: $statusCode but couldn't parse error details\n"
        }
    }

    fun isSyncFailureTelemetryNeeded(): Boolean {
        return when (this) {
            is HttpError401 -> false
            is HttpError410 -> false
            else -> true
        }
    }
}

internal object HeaderKeys {
    const val RETRY_AFTER_HEADER = "retry-after"
    const val X_CALCULATED_BE_TARGET = "x-calculatedbetarget"
    const val REQUEST_ID = "request-id"
}

private const val HTTP_400_ERROR = 400

data class HttpError400(
    override val headers: Map<String, String>,
    override val errorDetails: ErrorDetails? = null
) : HttpError() {
    override val statusCode: Int = HTTP_400_ERROR
}

private const val HTTP_401_ERROR = 401

data class HttpError401(
    override val headers: Map<String, String>,
    override val errorDetails: ErrorDetails? = null
) : HttpError() {
    override val statusCode: Int = HTTP_401_ERROR
}

private const val HTTP_403_ERROR = 403

sealed class HttpError403(
    override val headers: Map<String, String>,
    override val errorDetails: ErrorDetails? = null
) : HttpError() {
    override val statusCode: Int = HTTP_403_ERROR

    data class GenericError(
        override val headers: Map<String, String>,
        override val errorDetails: ErrorDetails? = null
    ) : HttpError403(headers, errorDetails)

    data class NoMailbox(
        override val headers: Map<String, String>,
        override val errorDetails: ErrorDetails?
    ) : HttpError403(headers, errorDetails)

    data class QuotaExceeded(
        override val headers: Map<String, String>,
        override val errorDetails: ErrorDetails? = null
    ) : HttpError403(headers, errorDetails)

    data class UnknownError(
        override val headers: Map<String, String>,
        override val errorDetails: ErrorDetails? = null
    ) : HttpError403(headers, errorDetails)
}

private const val HTTP_404_ERROR = 404

sealed class HttpError404(
    override val headers: Map<String, String>,
    override val statusCode: Int = HTTP_404_ERROR
) : HttpError() {

    data class Http404RestApiNotFound(
        override val headers: Map<String, String>,
        override val errorDetails: ErrorDetails? = null
    ) : HttpError404(headers)

    data class UnknownHttp404Error(
        override val headers: Map<String, String>,
        override val errorDetails: ErrorDetails? = null
    ) : HttpError404(headers)
}

private const val HTTP_409_ERROR = 409

data class HttpError409(
    override val headers: Map<String, String>,
    override val errorDetails: ErrorDetails? = null
) : HttpError() {
    override val statusCode: Int = HTTP_409_ERROR
}

private const val HTTP_410_ERROR = 410

sealed class HttpError410(
    override val headers: Map<String, String>,
    override val statusCode: Int = HTTP_410_ERROR
) : HttpError() {

    data class InvalidSyncToken(
        override val headers: Map<String, String>,
        override val errorDetails: ErrorDetails? = null
    ) : HttpError410(headers)

    data class InvalidateClientCache(
        override val headers: Map<String, String>,
        override val errorDetails: ErrorDetails? = null
    ) : HttpError410(headers)

    data class UnknownHttpError410(
        override val headers: Map<String, String>,
        override val errorDetails: ErrorDetails? = null
    ) : HttpError410(headers)
}

private const val HTTP_413_ERROR = 413

data class HttpError413(
    override val headers: Map<String, String>,
    override val errorDetails: ErrorDetails? = null
) : HttpError() {
    override val statusCode: Int = HTTP_413_ERROR
}

private const val HTTP_426_ERROR = 426

data class HttpError426(
    override val headers: Map<String, String>,
    override val errorDetails: ErrorDetails? = null
) : HttpError() {
    override val statusCode: Int = HTTP_426_ERROR
}

private const val HTTP_429_ERROR = 429

data class HttpError429(
    override val headers: Map<String, String>,
    override val errorDetails: ErrorDetails? = null
) : HttpError() {
    override val statusCode: Int = HTTP_429_ERROR
}

private const val HTTP_500_ERROR = 500

data class HttpError500(
    override val headers: Map<String, String>,
    override val errorDetails: ErrorDetails? = null
) : HttpError() {
    override val statusCode: Int = HTTP_500_ERROR
}

private const val HTTP_503_ERROR = 503

data class HttpError503(
    override val headers: Map<String, String>,
    override val errorDetails: ErrorDetails? = null
) : HttpError() {
    override val statusCode: Int = HTTP_503_ERROR
}

data class UnknownHttpError(
    override val headers: Map<String, String>,
    override val statusCode: Int,
    override val errorDetails: ErrorDetails? = null
) : HttpError()

private val moshi: Moshi by lazy {
    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
}

private val errorDetailsJsonAdapter: JsonAdapter<ErrorDetails> by lazy {
    moshi.adapter<ErrorDetails>(ErrorDetails::class.java)
}

private fun parseJsonErrorMessage(body: String, notesLogger: NotesLogger?): ErrorDetails? =
    try {
        tryParseNotesClientErrorMessage(body, notesLogger) ?: tryParseAutoDiscoverErrorMessage(body, notesLogger)
    } catch (ioe: IOException) {
        notesLogger?.e(message = "IOException while parsing json error message")
        notesLogger?.d(message = "IOException \nException info: $ioe")
        null
    }

private fun tryParseNotesClientErrorMessage(body: String, notesLogger: NotesLogger?): ErrorDetails? {
    return try {
        errorDetailsJsonAdapter.fromJson(body)
    } catch (jde: JsonDataException) {
        notesLogger?.e(message = "JsonDataException while parsing json error message as NotesClient error")
        notesLogger?.d(message = "JsonDataException \nException info: $jde")
        null
    }
}

private fun tryParseAutoDiscoverErrorMessage(body: String, notesLogger: NotesLogger?): ErrorDetails? {
    return try {
        val error = autoDiscoverErrorDetailsJsonAdapter.fromJson(body)
        if (error != null) {
            ErrorDetails(Error(code = error.ErrorCode, message = error.ErrorMessage))
        } else {
            null
        }
    } catch (jde: JsonDataException) {
        notesLogger?.e(message = "JsonDataException while parsing json error message as AutoDiscover error")
        notesLogger?.d(message = "JsonDataException \nException info: $jde")
        null
    }
}

private val autoDiscoverErrorDetailsJsonAdapter: JsonAdapter<AutoDiscoverErrorDetails> by lazy {
    moshi.adapter<AutoDiscoverErrorDetails>(AutoDiscoverErrorDetails::class.java)
}

internal fun toHttpError(
    statusCode: Int,
    body: String,
    headers: Map<String, String>,
    notesLogger: NotesLogger? = null
): HttpError {
    val errorDetails = parseJsonErrorMessage(body, notesLogger)
    return when (statusCode) {
        HTTP_400_ERROR -> handle400Error(headers, errorDetails)
        HTTP_401_ERROR -> handle401Error(headers, errorDetails)
        HTTP_403_ERROR -> handle403Error(headers, errorDetails)
        HTTP_404_ERROR -> handle404Error(headers, errorDetails)
        HTTP_409_ERROR -> handle409Error(headers, errorDetails)
        HTTP_410_ERROR -> handle410Error(headers, errorDetails)
        HTTP_413_ERROR -> handle413Error(headers, errorDetails)
        HTTP_426_ERROR -> handle426Error(headers, errorDetails)
        HTTP_429_ERROR -> handle429Error(headers, errorDetails)
        HTTP_500_ERROR -> handle500Error(headers, errorDetails)
        HTTP_503_ERROR -> handle503Error(headers, errorDetails)
        else -> UnknownHttpError(headers, statusCode, errorDetails)
    }
}

private fun handle400Error(headers: Map<String, String>, errorDetails: ErrorDetails?): HttpError400 =
    HttpError400(headers, errorDetails)

private fun handle401Error(headers: Map<String, String>, errorDetails: ErrorDetails?): HttpError401 =
    HttpError401(headers, errorDetails)

private fun handle403Error(headers: Map<String, String>, errorDetails: ErrorDetails?): HttpError403 {
    return when (errorDetails?.error?.code?.toLowerCase()) {
        "NoExchangeMailbox".toLowerCase() -> HttpError403.NoMailbox(headers, errorDetails)
        "QuotaExceeded".toLowerCase() -> HttpError403.QuotaExceeded(headers, errorDetails)
        "GenericError".toLowerCase() -> HttpError403.GenericError(headers, errorDetails)
        else -> HttpError403.UnknownError(headers, errorDetails)
    }
}

private fun handle404Error(headers: Map<String, String>, errorDetails: ErrorDetails?): HttpError404 {
    return when (errorDetails?.error?.code?.toLowerCase()) {
        "RestApiNotFound".toLowerCase() -> HttpError404.Http404RestApiNotFound(
            headers,
            errorDetails
        )
        else -> HttpError404.UnknownHttp404Error(headers, errorDetails)
    }
}

private fun handle409Error(headers: Map<String, String>, errorDetails: ErrorDetails?): HttpError409 =
    HttpError409(headers, errorDetails)

private fun handle410Error(headers: Map<String, String>, errorDetails: ErrorDetails?): HttpError410 {
    return when (errorDetails?.error?.code?.toLowerCase()) {
        "InvalidSyncToken".toLowerCase() -> HttpError410.InvalidSyncToken(
            headers,
            errorDetails
        )
        "InvalidClientCache".toLowerCase() -> HttpError410.InvalidateClientCache(
            headers,
            errorDetails
        )
        else -> HttpError410.UnknownHttpError410(headers, errorDetails)
    }
}

private fun handle413Error(headers: Map<String, String>, errorDetails: ErrorDetails?): HttpError413 =
    HttpError413(headers, errorDetails)

private fun handle426Error(headers: Map<String, String>, errorDetails: ErrorDetails?): HttpError426 =
    HttpError426(headers, errorDetails)

private fun handle429Error(headers: Map<String, String>, errorDetails: ErrorDetails?): HttpError429 =
    HttpError429(headers, errorDetails)

private fun handle500Error(headers: Map<String, String>, errorDetails: ErrorDetails?): HttpError500 =
    HttpError500(headers, errorDetails)

private fun handle503Error(headers: Map<String, String>, errorDetails: ErrorDetails?): HttpError503 =
    HttpError503(headers, errorDetails)
