package com.microsoft.notes.sync

import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.CreateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.Sync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateNote
import com.microsoft.notes.sync.models.localOnly.Note
import com.microsoft.notes.sync.models.localOnly.RemoteData
import org.hamcrest.CoreMatchers
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class PriorityQueueTest {
    val remoteData = RemoteData(
        id = "remoteId", changeKey = "somechangekey",
        lastServerVersion = remoteRichTextNote("remoteId"),
        createdAt = "2018-01-31T16:45:05.0000000Z",
        lastModifiedAt = "2018-01-31T16:50:31.0000000Z"
    )

    val note = Note(
        id = "id", remoteData = remoteData, document = richTextDocument(),
        color = Note.Color.BLUE, createdByApp = "SitckyNotesApp",
        documentModifiedAt = "2018-01-31T16:50:31.0000000Z",
        // todo implement
        media = listOf(),
        metadata = testRemoteNoteMetadata()
    )

    val createDummyBackupFile: (String) -> IPersist<String> = { _ ->
        object : IPersist<String> {
            override fun load(): String? = null
            override fun persist(objectToPersist: String) {}
        }
    }

    @Test
    fun should_be_in_order() {
        val queue = PriorityQueue(
            queue = emptyList(),
            persistenceOnDiskEnabled = false,
            createBackupFile = createDummyBackupFile
        )

        val operation1 = Sync(null)
        queue.push(operation1)
        assertThat<ApiRequestOperation>(queue.peek(), iz(operation1))
        assertThat(queue.count, iz(1))

        val operation2 = CreateNote(note)
        queue.push(operation2)
        assertThat<ApiRequestOperation>(queue.peek(), iz(operation2))
        assertThat(queue.count, iz(2))

        val operation3 = UpdateNote(note, uiBaseRevision = 0L)
        queue.push(operation3)
        assertThat<ApiRequestOperation>(queue.peek(), iz(operation2))
        assertThat(queue.count, iz(3))

        queue.remove(operation2)
        assertThat<ApiRequestOperation>(queue.peek(), iz(operation3))
        assertThat(queue.count, iz(2))

        queue.remove(operation3)
        assertThat<ApiRequestOperation>(queue.peek(), iz(operation1))
        assertThat<ApiRequestOperation>(queue.peek(), iz(operation1))
        assertThat(queue.count, iz(1))

        queue.remove(operation1)
        assertThat<ApiRequestOperation>(queue.peek(), CoreMatchers.nullValue())
        assertThat<ApiRequestOperation>(queue.peek(), CoreMatchers.nullValue())
        assertThat(queue.count, iz(0))
    }

    @Test
    fun can_remove_based_on_predicate() {
        val queue = PriorityQueue(
            queue = emptyList(),
            persistenceOnDiskEnabled = false,
            createBackupFile = createDummyBackupFile
        )

        val operation1 = CreateNote(note)
        val operation2 = CreateNote(note.copy(color = Note.Color.GREEN))
        queue.push(operation1)
        queue.push(operation2)
        assertThat(queue.count, iz(2))

        queue.removeIf { it is CreateNote && it.note.color == Note.Color.GREEN }

        assertThat(queue.count, iz(1))
        assertThat<ApiRequestOperation>(queue.peek(), iz(operation1))
    }

    @Test
    fun can_map_values() {
        val queue = PriorityQueue(
            queue = emptyList(),
            persistenceOnDiskEnabled = false,
            createBackupFile = createDummyBackupFile
        )

        val operation1 = CreateNote(note)
        queue.push(operation1)
        assertThat(queue.count, iz(1))

        val newOperation = operation1.copy(note = operation1.note.copy(color = Note.Color.PINK))

        queue.map { newOperation }

        assertThat(queue.count, iz(1))
        assertThat<ApiRequestOperation>(queue.peek(), iz(newOperation))
    }
}
