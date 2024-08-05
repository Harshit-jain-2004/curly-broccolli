package com.microsoft.notes.sync.outboundqueue

import com.microsoft.notes.sync.ApiError
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.CreateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.SamsungNotesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.Sync
import com.microsoft.notes.sync.ApiResponseEvent.ForbiddenError
import com.microsoft.notes.sync.ApiResponseEvent.FullSync
import com.microsoft.notes.sync.ApiResponseEvent.InvalidateClientCache
import com.microsoft.notes.sync.ApiResponseEvent.InvalidateSamsungNotesClientCache
import com.microsoft.notes.sync.ApiResponseEvent.NotAuthorized
import com.microsoft.notes.sync.ApiResponseEvent.OutboundQueueSyncInactive
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncError
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncError.SyncErrorType
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncFailed
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncStarted
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncSucceeded
import com.microsoft.notes.sync.ApiResponseEvent.RemoteSamsungNotesSyncFailed
import com.microsoft.notes.sync.ApiResponseEvent.RemoteSamsungNotesSyncStarted
import com.microsoft.notes.sync.ApiResponseEvent.UpgradeRequired
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.HttpError400
import com.microsoft.notes.sync.HttpError401
import com.microsoft.notes.sync.HttpError403
import com.microsoft.notes.sync.HttpError410
import com.microsoft.notes.sync.HttpError426
import com.microsoft.notes.sync.HttpError500
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.sync.note
import org.junit.Test

class OutboundQueueSyncTest : FeedFRESetup() {
    private val syncOperation = Sync(deltaToken = null)
    private val createNoteOperation = CreateNote(note = note("localId"))
    private val samsungNoteSyncOperation = SamsungNotesSync(null)
    private val TEST_USER_ID = "test@outlook.com"

    @Test
    fun `should emit event on success`() {
        val operationHandler = MockOperationHandler(sync = createSyncEvent(syncOperation))

        val events = listOf(
            RemoteNotesSyncStarted(),
            FullSync(Token.Delta("someDeltaToken"), emptyList()),
            RemoteNotesSyncSucceeded()
        )

        testEventSequenceSuccess(syncOperation, operationHandler, events, context = mockContext)
    }

    @Test
    fun `should only emit informational events on failure`() {
        val operationHandler = MockOperationHandler(
            sync = createFailedEvent(syncOperation, HttpError500(emptyMap()))
        )

        val events = listOf(
            RemoteNotesSyncStarted(),
            RemoteNotesSyncError(SyncErrorType.SyncFailure),
            RemoteNotesSyncFailed()
        )

        testEventSequenceFailure(syncOperation, operationHandler, events, context = mockContext)
    }

    @Test
    fun `should emit sync error event on NetworkUnavailable failure`() {
        val operationHandler = MockOperationHandler(
            sync = createFailedEvent(syncOperation, ApiError.NetworkError(Exception()))
        )

        val events = listOf(
            RemoteNotesSyncStarted(),
            RemoteNotesSyncError(SyncErrorType.NetworkUnavailable),
            RemoteNotesSyncFailed()
        )

        testEventSequenceFailure(syncOperation, operationHandler, events, context = mockContext)
    }

    @Test
    fun `should emit sync error event on NetworkUnavailable failure in the queue`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(createNoteOperation, ApiError.NetworkError(Exception())),
            sync = createFailedEvent(syncOperation, ApiError.NetworkError(Exception()))
        )

        val events = listOf(
            RemoteNotesSyncError(SyncErrorType.NetworkUnavailable)
        )

        testEventSequenceFailure(listOf(createNoteOperation, syncOperation), operationHandler, events, context = mockContext)
    }

    @Test
    fun `should handle operation on 400 in production`() {
        val operationHandler = MockOperationHandler(
            sync = createFailedEvent(syncOperation, HttpError400(emptyMap()))
        )

        testIgnored(syncOperation, operationHandler, isDebugMode = false, context = mockContext)
    }

    @Test(expected = AssertionError::class)
    fun `should handle operation on 400 in development`() {
        val operationHandler = MockOperationHandler(
            sync = createFailedEvent(syncOperation, HttpError400(emptyMap()))
        )

        testIgnored(syncOperation, operationHandler, isDebugMode = true, context = mockContext)
    }

    @Test
    fun `should emit sync error event on Unauthenticated failure`() {
        val operationHandler = MockOperationHandler(
            sync = createFailedEvent(syncOperation, HttpError401(emptyMap()))
        )

        val events = listOf(
            RemoteNotesSyncStarted(),
            OutboundQueueSyncInactive(),
            NotAuthorized(userID = TEST_USER_ID),
            RemoteNotesSyncError(SyncErrorType.Unauthenticated),
            RemoteNotesSyncFailed()
        )

        testEventSequenceFailure(syncOperation, operationHandler, events, context = mockContext)
    }

    @Test
    fun `should emit sync error event on Unauthenticated failure in the queue`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(createNoteOperation, HttpError401(emptyMap())),
            sync = createFailedEvent(syncOperation, HttpError401(emptyMap()))
        )

        val events = listOf(
            OutboundQueueSyncInactive(),
            NotAuthorized(userID = TEST_USER_ID),
            RemoteNotesSyncError(SyncErrorType.Unauthenticated)
        )

        testEventSequenceFailure(listOf(createNoteOperation, syncOperation), operationHandler, events, context = mockContext)
    }

    @Test
    fun `should emit sync error event on HttpError403 GenericError failure`() {
        val operationHandler = MockOperationHandler(
            sync = createFailedEvent(syncOperation, HttpError403.GenericError(emptyMap()))
        )

        val events = listOf(
            RemoteNotesSyncStarted(),
            OutboundQueueSyncInactive(),
            ForbiddenError(ForbiddenError.ErrorType.GenericSyncError(supportUrl = null)),
            RemoteNotesSyncError(SyncErrorType.SyncPaused),
            RemoteNotesSyncFailed()
        )

        testEventSequenceFailure(syncOperation, operationHandler, events, context = mockContext)
    }

    @Test
    fun `should emit sync error event on HttpError403 GenericError failure in the queue`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(createNoteOperation, HttpError403.GenericError(emptyMap())),
            sync = createFailedEvent(syncOperation, HttpError403.GenericError(emptyMap()))
        )

        val events = listOf(
            OutboundQueueSyncInactive(),
            ForbiddenError(ForbiddenError.ErrorType.GenericSyncError(supportUrl = null)),
            RemoteNotesSyncError(SyncErrorType.SyncPaused)
        )

        testEventSequenceFailure(listOf(createNoteOperation, syncOperation), operationHandler, events, context = mockContext)
    }

    @Test
    fun `should emit sync error event on HttpError403 NoMailbox failure`() {
        val operationHandler = MockOperationHandler(
            sync = createFailedEvent(syncOperation, HttpError403.NoMailbox(emptyMap(), null))
        )

        val events = listOf(
            RemoteNotesSyncStarted(),
            OutboundQueueSyncInactive(),
            ForbiddenError(ForbiddenError.ErrorType.NoMailbox),
            RemoteNotesSyncError(SyncErrorType.SyncPaused),
            RemoteNotesSyncFailed()
        )

        testEventSequenceFailure(syncOperation, operationHandler, events, context = mockContext)
    }

    @Test
    fun `should emit sync error event on HttpError403 NoMailbox failure in the queue`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(createNoteOperation, HttpError403.NoMailbox(emptyMap(), null)),
            sync = createFailedEvent(syncOperation, HttpError403.NoMailbox(emptyMap(), null))
        )

        val events = listOf(
            OutboundQueueSyncInactive(),
            ForbiddenError(ForbiddenError.ErrorType.NoMailbox),
            RemoteNotesSyncError(SyncErrorType.SyncPaused)
        )

        testEventSequenceFailure(listOf(createNoteOperation, syncOperation), operationHandler, events, context = mockContext)
    }

    @Test
    fun `should emit sync error event on HttpError403 QuotaExceeded failure`() {
        val operationHandler = MockOperationHandler(
            sync = createFailedEvent(syncOperation, HttpError403.QuotaExceeded(emptyMap(), null))
        )

        val events = listOf(
            RemoteNotesSyncStarted(),
            OutboundQueueSyncInactive(),
            ForbiddenError(ForbiddenError.ErrorType.QuotaExceeded),
            RemoteNotesSyncError(SyncErrorType.SyncPaused),
            RemoteNotesSyncFailed()
        )

        testEventSequenceFailure(syncOperation, operationHandler, events, context = mockContext)
    }

    @Test
    fun `should emit sync error event on HttpError403 QuotaExceeded failure in the queue`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(createNoteOperation, HttpError403.QuotaExceeded(emptyMap(), null)),
            sync = createFailedEvent(syncOperation, HttpError403.QuotaExceeded(emptyMap(), null))
        )

        val events = listOf(
            OutboundQueueSyncInactive(),
            ForbiddenError(ForbiddenError.ErrorType.QuotaExceeded),
            RemoteNotesSyncError(SyncErrorType.SyncPaused)
        )

        testEventSequenceFailure(listOf(createNoteOperation, syncOperation), operationHandler, events, context = mockContext)
    }

    @Test
    fun `should handle 410 error InvalidateClientCache`() {
        val operationHandler = MockOperationHandler(
            sync = createFailedEvent(syncOperation, HttpError410.InvalidateClientCache(emptyMap()))
        )

        val events = listOf(
            RemoteNotesSyncStarted(),
            OutboundQueueSyncInactive(),
            RemoteNotesSyncError(SyncErrorType.SyncPaused),
            InvalidateClientCache(),
            RemoteNotesSyncFailed()
        )

        testEventSequenceFailure(listOf(syncOperation), operationHandler, events, queueShouldBeCleared = true, context = mockContext)
    }

    @Test
    fun `should handle 410 error InvalidateClientCache in the queue`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(createNoteOperation, HttpError410.InvalidateClientCache(emptyMap())),
            sync = createFailedEvent(syncOperation, HttpError410.InvalidateClientCache(emptyMap()))
        )

        val events = listOf(
            OutboundQueueSyncInactive(),
            RemoteNotesSyncError(SyncErrorType.SyncPaused),
            InvalidateClientCache()
        )

        testEventSequenceFailure(
            listOf(createNoteOperation, syncOperation), operationHandler, events,
            queueShouldBeCleared = true, context = mockContext
        )
    }

    @Test
    fun `should handle 410 error InvalidateSamsungNotesClientCache in the queue`() {
        val operationHandler = MockOperationHandler(
            samsungNoteSync = createFailedEvent(samsungNoteSyncOperation, HttpError410.InvalidateClientCache(emptyMap()))
        )

        val events = listOf(
            RemoteSamsungNotesSyncStarted(),
            OutboundQueueSyncInactive(),
            InvalidateSamsungNotesClientCache(),
            RemoteSamsungNotesSyncFailed()
        )

        testEventSequenceFailure(
            listOf(samsungNoteSyncOperation), operationHandler, events,
            queueShouldBeCleared = true, context = mockContext
        )
    }

    @Test
    fun `should emit sync error event on UpgradeRequired failure`() {
        val operationHandler = MockOperationHandler(
            sync = createFailedEvent(syncOperation, HttpError426(emptyMap()))
        )

        val events = listOf(
            RemoteNotesSyncStarted(),
            OutboundQueueSyncInactive(),
            UpgradeRequired(),
            RemoteNotesSyncError(SyncErrorType.SyncPaused),
            RemoteNotesSyncFailed()
        )

        testEventSequenceFailure(syncOperation, operationHandler, events, context = mockContext)
    }

    @Test
    fun `should emit sync error event on UpgradeRequired failure in the queue`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(createNoteOperation, HttpError426(emptyMap())),
            sync = createFailedEvent(syncOperation, HttpError426(emptyMap()))
        )

        val events = listOf(
            OutboundQueueSyncInactive(),
            UpgradeRequired(),
            RemoteNotesSyncError(SyncErrorType.SyncPaused)
        )

        testEventSequenceFailure(listOf(createNoteOperation, syncOperation), operationHandler, events, context = mockContext)
    }
}
