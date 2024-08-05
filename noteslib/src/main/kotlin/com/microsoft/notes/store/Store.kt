package com.microsoft.notes.store

import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.AuthAction
import com.microsoft.notes.store.action.CompoundAction
import com.microsoft.notes.store.action.CreationAction
import com.microsoft.notes.store.action.DeleteAction
import com.microsoft.notes.store.action.NoteReferenceAction
import com.microsoft.notes.store.action.OutboundQueueSyncStatusAction
import com.microsoft.notes.store.action.PreferencesAction
import com.microsoft.notes.store.action.ReadAction
import com.microsoft.notes.store.action.SamsungNotesResponseAction
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.action.SyncStateAction
import com.microsoft.notes.store.action.UIAction
import com.microsoft.notes.store.action.UpdateAction
import com.microsoft.notes.store.reducer.AuthReducer
import com.microsoft.notes.store.reducer.CreationReducer
import com.microsoft.notes.store.reducer.DeleteReducer
import com.microsoft.notes.store.reducer.NoteReferenceReducer
import com.microsoft.notes.store.reducer.OutboundQueueSyncStatusReducer
import com.microsoft.notes.store.reducer.PreferencesReducer
import com.microsoft.notes.store.reducer.ReadReducer
import com.microsoft.notes.store.reducer.SamsungNotesReducer
import com.microsoft.notes.store.reducer.SyncResponseReducer
import com.microsoft.notes.store.reducer.SyncStateReducer
import com.microsoft.notes.store.reducer.UIReducer
import com.microsoft.notes.store.reducer.UpdateReducer
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.threading.ExecutorServices
import com.microsoft.notes.utils.threading.ThreadExecutor
import com.microsoft.notes.utils.threading.ThreadExecutorService
import java.util.concurrent.LinkedBlockingQueue

class StoreThreadService : ThreadExecutorService(
    ExecutorServices.store
)

open class Store(
    val sideEffects: SideEffectsList = SideEffectsList(),
    @Suppress("DEPRECATION") @Deprecated("You should use ui sideeffect to communicate back to the client")
    val stateHandlers: StateHandlersList = StateHandlersList(),
    val storeThread: ThreadExecutor? = null,
    val notesLogger: NotesLogger? = null,
    val createdByApp: String,
    val isDebugMode: Boolean
) {

    var state = State()
        protected set

    private var actions = LinkedBlockingQueue<Triple<Action, ThreadExecutor?, PromiseImpl<State>>>()

    /**
     Important : Note that the @param Promise makes just sense and will be invoked when we run
     on an asynchronous way since that's the sense of a Promise.
     When we run synchronous the Promise we return will never be invoked.
     */
    @Synchronized
    fun dispatch(action: Action, callbackThread: ThreadExecutor? = null): Promise<State> {
        val promise = PromiseImpl<State>()
        actions.offer(Triple(action, callbackThread, promise))
        if (storeThread != null) {
            storeThread.execute {
                handle(actions.poll())
            }
        } else {
            handle(actions.remove())
        }
        return promise
    }

    private fun handle(data: Triple<Action, ThreadExecutor?, PromiseImpl<State>>) =
        handle(data.first, data.second, data.third)

    private fun handle(
        action: Action,
        callbackThread: ThreadExecutor?,
        promise: PromiseImpl<State>
    ) {
        val newState = reduce(action, state)

        if (newState != state) {
            // State has changed, we need to dispatch
            dispatch(newState)
        }
        sideEffects.dispatch(action, state)
        if (callbackThread != null) {
            callbackThread.execute { promise.resolve(newState) }
        } else {
            promise.resolve(newState)
        }
    }

    internal fun dispatch(newState: State) {
        state = newState
        @Suppress("DEPRECATION")
        stateHandlers.dispatch(newState)
    }

    private fun reduce(action: Action, currentState: State): State {
        notesLogger?.i(message = "ACTION: ${action.toPIIFreeString()}")
        val newState = when (action) {
            is CompoundAction -> {
                action.actions.fold(currentState) { state, nextAction ->
                    reduceSimpleAction(nextAction, state)
                }
            }
            else -> reduceSimpleAction(action, currentState)
        }
        notesLogger?.i(message = "STATE: $newState")
        return newState
    }

    private fun reduceSimpleAction(action: Action, currentState: State): State {
        notesLogger?.d(message = "SIMPLE ACTION: $action")
        return when (action) {
            is CreationAction -> CreationReducer.reduce(action, currentState, notesLogger, isDebugMode)
            is UpdateAction -> UpdateReducer.reduce(action, currentState, notesLogger, isDebugMode)
            is ReadAction -> ReadReducer.reduce(action, currentState, notesLogger, isDebugMode)
            is DeleteAction -> DeleteReducer.reduce(action, currentState, notesLogger, isDebugMode)
            is SyncResponseAction -> SyncResponseReducer.reduce(action, currentState, notesLogger, isDebugMode)
            is AuthAction -> AuthReducer.reduce(action, currentState, notesLogger, isDebugMode)
            is SyncStateAction -> SyncStateReducer.reduce(action, currentState, notesLogger, isDebugMode)
            is OutboundQueueSyncStatusAction ->
                OutboundQueueSyncStatusReducer.reduce(action, currentState, notesLogger, isDebugMode)
            is UIAction -> UIReducer.reduce(action, currentState, notesLogger, isDebugMode)
            is NoteReferenceAction -> NoteReferenceReducer.reduce(action, currentState, notesLogger, isDebugMode)
            is SamsungNotesResponseAction -> SamsungNotesReducer.reduce(action, currentState, notesLogger, isDebugMode)
            is PreferencesAction -> PreferencesReducer.reduce(action, currentState, notesLogger, isDebugMode)
            else -> currentState
        }
    }
}
