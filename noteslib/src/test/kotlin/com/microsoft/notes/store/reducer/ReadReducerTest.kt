package com.microsoft.notes.store.reducer

import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.store.NotesList
import com.microsoft.notes.store.State
import com.microsoft.notes.store.UserState
import com.microsoft.notes.store.action.ReadAction
import com.microsoft.notes.store.action.ReadAction.FetchAllNotesAction
import com.microsoft.notes.store.addUserState
import com.microsoft.notes.store.getNoteReferencesCollectionForUser
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.utils.logging.TestConstants
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ReadReducerTest {

    @Test
    fun `should fetch all Notes`() {
        val notesCollection = createDummyNotesCollection()
        val noteLocalIDToUserIDMap = createNoteLocalIdToUserIDMap(notesCollection, TestConstants.TEST_USER_ID)
        val testUserState = UserState(notesList = NotesList.createNotesList(notesCollection, false))
        val state = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val newState = ReadReducer.reduce(
            FetchAllNotesAction(TestConstants.TEST_USER_ID, false), currentState = state,
            isDebugMode =
            true
        )

        with(state.getNotesCollectionForUser(TestConstants.TEST_USER_ID)) {
            assertThat(size, iz(newState.getNotesCollectionForUser(TestConstants.TEST_USER_ID).size))
            assertThat(this, iz(newState.getNotesCollectionForUser(TestConstants.TEST_USER_ID)))
        }
        assertThat(noteLocalIDToUserIDMap, iz(newState.noteLocalIDToUserIDMap))
    }

    @Test
    fun `should fetch all Notes and check scroll has changed correctly`() {
        val notesCollection = createDummyNotesCollection()
        val noteLocalIDToUserIDMap = createNoteLocalIdToUserIDMap(notesCollection, TestConstants.TEST_USER_ID)
        val testUserState = UserState(notesList = NotesList.createNotesList(notesCollection, false))
        val state = State().addUserState(testUserState, TestConstants.TEST_USER_ID)
        val newState = ReadReducer.reduce(
            FetchAllNotesAction(TestConstants.TEST_USER_ID, false), currentState = state,
            isDebugMode =
            true
        )

        with(state.getNotesCollectionForUser(TestConstants.TEST_USER_ID)) {
            assertThat(size, iz(newState.getNotesCollectionForUser(TestConstants.TEST_USER_ID).size))
            assertThat(this, iz(newState.getNotesCollectionForUser(TestConstants.TEST_USER_ID)))
        }
        assertThat(noteLocalIDToUserIDMap, iz(newState.noteLocalIDToUserIDMap))
    }

    @Test
    fun `should load notes`() {
        val notes = dummyNotesCollection()
        val noteLocalIDToUserIDMap = createNoteLocalIdToUserIDMap(notes, TestConstants.TEST_USER_ID)
        val action = ReadAction.NotesLoadedAction(
            allNotesLoaded = true, notesCollection = notes, samsungNotesCollection = emptyList(),
            noteReferencesCollection = emptyList(), meetingNotesCollection = emptyList(), userID = TestConstants.TEST_USER_ID
        )
        val newState = ReadReducer.reduce(action, currentState = State(), isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(notes))
            assertThat(noteLocalIDToUserIDMap, iz(noteLocalIDToUserIDMap))
        }
    }

    @Test
    fun `should load note references`() {
        val noteReferences = createDummyNoteReferences()
        val noteReferenceLocalIDTOUserIDMap = createNoteReferenceLocalIdToUserIDMap(noteReferences, TestConstants.TEST_USER_ID)
        val action = ReadAction.NotesLoadedAction(
            allNotesLoaded = true, notesCollection = emptyList(), samsungNotesCollection = emptyList(),
            noteReferencesCollection = noteReferences, meetingNotesCollection = emptyList(), userID = TestConstants.TEST_USER_ID
        )
        val newState = ReadReducer.reduce(action, currentState = State(), isDebugMode = true)

        with(newState) {
            assertThat(getNoteReferencesCollectionForUser(TestConstants.TEST_USER_ID), iz(noteReferences))
            assertThat(noteReferenceLocalIDToUserIDMap, iz(noteReferenceLocalIDTOUserIDMap))
        }
    }

    @Test
    fun `loaded notes should be distinct by localId`() {
        val initialNotes = dummyNotesCollection()
        val initialAction = ReadAction.NotesLoadedAction(
            allNotesLoaded = true, notesCollection = initialNotes,
            noteReferencesCollection = emptyList(), meetingNotesCollection = emptyList(), samsungNotesCollection = emptyList(), userID = TestConstants.TEST_USER_ID
        )
        val initialState = ReadReducer.reduce(initialAction, currentState = State(), isDebugMode = true)

        val newNotes = listOf(
            initialNotes[0].copy(color = Color.GREY),
            initialNotes[1].copy(color = Color.GREY)
        )

        val noteLocalIDToUserIDMap = createNoteLocalIdToUserIDMap(newNotes, TestConstants.TEST_USER_ID)
        val newAction = ReadAction.NotesLoadedAction(
            allNotesLoaded = true, notesCollection = newNotes, samsungNotesCollection = emptyList(),
            noteReferencesCollection = emptyList(), meetingNotesCollection = emptyList(), userID = TestConstants.TEST_USER_ID
        )
        val newState = ReadReducer.reduce(newAction, currentState = initialState, isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(2))
            assertThat(noteLocalIDToUserIDMap, iz(noteLocalIDToUserIDMap))
        }
    }

    private fun createDummyNotesCollection(): List<Note> {
        val note1 = Note(
            localId = "LOCAL_ID1",
            color = Color.YELLOW
        )
        val note2 = Note(
            localId = "LOCAL_ID2",
            document = Document(listOf(InlineMedia(localId = "PHOTO1", localUrl = "/1.jpg"))),
            color = Color.BLUE
        )
        return listOf(note1, note2)
    }

    private fun createDummyNoteReferences(): List<NoteReference> =
        listOf(
            NoteReference(
                localId = "LOCAL_ID_EX1",
                type = "OneNote",
                title = "Test Note 1",
                lastModifiedAt = 2L
            ),
            NoteReference(
                localId = "LOCAL_ID_EX2",
                type = "OneNote",
                title = "Test Note 2",
                lastModifiedAt = 1L
            )
        )
}
