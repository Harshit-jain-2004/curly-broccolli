package com.microsoft.notes.sync.outboundqueue

import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.CreateNote
import com.microsoft.notes.sync.ApiResponseEvent
import com.microsoft.notes.sync.Error
import com.microsoft.notes.sync.ErrorDetails
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.HttpError401
import com.microsoft.notes.sync.HttpError403
import com.microsoft.notes.sync.HttpError410
import com.microsoft.notes.sync.HttpError426
import com.microsoft.notes.sync.note
import org.junit.Test

class OutboundQueueSyncInactiveTest : FeedFRESetup() {
    private val outboundOperation = CreateNote(note("123"))

    @Test
    fun `should emit SyncInactive on 401 Not Authorised`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(
                outboundOperation,
                HttpError401(emptyMap())
            )
        )

        testSyncInactiveCall(operationHandler)
    }

    @Test
    fun `should emit SyncInactive on 403 No Mailbox`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(
                outboundOperation,
                HttpError403.NoMailbox(emptyMap(), ErrorDetails(Error("NoExchangeMailbox")))
            )
        )

        testSyncInactiveCall(operationHandler)
    }

    @Test
    fun `should emit SyncInactive on 403 Quota Exceeded`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(
                outboundOperation,
                HttpError403.NoMailbox(emptyMap(), ErrorDetails(Error("QuotaExceeded")))
            )
        )

        testSyncInactiveCall(operationHandler)
    }

    @Test
    fun `should emit SyncInactive on 403 Generic Error`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(
                outboundOperation,
                HttpError403.GenericError(emptyMap())
            )
        )

        testSyncInactiveCall(operationHandler)
    }

    @Test
    fun `should emit SyncInactive on 410 Invalidate Client Cache`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(
                outboundOperation,
                HttpError410.InvalidateClientCache(emptyMap())
            )
        )

        testSyncInactiveCall(operationHandler)
    }

    @Test
    fun `should emit SyncInactive on 426 Upgrade Required`() {
        val operationHandler = MockOperationHandler(
            create = createFailedEvent(
                outboundOperation,
                HttpError426(emptyMap())
            )
        )

        testSyncInactiveCall(operationHandler)
    }

    private fun testSyncInactiveCall(operationHandler: MockOperationHandler) {
        var calledSyncInactive = false
        val outboundQueue = createQueue(operationHandler, eventHandler = {
            if (it is ApiResponseEvent.OutboundQueueSyncInactive) {
                calledSyncInactive = true
            }
        }, isDebugMode = false, context = mockContext)

        outboundQueue.push(outboundOperation, workImmediately = false)
        outboundQueue.work().get()
        assert(calledSyncInactive)
        assert(outboundQueue.isPaused)
    }
}
