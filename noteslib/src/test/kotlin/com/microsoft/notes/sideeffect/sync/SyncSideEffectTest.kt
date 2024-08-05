package com.microsoft.notes.sideeffect.sync

import android.content.Context
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.ExperimentFeatureFlags
import com.microsoft.notes.store.SideEffect
import com.microsoft.notes.store.State
import com.microsoft.notes.store.StateHandler
import com.microsoft.notes.store.Store
import com.microsoft.notes.store.action.AuthenticatedSyncRequestAction
import com.microsoft.notes.store.action.AutoDiscoverAction
import com.microsoft.notes.store.action.SyncRequestAction
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.withNotesLoaded
import com.microsoft.notes.sync.AutoDiscoverCache
import com.microsoft.notes.sync.AutoDiscoverCallManager
import com.microsoft.notes.sync.NotesClientHost
import com.microsoft.notes.utils.logging.TestConstants
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.concurrent.CopyOnWriteArrayList
import org.mockito.Mockito.`when` as testWhen

class SyncSideEffectTest {
    val NOTE_LOCAL_ID_1 = "localId1"
    val NOTE_LOCAL_ID_2 = "localId2"

    val testNote1 = Note(localId = NOTE_LOCAL_ID_1, color = Color.BLUE)
    val testNote2 = Note(localId = NOTE_LOCAL_ID_2, color = Color.YELLOW)

    private val CREATED_BY_APP = "Test"

    val sideEffects: CopyOnWriteArrayList<SideEffect> = CopyOnWriteArrayList()
    val stateHandlers: CopyOnWriteArrayList<StateHandler> = CopyOnWriteArrayList()

    private lateinit var store: Store

    @Mock
    val mockSyncHandlerManager = mock<SyncHandlerManager> {}

    @Mock
    val mockAutoDiscoverCallManager = mock<AutoDiscoverCallManager> {}

    @Mock
    val mockAutoDiscoverCache = mock<AutoDiscoverCache>()

    @Mock
    val mockUserSyncHandler1 = mock<UserSyncHandler> {}

    @Mock
    val mockUserSyncHandler2 = mock<UserSyncHandler> {}

    @Mock
    val mockContext = mock<Context> {}

    lateinit var syncSideEffect: SyncSideEffect

    @Before
    fun setup() {
        store = object : Store(
            sideEffects = sideEffects, stateHandlers = stateHandlers,
            isDebugMode = true,
            createdByApp = CREATED_BY_APP
        ) {}

        syncSideEffect = SyncSideEffect(
            context = mockContext,
            store = store,
            experimentFeatureFlags = ExperimentFeatureFlags(),
            syncHandlerManager = mockSyncHandlerManager,
            autoDiscoverCallManager = mockAutoDiscoverCallManager,
            autoDiscoverCache = mockAutoDiscoverCache
        )
        sideEffects.add(syncSideEffect)
    }

    @Test
    fun should_handle_multi_user_auth_sync_request_through_different_sync_handler_multi_account_enabled() {
        val testSyncState = SyncState(
            auth = AuthState.AADAuthenticated(), deltaTokensLoaded = true,
            apiHostInitialized = false, pauseSync = {}
        )
        setupMockWithMultiAccountEnabled(testSyncState)
        val state = State().withNotesLoaded(newNotesLoaded = true, userID = TestConstants.TEST_USER_ID)

        val testAuthenticatedSyncRequestAction1 = AuthenticatedSyncRequestAction.RemoteChangedDetected(TestConstants.TEST_USER_ID)
        val testAuthenticatedSyncRequestAction2 = AuthenticatedSyncRequestAction.RemoteChangedDetected(TestConstants.TEST_USER_ID_2)
        syncSideEffect.handle(testAuthenticatedSyncRequestAction1, state)
        syncSideEffect.handle(testAuthenticatedSyncRequestAction2, state)

        verify(mockUserSyncHandler1, times(1)).handleAuthSyncRequestAction(testAuthenticatedSyncRequestAction1)
        verify(mockUserSyncHandler2, times(1)).handleAuthSyncRequestAction(testAuthenticatedSyncRequestAction2)
    }

    @Test
    fun should_handle_multi_user_auth_sync_request_through_same_sync_handler_multi_account_disabled() {
        val testSyncState = SyncState(
            auth = AuthState.AADAuthenticated(), deltaTokensLoaded = true,
            apiHostInitialized = false, pauseSync = {}
        )
        setupMockWithMultiAccountDisabled(testSyncState)
        val state = State().withNotesLoaded(newNotesLoaded = true, userID = TestConstants.TEST_USER_ID)

        val testAuthenticatedSyncRequestAction1 = AuthenticatedSyncRequestAction.RemoteChangedDetected(TestConstants.TEST_USER_ID)
        val testAuthenticatedSyncRequestAction2 = AuthenticatedSyncRequestAction.RemoteChangedDetected(TestConstants.TEST_USER_ID_2)
        syncSideEffect.handle(testAuthenticatedSyncRequestAction1, state)
        syncSideEffect.handle(testAuthenticatedSyncRequestAction2, state)

        verify(mockUserSyncHandler1, times(1)).handleAuthSyncRequestAction(testAuthenticatedSyncRequestAction1)
        verify(mockUserSyncHandler1, times(1)).handleAuthSyncRequestAction(testAuthenticatedSyncRequestAction2)
    }

    @Test
    fun should_handle_multi_user_sync_request_through_different_sync_handler_multi_account_enabled() {
        val testSyncState = SyncState(
            auth = AuthState.AADAuthenticated(), deltaTokensLoaded = true,
            apiHostInitialized = false, pauseSync = {}
        )
        setupMockWithMultiAccountEnabled(testSyncState)
        val state = State().withNotesLoaded(newNotesLoaded = true, userID = TestConstants.TEST_USER_ID)

        val testAuthenticatedSyncRequestAction1 = SyncRequestAction.CreateNote(
            testNote1,
            TestConstants
                .TEST_USER_ID
        )
        val testAuthenticatedSyncRequestAction2 = SyncRequestAction.CreateNote(
            testNote2,
            TestConstants
                .TEST_USER_ID_2
        )
        syncSideEffect.handle(testAuthenticatedSyncRequestAction1, state)
        syncSideEffect.handle(testAuthenticatedSyncRequestAction2, state)

        verify(mockUserSyncHandler1, times(1)).handleSyncRequestAction(testAuthenticatedSyncRequestAction1)
        verify(mockUserSyncHandler2, times(1)).handleSyncRequestAction(testAuthenticatedSyncRequestAction2)
    }

    @Test
    fun should_handle_multi_user_sync_request_through_same_sync_handler_multi_account_disabled() {
        val testSyncState = SyncState(
            auth = AuthState.AADAuthenticated(), deltaTokensLoaded = true,
            apiHostInitialized = false, pauseSync = {}
        )
        setupMockWithMultiAccountDisabled(testSyncState)
        val state = State().withNotesLoaded(newNotesLoaded = true, userID = TestConstants.TEST_USER_ID)

        val testAuthenticatedSyncRequestAction1 = SyncRequestAction.CreateNote(
            testNote1,
            TestConstants
                .TEST_USER_ID
        )
        val testAuthenticatedSyncRequestAction2 = SyncRequestAction.CreateNote(
            testNote2,
            TestConstants
                .TEST_USER_ID_2
        )
        syncSideEffect.handle(testAuthenticatedSyncRequestAction1, state)
        syncSideEffect.handle(testAuthenticatedSyncRequestAction2, state)

        verify(mockUserSyncHandler1, times(1)).handleSyncRequestAction(testAuthenticatedSyncRequestAction1)
        verify(mockUserSyncHandler1, times(1)).handleSyncRequestAction(testAuthenticatedSyncRequestAction2)
    }

    @Test
    fun should_handle_multi_user_sync_response_action_through_different_sync_handler_multi_account_enabled() {
        val testSyncState = SyncState(
            auth = AuthState.AADAuthenticated(), deltaTokensLoaded = true,
            apiHostInitialized = false, pauseSync = {}
        )
        setupMockWithMultiAccountEnabled(testSyncState)
        val state = State().withNotesLoaded(newNotesLoaded = true, userID = TestConstants.TEST_USER_ID)

        val testAuthenticatedSyncRequestAction1 = SyncResponseAction.InvalidateClientCache(TestConstants.TEST_USER_ID)
        val testAuthenticatedSyncRequestAction2 = SyncResponseAction.InvalidateClientCache(TestConstants.TEST_USER_ID_2)
        syncSideEffect.handle(testAuthenticatedSyncRequestAction1, state)
        syncSideEffect.handle(testAuthenticatedSyncRequestAction2, state)

        verify(mockUserSyncHandler1, times(1)).handleSyncResponseAction(testAuthenticatedSyncRequestAction1)
        verify(mockUserSyncHandler2, times(1)).handleSyncResponseAction(testAuthenticatedSyncRequestAction2)
    }

    @Test
    fun should_handle_multi_user_sync_response_action_through_same_sync_handler_multi_account_disabled() {
        val testSyncState = SyncState(
            auth = AuthState.AADAuthenticated(), deltaTokensLoaded = true,
            apiHostInitialized = false, pauseSync = {}
        )
        setupMockWithMultiAccountDisabled(testSyncState)
        val state = State().withNotesLoaded(newNotesLoaded = true, userID = TestConstants.TEST_USER_ID)

        val testAuthenticatedSyncRequestAction1 = SyncResponseAction.InvalidateClientCache(TestConstants.TEST_USER_ID)
        val testAuthenticatedSyncRequestAction2 = SyncResponseAction.InvalidateClientCache(TestConstants.TEST_USER_ID_2)
        syncSideEffect.handle(testAuthenticatedSyncRequestAction1, state)
        syncSideEffect.handle(testAuthenticatedSyncRequestAction2, state)

        verify(mockUserSyncHandler1, times(1)).handleSyncResponseAction(testAuthenticatedSyncRequestAction1)
        verify(mockUserSyncHandler1, times(1)).handleSyncResponseAction(testAuthenticatedSyncRequestAction2)
    }

    @Test
    fun should_set_autodiscover_url_in_cache_with_multi_account_enabled() {
        val testSyncState = SyncState(
            auth = AuthState.AADAuthenticated(), deltaTokensLoaded = true,
            apiHostInitialized = false, pauseSync = {}
        )
        setupMockWithMultiAccountEnabled(testSyncState)
        val state = State().withNotesLoaded(newNotesLoaded = true, userID = TestConstants.TEST_USER_ID)

        val action1 = AutoDiscoverAction.CacheHostUrl(
            NotesClientHost.ExpirableHost(
                "https://substrate.office" +
                    ".com/NotesClientFoo",
                100L
            ),
            TestConstants.TEST_USER_ID
        )
        val action2 = AutoDiscoverAction.CacheHostUrl(
            NotesClientHost.ExpirableHost(
                "https://substrate.office" +
                    ".com/NotesClientBar",
                200L
            ),
            TestConstants.TEST_USER_ID_2
        )
        syncSideEffect.handle(action1, state)
        syncSideEffect.handle(action2, state)

        verify(mockAutoDiscoverCache, times(1)).setExpirableHost(action1.userID, action1.host)
        verify(mockAutoDiscoverCache, times(1)).setExpirableHost(action2.userID, action2.host)
    }

    @Test
    fun should_set_autodiscover_url_in_cache_with_multi_account_disabled() {
        val testSyncState = SyncState(
            auth = AuthState.AADAuthenticated(), deltaTokensLoaded = true,
            apiHostInitialized = false, pauseSync = {}
        )
        setupMockWithMultiAccountDisabled(testSyncState)
        val state = State().withNotesLoaded(newNotesLoaded = true, userID = TestConstants.TEST_USER_ID)

        val action = AutoDiscoverAction.CacheHostUrl(
            NotesClientHost.ExpirableHost(
                "https://substrate.office" +
                    ".com/NotesClientFoo",
                100L
            ),
            TestConstants.TEST_USER_ID
        )
        syncSideEffect.handle(action, state)

        verify(mockAutoDiscoverCache, times(1)).setExpirableHost(action.userID, action.host)
    }

    @Test
    fun setHostUrl_should_host_url_in_user_sync_handler_if_exists() {
        val testSyncState = SyncState(
            auth = AuthState.AADAuthenticated(), deltaTokensLoaded = true,
            apiHostInitialized = false, pauseSync = {}
        )
        setupMockWithMultiAccountDisabled(testSyncState)
        val state = State().withNotesLoaded(newNotesLoaded = true, userID = TestConstants.TEST_USER_ID)

        val action = AutoDiscoverAction.SetHostUrl(
            NotesClientHost.ExpirableHost(
                "https://substrate.office" +
                    ".com/NotesClientFoo",
                100L
            ),
            TestConstants.TEST_USER_ID
        )
        syncSideEffect.handle(action, state)

        verify(mockUserSyncHandler1, times(1)).setApiHost(action.host)
        verify(mockUserSyncHandler2, times(0)).setApiHost(action.host)
    }

    private fun setupMockWithMultiAccountDisabled(testSyncState: SyncState) {
        testWhen(mockSyncHandlerManager.getSyncHandlerForUser(TestConstants.TEST_USER_ID)).thenReturn(mockUserSyncHandler1)
        testWhen(mockSyncHandlerManager.getSyncHandlerForUser(TestConstants.TEST_USER_ID_2)).thenReturn(mockUserSyncHandler1)
        testWhen(mockUserSyncHandler1.syncState).thenReturn(testSyncState)
    }

    private fun setupMockWithMultiAccountEnabled(testSyncState: SyncState) {
        testWhen(mockSyncHandlerManager.getSyncHandlerForUser(TestConstants.TEST_USER_ID)).thenReturn(mockUserSyncHandler1)
        testWhen(mockSyncHandlerManager.getSyncHandlerForUser(TestConstants.TEST_USER_ID_2)).thenReturn(mockUserSyncHandler2)
        testWhen(mockUserSyncHandler1.syncState).thenReturn(testSyncState)
        testWhen(mockUserSyncHandler2.syncState).thenReturn(testSyncState)
    }
}
