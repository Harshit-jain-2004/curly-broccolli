package com.microsoft.notes.reminders

import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.microsoft.notes.utils.utils.Constants.REMINDER_CHANNEL_DESC
import com.microsoft.notes.utils.utils.Constants.REMINDER_CHANNEL_ID
import com.microsoft.notes.utils.utils.Constants.REMINDER_CHANNEL_NAME
import android.app.NotificationManager as AndroidNotificationManager

class NotificationManager private constructor(context: Context) {
    private val notificationManager =
        context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager

    companion object {
        private var INSTANCE: NotificationManager? = null

        fun getInstance(context: Context): NotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationManager(context.applicationContext).also {
                    INSTANCE = it
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.createReminderNotificationChannel()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createReminderNotificationChannel() {
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            REMINDER_CHANNEL_NAME,
            AndroidNotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = REMINDER_CHANNEL_DESC
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showNotification(
        notificationBuilder: NotificationCompat.Builder,
        notificationId: Int
    ) {
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
