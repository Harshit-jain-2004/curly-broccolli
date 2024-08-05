package com.microsoft.notes.reminders

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.utils.utils.Constants.REMINDER_CHANNEL_ID

abstract class ReminderNotification(private val context: Context) : INotification {
    override fun getNotificationBuilder(): NotificationCompat.Builder {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        } else {
            NotificationCompat.Builder(context)
        }

        // Setting onenote icon and icon color for notification directly here
        // When the app is not running and alarm is triggered, NotesLibrary is not initialized.
        // therefore we cannot get NotesThemeOverride instance from NotesLibrary (and can't get the icon and color passed from host)
        // In future we will have to decouple noteslibrary initialization from app start,
        // so that noteslibrary actions (themes, db  etc) can be done when app is not running

        return builder
            .setSmallIcon(R.drawable.onenote_logo_notification_icon_small)
            .setColor(ContextCompat.getColor(context, R.color.sn_primary_color))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
    }
}
