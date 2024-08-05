package com.microsoft.notes.store

import com.microsoft.notes.store.action.Action
import com.microsoft.notes.utils.threading.ThreadExecutor
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Subscriber is not subscribed to its source automatically, is up to each Suscriber to subscribe to its
 * specific source.
 */
abstract class Subscriber<in T>(private val executeOnThisThread: ThreadExecutor? = null) {

    fun onNext(data: T) {
        executeOnThisThread?.execute { handle(data) } ?: handle(data)
    }

    abstract fun handle(data: T)
}

typealias SideEffectsList = CopyOnWriteArrayList<SideEffect>

abstract class SideEffect(private val executeOnThisThread: ThreadExecutor? = null) {
    fun onNext(action: Action, state: State) {
        executeOnThisThread?.execute { handle(action, state) } ?: handle(action, state)
    }

    abstract fun handle(action: Action, state: State)
}

fun SideEffectsList.dispatch(action: Action, state: State) {
    forEach { it.onNext(action, state) }
}

@Deprecated(message = "You should use uiBindings to communicate back to the client instead of state handlers")
typealias StateHandlersList = CopyOnWriteArrayList<StateHandler>

abstract class StateHandler(private val executeOnThisThread: ThreadExecutor? = null) {
    fun onNext(state: State) {
        executeOnThisThread?.execute { handle(state) } ?: handle(state)
    }

    abstract fun handle(state: State)
}

@Suppress("DEPRECATION")
@Deprecated(message = "You should use uiBindings to communicate back to the client instead of state handlers")
fun StateHandlersList.dispatch(state: State) {
    forEach { it.onNext(state) }
}
