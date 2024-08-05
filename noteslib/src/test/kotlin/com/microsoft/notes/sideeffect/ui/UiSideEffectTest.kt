package com.microsoft.notes.sideeffect.ui

import com.microsoft.notes.models.AccountType
import com.microsoft.notes.models.Note
import com.microsoft.notes.store.AuthState
import com.microsoft.notes.store.State
import com.microsoft.notes.store.Store
import com.microsoft.notes.store.action.AuthAction
import com.microsoft.notes.store.action.CreationAction
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.addNotes
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.store.getNotesLoadedForUser
import com.microsoft.notes.store.reducer.AuthReducer
import com.microsoft.notes.store.withNotesLoaded
import com.microsoft.notes.utils.logging.TestConstants
import com.microsoft.notes.utils.utils.Constants
import com.microsoft.notes.utils.utils.RoutingPrefix
import com.microsoft.notes.utils.utils.UserInfo
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.hamcrest.CoreMatchers.`is` as iz

class UiSideEffectTest {
    private val LOCAL_ID = "localId"
    @Mock
    lateinit var store: Store

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `should handle uiBindings correctly`() {
        val uiSideEffect = UiSideEffect(store)
        val uiBindings1 = object : NoteChanges, AuthChanges {
            override fun noteDeleted() {}
            override fun authChanged(auth: AuthState, userID: String) {}
            override fun notesUpdated(stickyNotesByUsers: HashMap<String, List<Note>>, notesLoaded: Boolean) {}
            override fun accountInfoForIntuneProtection(databaseName: String, userID: String, accountType: AccountType) {}
            override fun onRequestClientAuth(userID: String) {}
        }
        val uiBindings2 = object : NoteChanges, AuthChanges {
            override fun noteDeleted() {}
            override fun authChanged(auth: AuthState, userID: String) {}
            override fun notesUpdated(stickyNotesByUsers: HashMap<String, List<Note>>, notesLoaded: Boolean) {}
            override fun accountInfoForIntuneProtection(databaseName: String, userID: String, accountType: AccountType) {}
            override fun onRequestClientAuth(userID: String) {}
        }
        assertThat(uiSideEffect.addUiBindings(uiBindings1), iz(true))
        assertThat(uiSideEffect.addUiBindings(uiBindings2), iz(true))

        assertThat(uiSideEffect.removeUiBindings(uiBindings2), iz(true))
        assertThat(uiSideEffect.removeUiBindings(uiBindings1), iz(true))
        assertThat(
            uiSideEffect.removeUiBindings(object : NoteChanges, AuthChanges {
                override fun noteDeleted() {}
                override fun notesUpdated(stickyNotesByUsers: HashMap<String, List<Note>>, notesLoaded: Boolean) {}
                override fun authChanged(auth: AuthState, userID: String) {}
                override fun accountInfoForIntuneProtection(databaseName: String, userID: String, accountType: AccountType) {}

                override fun onRequestClientAuth(userID: String) {}
            }),
            iz(false)
        )
    }

    @Test
    fun `should handle AuthAction-NewAuthTokenAction correctly`() {
        val uiSideEffect = UiSideEffect(store)
        val uiBindings = object : NoteChanges, AuthChanges {
            override fun noteDeleted() {}
            override fun notesUpdated(stickyNotesByUsers: HashMap<String, List<Note>>, notesLoaded: Boolean) {}
            override fun authChanged(auth: AuthState, userID: String) {}
            override fun accountInfoForIntuneProtection(databaseName: String, userID: String, accountType: AccountType) {}
            override fun onRequestClientAuth(userID: String) {}
        }
        val uiBindingsSpy = spy<AuthChanges>(uiBindings)
        uiSideEffect.addUiBindings(uiBindingsSpy)
        val userInfo = UserInfo(
            userID = TestConstants.TEST_USER_ID,
            accessToken = TestConstants.TEST_TOKEN_USER_ID,
            accountType = AccountType.ADAL,
            userInfoSuffix = Constants.EMPTY_USER_INFO_SUFFIX,
            email = Constants.EMPTY_EMAIL,
            routingPrefix = RoutingPrefix.Unprefixed,
            tenantID = Constants.EMPTY_TENANT_ID
        )
        val newAuthTokenAction = AuthAction.NewAuthTokenAction(userInfo)
        val state = AuthReducer.reduce(action = newAuthTokenAction, currentState = State(), isDebugMode = true)
        uiSideEffect.handle(newAuthTokenAction, state)
        verify(uiBindingsSpy).authChanged(AuthState.AUTHENTICATED, userID = userInfo.userID)
    }

    @Test
    fun `should handle AuthAction-AccountInfoForIntuneProtection correctly`() {
        val uiSideEffect = UiSideEffect(store)
        val uiBindings = object : NoteChanges, AuthChanges {
            override fun noteDeleted() {}
            override fun notesUpdated(stickyNotesByUsers: HashMap<String, List<Note>>, notesLoaded: Boolean) {}
            override fun authChanged(auth: AuthState, userID: String) {}
            override fun accountInfoForIntuneProtection(databaseName: String, userID: String, accountType: AccountType) {}
            override fun onRequestClientAuth(userID: String) {}
        }
        val uiBindingsSpy = spy<AuthChanges>(uiBindings)
        uiSideEffect.addUiBindings(uiBindingsSpy)
        val userInfo = UserInfo(
            userID = TestConstants.TEST_USER_ID,
            accessToken = TestConstants.TEST_TOKEN_USER_ID,
            accountType = AccountType.ADAL,
            userInfoSuffix = Constants.EMPTY_USER_INFO_SUFFIX,
            email = Constants.EMPTY_EMAIL,
            routingPrefix = RoutingPrefix.Unprefixed,
            tenantID = Constants.EMPTY_TENANT_ID
        )
        val accountInfoForIntuneProtection = AuthAction.AccountInfoForIntuneProtection("", userInfo.userID, userInfo.accountType)
        uiSideEffect.handle(accountInfoForIntuneProtection, State())
        verify(uiBindingsSpy).accountInfoForIntuneProtection(
            "", userID = TestConstants.TEST_USER_ID,
            accountType = AccountType.ADAL
        )
    }

    @Test
    fun `should handle AuthAction-LogoutAction correctly`() {
        val uiSideEffect = UiSideEffect(store)
        val uiBindings = object : NoteChanges, AuthChanges {
            override fun noteDeleted() {}
            override fun notesUpdated(stickyNotesByUsers: HashMap<String, List<Note>>, notesLoaded: Boolean) {}
            override fun authChanged(auth: AuthState, userID: String) {}
            override fun accountInfoForIntuneProtection(databaseName: String, userID: String, accountType: AccountType) {}
            override fun onRequestClientAuth(userID: String) {}
        }
        val uiBindingsSpy = spy<AuthChanges>(uiBindings)
        uiSideEffect.addUiBindings(uiBindingsSpy)
        val logoutAction = AuthAction.LogoutAction(userID = TestConstants.TEST_USER_ID)
        val state = AuthReducer.reduce(action = logoutAction, currentState = State(), isDebugMode = true)
        uiSideEffect.handle(logoutAction, state)
        verify(uiBindingsSpy).authChanged(AuthState.UNAUTHENTICATED, userID = TestConstants.TEST_USER_ID)
    }

    @Test
    fun `should handle SyncResponseAction-NotAuthorized correctly`() {
        val uiSideEffect = UiSideEffect(store)
        val uiBindings = object : NoteChanges, AuthChanges {
            override fun noteDeleted() {}
            override fun notesUpdated(stickyNotesByUsers: HashMap<String, List<Note>>, notesLoaded: Boolean) {}
            override fun authChanged(auth: AuthState, userID: String) {}
            override fun accountInfoForIntuneProtection(databaseName: String, userID: String, accountType: AccountType) {}
            override fun onRequestClientAuth(userID: String) {}
        }
        val uiBindingsSpy = spy<AuthChanges>(uiBindings)
        uiSideEffect.addUiBindings(uiBindingsSpy)
        val notAuthorizedAction = SyncResponseAction.NotAuthorized(userID = TestConstants.TEST_USER_ID)
        uiSideEffect.handle(notAuthorizedAction, State())
        verify(uiBindingsSpy).authChanged(AuthState.NOT_AUTHORIZED, userID = TestConstants.TEST_USER_ID)
    }

    @Test
    fun `should handle any other action correctly`() {
        val uiSideEffect = UiSideEffect(store)
        val uiBindings = object : NoteChanges, AuthChanges {
            override fun noteDeleted() {}
            override fun notesUpdated(stickyNotesByUsers: HashMap<String, List<Note>>, notesLoaded: Boolean) {}
            override fun authChanged(auth: AuthState, userID: String) {}
            override fun accountInfoForIntuneProtection(databaseName: String, userID: String, accountType: AccountType) {}
            override fun onRequestClientAuth(userID: String) {}
        }
        val uiBindingsSpy = spy<NoteChanges>(uiBindings)
        uiSideEffect.addUiBindings(uiBindingsSpy)

        val note = Note(localId = LOCAL_ID)
        val state = State(currentUserID = TestConstants.TEST_USER_ID).withNotesLoaded(
            false,
            TestConstants.TEST_USER_ID
        )
            .addNotes(notes = listOf(note), userID = TestConstants.TEST_USER_ID)
        val createNoteAction = CreationAction.AddNoteAction(note, TestConstants.TEST_USER_ID)
        uiSideEffect.handle(createNoteAction, state)

        verify(uiBindingsSpy).notesUpdated(stickyNotesCollectionsByUser = hashMapOf(TestConstants.TEST_USER_ID to state.getNotesCollectionForUser(TestConstants.TEST_USER_ID)), notesLoaded = state.getNotesLoadedForUser(TestConstants.TEST_USER_ID))
        assertThat(state.userIDForLocalNoteID(LOCAL_ID), iz(TestConstants.TEST_USER_ID))
    }

    @Test
    fun `should use given state instead the one is currently in the store`() {
        val oldState = State(currentUserID = TestConstants.TEST_USER_ID)
        val store = Store(createdByApp = "app", isDebugMode = true)
        @Suppress("DEPRECATION")
        store.dispatch(oldState)

        val uiSideEffect = UiSideEffect(store)
        val uiBindings = object : NoteChanges, AuthChanges {
            override fun noteDeleted() {}
            override fun notesUpdated(stickyNotesByUsers: HashMap<String, List<Note>>, notesLoaded: Boolean) {}
            override fun authChanged(auth: AuthState, userID: String) {}
            override fun accountInfoForIntuneProtection(databaseName: String, userID: String, accountType: AccountType) {}
            override fun onRequestClientAuth(userID: String) {}
        }
        val uiBindingsSpy = spy<NoteChanges>(uiBindings)
        uiSideEffect.addUiBindings(uiBindingsSpy)

        val note = Note(localId = LOCAL_ID)
        val newState = oldState.withNotesLoaded(true, TestConstants.TEST_USER_ID)
            .addNotes(listOf(note), userID = TestConstants.TEST_USER_ID)
        val createNoteAction = CreationAction.AddNoteAction(note, TestConstants.TEST_USER_ID)
        uiSideEffect.handle(createNoteAction, newState)

        // This is to check that in this test, the state hasn't change. In real we couldn't have this
        // scenario since we always update the state first in our reducers, just added here to see that
        // are different instances.
        assertThat(store.state, iz(not(newState)))
        assertThat(store.state, iz(oldState))

        verify(uiBindingsSpy).notesUpdated(stickyNotesCollectionsByUser = hashMapOf(TestConstants.TEST_USER_ID to newState.getNotesCollectionForUser(TestConstants.TEST_USER_ID)), notesLoaded = newState.getNotesLoadedForUser(TestConstants.TEST_USER_ID))
        assertThat(newState.userIDForLocalNoteID(LOCAL_ID), iz(TestConstants.TEST_USER_ID))
    }
}
