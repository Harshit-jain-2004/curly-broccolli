package com.microsoft.notes.sync

import com.microsoft.notes.sync.ApiError.Exception
import com.microsoft.notes.sync.ApiError.FatalError
import com.microsoft.notes.sync.ApiError.InvalidJSONError
import com.microsoft.notes.sync.ApiError.NetworkError
import com.microsoft.notes.sync.ApiError.NonJSONError
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.RequestProperty
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.SyncProperty
import com.microsoft.notes.utils.logging.SeverityLevel
import com.microsoft.notes.utils.logging.SyncActionType
import java.net.UnknownHostException

data class SyncRequestTelemetry(
    val requestType: SyncRequestType,
    val noteId: String = "",
    var metaData: MutableList<Pair<String, String>> = mutableListOf(),
    val filterOutEventMarker: (EventMarkers) -> Boolean = { eventMarker ->
        eventMarker != EventMarkers.SyncRequestFailed
    }
) {
    companion object {
        private const val NO_ERROR_VALUE = ""
    }
    private var responseError: ApiError? = null
    private var severityLevel: SeverityLevel = SeverityLevel.Info
    private var requestStatus: SyncRequestStatus? = null
    private var errorValue: String = ""
    private var modifySyncScore = false
    private var isRetryOperation = false
    private var newRequestOnFailure: SyncRequestType? = null
    private var cvRequestId: String? = null

    enum class SyncRequestStatus {
        Success,
        NetworkError,
        HttpError,
        NonJSONError,
        InvalidJSONError,
        FatalError,
        Exception;
    }

    enum class SyncRequestType {
        FullSync,
        DeltaSync,
        NoteReferencesFullSync,
        NoteReferencesDeltaSync,
        SamsungNotesFullSync,
        SamsungNotesDeltaSync,
        MeetingNotesFullSync,
        CreateNote,
        UpdateNote,
        DeleteNote,
        DeleteNoteReference,
        DeleteSamsungNote,
        GetNoteForMerge,
        UpdateMedia,
        UploadMedia,
        DownloadMedia,
        DeleteMedia,
        UpdateMediaAltText,
        InvalidUpdateNote,
        InvalidUploadMedia,
        InvalidDeleteNote,
        InvalidDeleteMedia,
        InvalidUpdateMediaAltText;

        companion object {
            fun requestType(operation: ApiRequestOperation): SyncRequestType {
                return when (operation) {
                    is ApiRequestOperation.ValidApiRequestOperation.Sync -> {
                        if (operation.deltaToken == null) FullSync else DeltaSync
                    }
                    is ApiRequestOperation.ValidApiRequestOperation.CreateNote -> CreateNote
                    is ApiRequestOperation.ValidApiRequestOperation.UpdateNote -> UpdateNote
                    is ApiRequestOperation.ValidApiRequestOperation.DeleteNote -> DeleteNote
                    is ApiRequestOperation.ValidApiRequestOperation.DeleteNoteReference -> DeleteNoteReference
                    is ApiRequestOperation.ValidApiRequestOperation.DeleteSamsungNote -> DeleteSamsungNote
                    is ApiRequestOperation.ValidApiRequestOperation.GetNoteForMerge -> GetNoteForMerge
                    is ApiRequestOperation.ValidApiRequestOperation.NoteReferencesSync -> {
                        if (operation.deltaToken == null) NoteReferencesFullSync else NoteReferencesDeltaSync
                    }
                    is ApiRequestOperation.ValidApiRequestOperation.MeetingNotesSync -> {
                        MeetingNotesFullSync
                    }
                    is ApiRequestOperation.ValidApiRequestOperation.SamsungNotesSync -> {
                        if (operation.deltaToken == null) SamsungNotesFullSync else SamsungNotesDeltaSync
                    }
                    is ApiRequestOperation.ValidApiRequestOperation.UploadMedia -> UploadMedia
                    is ApiRequestOperation.ValidApiRequestOperation.DownloadMedia -> DownloadMedia
                    is ApiRequestOperation.ValidApiRequestOperation.DeleteMedia -> DeleteMedia
                    is ApiRequestOperation.ValidApiRequestOperation.UpdateMediaAltText -> UpdateMediaAltText
                    is ApiRequestOperation.InvalidApiRequestOperation.InvalidUpdateNote -> InvalidUpdateNote
                    is ApiRequestOperation.InvalidApiRequestOperation.InvalidUploadMedia -> InvalidUpdateNote
                    is ApiRequestOperation.InvalidApiRequestOperation.InvalidDeleteNote -> InvalidDeleteNote
                    is ApiRequestOperation.InvalidApiRequestOperation.InvalidDeleteMedia -> InvalidDeleteMedia
                    is ApiRequestOperation.InvalidApiRequestOperation.InvalidUpdateMediaAltText -> InvalidUpdateMediaAltText
                }
            }
        }
    }

    private fun <T> requestStatus(result: ApiResult<T>): SyncRequestStatus {
        return when (result) {
            is ApiResult.Success -> SyncRequestStatus.Success
            is ApiResult.Failure -> {
                when (result.error) {
                    is NetworkError -> SyncRequestStatus.NetworkError
                    is HttpError -> SyncRequestStatus.HttpError
                    is NonJSONError -> SyncRequestStatus.NonJSONError
                    is InvalidJSONError -> SyncRequestStatus.InvalidJSONError
                    is FatalError -> SyncRequestStatus.FatalError
                    is Exception -> SyncRequestStatus.Exception
                }
            }
        }
    }

    private fun extractErrorValue(error: ApiError?): String {
        return when (error) {
            is NetworkError -> error.error.javaClass.name
            is HttpError -> {
                error.statusCode.toString()
            }
            is NonJSONError -> error.error
            is FatalError -> error.message
            else -> NO_ERROR_VALUE
        }
    }

    private fun isBlocked(eventMarker: EventMarkers): Boolean {
        val error = responseError
        return when (eventMarker) {
            EventMarkers.SyncRequestFailed -> {
                when (error) {
                    is NetworkError -> error.error is UnknownHostException
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun getHttpErrorContext(): List<Pair<String, String>> {
        val httpContext = mutableListOf<Pair<String, String>>()
        val httpError = responseError as? HttpError
        httpError?.let { error ->
            httpContext.add(Pair(RequestProperty.HTTP_STATUS, errorValue))

            val xCalculatedBETarget = error.headers[HeaderKeys.X_CALCULATED_BE_TARGET]
            xCalculatedBETarget?.let {
                httpContext.add(Pair(SyncProperty.SERVICE_X_CALCULATED_BE_TARGET, xCalculatedBETarget))
            }

            val requestId = error.headers[HeaderKeys.REQUEST_ID]
            requestId?.let {
                httpContext.add(Pair(SyncProperty.SERVICE_REQUEST_ID, requestId))
            }
        }

        return httpContext
    }

    private fun getCommonSyncRequestContext(eventMarker: EventMarkers): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        list.add(Pair(SyncProperty.OPERATION_TYPE, requestType.name))
        cvRequestId?.let {
            list.add(Pair(SyncProperty.SERVICE_CORRELATION_VECTOR, it))
        }
        when (eventMarker) {
            EventMarkers.SyncRequestStarted -> list.add(Pair(NotesSDKTelemetryKeys.NoteProperty.ACTION, SyncActionType.START))
            EventMarkers.SyncRequestCompleted -> list.add(Pair(NotesSDKTelemetryKeys.NoteProperty.ACTION, SyncActionType.COMPLETE))
            EventMarkers.SyncRequestFailed -> list.add(Pair(NotesSDKTelemetryKeys.NoteProperty.ACTION, SyncActionType.FAIL))
        }

        return list
    }

    private fun getSyncRequestedFailedContext(eventMarker: EventMarkers): List<Pair<String, String>> {
        val context = getCommonSyncRequestContext(eventMarker).toMutableList()

        context.add(Pair(SyncProperty.IS_RETRIED, isRetryOperation.toString()))

        newRequestOnFailure?.let {
            context.add(Pair(SyncProperty.NEW_OPERATION, it.name))
        }

        requestStatus?.let { status ->
            if (status != SyncRequestStatus.Success) {
                context.add(Pair(SyncProperty.ERROR_TYPE, status.name))

                if (errorValue.isNotBlank()) {
                    context.add(Pair(SyncProperty.ERROR_VALUE, errorValue))
                }

                if (status == SyncRequestStatus.HttpError) {
                    context.addAll(getHttpErrorContext())
                }
            }
        }

        return context
    }

    private fun toTelemetrySchema(eventMarker: EventMarkers): Array<Pair<String, String>> {
        return when (eventMarker) {
            EventMarkers.SyncRequestStarted -> getCommonSyncRequestContext(eventMarker)
            EventMarkers.SyncRequestCompleted -> getCommonSyncRequestContext(eventMarker)
            EventMarkers.SyncRequestFailed -> getSyncRequestedFailedContext(eventMarker)
            else -> emptyList()
        }.toTypedArray()
    }

    fun <T> setStatus(result: ApiResult.Failure<T>, shouldModifySyncScore: Boolean = true): SyncRequestTelemetry {
        severityLevel = SeverityLevel.Error
        modifySyncScore = shouldModifySyncScore
        responseError = result.error
        requestStatus = requestStatus(result)
        errorValue = extractErrorValue(responseError)
        return this
    }

    fun <T> setStatus(result: ApiResult.Success<T>): SyncRequestTelemetry {
        modifySyncScore = false
        responseError = null
        requestStatus = requestStatus(result)
        errorValue = extractErrorValue(responseError)
        return this
    }

    fun wasRetryOperation(retry: Boolean): SyncRequestTelemetry {
        isRetryOperation = retry
        return this
    }

    fun newRequestOnFailure(requestType: SyncRequestType): SyncRequestTelemetry {
        newRequestOnFailure = requestType
        return this
    }

    fun setRequestId(id: String): SyncRequestTelemetry {
        cvRequestId = id
        return this
    }

    fun reset() {
        responseError = null
        severityLevel = SeverityLevel.Info
        requestStatus = null
        errorValue = ""
        modifySyncScore = false
        isRetryOperation = false
        cvRequestId = null
        newRequestOnFailure = null
    }

    fun recordTelemetry(notesLogger: NotesLogger?, eventMarker: EventMarkers) {
        if (!(
            filterOutEventMarker(eventMarker) ||
                isBlocked(eventMarker)
            )
        ) {
            notesLogger?.recordTelemetry(
                eventMarker = EventMarkers.SyncRequestAction,
                keyValuePairs = *toTelemetrySchema(eventMarker),
                severityLevel = severityLevel,
                isSyncScore = modifySyncScore
            )
        }
    }
}
