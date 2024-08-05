package com.microsoft.notes.ui.extensions

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

fun Context.sendAccessibilityAnnouncement(announcement: String) {
    val manager = this.getSystemService(Context.ACCESSIBILITY_SERVICE)
    if (manager is AccessibilityManager && manager.isEnabled) {
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
        if (event != null) {
            event.text.add(announcement)
            manager.sendAccessibilityEvent(event)
        }
    }
}
