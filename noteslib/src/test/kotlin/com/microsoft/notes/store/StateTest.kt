package com.microsoft.notes.store

import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.utils.logging.TestConstants
import com.microsoft.notes.utils.utils.Constants
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class StateTest {
    val NOTE_LOCAL_ID_1 = "localId1"
    val NOTE_LOCAL_ID_2 = "localId2"
    val NOTE_LOCAL_ID_3 = "localId3"
    val NOTE_LOCAL_ID_4 = "localId4"
    val NOTE_LOCAL_ID_5 = "localId5"
    val NOTE_LOCAL_ID_6 = "localId6"

    val testNote1 = Note(localId = NOTE_LOCAL_ID_1, color = Color.BLUE)
    val testNote2 = Note(localId = NOTE_LOCAL_ID_2, color = Color.YELLOW)
    val testNote3 = Note(localId = NOTE_LOCAL_ID_3, color = Color.CHARCOAL)
    val testNote4 = Note(localId = NOTE_LOCAL_ID_4, color = Color.PINK)
    val testNote5 = Note(localId = NOTE_LOCAL_ID_5, color = Color.PINK, localCreatedAt = 1, documentModifiedAt = 1)
    val testNote6 = Note(localId = NOTE_LOCAL_ID_6, color = Color.PINK, localCreatedAt = 2, documentModifiedAt = 2)

    @Test
    fun should_get_userId_for_note() {
        val testNotes: List<Note> = listOf(testNote1)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        assertThat(testState.userIDForLocalNoteID(localNoteID = NOTE_LOCAL_ID_1), iz(TestConstants.TEST_USER_ID))
        assertThat(testNotesList.notesCollection, iz(listOf(testNote1)))
    }

    @Test
    fun should_get_empty_userId_for_invalid_note() {
        val testNotes: List<Note> = listOf(testNote1)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )
        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        assertThat(testState.userIDForLocalNoteID(localNoteID = NOTE_LOCAL_ID_3), iz(Constants.EMPTY_USER_ID))
        assertThat(testNotesList.notesCollection, iz(listOf(testNote1)))
    }

    @Test
    fun should_add_note() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(userState = testUserState, userID = TestConstants.TEST_USER_ID)
        val newState = testState.addNotes(listOf(testNote3), TestConstants.TEST_USER_ID_2)

        with(newState) {
            assertThat(getNoteForNoteLocalId(testNote3.localId), iz(testNote3))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(testNotesList.notesCollection.size))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID_2).size, iz(1))
            assertThat(userIDForLocalNoteID(testNote3.localId), iz(TestConstants.TEST_USER_ID_2))
            assertThat(noteLocalIDToUserIDMap.size, iz(testNotesList.notesCollection.size + 1))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(listOf(testNote1, testNote2)))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID_2), iz(listOf(testNote3)))
        }
    }

    @Test
    fun should_add_multiple_notes() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )
        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(userState = testUserState, userID = TestConstants.TEST_USER_ID)

        val newState = testState.addNotes(listOf(testNote3, testNote4), TestConstants.TEST_USER_ID_2)

        with(newState) {
            assertThat(getNoteForNoteLocalId(testNote3.localId), iz(testNote3))
            assertThat(getNoteForNoteLocalId(testNote4.localId), iz(testNote4))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(testNotesList.notesCollection.size))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID_2).size, iz(2))
            assertThat(userIDForLocalNoteID(testNote3.localId), iz(TestConstants.TEST_USER_ID_2))
            assertThat(userIDForLocalNoteID(testNote4.localId), iz(TestConstants.TEST_USER_ID_2))
            assertThat(noteLocalIDToUserIDMap.size, iz(testNotesList.notesCollection.size + 2))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(listOf(testNote1, testNote2)))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID_2), iz(listOf(testNote3, testNote4)))
        }
    }

    @Test
    fun should_not_copy_on_add_no_notes() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )
        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(userState = testUserState, userID = TestConstants.TEST_USER_ID)

        val newState = testState.addNotes(emptyList(), TestConstants.TEST_USER_ID_2)
        assertThat(newState.getUserStateForUserID(TestConstants.TEST_USER_ID).notesList, iz(testNotesList))
        assertThat(newState.getNotesCollectionForUser(TestConstants.TEST_USER_ID_2), iz(emptyList()))
    }

    @Test
    fun should_not_copy_on_add_no_notes_distinct() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )
        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val newState = testState.addDistinctNotes(emptyList(), TestConstants.TEST_USER_ID_2)
        assertThat(newState.getUserStateForUserID(TestConstants.TEST_USER_ID).notesList, iz(testNotesList))
    }

    @Test
    fun should_delete_note() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val newState = testState.deleteNotes(listOf(testNote2), TestConstants.TEST_USER_ID)
        with(newState) {
            assert(getNoteForNoteLocalId(testNote2.localId) == null)
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(testNotesList.notesCollection.size - 1))
            assertThat(userIDForLocalNoteID(testNote2.localId), iz(Constants.EMPTY_USER_ID))
            assertThat(noteLocalIDToUserIDMap.size, iz(testNotesList.notesCollection.size - 1))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(listOf(testNote1)))
        }
    }

    @Test
    fun should_delete_multiple_notes() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val newState = testState.deleteNotes(listOf(testNote1, testNote2), TestConstants.TEST_USER_ID)

        with(newState) {
            assert(getNoteForNoteLocalId(testNote1.localId) == null)
            assert(getNoteForNoteLocalId(testNote2.localId) == null)
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(testNotesList.notesCollection.size - 2))
            assert(userIDForLocalNoteID(testNote1.localId) == Constants.EMPTY_USER_ID)
            assert(userIDForLocalNoteID(testNote2.localId) == Constants.EMPTY_USER_ID)
            assertThat(noteLocalIDToUserIDMap.size, iz(testNotesList.notesCollection.size - 2))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(listOf()))
        }
    }

    @Test
    fun should_not_delete_invalid_note() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesMap = mapOf(
            Pair(NOTE_LOCAL_ID_1, TestConstants.TEST_USER_ID),
            Pair(NOTE_LOCAL_ID_2, TestConstants.TEST_USER_ID)
        )
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val newState = testState.deleteNotes(listOf(testNote3), TestConstants.TEST_USER_ID)
        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(testNotesList.notesCollection.size))
            assertThat(noteLocalIDToUserIDMap.size, iz(testNotesList.notesCollection.size))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(listOf(testNote1, testNote2)))
        }
    }

    @Test
    fun should_not_copy_on_delete_no_notes() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val newState = testState.deleteNotes(emptyList(), TestConstants.TEST_USER_ID)

        assertThat(newState.getUserStateForUserID(TestConstants.TEST_USER_ID).notesList, iz(testNotesList))
    }

    @Test
    fun should_delete_note_by_id() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val newState = testState.deleteNoteByLocalId(testNote2.localId, TestConstants.TEST_USER_ID)
        with(newState) {
            assert(getNoteForNoteLocalId(testNote2.localId) == null)
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(testNotesList.notesCollection.size - 1))
            assertThat(userIDForLocalNoteID(testNote2.localId), iz(Constants.EMPTY_USER_ID))
            assertThat(noteLocalIDToUserIDMap.size, iz(testNotesList.notesCollection.size - 1))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(listOf(testNote1)))
        }
    }

    @Test
    fun should_delete_note_by_invalid_id() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val newState = testState.deleteNoteByLocalId(testNote3.localId, TestConstants.TEST_USER_ID)

        with(newState) {
            assert(getNoteForNoteLocalId(testNote3.localId) == null)
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(testNotesList.notesCollection.size))
            assertThat(noteLocalIDToUserIDMap.size, iz(testNotesList.notesCollection.size))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(listOf(testNote1, testNote2)))
        }
    }

    @Test
    fun should_not_copy_on_delete_no_notes_by_id() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val newState = testState.deleteNoteByLocalId("", TestConstants.TEST_USER_ID)

        assertThat(newState.getUserStateForUserID(TestConstants.TEST_USER_ID).notesList, iz(testNotesList))
    }

    @Test
    fun should_delete_all_notes() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val newState = testState.deleteAllNotes()
        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(0))
            assertThat(noteLocalIDToUserIDMap.size, iz(0))
            assertThat(getNotesLoadedForUser(TestConstants.TEST_USER_ID), iz(testNotesList.notesLoaded))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(listOf()))
        }
    }

    @Test
    fun should_delete_all_notes_for_user() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState2 = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val testNotes2: List<Note> = listOf(testNote3, testNote4)
        val testNotesList2 = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes2
        )

        val testUserState2 = UserState(notesList = testNotesList2)
        val testState = testState2.addUserState(testUserState2, TestConstants.TEST_USER_ID_2)

        val newState = testState.deleteAllNotesForUserID(TestConstants.TEST_USER_ID)
        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(0))
            assertThat(
                getNotesCollectionForUser(TestConstants.TEST_USER_ID_2).size,
                iz(
                    testNotesList2
                        .notesCollection.size
                )
            )
            assertThat(
                noteLocalIDToUserIDMap.size,
                iz(
                    testNotesList2
                        .notesCollection.size
                )
            )
            assertThat(getNotesLoadedForUser(TestConstants.TEST_USER_ID), iz(testNotesList.notesLoaded))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(listOf()))
        }
    }

    @Test
    fun should_migrate_all_notes_for_user() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState2 = State().addUserState(testUserState, Constants.EMPTY_USER_ID)

        val newState = testState2.withAuthenticationStateForUser(
            authState = AuthenticationState(authState = AuthState.AUTHENTICATED),
            userID = TestConstants.TEST_USER_ID_2
        )

        with(newState) {
            assertThat(getNotesCollectionForUser(Constants.EMPTY_USER_ID).size, iz(0))
            assertThat(
                getNotesCollectionForUser(TestConstants.TEST_USER_ID_2).size,
                iz(
                    testNotesList
                        .notesCollection.size
                )
            )
            assertThat(
                noteLocalIDToUserIDMap.size,
                iz(
                    testNotesList
                        .notesCollection.size
                )
            )
            assertThat(getNotesLoadedForUser(TestConstants.TEST_USER_ID_2), iz(testNotesList.notesLoaded))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID_2), iz(listOf(testNote1, testNote2)))
        }
    }

    @Test
    fun should_not_change_account_when_secondary_is_authenticated_and_primary_is_empty() {
        val state = State()
            .addUserState(UserState(), Constants.EMPTY_USER_ID)
            .addUserState(UserState(), "secondary")
            .copy(currentUserID = Constants.EMPTY_USER_ID)
        val newState = state.withAuthenticationStateForUser(authState = AuthenticationState(authState = AuthState.AUTHENTICATED), userID = "secondary")
        with(newState) {
            assertThat(currentUserID, iz(Constants.EMPTY_USER_ID))
        }
    }

    @Test
    fun should_use_empty_account_if_primary_account_is_unauthenticated() {
        val state = State()
            .addUserState(UserState(), "primary")
            .addUserState(UserState(), "secondary")
            .copy(currentUserID = "primary")
        val newState = state.withAuthenticationStateForUser(authState = AuthenticationState(authState = AuthState.UNAUTHENTICATED), userID = "primary")
        with(newState) {
            assertThat(currentUserID, iz(Constants.EMPTY_USER_ID))
        }
    }

    @Test
    fun should_keep_primary_account_if_secondary_account_is_unauthenticated() {
        val state = State()
            .addUserState(UserState(), "primary")
            .addUserState(UserState(), "secondary")
            .copy(currentUserID = "primary")
        val newState = state.withAuthenticationStateForUser(authState = AuthenticationState(authState = AuthState.UNAUTHENTICATED), userID = "secondary")
        with(newState) {
            assertThat(currentUserID, iz("primary"))
        }
    }

    @Test
    fun should_keep_primary_account_if_primary_account_is_unauthorized() {
        val state = State()
            .addUserState(UserState(), "primary")
            .addUserState(UserState(), "secondary")
            .copy(currentUserID = "primary")
        val newState = state.withAuthenticationStateForUser(authState = AuthenticationState(authState = AuthState.NOT_AUTHORIZED), userID = "primary")
        with(newState) {
            assertThat(currentUserID, iz("primary"))
        }
    }

    @Test
    fun should_keep_primary_account_if_secondary_account_is_unauthorized() {
        val state = State()
            .addUserState(UserState(), "primary")
            .addUserState(UserState(), "secondary")
            .copy(currentUserID = "primary")
        val newState = state.withAuthenticationStateForUser(authState = AuthenticationState(authState = AuthState.NOT_AUTHORIZED), userID = "secondary")
        with(newState) {
            assertThat(currentUserID, iz("primary"))
        }
    }

    @Test
    fun should_mark_note_as_deleted() {
        val noteToBeDeleted = testNote1.copy(isDeleted = false)
        val testNotesList = NotesList.emptyNotesList()

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID).addNotes(
            listOf
            (noteToBeDeleted),
            TestConstants.TEST_USER_ID
        )

        val newState = testState.markNoteWithLocalIdAsDeleted(noteToBeDeleted.localId)
        with(newState) {
            assertThat(noteLocalIDToUserIDMap, iz(testState.noteLocalIDToUserIDMap))
            assertThat(getNoteForNoteLocalId(noteToBeDeleted.localId)?.isDeleted, iz(true))
        }
    }

    @Test
    fun should_unmark_note_as_deleted() {
        val noteToBeDeleted = testNote1.copy(isDeleted = true)
        val testNotesList = NotesList.emptyNotesList()
        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)
            .addNotes(
                listOf
                (noteToBeDeleted),
                TestConstants.TEST_USER_ID
            )
        val newState = testState.unmarkNoteWithLocalIdAsDeleted(noteToBeDeleted.localId)
        with(newState) {
            assertThat(noteLocalIDToUserIDMap, iz(testState.noteLocalIDToUserIDMap))
            assertThat(getNoteForNoteLocalId(noteToBeDeleted.localId)?.isDeleted, iz(false))
        }
    }

    @Test
    fun should_replace_note() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesMap = mapOf(
            Pair(NOTE_LOCAL_ID_1, TestConstants.TEST_USER_ID),
            Pair(NOTE_LOCAL_ID_2, TestConstants.TEST_USER_ID)
        )
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val replacementNote = testNote1.copy(color = Color.GREEN)
        val newState = testState.replaceNote(replacementNote)
        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(listOf(replacementNote, testNote2)))
            assertThat(getNoteForNoteLocalId(NOTE_LOCAL_ID_1)?.color, iz(Color.GREEN))
            assertThat(noteLocalIDToUserIDMap, iz(testNotesMap))
        }
    }

    @Test
    fun should_replace_notes() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesMap = mapOf(
            Pair(NOTE_LOCAL_ID_1, TestConstants.TEST_USER_ID),
            Pair(NOTE_LOCAL_ID_2, TestConstants.TEST_USER_ID)
        )
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val replacementNote1 = testNote1.copy(color = Color.GREEN)
        val replacementNote2 = testNote2.copy(color = Color.CHARCOAL)
        val newState = testState.replaceNotes(listOf(replacementNote1, replacementNote2), userID = TestConstants.TEST_USER_ID)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(listOf(replacementNote1, replacementNote2)))
            assertThat(getNoteForNoteLocalId(NOTE_LOCAL_ID_1)?.color, iz(Color.GREEN))
            assertThat(getNoteForNoteLocalId(NOTE_LOCAL_ID_2)?.color, iz(Color.CHARCOAL))
            assertThat(noteLocalIDToUserIDMap, iz(testNotesMap))
        }
    }

    @Test
    fun should_not_copy_on_replace_no_notes() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesMap = mapOf(
            Pair(NOTE_LOCAL_ID_1, TestConstants.TEST_USER_ID),
            Pair(NOTE_LOCAL_ID_2, TestConstants.TEST_USER_ID)
        )
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val newState = testState.replaceNotes(emptyList(), userID = TestConstants.TEST_USER_ID)
        assertThat(newState.getUserStateForUserID(TestConstants.TEST_USER_ID).notesList, iz(testNotesList))
    }

    @Test
    fun should_not_replace_invalid_note() {
        val testNotes: List<Note> = listOf(testNote1, testNote2)
        val testNotesMap = mapOf(
            Pair(NOTE_LOCAL_ID_1, TestConstants.TEST_USER_ID),
            Pair(NOTE_LOCAL_ID_2, TestConstants.TEST_USER_ID)
        )
        val testNotesList = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes
        )

        val testUserState = UserState(notesList = testNotesList)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val replacementNote = testNote3.copy(color = Color.GREEN)
        val newState = testState.replaceNote(replacementNote)
        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID), iz(listOf(testNote1, testNote2)))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(testNotesList.notesCollection.size))
            assertThat(noteLocalIDToUserIDMap, iz(testState.noteLocalIDToUserIDMap))
        }
    }

    @Test
    fun should_return_combined_notes() {
        val testNotes1: List<Note> = listOf(testNote5)
        val testNotes2: List<Note> = listOf(testNote6)
        val testNotesList1 = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes1
        )
        val testNotesList2 = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes2
        )

        val testUserState = UserState(notesList = testNotesList1)
        val testUserState2 = UserState(notesList = testNotesList2)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)
            .addUserState(testUserState2, TestConstants.TEST_USER_ID_2)

        val combinedNotes = testState.combinedNotesForAllUsers()
        assertThat(combinedNotes.size, iz(2))
        assertThat(combinedNotes.contains(testNote5), iz(true))
        assertThat(combinedNotes.contains(testNote6), iz(true))
        assertThat("List should be sorted by documentModifiedAt", combinedNotes[0], iz(testNote6))
    }

    @Test
    fun should_return_notes_loaded_for_any_user() {
        val testNotes1: List<Note> = listOf(testNote1)
        val testNotes2: List<Note> = listOf(testNote2)
        val testNotesList1 = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes1
        )
        val testNotesList2 = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes2
        )

        val testUserState = UserState(notesList = testNotesList1)
        val testUserState2 = UserState(notesList = testNotesList2)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)
            .addUserState(testUserState2, TestConstants.TEST_USER_ID_2)

        val notesLoaded = testState.areNotesLoadedForAnyUser()
        assertThat(notesLoaded, iz(false))

        val testNotes3 = listOf(testNote3)
        val testNotesList3 = NotesList.createNotesList(
            notesLoaded = true,
            notesCollection = testNotes3
        )
        val testUserState3 = UserState(notesList = testNotesList3)

        val testState2 = testState.addUserState(testUserState3, TestConstants.TEST_USER_ID_3)

        val notesLoaded2 = testState2.areNotesLoadedForAnyUser()
        assertThat(notesLoaded2, iz(true))
    }

    @Test
    fun should_return_combined_notes_list() {
        val testNotes1: List<Note> = listOf(testNote5)
        val testNotes2: List<Note> = listOf(testNote6)
        val testNotesList1 = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes1
        )
        val testNotesList2 = NotesList.createNotesList(
            notesLoaded = false,
            notesCollection = testNotes2
        )

        val testUserState = UserState(notesList = testNotesList1)
        val testUserState2 = UserState(notesList = testNotesList2)
        val testState = State().addUserState(testUserState, TestConstants.TEST_USER_ID)
            .addUserState(testUserState2, TestConstants.TEST_USER_ID_2)

        val combinedNotesList = testState.combinedNotesListForAllUsers()
        assertThat(combinedNotesList.notesCollection.size, iz(2))
        assertThat(combinedNotesList.notesCollection.contains(testNote5), iz(true))
        assertThat(combinedNotesList.notesCollection.contains(testNote6), iz(true))
        assertThat(
            "List should be sorted by documentModifiedAt",
            combinedNotesList.notesCollection[0], iz(testNote6)
        )
        assertThat(combinedNotesList.notesLoaded, iz(false))
    }
}
