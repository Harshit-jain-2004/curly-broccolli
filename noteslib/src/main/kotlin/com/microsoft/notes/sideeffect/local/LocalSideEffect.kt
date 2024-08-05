package com.microsoft.notes.sideeffect.local

import com.microsoft.notes.store.SideEffect
import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateTimeReminderAction
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.threading.ExecutorServices
import com.microsoft.notes.utils.threading.ThreadExecutor
import com.microsoft.notes.utils.threading.ThreadExecutorService

class LocalSideEffectExecutorService : ThreadExecutorService(
    ExecutorServices.systemSideEffect
)

class LocalSideEffect(
    val systemThread: ThreadExecutor? = null,
    val reminderManager: TimeReminderManager,
    val notesLogger: NotesLogger? = null
) : SideEffect(systemThread) {
    override fun handle(action: Action, state: State) {

        when (action) {
            is UpdateTimeReminderAction -> {
                reminderManager.calculateAndScheduleReminder(
                    noteId = action.noteLocalId,
                    reminder = action.timeReminder
                )
            }
        }
    }
}
