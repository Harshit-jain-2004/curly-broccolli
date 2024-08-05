package com.microsoft.notes.ui.noteslist

import com.microsoft.notes.store.SyncErrorState
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class UserNotificationTests {
    @Test
    fun should_return_sync_user_notification() {
        val testUserNotifications = UserNotifications().with(SyncErrorUserNotification(SyncErrorState.GenericError))
        assert(testUserNotifications.get() is SyncErrorUserNotification)
    }

    @Test
    fun should_return_future_note_user_notification() {
        val testUserNotifications = UserNotifications().with(FutureNoteUserNotification(1))
        assert(testUserNotifications.get() is FutureNoteUserNotification)
    }

    @Test
    fun should_return_sync_note_user_notification_when_multiple_notifications_is_present() {
        val testUserNotifications = UserNotifications().with(FutureNoteUserNotification(1))
            .with(SyncErrorUserNotification(SyncErrorState.GenericError))

        assert(testUserNotifications.get() is SyncErrorUserNotification)
    }

    @Test
    fun should_return_user_notification_by_type() {
        val testUserNotifications = UserNotifications().with(FutureNoteUserNotification(1))
            .with(SyncErrorUserNotification(SyncErrorState.GenericError))

        assert(testUserNotifications.get(UserNotificationType.SyncError) is SyncErrorUserNotification)
    }

    @Test
    fun should_return_user_notification_from_remaining_user_notifications() {
        val testUserNotifications = UserNotifications().with(FutureNoteUserNotification(1))
            .with(SyncErrorUserNotification(SyncErrorState.GenericError))
            .remove(UserNotificationType.SyncError)

        assert(testUserNotifications.get() is FutureNoteUserNotification)
        assertThat(testUserNotifications.get(UserNotificationType.SyncError), nullValue())
    }

    @Test
    fun should_return_contains_as_true_for_avaialble_user_notification() {
        val testUserNotifications = UserNotifications().with(FutureNoteUserNotification(1))

        assertThat(testUserNotifications.containsType(UserNotificationType.SyncError), iz(false))
        assertThat(testUserNotifications.containsType(UserNotificationType.FutureNote), iz(true))
    }
}
