package com.microsoft.notes.sync.outboundqueue

import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.GetNoteForMerge
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateNote
import com.microsoft.notes.sync.ApiResponseEvent
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.HttpError404
import com.microsoft.notes.sync.HttpError409
import com.microsoft.notes.sync.HttpError500
import com.microsoft.notes.sync.note
import com.microsoft.notes.sync.remoteRichTextNote
import org.junit.Assert
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class OutboundQueueUpdateNoteTest : FeedFRESetup() {
    private val outboundOperation = UpdateNote(note("123"), uiBaseRevision = 0L)

    @Test
    fun `should emit event on success`() {
        val remoteNote = remoteRichTextNote("remoteId")
        val operationHandler = MockOperationHandler(update = createUpdateEvent(outboundOperation, remoteNote))
        val event = ApiResponseEvent.NoteUpdated("123", remoteNote, uiBaseRevision = 0L)

        testSuccess(outboundOperation, operationHandler, event, context = mockContext)
    }

    @Test
    fun `should not emit event on failure`() {
        val operationHandler = MockOperationHandler(
            update = createFailedEvent(outboundOperation, HttpError500(emptyMap()))
        )

        testFailure(outboundOperation, operationHandler, context = mockContext)
    }

    @Test
    fun `should remove operation on 404 RestApiNotFound`() {
        val operationHandler = MockOperationHandler(
            update = createFailedEvent(outboundOperation, HttpError404.Http404RestApiNotFound(emptyMap()))
        )

        testIgnored(outboundOperation, operationHandler, context = mockContext)
    }

    @Test
    fun `should remove operation on 404`() {
        val operationHandler = MockOperationHandler(
            update = createFailedEvent(outboundOperation, HttpError404.UnknownHttp404Error(emptyMap()))
        )

        testIgnored(outboundOperation, operationHandler, context = mockContext)
    }

    @Test
    fun `should enqueue merge on 409`() {
        val operationHandler = MockOperationHandler(
            update = createFailedEvent(outboundOperation, HttpError409(emptyMap()))
        )
        var called = false
        val outboundQueue = createQueue(operationHandler, eventHandler = { called = true }, isDebugMode = true, context = mockContext)
        outboundQueue.push(outboundOperation, workImmediately = false)
        outboundQueue.work().get()

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, iz(1))
        val operation = outboundQueue.toList().first() as GetNoteForMerge
        Assert.assertThat(operation.note, iz(outboundOperation.note))
    }
}
