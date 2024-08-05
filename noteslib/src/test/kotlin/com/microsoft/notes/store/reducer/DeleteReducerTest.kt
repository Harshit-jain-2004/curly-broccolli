package com.microsoft.notes.store.reducer

import com.microsoft.notes.models.Note
import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.DeleteAction
import com.microsoft.notes.store.action.DeleteAction.DeleteAllNotesAction
import com.microsoft.notes.store.addNotes
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.store.withNotesLoaded
import com.microsoft.notes.utils.logging.TestConstants
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class DeleteReducerTest {
    private val LOCAL_ID = "LOCAL_ID"

    @Test
    fun should_delete_all_Notes() {
        val action = DeleteAllNotesAction(TestConstants.TEST_USER_ID)
        val newState = DeleteReducer.reduce(action, currentState = State(), isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).isEmpty(), iz(true))
            assertThat(noteLocalIDToUserIDMap.isEmpty(), iz(true))
        }
    }

    @Test
    fun should_mark_note_as_deleted() {
        val newLocalID = LOCAL_ID + 1
        val note = Note(localId = newLocalID)
        assertThat(note.isDeleted, iz(false))

        val state = State().withNotesLoaded(false, TestConstants.TEST_USER_ID).addNotes(listOf(note), TestConstants.TEST_USER_ID)
        val action = DeleteAction.MarkNoteAsDeletedAction(note.localId, null, TestConstants.TEST_USER_ID)
        val newState = DeleteReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).first().isDeleted, iz(true))
            assertThat(userIDForLocalNoteID(newLocalID), iz(TestConstants.TEST_USER_ID))
        }
    }

    @Test
    fun should_unmark_note_as_deleted() {
        val note = Note(localId = LOCAL_ID, isDeleted = true)
        assertThat(note.isDeleted, iz(true))

        val state = State().withNotesLoaded(false, TestConstants.TEST_USER_ID).addNotes(listOf(note), TestConstants.TEST_USER_ID)
        val action = DeleteAction.UnmarkNoteAsDeletedAction(note.localId, null, TestConstants.TEST_USER_ID)
        val newState = DeleteReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).first().isDeleted, iz(false))
            assertThat(userIDForLocalNoteID(LOCAL_ID), iz(TestConstants.TEST_USER_ID))
        }
    }
}
