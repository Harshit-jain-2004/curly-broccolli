package com.microsoft.notes.sampleapp

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

class ViewGetterAction(private val id: Int) : ViewAction {
    private var view: View? = null

    override fun perform(uiController: UiController?, view: View?) {
        this.view = view
    }

    override fun getConstraints(): Matcher<View> = ViewMatchers.withId(id)

    override fun getDescription(): String = "performing GetRecyclerViewAction on view with id: $id"

    fun getView(): View? = view
}