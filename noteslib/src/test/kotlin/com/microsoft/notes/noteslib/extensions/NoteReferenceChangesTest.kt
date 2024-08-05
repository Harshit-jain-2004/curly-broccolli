package com.microsoft.notes.noteslib.extensions

import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.NoteReferenceChanges
import com.microsoft.notes.models.NoteReferenceUpdate
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class NoteReferenceChangesTest {

    @Test
    fun `note reference delete changes test`() {
        val localNotes = generateDummyNoteReferences(6)
        val deletedNotes = listOf(localNotes[1], localNotes[3])
        val remoteNotes = localNotes - deletedNotes

        val expectedChanges = NoteReferenceChanges(toDelete = deletedNotes)
        assertThat(calculateNoteReferencesListChanges(localNotes, remoteNotes), iz(expectedChanges))
    }

    @Test
    fun `note reference create changes test`() {
        val localNotes = generateDummyNoteReferences(6, 0)
        val createdNotes = generateDummyNoteReferences(3, 10)
        val remoteNotes = localNotes + createdNotes

        val expectedChanges = NoteReferenceChanges(toCreate = createdNotes)
        assertThat(calculateNoteReferencesListChanges(localNotes, remoteNotes), iz(expectedChanges))
    }

    @Test
    fun `note reference replace changes test`() {
        val localNotes = generateDummyNoteReferences(6, 0)
        val replacementNotes = listOf(
            localNotes[0].copy(title = "newContent", lastModifiedAt = localNotes[0].lastModifiedAt + 60000),
            localNotes[3].copy(title = "AlsoNewContent", lastModifiedAt = localNotes[0].lastModifiedAt + 60000)
        )
        val remoteNotes = localNotes.map { localNote ->
            replacementNotes.find { it.localId == localNote.localId } ?: localNote
        }

        val expectedChanges = NoteReferenceChanges(toReplace = replacementNotes.map { NoteReferenceUpdate(it) })
        assertThat(calculateNoteReferencesListChanges(localNotes, remoteNotes), iz(expectedChanges))
    }

    private fun generateDummyNoteReferences(size: Int, firstNoteId: Int = 0): List<NoteReference> =
        (firstNoteId until firstNoteId + size).map {
            NoteReference(
                it.toString(),
                title = "Content $it",
                type = "source",
                lastModifiedAt = System.currentTimeMillis() - 60000 * it
            )
        }
}
