package com.microsoft.notes.notesReference

import com.microsoft.notes.store.Promise
import com.microsoft.notes.store.State
import com.microsoft.notes.utils.threading.ExecutorServices
import com.microsoft.notes.utils.threading.ThreadExecutor
import com.microsoft.notes.utils.threading.ThreadExecutorService
import org.jdeferred.impl.DeferredObject

/**
 * Class to help process NoteRef Change signals serially
 * We translate NoteRef Change signals (eg: onPageChangedLocalSignal) to NoteRefChanges
 * and dispatch ApplyChangesAction on that.
 * This calculation of NoteRefChanges should be done only after all previous ApplyChangesAction have been processed.
 */
class SerialActionDispatcher {
    private val dispatcherThread: ThreadExecutor = object : ThreadExecutorService(ExecutorServices.serialNoteRefDispatcher) {}

    // Promise callback thread should be different than dispatcherThread
    fun dispatchActionTask(action: () -> Promise<State>) {
        dispatcherThread.execute {
            val d = DeferredObject<Unit, Unit, Unit>()
            val promise = action.invoke()
            promise.then {
                d.resolve(Unit)
            }
            promise.fail {
                d.resolve(Unit)
            }

            while (d.isPending) {
                try {
                    d.waitSafely()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
