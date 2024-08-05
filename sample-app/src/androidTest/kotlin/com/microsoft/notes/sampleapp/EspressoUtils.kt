package com.microsoft.notes.sampleapp

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers

fun <T : View> getView(id: Int): T {
    val viewGetterAction = ViewGetterAction(id)
    onView(ViewMatchers.withId(id))
            .perform(viewGetterAction)
    return viewGetterAction.getView() as T
}