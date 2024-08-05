package com.microsoft.notes.store.reducer

import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.DocumentType
import com.microsoft.notes.store.NotesList
import com.microsoft.notes.store.State
import com.microsoft.notes.store.SyncErrorState
import com.microsoft.notes.store.UserState
import com.microsoft.notes.store.action.UIAction
import com.microsoft.notes.store.addUserState
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.store.getUserStateForUserID
import com.microsoft.notes.store.withNotesLoaded
import com.microsoft.notes.ui.noteslist.FutureNoteUserNotification
import com.microsoft.notes.ui.noteslist.UserNotificationType
import com.microsoft.notes.ui.noteslist.UserNotifications
import com.microsoft.notes.utils.logging.TestConstants
import com.microsoft.notes.utils.utils.Constants
import org.junit.Assert
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class UIReducerTests {
    val NOTE_LOCAL_ID_1 = "localId1"
    val NOTE_LOCAL_ID_2 = "localId2"

    val testNote = Note(localId = NOTE_LOCAL_ID_1, color = Color.BLUE)
    val testFutureNote = Note(
        localId = NOTE_LOCAL_ID_2, color = Color.YELLOW,
        document = Document(type = DocumentType.FUTURE)
    )

    @Test
    fun should_change_current_user_id_on_account_changed_action() {
        val state = State().withNotesLoaded(false, TestConstants.TEST_USER_ID)
        val action = UIAction.AccountChanged(
            userID = TestConstants.TEST_USER_ID_2
        )
        val newState = UIReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            Assert.assertThat(currentUserID, iz(TestConstants.TEST_USER_ID_2))
        }
    }

    @Test
    fun should_not_change_current_user_id_on_non_account_changed_action() {
        val state = State().withNotesLoaded(false, TestConstants.TEST_USER_ID)
        val action = UIAction.SwipeToRefreshStarted()
        val newState = UIReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            Assert.assertThat(currentUserID, iz(Constants.EMPTY_USER_ID))
        }
    }

    @Test
    fun should_update_current_user_id_on_update_current_user_id_action() {
        val state = State().withNotesLoaded(false, TestConstants.TEST_USER_ID)
        val action = UIAction.UpdateCurrentUserID(TestConstants.TEST_USER_ID_2)
        val newState = UIReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            Assert.assertThat(currentUserID, iz(TestConstants.TEST_USER_ID_2))
        }
    }

    @Test
    fun should_update_future_note_user_notification_on_action() {
        val testUserState = UserState(
            notesList = NotesList.createNotesList(
                notesCollection = listOf(testFutureNote),
                notesLoaded = false
            )
        )
        val state = State().addUserState(testUserState, TestConstants.TEST_USER_ID)
        val action = UIAction.UpdateFutureNoteUserNotification(
            state.getNotesCollectionForUser(
                TestConstants
                    .TEST_USER_ID
            ),
            TestConstants.TEST_USER_ID
        )
        val newState = UIReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState.getUserStateForUserID(TestConstants.TEST_USER_ID)) {
            Assert.assertThat(userNotifications.userNotifications.size, iz(1))
            Assert.assertThat(userNotifications.get()?.type, iz(UserNotificationType.FutureNote))
        }
    }

    @Test
    fun should_not_update_future_note_user_notification_on_action() {
        val testSyncErrorState = SyncErrorState.GenericError
        val testUserState = UserState(
            notesList = NotesList.createNotesList(
                notesCollection = listOf(testNote),
                notesLoaded = false
            ),
            currentSyncErrorState = testSyncErrorState
        )
        val state = State().addUserState(testUserState, TestConstants.TEST_USER_ID)
        val action = UIAction.UpdateFutureNoteUserNotification(
            state.getNotesCollectionForUser(
                TestConstants
                    .TEST_USER_ID
            ),
            TestConstants.TEST_USER_ID
        )
        val newState = UIReducer.reduce(action, currentState = state, isDebugMode = true)
        Assert.assertThat(newState.getUserStateForUserID(TestConstants.TEST_USER_ID).userNotifications.userNotifications.size, iz(0))
        Assert.assertTrue(newState.getUserStateForUserID(TestConstants.TEST_USER_ID).currentSyncErrorState is SyncErrorState.GenericError)
    }

    @Test
    fun should_clear_future_note_user_notification_on_action() {
        val testSyncErrorState = SyncErrorState.GenericError
        val testUserState = UserState(
            notesList = NotesList.createNotesList(
                notesCollection = listOf(testNote),
                notesLoaded = false
            ),
            currentSyncErrorState = testSyncErrorState,
            userNotifications = UserNotifications().with(FutureNoteUserNotification(futureNoteCount = 1))
        )
        val state = State().addUserState(testUserState, TestConstants.TEST_USER_ID)
        val action = UIAction.UpdateFutureNoteUserNotification(
            state.getNotesCollectionForUser(
                TestConstants
                    .TEST_USER_ID
            ),
            TestConstants.TEST_USER_ID
        )
        val newState = UIReducer.reduce(action, currentState = state, isDebugMode = true)
        Assert.assertThat(newState.getUserStateForUserID(TestConstants.TEST_USER_ID).userNotifications.userNotifications.size, iz(0))
        Assert.assertTrue(newState.getUserStateForUserID(TestConstants.TEST_USER_ID).currentSyncErrorState is SyncErrorState.GenericError)
    }
}
