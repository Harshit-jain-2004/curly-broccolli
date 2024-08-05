package com.microsoft.notes.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.microsoft.notes.utils.utils.Constants

class AlarmBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val noteLocalId: String = intent?.getStringExtra(Constants.NOTE_LOCAL_ID) ?: return
        val noteDeepLink = Constants.NOTE_DEEP_LINK_PREFIX.plus(noteLocalId)

        val notePreview: String = intent.extras?.get(Constants.NOTE_PREVIEW).toString()
        val alarmDateTime: String = intent.extras?.get(Constants.ALARM_DATE_TIME).toString()
        val alarmLocalId: String = intent.extras?.get(Constants.ALARM_LOCAL_ID).toString()

        val timeReminderNotification = TimeReminderNotification(
            context.applicationContext,
            TimeReminderNotificationParams(
                alarmLocalId.hashCode(),
                noteDeepLink,
                notePreview,
                alarmDateTime
            )
        )
        timeReminderNotification.showNotification()
    }
}
