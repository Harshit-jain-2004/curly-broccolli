package com.microsoft.notes.sync.apiresulthandler

import com.microsoft.notes.sync.ApiError.FatalError
import com.microsoft.notes.sync.ApiError.NetworkError
import com.microsoft.notes.sync.ApiRequestOperation
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.CreateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.SamsungNotesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateNote
import com.microsoft.notes.sync.ApiResponseEvent
import com.microsoft.notes.sync.ApiResponseEvent.ForbiddenError
import com.microsoft.notes.sync.ApiResponseEvent.Gone
import com.microsoft.notes.sync.ApiResponseEvent.InvalidateClientCache
import com.microsoft.notes.sync.ApiResponseEvent.InvalidateSamsungNotesClientCache
import com.microsoft.notes.sync.ApiResponseEvent.NotAuthorized
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncError.SyncErrorType
import com.microsoft.notes.sync.ApiResponseEvent.UpgradeRequired
import com.microsoft.notes.sync.ApiResult
import com.microsoft.notes.sync.ApiResultHandler
import com.microsoft.notes.sync.HttpError400
import com.microsoft.notes.sync.HttpError401
import com.microsoft.notes.sync.HttpError403
import com.microsoft.notes.sync.HttpError404
import com.microsoft.notes.sync.HttpError409
import com.microsoft.notes.sync.HttpError410
import com.microsoft.notes.sync.HttpError426
import com.microsoft.notes.sync.HttpError429
import com.microsoft.notes.sync.HttpError503
import com.microsoft.notes.sync.QueueInstruction
import com.microsoft.notes.sync.QueueInstruction.BroadcastEvent
import com.microsoft.notes.sync.QueueInstruction.BroadcastSyncErrorEvent
import com.microsoft.notes.sync.QueueInstruction.DelayQueue
import com.microsoft.notes.sync.QueueInstruction.LogTelemetry
import com.microsoft.notes.sync.QueueInstruction.PauseQueue
import com.microsoft.notes.sync.QueueInstruction.RemoveOperation
import com.microsoft.notes.sync.QueueInstruction.ReplaceOperation
import com.microsoft.notes.sync.QueueInstruction.ResetQueue
import com.microsoft.notes.sync.QueueInstruction.SetDelay
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.sync.note
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ApiResultHandlerErrorsTest {
    companion object {
        private const val LOCAL_ID = "LOCAL_ID"
        private const val REMOTE_ID = "REMOTE_ID"
    }

    private val note = note(LOCAL_ID, REMOTE_ID)

    @Test
    fun `should handle FatalError`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(FatalError("FatalError"))
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(3))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is RemoveOperation }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
    }

    @Test
    fun `should handle NetworkError`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(NetworkError(Exception("NetworkError")))
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(4))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastSyncErrorEvent }, iz(notNullValue()))

        val broadcastSyncError = instructions.find { it is BroadcastSyncErrorEvent } as BroadcastSyncErrorEvent
        assertThat(broadcastSyncError.syncErrorType, iz(SyncErrorType.NetworkUnavailable))

        val setDelayInstruction: SetDelay = instructions.find { it is SetDelay } as SetDelay
        assertThat(setDelayInstruction.delta is SetDelay.DelayDelta.Exponential, iz(notNullValue()))
        val setDeltaExponentialInstruction: SetDelay.DelayDelta.Exponential = setDelayInstruction.delta as SetDelay.DelayDelta.Exponential
        assert(setDeltaExponentialInstruction.factor == 2L)
    }

    @Test
    fun `should handle HttpError400`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError400(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(2))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is RemoveOperation }, iz(notNullValue()))
    }

    @Test
    fun `should handle HttpError401`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError401(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(4))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is PauseQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastEvent }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastSyncErrorEvent }, iz(notNullValue()))

        val broadcastEvent = instructions.find { it is BroadcastEvent } as BroadcastEvent
        assertThat(broadcastEvent.event, iz(instanceOf(NotAuthorized::class.java)))

        val broadcastSyncError = instructions.find { it is BroadcastSyncErrorEvent } as BroadcastSyncErrorEvent
        assertThat(broadcastSyncError.syncErrorType, iz(SyncErrorType.Unauthenticated))
    }

    @Test
    fun `should handle HttpError403 known error`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError403.NoMailbox(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(4))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is PauseQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastEvent }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastSyncErrorEvent }, iz(notNullValue()))

        val instruction = instructions.find { it is BroadcastEvent } as BroadcastEvent
        assertThat(instruction.event, iz(instanceOf(ForbiddenError::class.java)))
    }

    @Test
    fun `should handle HttpError403 UnknownError`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError403.UnknownError(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(4))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastSyncErrorEvent }, iz(notNullValue()))

        val broadcastSyncError = instructions.find { it is BroadcastSyncErrorEvent } as BroadcastSyncErrorEvent
        assertThat(broadcastSyncError.syncErrorType, iz(SyncErrorType.SyncFailure))
    }

    @Test
    fun `should handle HttpError404`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError404.Http404RestApiNotFound(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(5))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastSyncErrorEvent }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastEvent }, iz(notNullValue()))

        val broadcastSyncError = instructions.find { it is BroadcastSyncErrorEvent } as BroadcastSyncErrorEvent
        assertThat(broadcastSyncError.syncErrorType, iz(SyncErrorType.SyncFailure))

        val broadcastEvent = instructions.find { it is BroadcastEvent } as BroadcastEvent
        assertThat(broadcastEvent.event, iz(instanceOf(Gone::class.java)))
    }

    @Test
    fun `should handle HttpError404 with ignore404s`() {
        val operation = UpdateNote(note = note, uiBaseRevision = 1)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError404.Http404RestApiNotFound(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(3))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is RemoveOperation }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastEvent }, iz(notNullValue()))

        val broadcastEvent = instructions.find { it is BroadcastEvent } as BroadcastEvent
        assertThat(broadcastEvent.event, iz(instanceOf(Gone::class.java)))
    }

    @Test
    fun `should handle HttpError409`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError409(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(4))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastSyncErrorEvent }, iz(notNullValue()))

        val broadcastSyncError = instructions.find { it is BroadcastSyncErrorEvent } as BroadcastSyncErrorEvent
        assertThat(broadcastSyncError.syncErrorType, iz(SyncErrorType.SyncFailure))
    }

    @Test
    fun `should handle HttpError409 with recoverWith`() {
        val operation = UpdateNote(note = note, uiBaseRevision = 1)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError409(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(2))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is ReplaceOperation }, iz(notNullValue()))
    }

    @Test
    fun `should handle HttpError410 InvalidSyncToken`() {
        val operation = ApiRequestOperation.ValidApiRequestOperation.Sync(deltaToken = Token.Delta("invalid"))
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError410.InvalidSyncToken(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(4))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is ReplaceOperation }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
    }

    @Test
    fun `should handle HttpError410 InvalidateClientCache`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError410.InvalidateClientCache(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(7))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is PauseQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is ResetQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastEvent }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastSyncErrorEvent }, iz(notNullValue()))

        val instruction = instructions.find { it is BroadcastEvent } as BroadcastEvent
        assertThat(instruction.event, iz(instanceOf(InvalidateClientCache::class.java)))

        val broadcastSyncError = instructions.find { it is BroadcastSyncErrorEvent } as BroadcastSyncErrorEvent
        assertThat(broadcastSyncError.syncErrorType, iz(SyncErrorType.SyncPaused))

        val setDelayInstruction: SetDelay = instructions.find { it is SetDelay } as SetDelay
        assertThat(setDelayInstruction.delta is SetDelay.DelayDelta.Exponential, iz(notNullValue()))
        val setDeltaExponentialInstruction: SetDelay.DelayDelta.Exponential = setDelayInstruction.delta as SetDelay.DelayDelta.Exponential
        assert(setDeltaExponentialInstruction.factor == 2L)
    }

    @Test
    fun `should handle HttpError410 InvalidateSamsungNotesClientCache`() {
        val operation = SamsungNotesSync(null)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError410.InvalidateClientCache(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(7))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is PauseQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is ResetQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastEvent }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastSyncErrorEvent }, iz(notNullValue()))

        val instruction = instructions.find { it is BroadcastEvent } as BroadcastEvent
        assertThat(instruction.event, iz(instanceOf(InvalidateSamsungNotesClientCache::class.java)))

        val broadcastSyncError = instructions.find { it is BroadcastSyncErrorEvent } as BroadcastSyncErrorEvent
        assertThat(broadcastSyncError.syncErrorType, iz(SyncErrorType.SyncPaused))

        val setDelayInstruction: SetDelay = instructions.find { it is SetDelay } as SetDelay
        assertThat(setDelayInstruction.delta is SetDelay.DelayDelta.Exponential, iz(notNullValue()))
        val setDeltaExponentialInstruction: SetDelay.DelayDelta.Exponential = setDelayInstruction.delta as SetDelay.DelayDelta.Exponential
        assert(setDeltaExponentialInstruction.factor == 2L)
    }

    @Test
    fun `should handle HttpError410 UnknownHttpError410 on CreateNote`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError410.UnknownHttpError410(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(3))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
    }

    @Test
    fun `should handle HttpError410 UnknownHttpError410 on Full Sync`() {
        val operation = ApiRequestOperation.ValidApiRequestOperation.Sync(deltaToken = null)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError410.UnknownHttpError410(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(4))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is ReplaceOperation }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
    }

    @Test
    fun `should handle HttpError410 UnknownHttpError410 on Delta Sync`() {
        val operation = ApiRequestOperation.ValidApiRequestOperation.Sync(deltaToken = Token.Delta("foo"))
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError410.UnknownHttpError410(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(4))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is ReplaceOperation }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
    }

    @Test
    fun `should handle HttpError426`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError426(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(4))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is PauseQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastEvent }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastSyncErrorEvent }, iz(notNullValue()))

        val instruction = instructions.find { it is BroadcastEvent } as BroadcastEvent
        assertThat(instruction.event, iz(instanceOf(UpgradeRequired::class.java)))

        val broadcastSyncError = instructions.find { it is BroadcastSyncErrorEvent } as BroadcastSyncErrorEvent
        assertThat(broadcastSyncError.syncErrorType, iz(SyncErrorType.SyncPaused))
    }

    @Test
    fun `should handle HttpError429`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError429(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(3))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
    }

    @Test
    fun `should handle HttpError503`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Failure<ApiResponseEvent>(
            HttpError503(headers = mapOf(), errorDetails = null)
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(3))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))
        assertThat(instructions.find { it is DelayQueue }, iz(notNullValue()))
    }

    private fun getInstructions(
        operation: ApiRequestOperation.ValidApiRequestOperation,
        result: ApiResult<ApiResponseEvent>
    ): List<QueueInstruction> {
        val handler = ApiResultHandler(notesLogger = null, isDebugMode = false)
        operation.requestId = ""
        operation.realTimeSessionId = ""
        return handler.handleResult(result = result, operation = operation)
    }
}
