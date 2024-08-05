package com.microsoft.notes.sync.outboundqueue

import com.microsoft.notes.sync.ApiRequestOperation
import com.microsoft.notes.sync.ApiResponseEvent
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.HttpError404
import com.microsoft.notes.sync.HttpError500
import com.microsoft.notes.sync.note
import com.microsoft.notes.sync.remoteRichTextNote
import org.junit.Test

class OutboundQueueMergeNoteTest : FeedFRESetup() {
    private val outboundOperation = ApiRequestOperation.ValidApiRequestOperation.GetNoteForMerge(
        note("123"),
        uiBaseRevision = 0L
    )

    @Test
    fun `should emit event on success`() {
        val remoteNote = remoteRichTextNote("remoteId")
        val operationHandler = MockOperationHandler(
            getNoteForMerge = createMergeEvent(outboundOperation, remoteNote)
        )
        val event = ApiResponseEvent.NoteFetchedForMerge("123", remoteNote, uiBaseRevision = 0L)

        testSuccess(outboundOperation, operationHandler, event, context = mockContext)
    }

    @Test
    fun `should not emit event on failure`() {
        val operationHandler = MockOperationHandler(
            getNoteForMerge = createFailedEvent(outboundOperation, HttpError500(emptyMap()))
        )

        testFailure(outboundOperation, operationHandler, context = mockContext)
    }

    @Test
    fun `should remove operation on 404 RestApiNotFound`() {
        val operationHandler = MockOperationHandler(
            getNoteForMerge = createFailedEvent(
                outboundOperation,
                HttpError404.Http404RestApiNotFound(emptyMap())
            )
        )

        testIgnored(outboundOperation, operationHandler, context = mockContext)
    }

    @Test
    fun `should remove operation on 404`() {
        val operationHandler = MockOperationHandler(
            getNoteForMerge = createFailedEvent(
                outboundOperation,
                HttpError404.UnknownHttp404Error(emptyMap())
            )
        )

        testIgnored(outboundOperation, operationHandler, context = mockContext)
    }
}
