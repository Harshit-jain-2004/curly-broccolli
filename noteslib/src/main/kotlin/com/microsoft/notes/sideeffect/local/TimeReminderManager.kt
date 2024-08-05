package com.microsoft.notes.sideeffect.local

import android.content.Context
import com.microsoft.notes.models.Reminder
import com.microsoft.notes.models.ScheduledAlarm

class TimeReminderManager private constructor(
    private val appContext: Context,
    private val localAlarmManager: LocalAlarmManager
) {

    companion object {
        @Volatile
        private var INSTANCE: TimeReminderManager? = null

        fun getInstance(appContext: Context): TimeReminderManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimeReminderManager(
                    appContext.applicationContext,
                    LocalAlarmManager()
                ).also {
                    INSTANCE = it
                }
            }
        }
    }

    fun calculateAndScheduleReminder(
        noteId: String,
        reminder: Reminder.TimeReminder
    ) {
        val reminderInstances = ReminderInstanceCalculator.getReminderInstances(reminder)

        /**
         *  ToDo: compare with existing reminders of the note, based on that create/update/delete reminders
         *  For now scheduling the reminders directly
         */
        for (reminderInstance in reminderInstances) {
            val scheduledAlarm =
                ScheduledAlarm(noteId, noteId.plus(reminderInstance), reminderInstance, false)

            localAlarmManager.scheduleAlarm(
                context = appContext,
                alarmDateTime = scheduledAlarm.alarmDateTime,
                alarmLocalId = scheduledAlarm.alarmLocalId,
                noteLocalId = noteId
            )

            // ToDo Insert Scheduled Alarm in DB
        }
    }
}
