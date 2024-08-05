package com.microsoft.notes.store

import com.microsoft.notes.ui.noteslist.UserNotifications

fun State.getNotificationsForAllUsers(): Map<String, UserNotifications> =
    userIDToUserStateMap.map { it.key to it.value.userNotifications }.toMap()
