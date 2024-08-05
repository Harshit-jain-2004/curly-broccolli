package com.microsoft.notes.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.microsoft.notes.utils.utils.Constants

class TimeReminderNotification(
    private val context: Context,
    private val notificationParams: TimeReminderNotificationParams
) :
    ReminderNotification(context) {
    private var notificationManager: NotificationManager = NotificationManager.getInstance(context)

    override fun showNotification() {
        val builder = getNotificationBuilder()
            .setContentTitle(notificationParams.notePreview)
            .setContentText(notificationParams.alarmDateTime)
            .setContentIntent(
                getIntentToOpenNote(
                    notificationParams.noteDeepLink,
                    notificationParams.notificationId
                )
            )
            .setAutoCancel(true)

        this.notificationManager.showNotification(builder, notificationParams.notificationId)
    }

    private fun getIntentToOpenNote(noteDeepLink: String?, notificationId: Int): PendingIntent {
        val deepLinkIntent =
            Intent(Constants.ACTION_OPEN_SN_FROM_DEEPLINK, Uri.parse(noteDeepLink))

        return PendingIntent.getActivity(
            context,
            notificationId,
            deepLinkIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

class TimeReminderNotificationParams(
    notificationId: Int,
    val noteDeepLink: String,
    val notePreview: String,
    val alarmDateTime: String
) : NotificationParams(notificationId)
