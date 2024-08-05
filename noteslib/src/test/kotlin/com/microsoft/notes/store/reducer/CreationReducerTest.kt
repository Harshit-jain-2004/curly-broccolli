package com.microsoft.notes.store.reducer

import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.mediaList
import com.microsoft.notes.richtext.scheme.mediaListCount
import com.microsoft.notes.richtext.scheme.size
import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.CreationAction
import com.microsoft.notes.store.getNoteForNoteLocalId
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.utils.logging.TestConstants
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class CreationReducerTest {

    private val LOCAL_ID = "LOCAL_ID"
    private val LOCAL_URL = "/1.jpg"

    @Test
    fun should_add_note_to_collection() {
        val note = Note()
        val action = CreationAction.AddNoteAction(note, TestConstants.TEST_USER_ID)
        val newState = CreationReducer.reduce(action, currentState = State(), isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(1))
            assertThat(getNoteForNoteLocalId(note.localId), iz(notNullValue()))
        }
    }

    @Test
    fun should_add_new_Note_with_Document() {
        val media = InlineMedia(localId = LOCAL_ID, localUrl = LOCAL_URL)
        val note = Note(localId = LOCAL_ID, document = Document(listOf(media)))

        val state = State()
        val action = CreationAction.AddNoteAction(note, TestConstants.TEST_USER_ID)
        val newState = CreationReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(1))

            fun verifyNote(noteToVerify: Note) {
                with(noteToVerify.document) {
                    assertThat(size(), iz(1))
                    assertThat(mediaListCount(), iz(1))
                    assertThat(mediaList().first(), iz(media))
                }
            }
            verifyNote(getNotesCollectionForUser(TestConstants.TEST_USER_ID).first())
        }
    }
}
