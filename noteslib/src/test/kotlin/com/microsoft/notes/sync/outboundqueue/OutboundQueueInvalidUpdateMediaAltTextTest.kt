package com.microsoft.notes.sync.outboundqueue

import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidUpdateMediaAltText
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.note
import org.junit.Assert
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class OutboundQueueInvalidUpdateMediaAltTextTest : FeedFRESetup() {
    companion object {
        private const val NOTE_LOCAL_ID = "NOTE_LOCAL_ID"
        private const val NOTE_REMOTE_ID = "NOTE_REMOTE_ID"
        private const val MEDIA_LOCAL_ID = "MEDIA_LOCAL_ID"
        private const val ALT_TEXT = "ALT_TEXT"
        private const val UI_BASE_REVISION: Long = 1
    }

    private val operation = InvalidUpdateMediaAltText(
        note = note(NOTE_LOCAL_ID, NOTE_REMOTE_ID),
        mediaLocalId = MEDIA_LOCAL_ID,
        altText = ALT_TEXT,
        uiBaseRevision = UI_BASE_REVISION
    )

    @Test
    fun `should not be worked off`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        outboundQueue.push(operation, workImmediately = false)
        outboundQueue.work().get()
        outboundQueue.work().get()
        outboundQueue.work().get()

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, iz(1))
    }
}
