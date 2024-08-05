package com.microsoft.notes.sideeffect.persistence.mapper

import com.microsoft.notes.models.AccountType
import com.microsoft.notes.models.Changes
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.NoteUpdate
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.sideeffect.persistence.NotesDatabaseManager
import com.microsoft.notes.sideeffect.persistence.PersistenceSideEffect
import com.microsoft.notes.sideeffect.persistence.Preference
import com.microsoft.notes.sideeffect.persistence.PreferenceKeys
import com.microsoft.notes.sideeffect.persistence.dao.MeetingNoteDao
import com.microsoft.notes.sideeffect.persistence.dao.NoteDao
import com.microsoft.notes.sideeffect.persistence.dao.NoteReferenceDao
import com.microsoft.notes.sideeffect.persistence.dao.PreferencesDao
import com.microsoft.notes.sideeffect.persistence.handler.ReadHandler
import com.microsoft.notes.store.SideEffect
import com.microsoft.notes.store.State
import com.microsoft.notes.store.StateHandler
import com.microsoft.notes.store.Store
import com.microsoft.notes.store.action.AuthAction
import com.microsoft.notes.store.action.CreationAction
import com.microsoft.notes.store.action.DeleteAction
import com.microsoft.notes.store.action.ReadAction
import com.microsoft.notes.store.action.SamsungNotesResponseAction
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.action.UpdateAction
import com.microsoft.notes.store.addNotes
import com.microsoft.notes.store.addSamsungNotes
import com.microsoft.notes.ui.extensions.SAMSUNG_NOTES_APP_NAME
import com.microsoft.notes.utils.logging.TestConstants
import com.microsoft.notes.utils.logging.TestConstants.Companion.TEST_USER_ID
import com.microsoft.notes.utils.utils.Constants
import com.microsoft.notes.utils.utils.RoutingPrefix
import com.microsoft.notes.utils.utils.UserInfo
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argForWhich
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CopyOnWriteArrayList
import com.microsoft.notes.sideeffect.persistence.Note as PersistenceNote
import org.mockito.Mockito.`when` as testWhen

class PersistenceSideEffectTest {
    val NOTE_LOCAL_ID_1 = "localId1"
    val NOTE_LOCAL_ID_2 = "localId2"

    val NOTE_SAMSUNG_ID_1 = "samsungId1"
    val NOTE_SAMSUNG_ID_2 = "samsungId2"

    val NOTE_REFERENCE_LOCAL_ID_1 = "localId_ex1"
    val NOTE_REFERENCE_LOCAL_ID_2 = "localId_ex2"

    val testNote1 = Note(localId = NOTE_LOCAL_ID_1, color = Color.BLUE)
    val testNote2 = Note(localId = NOTE_LOCAL_ID_2, color = Color.YELLOW)

    private val testSamsungPersistenceNote1 = PersistenceNote(
        id = NOTE_SAMSUNG_ID_1,
        isDeleted = false,
        color = 0,
        localCreatedAt = 0L,
        documentModifiedAt = 0L,
        remoteData = null,
        document = """{type: "SAMSUNG_NOTE"}""",
        media = "[]",
        createdByApp = SAMSUNG_NOTES_APP_NAME,
        title = null,
        pinnedAt = null
    )
    private val testSamsungPersistenceNote2 = PersistenceNote(
        id = NOTE_SAMSUNG_ID_2,
        isDeleted = false,
        color = 0,
        localCreatedAt = 0L,
        documentModifiedAt = 0L,
        remoteData = null,
        document = """{type: "SAMSUNG_NOTE"}""",
        media = "[]",
        createdByApp = SAMSUNG_NOTES_APP_NAME,
        title = null,
        pinnedAt = null
    )

    val testNoteReference1 = NoteReference(localId = NOTE_REFERENCE_LOCAL_ID_1, title = "Test 1")
    val testNoteReference2 = NoteReference(localId = NOTE_REFERENCE_LOCAL_ID_2, title = "Test 2")

    private val CREATED_BY_APP = "Test"
    val sideEffects: CopyOnWriteArrayList<SideEffect> = CopyOnWriteArrayList()
    val stateHandlers: CopyOnWriteArrayList<StateHandler> = CopyOnWriteArrayList()

    private lateinit var store: Store

    @Mock
    val mockStore: Store = mock()

    @Mock
    val mockNotesDatabaseManager = mock<NotesDatabaseManager> { }

    @Mock
    val mockTestNotesDB1 = mock<NotesDatabase> {}

    @Mock
    val mockTestNotesDB2 = mock<NotesDatabase> {}

    @Mock
    val mockNotesDao1 = mock<NoteDao> {}

    @Mock
    val mockNotesDao2 = mock<NoteDao> {}

    @Mock
    val mockNoteReferencesDao1 = mock<NoteReferenceDao> {}

    @Mock
    val mockNoteReferencesDao2 = mock<NoteReferenceDao> {}

    @Mock
    val mockPreferencesDao1 = mock<PreferencesDao> {}

    @Mock
    val mockPreferenesDao2 = mock<PreferencesDao> {}

    @Mock
    val mockMeetingNotesDao1: MeetingNoteDao = mock<MeetingNoteDao> {}

    @Mock
    val mockMeetingNotesDao2: MeetingNoteDao = mock<MeetingNoteDao> {}

    lateinit var persistenceSideEffect: PersistenceSideEffect

    @Before
    fun setup() {
        store = object : Store(
            sideEffects = sideEffects, stateHandlers = stateHandlers,
            isDebugMode = true,
            createdByApp = CREATED_BY_APP
        ) {}
        persistenceSideEffect = PersistenceSideEffect(store, null, mockNotesDatabaseManager, null)
        sideEffects.add(persistenceSideEffect)
    }

    private fun setupMocksWithMultiAccountEnabled() {
        testWhen(mockNotesDatabaseManager.getNotesDatabaseForUser(TestConstants.TEST_USER_ID)).thenReturn(mockTestNotesDB1)
        testWhen(mockNotesDatabaseManager.getNotesDatabaseForUser(TestConstants.TEST_USER_ID_2)).thenReturn(mockTestNotesDB2)
        testWhen(mockTestNotesDB1.noteDao()).thenReturn(mockNotesDao1)
        testWhen(mockTestNotesDB2.noteDao()).thenReturn(mockNotesDao2)
        testWhen(mockTestNotesDB1.noteReferenceDao()).thenReturn(mockNoteReferencesDao1)
        testWhen(mockTestNotesDB2.noteReferenceDao()).thenReturn(mockNoteReferencesDao2)
        testWhen(mockTestNotesDB1.meetingNoteDao()).thenReturn(mockMeetingNotesDao1)
        testWhen(mockTestNotesDB2.meetingNoteDao()).thenReturn(mockMeetingNotesDao2)
    }

    private fun setupMocksWithMultiAccountDisabled() {
        testWhen(mockNotesDatabaseManager.getNotesDatabaseForUser(TestConstants.TEST_USER_ID)).thenReturn(mockTestNotesDB1)
        testWhen(mockNotesDatabaseManager.getNotesDatabaseForUser(TestConstants.TEST_USER_ID_2)).thenReturn(mockTestNotesDB1)
        testWhen(mockTestNotesDB1.noteDao()).thenReturn(mockNotesDao1)
        testWhen(mockTestNotesDB1.noteReferenceDao()).thenReturn(mockNoteReferencesDao1)
        testWhen(mockTestNotesDB1.meetingNoteDao()).thenReturn(mockMeetingNotesDao1)
    }

    @Test
    fun should_add_notes_to_different_db() {
        setupMocksWithMultiAccountEnabled()
        val state = State()

        persistenceSideEffect.handle(CreationAction.AddNoteAction(testNote1, TestConstants.TEST_USER_ID), state)
        persistenceSideEffect.handle(CreationAction.AddNoteAction(testNote2, TestConstants.TEST_USER_ID_2), state)

        verify(mockNotesDao1, times(1)).insert(testNote1.toPersistenceNote())
        verify(mockNotesDao2, times(1)).insert(testNote2.toPersistenceNote())
    }

    @Test
    fun should_add_notes_to_different_db_multi_account_enabled() {
        setupMocksWithMultiAccountEnabled()

        val state = State()
        persistenceSideEffect.handle(CreationAction.AddNoteAction(testNote1, TestConstants.TEST_USER_ID), state)
        persistenceSideEffect.handle(CreationAction.AddNoteAction(testNote2, TestConstants.TEST_USER_ID_2), state)

        verify(mockNotesDao1, times(1)).insert(testNote1.toPersistenceNote())
        verify(mockNotesDao2, times(1)).insert(testNote2.toPersistenceNote())
    }

    @Test
    fun should_add_notes_to_same_db_multi_account_disabled() {
        setupMocksWithMultiAccountDisabled()

        val state = State()

        persistenceSideEffect.handle(CreationAction.AddNoteAction(testNote1, TestConstants.TEST_USER_ID), state)
        persistenceSideEffect.handle(CreationAction.AddNoteAction(testNote2, TestConstants.TEST_USER_ID_2), state)

        verify(mockNotesDao1, times(1)).insert(testNote1.toPersistenceNote())
        verify(mockNotesDao1, times(1)).insert(testNote2.toPersistenceNote())
    }

    @Test
    fun should_update_notes_to_different_db_multi_account_enabled() {
        setupMocksWithMultiAccountEnabled()
        val state = State()
            .addNotes(listOf(testNote1), TestConstants.TEST_USER_ID)
            .addNotes(listOf(testNote2), TestConstants.TEST_USER_ID_2)

        persistenceSideEffect.handle(
            UpdateAction.UpdateActionWithId.UpdateNoteWithColorAction(
                noteLocalId = testNote1.localId,
                color = Color.CHARCOAL,
                uiRevision = 0,
                userID = TestConstants.TEST_USER_ID
            ),
            state
        )

        persistenceSideEffect.handle(
            UpdateAction.UpdateActionWithId.UpdateNoteWithColorAction(
                noteLocalId = testNote2.localId,
                color = Color.CHARCOAL,
                uiRevision = 0,
                userID = TestConstants.TEST_USER_ID_2
            ),
            state
        )

        verify(mockNotesDao1, times(1)).updateColor(testNote1.localId, testNote1.color.toPersistenceColor())
        verify(mockNotesDao2, times(1)).updateColor(testNote2.localId, testNote2.color.toPersistenceColor())
    }

    @Test
    fun should_update_notes_to_same_db_multi_account_disabled() {
        setupMocksWithMultiAccountDisabled()
        val state = State()
            .addNotes(listOf(testNote1), TestConstants.TEST_USER_ID)
            .addNotes(listOf(testNote2), TestConstants.TEST_USER_ID_2)

        persistenceSideEffect.handle(
            UpdateAction.UpdateActionWithId.UpdateNoteWithColorAction(
                noteLocalId = testNote1.localId,
                color = Color.CHARCOAL,
                uiRevision = 0,
                userID = TestConstants.TEST_USER_ID
            ),
            state
        )

        persistenceSideEffect.handle(
            UpdateAction.UpdateActionWithId.UpdateNoteWithColorAction(
                noteLocalId = testNote2.localId,
                color = Color.CHARCOAL,
                uiRevision = 0,
                userID = TestConstants.TEST_USER_ID_2
            ),
            state
        )

        verify(mockNotesDao1, times(1)).updateColor(testNote1.localId, testNote1.color.toPersistenceColor())
        verify(mockNotesDao1, times(1)).updateColor(testNote2.localId, testNote2.color.toPersistenceColor())
    }

    @Test
    fun should_delete_notes_from_different_db_multi_account_enabled() {
        setupMocksWithMultiAccountEnabled()

        val state = State()
            .addNotes(listOf(testNote1), TestConstants.TEST_USER_ID)
            .addNotes(listOf(testNote2), TestConstants.TEST_USER_ID_2)

        persistenceSideEffect.handle(DeleteAction.DeleteAllNotesAction(userID = TestConstants.TEST_USER_ID_2), state)
        persistenceSideEffect.handle(DeleteAction.DeleteAllNotesAction(userID = TestConstants.TEST_USER_ID), state)

        verify(mockNotesDao1, times(1)).deleteAll()
        verify(mockNotesDao2, times(1)).deleteAll()
    }

    @Test
    fun should_delete_notes_from_same_db_multi_account_disabled() {
        setupMocksWithMultiAccountDisabled()

        val state = State()
            .addNotes(listOf(testNote1), TestConstants.TEST_USER_ID)
            .addNotes(listOf(testNote2), TestConstants.TEST_USER_ID_2)

        persistenceSideEffect.handle(DeleteAction.DeleteAllNotesAction(userID = TestConstants.TEST_USER_ID_2), state)
        persistenceSideEffect.handle(DeleteAction.DeleteAllNotesAction(userID = TestConstants.TEST_USER_ID), state)

        verify(mockNotesDao1, times(2)).deleteAll()
    }

    @Test
    fun should_read_notes_from_different_db_multi_account_enabled() {
        setupMocksWithMultiAccountEnabled()

        val state = State()
            .addNotes(listOf(testNote1), TestConstants.TEST_USER_ID)
            .addNotes(listOf(testNote2), TestConstants.TEST_USER_ID_2)

        testWhen(mockTestNotesDB1.preferencesDao()).thenReturn(mockPreferencesDao1)
        testWhen(mockTestNotesDB2.preferencesDao()).thenReturn(mockPreferenesDao2)

        persistenceSideEffect.handle(ReadAction.FetchAllNotesAction(userID = TestConstants.TEST_USER_ID, isSamsungNotesSyncEnabled = false), state)
        persistenceSideEffect.handle(ReadAction.FetchAllNotesAction(userID = TestConstants.TEST_USER_ID_2, isSamsungNotesSyncEnabled = false), state)

        verify(mockNotesDao1, times(1)).getFirstOrderByDocumentModifiedAt(ReadHandler.notesDBPageSize)
        verify(mockNotesDao2, times(1)).getFirstOrderByDocumentModifiedAt(ReadHandler.notesDBPageSize)
    }

    @Test
    fun should_read_notes_from_same_db_multi_account_disabled() {
        setupMocksWithMultiAccountDisabled()

        val state = State()
            .addNotes(listOf(testNote1), TestConstants.TEST_USER_ID)
            .addNotes(listOf(testNote2), TestConstants.TEST_USER_ID_2)

        testWhen(mockTestNotesDB1.preferencesDao()).thenReturn(mockPreferencesDao1)

        persistenceSideEffect.handle(ReadAction.FetchAllNotesAction(userID = TestConstants.TEST_USER_ID, isSamsungNotesSyncEnabled = false), state)
        persistenceSideEffect.handle(ReadAction.FetchAllNotesAction(userID = TestConstants.TEST_USER_ID_2, isSamsungNotesSyncEnabled = false), state)

        verify(mockNotesDao1, times(2)).getFirstOrderByDocumentModifiedAt(ReadHandler.notesDBPageSize)
    }

    @Test
    fun should_read_note_references_multi_account_enabled() {
        setupMocksWithMultiAccountEnabled()
        val state = State()
        testWhen(mockTestNotesDB1.preferencesDao()).thenReturn(mockPreferencesDao1)
        testWhen(mockTestNotesDB2.preferencesDao()).thenReturn(mockPreferenesDao2)

        persistenceSideEffect.handle(ReadAction.FetchAllNotesAction(TestConstants.TEST_USER_ID, isSamsungNotesSyncEnabled = false), state)
        persistenceSideEffect.handle(ReadAction.FetchAllNotesAction(TestConstants.TEST_USER_ID_2, isSamsungNotesSyncEnabled = false), state)

        verify(mockNoteReferencesDao1, times(1)).getAll()
        verify(mockNoteReferencesDao2, times(1)).getAll()
    }

    @Test
    fun should_delete_notes_to_different_db_multi_account_enabled() {
        setupMocksWithMultiAccountEnabled()

        val state = State()
            .addNotes(listOf(testNote1), TestConstants.TEST_USER_ID)
            .addNotes(listOf(testNote2), TestConstants.TEST_USER_ID_2)

        persistenceSideEffect.handle(
            SyncResponseAction.PermanentlyDeleteNote(
                noteLocalId = testNote1.localId,
                userID = TestConstants.TEST_USER_ID
            ),
            state
        )
        persistenceSideEffect.handle(
            SyncResponseAction.PermanentlyDeleteNote(
                noteLocalId = testNote2.localId,
                userID = TestConstants.TEST_USER_ID_2
            ),
            state
        )

        verify(mockNotesDao1, times(1)).deleteNoteById(testNote1.localId)
        verify(mockNotesDao2, times(1)).deleteNoteById(testNote2.localId)
    }

    @Test
    fun should_sync_notes_to_same_db_multi_account_disabled() {
        setupMocksWithMultiAccountDisabled()

        val state = State()
            .addNotes(listOf(testNote1), TestConstants.TEST_USER_ID)
            .addNotes(listOf(testNote2), TestConstants.TEST_USER_ID_2)

        persistenceSideEffect.handle(
            SyncResponseAction.PermanentlyDeleteNote(
                noteLocalId = testNote1.localId,
                userID = TestConstants.TEST_USER_ID
            ),
            state
        )
        persistenceSideEffect.handle(
            SyncResponseAction.PermanentlyDeleteNote(
                noteLocalId = testNote2.localId,
                userID = TestConstants.TEST_USER_ID_2
            ),
            state
        )

        verify(mockNotesDao1, times(1)).deleteNoteById(testNote1.localId)
        verify(mockNotesDao1, times(1)).deleteNoteById(testNote2.localId)
    }

    @Test
    fun should_handle_login() {
        setupMocksWithMultiAccountEnabled()

        val state = State()
            .addNotes(listOf(testNote1), TestConstants.TEST_USER_ID)
            .addNotes(listOf(testNote2), TestConstants.TEST_USER_ID_2)

        val userInfo = UserInfo(
            userID = TestConstants.TEST_USER_ID,
            accessToken = TestConstants.TEST_TOKEN_USER_ID,
            accountType = AccountType.ADAL,
            userInfoSuffix = Constants.EMPTY_USER_INFO_SUFFIX,
            email = Constants.EMPTY_EMAIL,
            routingPrefix = RoutingPrefix.Unprefixed,
            tenantID = Constants.EMPTY_TENANT_ID
        )

        val userInfo2 = UserInfo(
            userID = TestConstants.TEST_USER_ID_2,
            accessToken = TestConstants.TEST_TOKEN_USER_ID_2,
            accountType = AccountType.MSA,
            userInfoSuffix = TestConstants.TEST_USER_INFO_SUFFIX,
            email = Constants.EMPTY_EMAIL,
            routingPrefix = RoutingPrefix.Unprefixed,
            tenantID = Constants.EMPTY_TENANT_ID
        )

        testWhen(mockNotesDatabaseManager.handleNewUser(userInfo)).thenReturn("")
        testWhen(mockNotesDatabaseManager.handleNewUser(userInfo2)).thenReturn("")

        persistenceSideEffect.handle(
            AuthAction.NewAuthTokenAction(
                userInfo
            ),
            state
        )
        persistenceSideEffect.handle(
            AuthAction.NewAuthTokenAction(
                userInfo2
            ),
            state
        )

        verify(mockNotesDatabaseManager, times(1)).handleNewUser(
            UserInfo(
                userID = TestConstants.TEST_USER_ID,
                accessToken = TestConstants.TEST_TOKEN_USER_ID, accountType = AccountType.ADAL,
                userInfoSuffix =
                Constants.EMPTY_USER_INFO_SUFFIX,
                email = Constants.EMPTY_EMAIL, routingPrefix = RoutingPrefix.Unprefixed,
                tenantID = Constants.EMPTY_TENANT_ID
            )
        )
        verify(mockNotesDatabaseManager, times(1)).handleNewUser(
            UserInfo(
                userID = TestConstants.TEST_USER_ID_2,
                accessToken = TestConstants.TEST_TOKEN_USER_ID_2, accountType = AccountType.MSA,
                userInfoSuffix =
                TestConstants.TEST_USER_INFO_SUFFIX,
                email = Constants.EMPTY_EMAIL, routingPrefix = RoutingPrefix.Unprefixed,
                tenantID = Constants.EMPTY_TENANT_ID
            )
        )
    }

    @Test
    fun should_handle_logout() {
        setupMocksWithMultiAccountEnabled()

        val state = State()
            .addNotes(listOf(testNote1), TestConstants.TEST_USER_ID)
            .addNotes(listOf(testNote2), TestConstants.TEST_USER_ID_2)

        persistenceSideEffect.handle(
            AuthAction.LogoutAction(
                userID = TestConstants.TEST_USER_ID
            ),
            state
        )
        persistenceSideEffect.handle(
            AuthAction.LogoutAction(
                userID = TestConstants.TEST_USER_ID_2
            ),
            state
        )

        verify(mockNotesDatabaseManager, times(1)).handleLogout(TestConstants.TEST_USER_ID)
        verify(mockNotesDatabaseManager, times(1)).handleLogout(TestConstants.TEST_USER_ID_2)
    }

    @Test
    fun `Samsung Notes Action with multi-account disabled`() {
        setupMocksWithMultiAccountDisabled()

        val toCreate = listOf(testNote1)
        val toReplace = listOf(NoteUpdate(testNote1))
        val toDelete = listOf(testNote1)
        val mockChanges = Changes(toCreate, toReplace, toDelete)
        whenever(mockTestNotesDB1.preferencesDao()).thenReturn(mockPreferencesDao1)

        val mockDeltaToken = "mockDeltaToken"
        persistenceSideEffect.handle(
            SamsungNotesResponseAction.ApplyChanges(mockChanges, TestConstants.TEST_USER_ID, mockDeltaToken), State()
        )

        verify(mockNotesDao1, times(1)).insert(toCreate.map { it.toPersistenceNote() })
        verify(mockNotesDao1, times(1)).update(toReplace.map { it.noteFromServer.toPersistenceNote() })
        verify(mockNotesDao1, times(1)).delete(toDelete.map { it.toPersistenceNote() })
        verify(mockPreferencesDao1, times(1)).insertOrUpdate(
            Preference(id = PreferenceKeys.samsungNotesDeltaToken, value = mockDeltaToken)
        )
    }

    @Test
    fun `Samsung Notes Action with multi-account enabled`() {
        setupMocksWithMultiAccountEnabled()

        val toCreate = listOf(testNote1)
        val toReplace = listOf(NoteUpdate(testNote1))
        val toDelete = listOf(testNote1)
        val mockChanges = Changes(toCreate, toReplace, toDelete)
        whenever(mockTestNotesDB1.preferencesDao()).thenReturn(mockPreferencesDao1)
        whenever(mockTestNotesDB2.preferencesDao()).thenReturn(mockPreferencesDao1)

        val mockDeltaToken = "mockDeltaToken"
        persistenceSideEffect.handle(
            SamsungNotesResponseAction.ApplyChanges(mockChanges, TestConstants.TEST_USER_ID, mockDeltaToken), State()
        )
        persistenceSideEffect.handle(
            SamsungNotesResponseAction.ApplyChanges(mockChanges, TestConstants.TEST_USER_ID_2, mockDeltaToken), State()
        )

        verify(mockNotesDao1, times(1)).insert(toCreate.map { it.toPersistenceNote() })
        verify(mockNotesDao1, times(1)).update(toReplace.map { it.noteFromServer.toPersistenceNote() })
        verify(mockNotesDao1, times(1)).delete(toDelete.map { it.toPersistenceNote() })

        verify(mockNotesDao2, times(1)).insert(toCreate.map { it.toPersistenceNote() })
        verify(mockNotesDao2, times(1)).update(toReplace.map { it.noteFromServer.toPersistenceNote() })
        verify(mockNotesDao2, times(1)).delete(toDelete.map { it.toPersistenceNote() })

        verify(mockPreferencesDao1, times(2)).insertOrUpdate(
            Preference(id = PreferenceKeys.samsungNotesDeltaToken, value = mockDeltaToken)
        )
    }

    @Test
    fun `Samsung Notes should not be filtered when isSamsungNotesSyncEnabled is true`() {
        val samsungPersistenceNotes = listOf(testSamsungPersistenceNote1, testSamsungPersistenceNote2)
        val samsungStoreNotes = samsungPersistenceNotes.toStoreNoteList()
        whenever(mockNotesDao1.getFirstOrderByDocumentModifiedAt(any())).thenReturn(samsungPersistenceNotes)
        whenever(mockNotesDatabaseManager.getNotesDatabaseForUser(TEST_USER_ID)).thenReturn(mockTestNotesDB1)
        whenever(mockTestNotesDB1.preferencesDao()).thenReturn(mockPreferencesDao1)
        whenever(mockTestNotesDB1.noteDao()).thenReturn(mockNotesDao1)
        whenever(mockTestNotesDB1.noteReferenceDao()).thenReturn(mockNoteReferencesDao1)
        whenever(mockTestNotesDB1.meetingNoteDao()).thenReturn(mockMeetingNotesDao1)

        val state = State().addSamsungNotes(samsungStoreNotes, TEST_USER_ID)
        val persistenceSideEffectWithMockStore = PersistenceSideEffect(mockStore, null, mockNotesDatabaseManager, null)
        persistenceSideEffectWithMockStore.handle(ReadAction.FetchAllNotesAction(TEST_USER_ID, isSamsungNotesSyncEnabled = true), state)

        verify(mockStore).dispatch(argForWhich<ReadAction.NotesLoadedAction> { samsungNotesCollection == samsungStoreNotes }, eq(null))
    }

    @Test
    fun `Samsung Notes should be filtered when isSamsungNotesSyncEnabled is false`() {
        val samsungPersistenceNotes = listOf(testSamsungPersistenceNote1, testSamsungPersistenceNote2)
        val samsungStoreNotes = samsungPersistenceNotes.toStoreNoteList()
        whenever(mockNotesDao1.getFirstOrderByDocumentModifiedAt(any())).thenReturn(samsungPersistenceNotes)
        whenever(mockNotesDatabaseManager.getNotesDatabaseForUser(TEST_USER_ID)).thenReturn(mockTestNotesDB1)
        whenever(mockTestNotesDB1.preferencesDao()).thenReturn(mockPreferencesDao1)
        whenever(mockTestNotesDB1.noteDao()).thenReturn(mockNotesDao1)
        whenever(mockTestNotesDB1.noteReferenceDao()).thenReturn(mockNoteReferencesDao1)
        whenever(mockTestNotesDB1.meetingNoteDao()).thenReturn(mockMeetingNotesDao1)

        val state = State().addSamsungNotes(samsungStoreNotes, TEST_USER_ID)
        val persistenceSideEffectWithMockStore = PersistenceSideEffect(mockStore, null, mockNotesDatabaseManager, null)
        persistenceSideEffectWithMockStore.handle(ReadAction.FetchAllNotesAction(TEST_USER_ID, isSamsungNotesSyncEnabled = false), state)

        verify(mockStore).dispatch(argForWhich<ReadAction.NotesLoadedAction> { samsungNotesCollection.isEmpty() }, eq(null))
    }
}
