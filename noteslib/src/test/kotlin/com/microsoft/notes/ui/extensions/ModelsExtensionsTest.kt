package com.microsoft.notes.ui.extensions

import android.app.Application
import android.content.Context
import android.os.Build
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.BuildConfig
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.DocumentType
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.hamcrest.CoreMatchers.`is` as iz

@Ignore("Ignore this test.We will enable it again once we move to androidX and update robolectric version")
@RunWith(RobolectricTestRunner::class)
@Config(
    constants = BuildConfig::class,
    sdk = [Build.VERSION_CODES.LOLLIPOP],
    application = Application::class
)
class ModelsExtensionsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.application
    }

    @Test
    fun `should filter future and deleted notes`() {
        val future1 = Note(document = Document(type = DocumentType.FUTURE))
        val future2 = Note(document = Document(type = DocumentType.FUTURE))
        val future3 = Note(document = Document(type = DocumentType.FUTURE))
        val ink1 = Note(document = Document(type = DocumentType.RENDERED_INK))
        val ink2 = Note(document = Document(type = DocumentType.RENDERED_INK))
        val text1 = Note(document = Document(type = DocumentType.RICH_TEXT), isDeleted = true)
        val text2 = Note(document = Document(type = DocumentType.RICH_TEXT))

        val notesCollection = listOf(future1, future2, future3, ink1, ink2, text1, text2)
        val notesFiltered = notesCollection.filterDeletedAndFutureNotes()
        with(notesFiltered) {
            assertThat(size, iz(3))
            assertThat(component1(), iz(ink1))
            assertThat(component2(), iz(ink2))
            assertThat(component3(), iz(text2))
        }
    }
}
