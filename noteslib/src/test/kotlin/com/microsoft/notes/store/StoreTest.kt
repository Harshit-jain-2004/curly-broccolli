package com.microsoft.notes.store

import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.CompoundAction
import com.microsoft.notes.store.action.CreationAction.AddNoteAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithColorAction
import com.microsoft.notes.utils.logging.TestConstants
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.concurrent.CopyOnWriteArrayList
import org.hamcrest.CoreMatchers.`is` as iz

class DummyAction : Action {
    override fun toLoggingIdentifier(): String = "DummyAction"
}

class StoreTest {
    private val LOCAL_ID = "localId"
    private val CREATED_BY_APP = "Test"

    lateinit var store: Store
    val sideEffects: CopyOnWriteArrayList<SideEffect> = CopyOnWriteArrayList()
    val stateHandlers: CopyOnWriteArrayList<StateHandler> = CopyOnWriteArrayList()

    @Before
    fun setup() {
        store = object : Store(
            sideEffects = sideEffects, stateHandlers = stateHandlers, isDebugMode = true,
            createdByApp = CREATED_BY_APP
        ) {}
    }

    @Test
    fun should_handle_Action_with_State_change() {
        val sideEffect = object : SideEffect() {
            override fun handle(action: Action, state: State) {}
        }
        val stateHandler = object : StateHandler() {
            override fun handle(state: State) {}
        }
        val sideEffectSpy = Mockito.spy(sideEffect)
        val stateHandlerSpy = Mockito.spy(stateHandler)

        val note = Note(localId = LOCAL_ID, color = Color.GREEN)
        val initialState = State().withNotesLoaded(false, TestConstants.TEST_USER_ID).addNotes(listOf(note), userID = TestConstants.TEST_USER_ID)
        store.dispatch(initialState)

        val action = UpdateNoteWithColorAction(
            noteLocalId = LOCAL_ID, color = Color.PINK, uiRevision = 1,
            userID = TestConstants.TEST_USER_ID
        )

        store.sideEffects.add(sideEffectSpy)
        @Suppress("DEPRECATION")
        store.stateHandlers.add(stateHandlerSpy)

        store.dispatch(action)

        assertThat(initialState, iz(not(store.state)))
        assertThat(initialState.userIDForLocalNoteID(LOCAL_ID), iz(TestConstants.TEST_USER_ID))
        verify(sideEffectSpy).handle(action, store.state)
        verify(stateHandlerSpy).handle(any())
    }

    @Test
    fun should_handle_Action_without_State_change() {
        val sideEffect = object : SideEffect() {
            override fun handle(action: Action, state: State) {}
        }
        val stateHandler = object : StateHandler() {
            override fun handle(state: State) {}
        }
        val sideEffectSpy = Mockito.spy(sideEffect)
        val stateHandlerSpy = Mockito.spy(stateHandler)

        val action = DummyAction()

        store.sideEffects.add(sideEffectSpy)
        store.stateHandlers.add(stateHandlerSpy)

        val oldState = store.state
        store.dispatch(action)

        assertThat(oldState, iz(store.state))
        assertThat(store.state.noteLocalIDToUserIDMap.isEmpty(), iz(true))
        verify(sideEffectSpy).handle(any<DummyAction>(), any<State>())
        verify(stateHandlerSpy, times(0)).handle(any<State>())
    }

    @Test
    fun should_return_current_state_on_dummy_action() {
        val action = DummyAction()

        val oldState = store.state
        store.dispatch(action)

        assertThat(oldState, iz(store.state))
    }

    @Test
    fun should_reduce_compound_action() {
        val LOCAL_CREATED_AT: Long = 1517417105000
        val DOCUMENT_MODIFIED_AT: Long = 1517417431000
        val note = Note(
            localId = LOCAL_ID, color = Color.GREEN,
            localCreatedAt = LOCAL_CREATED_AT,
            documentModifiedAt = DOCUMENT_MODIFIED_AT
        )
        val initialState = State().withNotesLoaded(false, TestConstants.TEST_USER_ID)
            .addNotes(listOf(note), TestConstants.TEST_USER_ID)
        store.dispatch(initialState)

        val compoundAction = CompoundAction(
            UpdateNoteWithColorAction(LOCAL_ID, Color.PINK, uiRevision = 1, userID = TestConstants.TEST_USER_ID),
            UpdateNoteWithColorAction(LOCAL_ID, Color.BLUE, uiRevision = 2, userID = TestConstants.TEST_USER_ID)
        )
        store.dispatch(compoundAction)

        var expectedNote = Note(
            localId = LOCAL_ID,
            color = Color.BLUE,
            localCreatedAt = LOCAL_CREATED_AT,
            documentModifiedAt = DOCUMENT_MODIFIED_AT,
            uiRevision = 2
        )
        expectedNote = expectedNote.copy(uiShadow = expectedNote)

        with(store.state) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).first(), iz(expectedNote))
            assertThat(userIDForLocalNoteID(LOCAL_ID), iz(TestConstants.TEST_USER_ID))
        }
    }

    @Test
    fun should_return_current_state_on_compound_action_with_dummy() {
        val initialState = State()
        store.dispatch(initialState)

        val newNote = Note()
        val compoundAction = CompoundAction(
            AddNoteAction(newNote, TestConstants.TEST_USER_ID),
            DummyAction()
        )
        store.dispatch(compoundAction)

        with(store.state) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(1))
            assertThat(noteLocalIDToUserIDMap.size, iz(1))
        }
    }

    @Test
    fun should_apply_compound_action_in_order() {
        val note = Note(localId = LOCAL_ID, color = Color.GREEN, createdByApp = CREATED_BY_APP)
        val initialState = State().withNotesLoaded(false, TestConstants.TEST_USER_ID)
            .addNotes(listOf(note), TestConstants.TEST_USER_ID)
        store.dispatch(initialState)

        val action = CompoundAction(
            UpdateNoteWithColorAction(noteLocalId = LOCAL_ID, color = Color.PINK, uiRevision = 1, userID = TestConstants.TEST_USER_ID),
            UpdateNoteWithColorAction(noteLocalId = LOCAL_ID, color = Color.PURPLE, uiRevision = 2, userID = TestConstants.TEST_USER_ID)
        )

        store.dispatch(action)
        with(store.state) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).first().color, iz(Color.PURPLE))
            assertThat(userIDForLocalNoteID(LOCAL_ID), iz(TestConstants.TEST_USER_ID))
        }
    }
}
