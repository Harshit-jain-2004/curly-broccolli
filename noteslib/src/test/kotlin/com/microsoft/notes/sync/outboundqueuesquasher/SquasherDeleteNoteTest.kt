package com.microsoft.notes.sync.outboundqueuesquasher

import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidDeleteNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteNote
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.outboundqueue.MockOperationHandler
import com.microsoft.notes.sync.outboundqueue.createQueue
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Test

class SquasherDeleteNoteTest : FeedFRESetup() {
    @Test
    fun `should squash multiple InvalidDeleteNote with the same ids`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        val deleteNote = InvalidDeleteNote("NOTE_LOCAL_ID")
        outboundQueue.push(InvalidDeleteNote("NOTE_LOCAL_ID"), workImmediately = false)
        outboundQueue.push(deleteNote, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, CoreMatchers.`is`(1))
        Assert.assertThat(outboundQueue.toList()[0] as InvalidDeleteNote, CoreMatchers.`is`(deleteNote))
    }

    @Test
    fun `should not squash multiple InvalidDeleteNote when their ids are different`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        val deleteNote = InvalidDeleteNote("NOTE_LOCAL_ID_1")
        outboundQueue.push(InvalidDeleteNote("NOTE_LOCAL_ID_2"), workImmediately = false)
        outboundQueue.push(deleteNote, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, CoreMatchers.`is`(2))
    }

    @Test
    fun `should squash multiple DeleteNote with the same ids`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        val deleteNote = DeleteNote("NOTE_LOCAL_ID", "NOTE_REMOTE_ID")
        outboundQueue.push(DeleteNote("NOTE_LOCAL_ID", "NOTE_REMOTE_ID"), workImmediately = false)
        outboundQueue.push(deleteNote, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, CoreMatchers.`is`(1))
        Assert.assertThat(outboundQueue.toList()[0] as DeleteNote, CoreMatchers.`is`(deleteNote))
    }

    @Test
    fun `should not squash multiple DeleteNote when their ids are different`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        val deleteNote = DeleteNote("NOTE_LOCAL_ID_1", "NOTE_REMOTE_ID_1")
        outboundQueue.push(DeleteNote("NOTE_LOCAL_ID_2", "NOTE_REMOTE_ID_2"), workImmediately = false)
        outboundQueue.push(deleteNote, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, CoreMatchers.`is`(2))
    }

    @Test
    fun `should delete DeleteNote if the incoming InvalidDeleteNote has the same ids`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        val invalidDeleteNote = InvalidDeleteNote("NOTE_LOCAL_ID")
        outboundQueue.push(DeleteNote("NOTE_LOCAL_ID", "NOTE_REMOTE_ID"), workImmediately = false)
        outboundQueue.push(invalidDeleteNote, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, CoreMatchers.`is`(1))
        Assert.assertThat(outboundQueue.toList()[0] as InvalidDeleteNote, CoreMatchers.`is`(invalidDeleteNote))
    }

    @Test
    fun `should delete InvalidDeleteNote if the incoming DeleteNote has the same ids`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        val deleteNote = DeleteNote("NOTE_LOCAL_ID", "NOTE_REMOTE_ID")
        outboundQueue.push(InvalidDeleteNote("NOTE_LOCAL_ID"), workImmediately = false)
        outboundQueue.push(deleteNote, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, CoreMatchers.`is`(1))
        Assert.assertThat(outboundQueue.toList()[0] as DeleteNote, CoreMatchers.`is`(deleteNote))
    }
}
