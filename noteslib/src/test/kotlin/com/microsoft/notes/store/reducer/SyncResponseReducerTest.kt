package com.microsoft.notes.store.reducer

import com.microsoft.notes.models.Note
import com.microsoft.notes.models.RemoteData
import com.microsoft.notes.store.NotesList
import com.microsoft.notes.store.State
import com.microsoft.notes.store.UserState
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.addUserState
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.utils.logging.TestConstants
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class SyncResponseReducerTest {
    private val LOCAL_ID = "localId"
    private val TEST_USER_ID = "test@outlook.com"

    // TODO Add rest of the test cases to cover SyncResponseReducer.kt

    @Test
    fun should_permanently_delete_note() {
        val note = Note(localId = LOCAL_ID)
        assertThat(note.isDeleted, iz(false))
        val testUserState = UserState(
            notesList = NotesList.createNotesList(
                notesCollection = listOf(note),
                notesLoaded =
                false
            )
        )
        val state = State().addUserState(testUserState, TestConstants.TEST_USER_ID)
        assertThat(state.getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(1))

        val action = SyncResponseAction.PermanentlyDeleteNote(note.localId, TEST_USER_ID)
        val newState = SyncResponseReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).isEmpty(), iz(true))
            assert(noteLocalIDToUserIDMap.isEmpty())
        }
    }

    @Test
    fun should_merge_in_note() {
        val uiShadow = Note()
        val note = Note(localId = LOCAL_ID, uiShadow = uiShadow)
        val lastServerVersion = Note()
        val remoteData = RemoteData(
            id = "123", changeKey = "456",
            lastServerVersion = lastServerVersion, lastModifiedAt = 0, createdAt = 0
        )
        val remoteNote = Note(remoteData = remoteData)
        val testUserState = UserState(
            notesList = NotesList.createNotesList(
                notesCollection = listOf(note),
                notesLoaded =
                false
            )
        )
        val state = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        assertThat(state.getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(1))

        val action = SyncResponseAction.ApplyConflictResolution(
            note.localId,
            remoteNote,
            uiBaseRevision = 20,
            userID = TEST_USER_ID
        )

        val newState = SyncResponseReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            val actualNote = getNotesCollectionForUser(TestConstants.TEST_USER_ID).first()
            assertThat(actualNote.remoteData!!.changeKey, iz("456"))
            assertThat(actualNote.remoteData!!.lastServerVersion, iz(lastServerVersion))
            assertThat(actualNote.uiShadow, iz(uiShadow))
            assertThat(userIDForLocalNoteID(LOCAL_ID), iz(TestConstants.TEST_USER_ID))
        }
    }
}
