package com.microsoft.notes.store

import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.CreationAction
import com.microsoft.notes.utils.logging.TestConstants
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import java.util.concurrent.CopyOnWriteArrayList
import org.hamcrest.CoreMatchers.`is` as iz

class SubscribersTest {
    lateinit var sideEffects: CopyOnWriteArrayList<SideEffect>
    lateinit var stateHandlers: CopyOnWriteArrayList<StateHandler>

    @Before
    fun setup() {
        sideEffects = CopyOnWriteArrayList()
        stateHandlers = CopyOnWriteArrayList()
    }

    @Test
    fun should_subscribe() {
        val sideEffect1 = object : SideEffect() {
            override fun handle(action: Action, state: State) {}
        }
        val stateHandler1 = object : StateHandler() {
            override fun handle(state: State) {}
        }

        sideEffects.add(sideEffect1)
        assertThat(sideEffects.isEmpty(), iz(false))
        assertThat(sideEffects.count(), iz(1))
        assertThat(sideEffects.contains(sideEffect1), iz(true))

        stateHandlers.add(stateHandler1)
        assertThat(stateHandlers.isEmpty(), iz(false))
        assertThat(stateHandlers.count(), iz(1))
        assertThat(stateHandlers.contains(stateHandler1), iz(true))
    }

    @Test
    fun should_dispatch() {
        val action = CreationAction.AddNoteAction(note = Note(color = Color.GREY), userID = TestConstants.TEST_USER_ID)

        val sideEffect1 = object : SideEffect() {
            override fun handle(action: Action, state: State) {
                assertThat(action is (CreationAction.AddNoteAction), iz(true))
                val note = (action as CreationAction.AddNoteAction).note
                assertThat(note.color, iz(Color.GREY))
            }
        }

        sideEffects.add(sideEffect1)
        assertThat(sideEffects.isEmpty(), iz(false))
        assertThat(sideEffects.count(), iz(1))

        sideEffects.dispatch(action, State())
    }

    @Test
    fun should_dispatch_to_different_subscribers() {
        val sideEffect = object : SideEffect() {
            override fun handle(action: Action, state: State) {}
        }
        val sideEffect1 = spy(sideEffect)
        val sideEffect2 = spy(sideEffect)
        val sideEffect3 = spy(sideEffect)

        sideEffects.add(sideEffect1)
        sideEffects.add(sideEffect2)
        sideEffects.add(sideEffect3)

        assertThat(sideEffects.isEmpty(), iz(false))
        assertThat(sideEffects.count(), iz(3))

        val action = CreationAction.AddNoteAction(note = Note(color = Color.GREY), userID = TestConstants.TEST_USER_ID)
        sideEffects.dispatch(action, State())

        verify(sideEffect1).onNext(any<CreationAction.AddNoteAction>(), any<State>())
        verify(sideEffect2).onNext(any<CreationAction.AddNoteAction>(), any<State>())
        verify(sideEffect3).onNext(any<CreationAction.AddNoteAction>(), any<State>())
    }

    @Test
    fun should_unsubscribe() {
        val sideEffect1 = object : SideEffect() {
            override fun handle(action: Action, state: State) {}
        }
        val sideEffect2 = object : SideEffect() {
            override fun handle(action: Action, state: State) {}
        }
        val sideEffect3 = object : SideEffect() {
            override fun handle(action: Action, state: State) {}
        }

        sideEffects.add(sideEffect1)
        sideEffects.add(sideEffect2)
        sideEffects.add(sideEffect3)

        assertThat(sideEffects.isEmpty(), iz(false))
        assertThat(sideEffects.count(), iz(3))
        assertThat(sideEffects.contains(sideEffect1), iz(true))
        assertThat(sideEffects.contains(sideEffect2), iz(true))
        assertThat(sideEffects.contains(sideEffect3), iz(true))

        sideEffects.remove(sideEffect1)
        assertThat(sideEffects.contains(sideEffect1), iz(false))
        assertThat(sideEffects.count(), iz(2))

        sideEffects.remove(sideEffect2)
        assertThat(sideEffects.contains(sideEffect2), iz(false))
        assertThat(sideEffects.count(), iz(1))

        sideEffects.remove(sideEffect3)
        assertThat(sideEffects.contains(sideEffect3), iz(false))
        assertThat(sideEffects.isEmpty(), iz(true))
    }
}
