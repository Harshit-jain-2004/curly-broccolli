package com.microsoft.notes.reminders

import androidx.core.app.NotificationCompat

interface INotification {
    fun getNotificationBuilder(): NotificationCompat.Builder
    fun showNotification()
}

abstract class NotificationParams(val notificationId: Int)
