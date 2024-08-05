package com.microsoft.notes.sync.outboundqueue

import com.microsoft.notes.sync.ApiResult
import com.microsoft.notes.sync.HttpError401
import com.microsoft.notes.sync.HttpError404
import com.microsoft.notes.sync.HttpError409
import com.microsoft.notes.sync.HttpError500
import com.microsoft.notes.sync.HttpError503
import com.microsoft.notes.sync.SyncRequestTelemetry
import com.microsoft.notes.sync.SyncRequestTelemetry.SyncRequestType
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.ExpirationDate
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.SamplingPolicy
import com.microsoft.notes.utils.logging.SeverityLevel
import com.microsoft.notes.utils.logging.SyncActionType
import com.microsoft.notes.utils.logging.TelemetryData
import com.microsoft.notes.utils.logging.TelemetryLogger
import com.microsoft.notes.utils.logging.createTelemetryData
import okio.BufferedSource
import org.junit.Assert
import org.junit.Test

class SyncTelemetryTests {
    private fun requestTypeContext(type: String) = Pair("Operation", type)
    private fun errorTypeContext(errorType: String) = Pair("ErrorType", errorType)
    private fun errorValueContext(value: String) = Pair("ErrorValue", value)
    private fun retryContext(retry: Boolean) = Pair("Retry", retry.toString())
    private fun newOperationContext(type: String) = Pair("NewOperation", type)
    private fun httpStatusContext(code: String) = Pair("HttpStatus", code)
    private fun syncRequestAction(actionType: String) = Pair("Action", actionType)

    private val operationContext = Pair("ServiceCorrelationVector", "abcde.1")
    private val noteIdContext = Pair("NoteLocalId", "notes_123")
    private val blockIdContext = Pair("block_id", "block_123")

    private var referenceTelemetryData: TelemetryData? = null

    private val notesLogger = NotesLogger(
        telemetryLogger = object : TelemetryLogger {
            private fun testEvent(telemetryData: TelemetryData) {
                val result = (
                    telemetryData.eventHeaders == referenceTelemetryData?.eventHeaders &&
                        telemetryData.eventProperties == referenceTelemetryData?.eventProperties
                    )

                if (telemetryData.eventProperties.containsKey("isSyncScore")) {
                    Assert.assertEquals(
                        SamplingPolicy.Critical,
                        telemetryData.eventHeaders.samplingPolicy
                    )
                    Assert.assertEquals(
                        ExpirationDate.Perpetual,
                        telemetryData.eventHeaders.expirationDate
                    )
                }

                Assert.assertTrue(result)
            }

            override fun logEvent(telemetryData: TelemetryData) {
                testEvent(telemetryData)
            }

            override fun logEventSyncScore(telemetryData: TelemetryData) {
                testEvent(telemetryData)
            }
        }
    )

    @Test
    fun should_emit_success_telemetry_marker() {
        val eventMarker = EventMarkers.SyncRequestStarted
        val requestType = SyncRequestType.UploadMedia

        referenceTelemetryData = createTelemetryData(
            eventName = EventMarkers.SyncRequestAction,
            keyValuePairs = *arrayOf(
                requestTypeContext("UploadMedia"),
                operationContext,
                syncRequestAction(SyncActionType.START)
            ),
            severityLevel = SeverityLevel.Info,
            isSyncScore = false
        )

        val testTelemetryBundle = SyncRequestTelemetry(
            requestType = requestType,
            noteId = noteIdContext.second,
            metaData = mutableListOf(blockIdContext),
            filterOutEventMarker = { false }
        )

        testTelemetryBundle.setRequestId(operationContext.second).recordTelemetry(notesLogger, eventMarker)
    }

    @Test
    fun should_emit_completed_telemetry_marker() {
        val marker = EventMarkers.SyncRequestCompleted
        val requestType = SyncRequestType.UploadMedia

        referenceTelemetryData = createTelemetryData(
            eventName = EventMarkers.SyncRequestAction,
            keyValuePairs = *arrayOf(
                requestTypeContext("UploadMedia"),
                operationContext,
                syncRequestAction(SyncActionType.COMPLETE)
            ),
            severityLevel = SeverityLevel.Info,
            isSyncScore = false
        )

        val testTelemetryBundle = SyncRequestTelemetry(
            requestType = requestType,
            noteId = noteIdContext.second,
            metaData = mutableListOf(blockIdContext),
            filterOutEventMarker = { false }
        )

        testTelemetryBundle.setRequestId(operationContext.second)
            .recordTelemetry(notesLogger, marker)
    }

    @Test
    fun should_emit_failed_triggered_event_with_retry() {
        val marker = EventMarkers.SyncRequestFailed
        val requestType = SyncRequestType.UploadMedia
        val apiResult = ApiResult.Failure<BufferedSource>(error = HttpError401(emptyMap()))
        val shouldRetry = true

        referenceTelemetryData = createTelemetryData(
            eventName = EventMarkers.SyncRequestAction,
            keyValuePairs = *arrayOf(
                requestTypeContext("UploadMedia"),
                operationContext,
                errorTypeContext("HttpError"),
                errorValueContext("401"),
                retryContext(true),
                httpStatusContext("401"),
                syncRequestAction(SyncActionType.FAIL)
            ),
            severityLevel = SeverityLevel.Error,
            isSyncScore = false
        )

        val testTelemetryBundle = SyncRequestTelemetry(
            requestType = requestType,
            noteId = noteIdContext.second,
            metaData = mutableListOf(blockIdContext),
            filterOutEventMarker = { false }
        )

        testTelemetryBundle.setStatus(apiResult, false)
            .setRequestId(operationContext.second)
            .wasRetryOperation(shouldRetry)
            .recordTelemetry(notesLogger, marker)
    }

    @Test
    fun should_emit_failed_triggered_event_with_no_retry() {
        val marker = EventMarkers.SyncRequestFailed
        val requestType = SyncRequestType.UpdateNote
        val apiResult = ApiResult.Failure<BufferedSource>(error = HttpError409(emptyMap()))
        val shouldRetry = false
        val newOperation = SyncRequestType.GetNoteForMerge

        val testTelemetryBundle = SyncRequestTelemetry(
            requestType = requestType,
            filterOutEventMarker = { false }
        )

        referenceTelemetryData = createTelemetryData(
            eventName = EventMarkers.SyncRequestAction,
            keyValuePairs = *arrayOf(
                requestTypeContext("UpdateNote"),
                operationContext,
                errorValueContext("409"),
                errorTypeContext("HttpError"),
                retryContext(false),
                newOperationContext("GetNoteForMerge"),
                httpStatusContext("409"),
                syncRequestAction(SyncActionType.FAIL)
            ),
            severityLevel = SeverityLevel.Error,
            isSyncScore = false
        )

        testTelemetryBundle.setStatus(apiResult, false)
            .setRequestId(operationContext.second)
            .wasRetryOperation(shouldRetry)
            .newRequestOnFailure(newOperation)
            .recordTelemetry(notesLogger, marker)
    }

    @Test
    fun should_emit_failed_triggered_event_with_sync_score() {
        val marker = EventMarkers.SyncRequestFailed
        val requestType = SyncRequestType.UpdateNote
        val apiResult = ApiResult.Failure<BufferedSource>(error = HttpError500(emptyMap()))
        val shouldRetry = false

        val testTelemetryBundle = SyncRequestTelemetry(
            requestType = requestType,
            filterOutEventMarker = { false }
        )

        referenceTelemetryData = createTelemetryData(
            eventName = EventMarkers.SyncRequestAction,
            keyValuePairs = *arrayOf(
                requestTypeContext("UpdateNote"),
                operationContext,
                errorValueContext("500"),
                errorTypeContext("HttpError"),
                httpStatusContext("500"),
                retryContext(false),
                syncRequestAction(SyncActionType.FAIL)
            ),
            severityLevel = SeverityLevel.Error,
            isSyncScore = true
        )

        testTelemetryBundle.setStatus(apiResult)
            .setRequestId(operationContext.second)
            .wasRetryOperation(shouldRetry)
            .recordTelemetry(notesLogger, marker)
    }

    @Test
    fun should_emit_failed_triggered_event_with_sync_score_for_non_restapifound_errors() {
        val marker = EventMarkers.SyncRequestFailed
        val requestType = SyncRequestType.UpdateNote
        val apiResult = ApiResult.Failure<BufferedSource>(error = HttpError404.UnknownHttp404Error(emptyMap()))
        val shouldRetry = false

        val testTelemetryBundle = SyncRequestTelemetry(
            requestType = requestType,
            filterOutEventMarker = { false }
        )

        referenceTelemetryData = createTelemetryData(
            eventName = EventMarkers.SyncRequestAction,
            keyValuePairs = *arrayOf(
                requestTypeContext("UpdateNote"),
                operationContext,
                errorValueContext("404"),
                errorTypeContext("HttpError"),
                httpStatusContext("404"),
                retryContext(false),
                syncRequestAction(SyncActionType.FAIL)
            ),
            severityLevel = SeverityLevel.Error,
            isSyncScore = true
        )

        testTelemetryBundle.setStatus(apiResult)
            .setRequestId(operationContext.second)
            .wasRetryOperation(shouldRetry)
            .recordTelemetry(notesLogger, marker)
    }

    @Test
    fun should_emit_failed_triggered_event_without_sync_score() {
        val marker = EventMarkers.SyncRequestFailed
        val requestType = SyncRequestType.UpdateNote
        val apiResult = ApiResult.Failure<BufferedSource>(error = HttpError404.Http404RestApiNotFound(emptyMap()))
        val shouldRetry = false

        val testTelemetryBundle = SyncRequestTelemetry(
            requestType = requestType,
            filterOutEventMarker = { false }
        )

        referenceTelemetryData = createTelemetryData(
            eventName = EventMarkers.SyncRequestAction,
            keyValuePairs = *arrayOf(
                requestTypeContext("UpdateNote"),
                operationContext,
                errorValueContext("404"),
                errorTypeContext("HttpError"),
                httpStatusContext("404"),
                retryContext(false),
                syncRequestAction(SyncActionType.FAIL)
            ),
            severityLevel = SeverityLevel.Error,
            isSyncScore = false
        )

        testTelemetryBundle.setStatus(apiResult, false)
            .setRequestId(operationContext.second)
            .wasRetryOperation(shouldRetry)
            .recordTelemetry(notesLogger, marker)
    }

    @Test
    fun should_not_log_first_503_to_sync_score() {
        val marker = EventMarkers.SyncRequestFailed
        val requestType = SyncRequestType.DeltaSync
        val apiResult = ApiResult.Failure<BufferedSource>(error = HttpError503(emptyMap()))
        val shouldRetry = true

        val firstTelemetryBundle = SyncRequestTelemetry(
            requestType = requestType,
            filterOutEventMarker = { false }
        )

        referenceTelemetryData = createTelemetryData(
            eventName = EventMarkers.SyncRequestAction,
            keyValuePairs = *arrayOf(
                requestTypeContext("DeltaSync"),
                operationContext,
                errorValueContext("503"),
                errorTypeContext("HttpError"),
                httpStatusContext("503"),
                retryContext(true),
                syncRequestAction(SyncActionType.FAIL)
            ),
            severityLevel = SeverityLevel.Error,
            isSyncScore = false
        )

        // Assert the first 503 was not logged to sync score
        firstTelemetryBundle.setStatus(apiResult, false)
            .setRequestId(operationContext.second)
            .wasRetryOperation(shouldRetry)
            .recordTelemetry(notesLogger, marker)

        val secondTelemetryBundle = SyncRequestTelemetry(
            requestType = requestType,
            filterOutEventMarker = { false }
        )

        referenceTelemetryData = createTelemetryData(
            eventName = EventMarkers.SyncRequestAction,
            keyValuePairs = *arrayOf(
                requestTypeContext("DeltaSync"),
                operationContext,
                errorValueContext("503"),
                errorTypeContext("HttpError"),
                httpStatusContext("503"),
                retryContext(true),
                syncRequestAction(SyncActionType.FAIL)
            ),
            severityLevel = SeverityLevel.Error,
            isSyncScore = true
        )

        // Assert the second 503 was logged to sync score
        secondTelemetryBundle.setStatus(apiResult, true)
            .setRequestId(operationContext.second)
            .wasRetryOperation(shouldRetry)
            .recordTelemetry(notesLogger, marker)
    }
}
