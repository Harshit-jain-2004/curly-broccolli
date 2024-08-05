package com.microsoft.notes.sync.apiresulthandler

import com.microsoft.notes.sync.ApiRequestOperation
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.CreateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.Sync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateNote
import com.microsoft.notes.sync.ApiResponseEvent
import com.microsoft.notes.sync.ApiResponseEvent.NoteCreated
import com.microsoft.notes.sync.ApiResponseEvent.NoteUpdated
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncSucceeded
import com.microsoft.notes.sync.ApiResult
import com.microsoft.notes.sync.ApiResultHandler
import com.microsoft.notes.sync.QueueInstruction
import com.microsoft.notes.sync.QueueInstruction.BroadcastEvent
import com.microsoft.notes.sync.QueueInstruction.LogTelemetry
import com.microsoft.notes.sync.QueueInstruction.MapQueue
import com.microsoft.notes.sync.QueueInstruction.RemoveOperation
import com.microsoft.notes.sync.QueueInstruction.SetDelay
import com.microsoft.notes.sync.note
import com.microsoft.notes.sync.remoteRichTextNote
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ApiResultHandlerSuccessTest {
    companion object {
        private const val LOCAL_ID = "LOCAL_ID"
        private const val REMOTE_ID = "REMOTE_ID"
    }

    private val note = note(LOCAL_ID, REMOTE_ID)

    @Test
    fun `should handle CreateNote`() {
        val operation = CreateNote(note = note)
        val result = ApiResult.Success(
            NoteCreated(
                localId = LOCAL_ID, remoteNote = remoteRichTextNote(REMOTE_ID)
            )
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(5))
        assertThat(instructions.find { it is RemoveOperation }, iz(notNullValue()))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastEvent }, iz(notNullValue()))
        assertThat(instructions.find { it is MapQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))

        val broadcastEvent = instructions.find { it is BroadcastEvent } as BroadcastEvent
        assertThat(broadcastEvent.event, iz(instanceOf(NoteCreated::class.java)))
    }

    @Test
    fun `should handle UpdateNote`() {
        val operation = UpdateNote(note = note, uiBaseRevision = 1)
        val result = ApiResult.Success(
            NoteUpdated(
                localId = LOCAL_ID, remoteNote = remoteRichTextNote(REMOTE_ID), uiBaseRevision = 1
            )
        )
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(5))
        assertThat(instructions.find { it is RemoveOperation }, iz(notNullValue()))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastEvent }, iz(notNullValue()))
        assertThat(instructions.find { it is MapQueue }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))

        val broadcastEvent = instructions.find { it is BroadcastEvent } as BroadcastEvent
        assertThat(broadcastEvent.event, iz(instanceOf(NoteUpdated::class.java)))
    }

    @Test
    fun `should handle other operation`() {
        // this test checks an operation which does not have MapQueue instruction

        val operation = Sync(deltaToken = null)
        val result = ApiResult.Success(RemoteNotesSyncSucceeded())
        val instructions = getInstructions(operation, result)

        assertThat(instructions.size, iz(4))
        assertThat(instructions.find { it is RemoveOperation }, iz(notNullValue()))
        assertThat(instructions.find { it is LogTelemetry }, iz(notNullValue()))
        assertThat(instructions.find { it is BroadcastEvent }, iz(notNullValue()))
        assertThat(instructions.find { it is SetDelay }, iz(notNullValue()))

        val broadcastEvent = instructions.find { it is BroadcastEvent } as BroadcastEvent
        assertThat(broadcastEvent.event, iz(instanceOf(RemoteNotesSyncSucceeded::class.java)))
    }

    private fun getInstructions(
        operation: ApiRequestOperation.ValidApiRequestOperation,
        result: ApiResult<ApiResponseEvent>
    ): List<QueueInstruction> {
        val handler = ApiResultHandler(notesLogger = null, isDebugMode = false)
        operation.requestId = ""
        operation.realTimeSessionId = ""
        return handler.handleResult(result = result, operation = operation)
    }
}
