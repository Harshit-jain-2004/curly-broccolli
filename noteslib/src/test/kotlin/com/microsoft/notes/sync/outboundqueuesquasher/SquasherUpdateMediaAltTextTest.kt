package com.microsoft.notes.sync.outboundqueuesquasher

import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidUpdateMediaAltText
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateMediaAltText
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.note
import com.microsoft.notes.sync.outboundqueue.MockOperationHandler
import com.microsoft.notes.sync.outboundqueue.createQueue
import org.junit.Assert
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class SquasherUpdateMediaAltTextTest : FeedFRESetup() {
    companion object {
        private const val NOTE_LOCAL_ID = "NOTE_LOCAL_ID"
        private const val NOTE_REMOTE_ID = "NOTE_REMOTE_ID"
        private const val MEDIA_LOCAL_ID = "MEDIA_LOCAL_ID"
        private const val MEDIA_REMOTE_ID = "MEDIA_REMOTE_ID"
        private const val MEDIA_LOCAL_ID_2 = "MEDIA_LOCAL_ID_2"
        private const val MEDIA_REMOTE_ID_2 = "MEDIA_REMOTE_ID_2"
        private const val ALT_TEXT = "ALT_TEXT"
        private const val UI_BASE_REVISION: Long = 1
    }

    private val invalidUpdateMediaAltText = InvalidUpdateMediaAltText(
        note = note(NOTE_LOCAL_ID, NOTE_REMOTE_ID),
        mediaLocalId = MEDIA_LOCAL_ID,
        altText = ALT_TEXT,
        uiBaseRevision = UI_BASE_REVISION
    )
    private val updateMediaAltText = UpdateMediaAltText(
        note = note(NOTE_LOCAL_ID, NOTE_REMOTE_ID),
        localMediaId = MEDIA_LOCAL_ID,
        remoteMediaId = MEDIA_REMOTE_ID,
        altText = ALT_TEXT,
        uiBaseRevision = UI_BASE_REVISION
    )
    private val updateMediaAltTextAgain = UpdateMediaAltText(
        note = note(NOTE_LOCAL_ID, NOTE_REMOTE_ID),
        localMediaId = MEDIA_LOCAL_ID,
        remoteMediaId = MEDIA_REMOTE_ID,
        altText = ALT_TEXT,
        uiBaseRevision = UI_BASE_REVISION
    )
    private val updateMediaAltText2 = UpdateMediaAltText(
        note = note(NOTE_LOCAL_ID, NOTE_REMOTE_ID),
        localMediaId = MEDIA_LOCAL_ID_2,
        remoteMediaId = MEDIA_REMOTE_ID_2,
        altText = ALT_TEXT,
        uiBaseRevision = UI_BASE_REVISION
    )

    @Test
    fun `should squash InvalidUpdateMediaAltText on UpdateMediaAltText`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        outboundQueue.push(invalidUpdateMediaAltText, workImmediately = false)
        outboundQueue.push(updateMediaAltText, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, iz(1))
        Assert.assertThat(outboundQueue.toList()[0] as UpdateMediaAltText, iz(updateMediaAltText))
    }

    @Test
    fun `should not squash InvalidUpdateMediaAltText on different UpdateMediaAltText`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        outboundQueue.push(invalidUpdateMediaAltText, workImmediately = false)
        outboundQueue.push(updateMediaAltText2, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, iz(2))
        Assert.assertThat(outboundQueue.toList()[0] as UpdateMediaAltText, iz(updateMediaAltText2))
        Assert.assertThat(outboundQueue.toList()[1] as InvalidUpdateMediaAltText, iz(invalidUpdateMediaAltText))
    }

    @Test
    fun `should squash UpdateMediaAltText on UpdateMediaAltText`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        outboundQueue.push(updateMediaAltText, workImmediately = false)
        outboundQueue.push(updateMediaAltTextAgain, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, iz(1))
        Assert.assertThat(outboundQueue.toList()[0] as UpdateMediaAltText, iz(updateMediaAltTextAgain))
    }

    @Test
    fun `should not squash UpdateMediaAltText on different UpdateMediaAltText`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        outboundQueue.push(updateMediaAltText, workImmediately = false)
        outboundQueue.push(updateMediaAltText2, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, iz(2))
        Assert.assertThat(outboundQueue.toList()[0] as UpdateMediaAltText, iz(updateMediaAltText))
        Assert.assertThat(outboundQueue.toList()[1] as UpdateMediaAltText, iz(updateMediaAltText2))
    }
}
