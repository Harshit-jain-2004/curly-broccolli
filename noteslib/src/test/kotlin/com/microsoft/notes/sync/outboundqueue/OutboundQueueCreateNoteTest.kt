package com.microsoft.notes.sync.outboundqueue

import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.CreateNote
import com.microsoft.notes.sync.ApiResponseEvent
import com.microsoft.notes.sync.Error
import com.microsoft.notes.sync.ErrorDetails
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.HttpError403
import com.microsoft.notes.sync.HttpError500
import com.microsoft.notes.sync.note
import com.microsoft.notes.sync.remoteRichTextNote
import org.junit.Test

class OutboundQueueCreateNoteTest : FeedFRESetup() {
    private val outboundOperation = CreateNote(note("123"))

    @Test
    fun `should emit event on success`() {
        val remoteNote = remoteRichTextNote("remoteId")
        val operationHandler = MockOperationHandler(create = createCreateEvent(outboundOperation, remoteNote))
        val event = ApiResponseEvent.NoteCreated("123", remoteNote)

        testSuccess(outboundOperation, operationHandler, event, context = mockContext)
    }

    @Test
    fun `should not emit event on failure`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(outboundOperation, HttpError500(emptyMap()))
        )

        testFailure(outboundOperation, operationHandler, mockContext)
    }

    @Test
    fun `should emit generic error on 403 when mailbox related`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(
                outboundOperation,
                HttpError403.NoMailbox(emptyMap(), ErrorDetails(Error("NoExchangeMailbox")))
            )
        )

        var calledFailedEvent = false
        var calledSyncInactive = false
        val outboundQueue = createQueue(operationHandler, eventHandler = {
            when (it) {
                is ApiResponseEvent.ForbiddenError -> {
                    assert(it.error is ApiResponseEvent.ForbiddenError.ErrorType.NoMailbox)
                    calledFailedEvent = true
                }
                is ApiResponseEvent.OutboundQueueSyncInactive -> {
                    calledSyncInactive = true
                }
                else -> assert(false) { "response is not expected event " + it.javaClass.name }
            }
        }, isDebugMode = false, context = mockContext)

        outboundQueue.push(outboundOperation, workImmediately = false)
        outboundQueue.work().get()
        assert(calledFailedEvent)
        assert(calledSyncInactive)
        assert(outboundQueue.isPaused)
    }

    @Test
    fun `should emit generic error on 403 when error is marked as generic`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(outboundOperation, HttpError403.GenericError(emptyMap()))
        )

        var calledFailedEvent = false
        var calledSyncInactive = false
        val outboundQueue = createQueue(operationHandler, eventHandler = {
            when (it) {
                is ApiResponseEvent.ForbiddenError -> {
                    assert(it.error is ApiResponseEvent.ForbiddenError.ErrorType.GenericSyncError)
                    calledFailedEvent = true
                }
                is ApiResponseEvent.OutboundQueueSyncInactive -> {
                    calledSyncInactive = true
                }
                else -> assert(false) { "response is not expected event " + it.javaClass.name }
            }
        }, isDebugMode = false, context = mockContext)

        outboundQueue.push(outboundOperation, workImmediately = false)
        outboundQueue.work().get()
        assert(calledFailedEvent)
        assert(calledSyncInactive)
        assert(outboundQueue.isPaused)
    }

    @Test
    fun `should not emit generic error on 403 when error does not include error code`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(outboundOperation, HttpError403.UnknownError(emptyMap()))
        )

        var called = false
        val outboundQueue = createQueue(operationHandler, eventHandler = {
            called = true
        }, isDebugMode = false, context = mockContext)

        outboundQueue.push(outboundOperation, workImmediately = false)
        outboundQueue.work().get()
        assert(!called)
        assert(!outboundQueue.isPaused)
    }
}
