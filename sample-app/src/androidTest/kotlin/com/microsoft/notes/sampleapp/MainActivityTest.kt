package com.microsoft.notes.sampleapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.`is` as iz

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    var activityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun test_basic_edit_note_typing_functionality() {
        val beforeRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        val oldCount = beforeRecyclerView?.adapter?.itemCount as Int

        val text = "Espresso: ${Math.random()}"
        onView(withId(R.id.newNoteFab)).perform(click())
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText(text))
                .check(matches(withText("$text\n")))
                .perform(closeSoftKeyboard(), pressBack())

        val afterRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        assertThat(afterRecyclerView?.adapter?.itemCount, iz(oldCount + 1))
    }

    @Test
    fun test_basic_edit_note_inking_functionality() {
        val beforeRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        val oldCount = beforeRecyclerView?.adapter?.itemCount as Int

        onView(withId(R.id.newInkNoteFab)).perform(click())
        onView(withId(R.id.noteGalleryItemInkView))
                .perform(swipeDown())
                .perform(pressBack())

        val afterRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        assertThat(afterRecyclerView?.adapter?.itemCount, iz(oldCount + 1))
    }

    @Test
    fun test_edit_to_list_then_reedit_functionality() {
        val beforeRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        val oldCount = beforeRecyclerView?.adapter?.itemCount as Int

        val text1 = "Espresso: ${Math.random()}"
        onView(withId(R.id.newNoteFab)).perform(click())
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText(text1))
                .perform(closeSoftKeyboard(), pressBack())

        var afterRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        assertThat(afterRecyclerView?.adapter?.itemCount, iz(oldCount + 1))

        val text2 = "Espresso: ${Math.random()}"
        onView(withId(R.id.notesRecyclerView))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        onView(withId(R.id.noteBodyEditText))
                .check(matches(withText("$text1\n")))
                .perform(typeText(text2), closeSoftKeyboard())
                .check(matches(withText("$text1$text2\n")))
                .perform(pressBack())

        afterRecyclerView = getView(R.id.notesRecyclerView)
        assertThat(afterRecyclerView?.adapter?.itemCount, iz(oldCount + 1))
    }

    @Test
    fun test_back_on_empty_note_deletes_note_functionality() {
        val beforeRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        val oldCount = beforeRecyclerView?.adapter?.itemCount as Int

        onView(withId(R.id.newNoteFab)).perform(click())
        onView(withId(R.id.noteBodyEditText))
                .perform(closeSoftKeyboard(), pressBack())

        val afterRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        assertThat(afterRecyclerView?.adapter?.itemCount, iz(oldCount))
    }

    @Test
    fun test_back_on_empty_ink_note_deletes_ink_note_functionality() {
        val beforeRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        val oldCount = beforeRecyclerView?.adapter?.itemCount as Int

        onView(withId(R.id.newInkNoteFab)).perform(click())
        onView(withId(R.id.noteGalleryItemInkView)).perform(pressBack())

        val afterRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        assertThat(afterRecyclerView?.adapter?.itemCount, iz(oldCount))
    }

    @Test
    fun test_back_on_empty_note_deletes_note_after_style_press_functionality() {
        val beforeRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        val oldCount = beforeRecyclerView?.adapter?.itemCount as Int

        onView(withId(R.id.newNoteFab)).perform(click())
        onView(withId(R.id.boldButton)).perform(click())
        onView(withId(R.id.noteBodyEditText))
                .perform(closeSoftKeyboard(), pressBack())

        val afterRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        assertThat(afterRecyclerView?.adapter?.itemCount, iz(oldCount))
    }

    @Test
    fun test_delete_note_functionality() {
        val beforeRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        val oldCount = beforeRecyclerView?.adapter?.itemCount as Int

        val text = "Espresso: ${Math.random()}"
        onView(withId(R.id.newNoteFab)).perform(click())
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText(text))
                .perform(closeSoftKeyboard())

        onView(withId(R.id.edit_note_options)).perform(click())
        onView(withId(R.id.deleteNote)).perform(click())
        onView(withText("DELETE")).perform(click())

        val afterRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        assertThat(afterRecyclerView?.adapter?.itemCount, iz(oldCount))
    }

    @Test
    fun test_delete_ink_note_functionality() {
        val beforeRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        val oldCount = beforeRecyclerView?.adapter?.itemCount as Int

        onView(withId(R.id.newInkNoteFab)).perform(click())
        onView(withId(R.id.noteGalleryItemInkView)).perform(swipeDown())
        onView(withId(R.id.edit_note_options)).perform(click())
        onView(withId(R.id.deleteNote)).perform(click())
        onView(withText("DELETE")).perform(click())

        val afterRecyclerView = getView<RecyclerView>(R.id.notesRecyclerView)
        assertThat(afterRecyclerView?.adapter?.itemCount, iz(oldCount))
    }
}