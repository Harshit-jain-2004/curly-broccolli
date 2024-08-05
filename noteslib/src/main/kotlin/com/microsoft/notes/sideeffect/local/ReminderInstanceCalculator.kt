package com.microsoft.notes.sideeffect.local

import com.microsoft.notes.models.RecurrenceType
import com.microsoft.notes.models.Reminder

object ReminderInstanceCalculator {

    fun getReminderInstances(reminder: Reminder.TimeReminder): List<Long> {
        val instanceList = mutableListOf<Long>()
        when (reminder.recurrenceType) {
            RecurrenceType.SINGLE -> instanceList.add(reminder.reminderDateTime)
            else -> {}
        }
        return instanceList.toList()
    }
}
