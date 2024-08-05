package com.microsoft.notes.ui.note.reminder

import com.microsoft.notes.models.RecurrenceType
import com.microsoft.notes.models.Reminder.TimeReminder
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.richtext.editor.extensions.increaseRevision
import com.microsoft.notes.ui.shared.StickyNotesPresenter

class ReminderPresenter : StickyNotesPresenter() {

    lateinit var currentNoteId: String

    @Suppress("UNUSED_PARAMETER")
    fun addTimeReminder(reminderTime: Long) {
        val timeReminder = TimeReminder(reminderTime, RecurrenceType.SINGLE)
        val note = NotesLibrary.getInstance().getNoteById(currentNoteId) ?: return
        note.localId.let {
            NotesLibrary.getInstance()
                .updateTimeReminder(it, timeReminder, note.uiRevision.increaseRevision())
        }
    }

    override fun addUiBindings() {}

    override fun removeUiBindings() {}
}
