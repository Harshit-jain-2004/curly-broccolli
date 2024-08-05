package com.microsoft.notes.sync.outboundqueue

import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteMedia
import com.microsoft.notes.sync.ApiResponseEvent.MediaDeleted
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.HttpError404.Http404RestApiNotFound
import com.microsoft.notes.sync.HttpError404.UnknownHttp404Error
import com.microsoft.notes.sync.HttpError500
import org.junit.Test

class OutboundQueueDeleteMediaTest : FeedFRESetup() {
    private val outboundOperation = DeleteMedia("123", "remoteId", "456", "remoteMediaId")

    @Test
    fun `should emit event on success`() {
        val operationHandler = MockOperationHandler(deleteMedia = createDeleteMediaEvent(outboundOperation))
        val event = MediaDeleted("123", "456", "remoteMediaId")

        testSuccess(outboundOperation, operationHandler, event, mockContext)
    }

    @Test
    fun `should not emit event on failure`() {
        val operationHandler = MockOperationHandler(
            deleteMedia = createFailedEvent(outboundOperation, HttpError500(emptyMap()))
        )

        testFailure(outboundOperation, operationHandler, mockContext)
    }

    @Test
    fun `should remove operation on 404 RestApiNotFound`() {
        val operationHandler = MockOperationHandler(
            deleteMedia = createFailedEvent(outboundOperation, Http404RestApiNotFound(emptyMap()))
        )

        testIgnored(outboundOperation, operationHandler, context = mockContext)
    }

    @Test
    fun `should remove operation on 404`() {
        val operationHandler = MockOperationHandler(
            deleteMedia = createFailedEvent(outboundOperation, UnknownHttp404Error(emptyMap()))
        )

        testIgnored(outboundOperation, operationHandler, context = mockContext)
    }
}
