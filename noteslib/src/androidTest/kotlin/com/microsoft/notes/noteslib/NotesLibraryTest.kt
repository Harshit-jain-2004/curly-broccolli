package com.microsoft.notes.noteslib

import android.content.Context
import androidx.test.runner.AndroidJUnit4
import com.microsoft.notes.models.Note
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.hamcrest.CoreMatchers.`is` as iz

@RunWith(AndroidJUnit4::class)
class NotesLibraryTest {

    @Mock
    lateinit var context: Context
    lateinit var notesLibrary: NotesLibrary

    val LOCAL_ID_1 = "localId1"
    val TEST_USER_ID = "test@outlook.com"

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val notesLibraryConfiguration = TestNotesLibraryConfiguration.Builder.build(context, null)
        NotesLibrary.init(notesLibraryConfiguration)
        notesLibrary = NotesLibrary.getInstance()
    }

    // FIXME add some tests for library functions
    @Test
    @Ignore
    fun should_get_Note_by_id() {
        val sourceNote = Note(localId = LOCAL_ID_1)
        notesLibrary.addNote(sourceNote, TEST_USER_ID)

        val resultNote = notesLibrary.getNoteById(LOCAL_ID_1)
        assertThat(resultNote, iz(sourceNote))
    }
}
