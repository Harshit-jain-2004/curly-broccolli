package com.microsoft.notes.ui.noteslist

import com.microsoft.notes.store.SyncErrorState

data class UserNotifications(val userNotifications: Map<UserNotificationType, UserNotification> = emptyMap()) {
    fun with(userNotification: UserNotification): UserNotifications {
        val mutableUserNotifications = userNotifications.toMutableMap()
        mutableUserNotifications.put(userNotification.type, userNotification)
        return copy(userNotifications = mutableUserNotifications)
    }

    fun remove(userNotificationType: UserNotificationType): UserNotifications {
        val mutableUserNotifications = userNotifications.toMutableMap()
        mutableUserNotifications.remove(userNotificationType)
        return copy(userNotifications = mutableUserNotifications)
    }

    fun containsType(userNotificationType: UserNotificationType): Boolean =
        userNotifications.containsKey(userNotificationType)

    fun get(): UserNotification? {
        if (userNotifications.containsKey(UserNotificationType.AuthError)) {
            return userNotifications.get(UserNotificationType.AuthError)
        } else if (userNotifications.containsKey(UserNotificationType.SyncError)) {
            return userNotifications.get(UserNotificationType.SyncError)
        } else if (userNotifications.containsKey(UserNotificationType.FutureNote)) {
            return userNotifications.get(UserNotificationType.FutureNote)
        }
        return null
    }

    fun get(type: UserNotificationType): UserNotification? {
        if (userNotifications.containsKey(type)) {
            return userNotifications.get(type)
        }
        return null
    }
}

data class UserNotificationUiRes(
    val title: String,
    val description: String?,
    val iconResId: Int,
    val type: UserNotificationType,
    val buttonInfo: NotificationButtonInfo? = null
)

data class NotificationButtonInfo(
    val title: String,
    val event: ClickEvent
)

typealias ClickEvent = () -> Unit

sealed class UserNotification(
    val type: UserNotificationType
)

class SyncErrorUserNotification(val syncErrorType: SyncErrorState) :
    UserNotification(UserNotificationType.SyncError)

class AuthErrorUserNotification :
    UserNotification(UserNotificationType.AuthError)

class FutureNoteUserNotification(val futureNoteCount: Int) :
    UserNotification(UserNotificationType.FutureNote)

// keep it sorted as per priority
// first item in enum has the highest priority
enum class UserNotificationType {
    AuthError,
    SyncError,
    FutureNote,
}

fun getUserNotification(userIdToNotificationsMap: Map<String, UserNotifications>):
    Pair<String, UserNotification>? {
    UserNotificationType.values().forEach { notificationType ->
        userIdToNotificationsMap.forEach { (userId, userNotifications) ->
            userNotifications.get(notificationType)?.let {
                return Pair(userId, it)
            }
        }
    }
    return null
}
