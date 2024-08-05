package com.microsoft.notes.sync.outboundqueue

import com.microsoft.notes.sync.ApiRequestOperation
import com.microsoft.notes.sync.ApiResponseEvent
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.HttpError404
import com.microsoft.notes.sync.HttpError500
import org.junit.Test

class OutboundQueueDeleteNoteTest : FeedFRESetup() {
    private val outboundOperation = ApiRequestOperation.ValidApiRequestOperation.DeleteNote("123", "remoteId")

    @Test
    fun `should emit event on success`() {
        val operationHandler = MockOperationHandler(delete = createDeleteEvent(outboundOperation))
        val event = ApiResponseEvent.NoteDeleted("123", "remoteId")

        testSuccess(outboundOperation, operationHandler, event, context = mockContext)
    }

    @Test
    fun `should not emit event on failure`() {
        val operationHandler = MockOperationHandler(
            delete = createFailedEvent(outboundOperation, HttpError500(emptyMap()))
        )

        testFailure(outboundOperation, operationHandler, context = mockContext)
    }

    @Test
    fun `should remove operation on 404 RestApiNotFound`() {
        val operationHandler = MockOperationHandler(
            delete = createFailedEvent(outboundOperation, HttpError404.Http404RestApiNotFound(emptyMap()))
        )

        testIgnored(outboundOperation, operationHandler, context = mockContext)
    }

    @Test
    fun `should remove operation on 404`() {
        val operationHandler = MockOperationHandler(
            delete = createFailedEvent(outboundOperation, HttpError404.UnknownHttp404Error(emptyMap()))
        )

        testIgnored(outboundOperation, operationHandler, context = mockContext)
    }
}
