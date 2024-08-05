package com.microsoft.notes.sync.outboundqueue

import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateMediaAltText
import com.microsoft.notes.sync.ApiResponseEvent.MediaAltTextUpdated
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.HttpError404.Http404RestApiNotFound
import com.microsoft.notes.sync.HttpError404.UnknownHttp404Error
import com.microsoft.notes.sync.HttpError500
import com.microsoft.notes.sync.models.MediaAltTextUpdate
import com.microsoft.notes.sync.note
import org.junit.Test

class OutboundQueueUpdateMediaAltTextTest : FeedFRESetup() {
    companion object {
        private const val NOTE_LOCAL_ID = "NOTE_LOCAL_ID"
        private const val NOTE_REMOTE_ID = "NOTE_REMOTE_ID"
        private const val MEDIA_LOCAL_ID = "MEDIA_LOCAL_ID"
        private const val MEDIA_REMOTE_ID = "MEDIA_REMOTE_ID"
        private const val ALT_TEXT = "ALT_TEXT"
        private const val UI_BASE_REVISION: Long = 1
    }

    private val outboundOperation = UpdateMediaAltText(
        note = note(NOTE_LOCAL_ID, NOTE_REMOTE_ID),
        localMediaId = MEDIA_LOCAL_ID,
        remoteMediaId = MEDIA_REMOTE_ID,
        altText = ALT_TEXT,
        uiBaseRevision = UI_BASE_REVISION
    )

    @Test
    fun `should emit event on success`() {
        val operationHandler = MockOperationHandler(
            updateMediaAltText = createUpdateMediaAltTextEvent(outboundOperation)
        )
        val event = MediaAltTextUpdated(
            noteId = NOTE_LOCAL_ID,
            mediaAltTextUpdate = MediaAltTextUpdate(
                changeKey = "",
                id = MEDIA_REMOTE_ID,
                createdWithLocalId = MEDIA_LOCAL_ID,
                mimeType = "",
                lastModified = "",
                altText = ALT_TEXT,
                imageDimensions = null
            )
        )

        testSuccess(outboundOperation, operationHandler, event, context = mockContext)
    }

    @Test
    fun `should not emit event on failure`() {
        val operationHandler = MockOperationHandler(
            updateMediaAltText = createFailedEvent(outboundOperation, HttpError500(emptyMap()))
        )

        testFailure(outboundOperation, operationHandler, context = mockContext)
    }

    @Test
    fun `should remove operation on 404 RestApiNotFound`() {
        val operationHandler = MockOperationHandler(
            updateMediaAltText = createFailedEvent(outboundOperation, Http404RestApiNotFound(emptyMap()))
        )

        testIgnored(outboundOperation, operationHandler, context = mockContext)
    }

    @Test
    fun `should remove operation on 404`() {
        val operationHandler = MockOperationHandler(
            updateMediaAltText = createFailedEvent(outboundOperation, UnknownHttp404Error(emptyMap()))
        )

        testIgnored(outboundOperation, operationHandler, context = mockContext)
    }
}
