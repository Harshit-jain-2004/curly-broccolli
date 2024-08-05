package com.microsoft.notes.noteslib

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.microsoft.notes.sync.RequestPriority
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.`is` as iz

@RunWith(AndroidJUnit4::class)
class RequestPriorityTest {

    private lateinit var notesLibrary: NotesLibrary

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getTargetContext()
        val notesLibraryConfiguration = TestNotesLibraryConfiguration.Builder.build(context, null)
        NotesLibrary.init(notesLibraryConfiguration)
        notesLibrary = NotesLibrary.getInstance()
    }

    @Test
    fun should_set_Request_Priority_to_background_by_default() {
        assertThat(notesLibrary.requestPriority, iz(RequestPriority.background))
    }

    @Test
    fun should_set_Request_Priority_to_foreground() {
        notesLibrary.setRequestPriority(RequestPriority.foreground)
        assertThat(notesLibrary.requestPriority, iz(RequestPriority.foreground))
    }

    @Test
    fun should_reset_Request_Priority_to_background() {
        notesLibrary.setRequestPriority(RequestPriority.foreground)
        notesLibrary.setRequestPriority(RequestPriority.background)
        assertThat(notesLibrary.requestPriority, iz(RequestPriority.background))
    }
}
