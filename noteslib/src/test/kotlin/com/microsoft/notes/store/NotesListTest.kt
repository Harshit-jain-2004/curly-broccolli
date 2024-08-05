package com.microsoft.notes.store

import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class NotesListTest {
    val NOTE_LOCAL_ID_1 = "localId1"

    val testNote1 = Note(localId = NOTE_LOCAL_ID_1, color = Color.BLUE)

    @Test
    fun should_get_note() {
        val testNotes: List<Note> = listOf(testNote1)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        assertThat(testNotesList.notesCollection, iz(listOf(testNote1)))
    }

    @Test
    fun should_get_empty_invalid_note() {
        val testNotes: List<Note> = listOf(testNote1)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        assertThat(testNotesList.notesCollection, iz(listOf(testNote1)))
    }
}
