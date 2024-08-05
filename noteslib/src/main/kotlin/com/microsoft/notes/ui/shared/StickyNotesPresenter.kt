package com.microsoft.notes.ui.shared

import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.ui.LifecycleCallbacks
import com.microsoft.notes.utils.logging.EventMarkers

abstract class StickyNotesPresenter : LifecycleCallbacks {

    override var shouldHandleStateUpdates: Boolean = false
    override var areListenersAdded: Boolean = false

    override fun onStart() {
        shouldHandleStateUpdates = true
        if (!areListenersAdded) {
            addUiBindings()
            areListenersAdded = true
        }
    }

    override fun onResume() {
        // no action
    }

    override fun onPause() {
        // no action
    }

    override fun onStop() {
        shouldHandleStateUpdates = false
    }

    override fun onDestroy() {
        if (areListenersAdded) {
            removeUiBindings()
            areListenersAdded = false
        }
    }

    fun finish() {
        shouldHandleStateUpdates = false
    }

    // telemetry
    open fun recordTelemetry(eventMarker: EventMarkers, vararg keyValuePairs: Pair<String, String>) {
        NotesLibrary.getInstance().recordTelemetry(eventMarker, *keyValuePairs)
    }

    protected fun runIfActivityIsRunning(block: () -> Unit) {
        if (shouldHandleStateUpdates) {
            block()
        }
    }

    protected fun runOnClientThread(fn: () -> Unit) {
        NotesLibrary.getInstance().clientThread?.execute { fn() } ?: fn()
    }

    abstract fun addUiBindings()
    abstract fun removeUiBindings()
}
