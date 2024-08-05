package com.microsoft.notes.sync.outboundqueuesquasher

import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidDeleteMedia
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidUpdateMediaAltText
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidUploadMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateMediaAltText
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UploadMedia
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.note
import com.microsoft.notes.sync.outboundqueue.MockOperationHandler
import com.microsoft.notes.sync.outboundqueue.createQueue
import org.junit.Assert
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class SquasherDeleteMediaTest : FeedFRESetup() {
    companion object {
        private const val NOTE_LOCAL_ID = "NOTE_LOCAL_ID"
        private const val NOTE_REMOTE_ID = "NOTE_REMOTE_ID"
        private const val MEDIA_LOCAL_ID = "MEDIA_LOCAL_ID"
        private const val MEDIA_REMOTE_ID = "MEDIA_REMOTE_ID"
        private const val ALT_TEXT = "ALT_TEXT"
        private const val UI_BASE_REVISION: Long = 1
    }

    private val uploadMedia = UploadMedia(
        note = note(NOTE_LOCAL_ID, NOTE_REMOTE_ID),
        mediaLocalId = MEDIA_LOCAL_ID,
        localUrl = "", mimeType = ""
    )
    private val invalidUploadMedia = InvalidUploadMedia(
        note = note(NOTE_LOCAL_ID, NOTE_REMOTE_ID),
        mediaLocalId = MEDIA_LOCAL_ID,
        localUrl = "",
        mimeType = ""
    )
    private val updateMediaAltText = UpdateMediaAltText(
        note = note(NOTE_LOCAL_ID, NOTE_REMOTE_ID),
        localMediaId = MEDIA_LOCAL_ID,
        remoteMediaId = MEDIA_REMOTE_ID,
        altText = ALT_TEXT,
        uiBaseRevision = UI_BASE_REVISION
    )
    private val invalidUpdateMediaAltText = InvalidUpdateMediaAltText(
        note = note(NOTE_LOCAL_ID, NOTE_REMOTE_ID),
        mediaLocalId = MEDIA_LOCAL_ID,
        altText = ALT_TEXT,
        uiBaseRevision = UI_BASE_REVISION
    )
    private val invalidDeleteMedia = InvalidDeleteMedia(
        noteLocalId = NOTE_LOCAL_ID,
        mediaLocalId = MEDIA_LOCAL_ID
    )
    private val deleteMedia = DeleteMedia(
        localNoteId = NOTE_LOCAL_ID,
        remoteNoteId = NOTE_REMOTE_ID,
        localMediaId = MEDIA_LOCAL_ID,
        remoteMediaId = MEDIA_REMOTE_ID
    )

    @Test
    fun `should squash UploadMedia on delete`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        outboundQueue.push(uploadMedia, workImmediately = false)
        outboundQueue.push(deleteMedia, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, iz(1))
        Assert.assertThat(outboundQueue.toList()[0] as DeleteMedia, iz(deleteMedia))
    }

    @Test
    fun `should squash InvalidUploadMedia on delete`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        outboundQueue.push(invalidUploadMedia, workImmediately = false)
        outboundQueue.push(deleteMedia, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, iz(1))
        Assert.assertThat(outboundQueue.toList()[0] as DeleteMedia, iz(deleteMedia))
    }

    @Test
    fun `should squash InvalidDeleteMedia on delete`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        outboundQueue.push(invalidDeleteMedia, workImmediately = false)
        outboundQueue.push(deleteMedia, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, iz(1))
        Assert.assertThat(outboundQueue.toList()[0] as DeleteMedia, iz(deleteMedia))
    }

    @Test
    fun `should squash UpdateMediaAltText on delete`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        outboundQueue.push(updateMediaAltText, workImmediately = false)
        outboundQueue.push(deleteMedia, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, iz(1))
        Assert.assertThat(outboundQueue.toList()[0] as DeleteMedia, iz(deleteMedia))
    }

    @Test
    fun `should squash InvalidUpdateMediaAltText on delete`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        outboundQueue.push(invalidUpdateMediaAltText, workImmediately = false)
        outboundQueue.push(deleteMedia, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, iz(1))
        Assert.assertThat(outboundQueue.toList()[0] as DeleteMedia, iz(deleteMedia))
    }
}
