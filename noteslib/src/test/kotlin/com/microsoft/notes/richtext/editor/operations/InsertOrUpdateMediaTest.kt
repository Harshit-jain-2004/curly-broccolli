package com.microsoft.notes.richtext.editor.operations

import android.os.Build
import com.microsoft.notes.noteslib.BuildConfig
import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.editor.RoboTestApplication
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Range
import org.junit.Assert.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.hamcrest.CoreMatchers.`is` as iz

@Ignore("Ignore this test.We will enable it again once we move to androidX and update robolectric version")
@RunWith(RobolectricTestRunner::class)
@Config(
    constants = BuildConfig::class,
    sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP),
    application = RoboTestApplication::class
)
class InsertOrUpdateMediaTest {

    val URL = "/data/image/test.png"
    val MIME_TYPE = "image/png"
    val ALT_TEXT = "alttext"

    @Test
    fun should_insertMedia_replaces_empty_paragraph() {
        val range = Range(startBlock = 1, startOffset = 0)
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(),
                    Paragraph()
                ),
                range = range
            )
        )
        val newMedia = InlineMedia(localUrl = URL, mimeType = MIME_TYPE)
        val newState = state.insertMedia(newMedia)
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    InlineMedia(localId = newState.document.blocks[1].localId, localUrl = URL, mimeType = MIME_TYPE),
                    state.document.blocks[2]
                )
            )
        )
    }

    @Test
    fun should_insertMedia_inserts_before_beginning_of_paragraph() {
        val range = Range(startBlock = 1, startOffset = 0)
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("test")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newMedia = InlineMedia(localUrl = URL, mimeType = MIME_TYPE)
        val newState = state.insertMedia(newMedia)
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    InlineMedia(localId = newState.document.blocks[1].localId, localUrl = URL, mimeType = MIME_TYPE),
                    state.document.blocks[1],
                    state.document.blocks[2]
                )
            )
        )
    }

    @Test
    fun should_insertMedia_inserts_after_end_of_paragraph() {
        val range = Range(startBlock = 1, startOffset = 4)
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("test")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newMedia = InlineMedia(localUrl = URL, mimeType = MIME_TYPE)
        val newState = state.insertMedia(newMedia)
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    state.document.blocks[1],
                    InlineMedia(localId = newState.document.blocks[2].localId, localUrl = URL, mimeType = MIME_TYPE),
                    state.document.blocks[2]
                )
            )
        )
    }

    @Test
    fun should_insertMedia_inserts_in_middle_of_paragraph_splits_before_and_after() {
        val range = Range(startBlock = 1, startOffset = 2)
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("test")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newMedia = InlineMedia(localUrl = URL, mimeType = MIME_TYPE)
        val newState = state.insertMedia(newMedia)
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(localId = state.document.blocks[1].localId, content = Content("te")),
                    InlineMedia(localId = newState.document.blocks[2].localId, localUrl = URL, mimeType = MIME_TYPE),
                    Paragraph(localId = newState.document.blocks[3].localId, content = Content("st")),
                    state.document.blocks[2]
                )
            )
        )
    }

    @Test
    fun should_insertMedia_inserts_before_media() {
        val range = Range(startBlock = 1, startOffset = 0)
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    InlineMedia(),
                    Paragraph()
                ),
                range = range
            )
        )
        val newMedia = InlineMedia(localUrl = URL, mimeType = MIME_TYPE)
        val newState = state.insertMedia(newMedia)
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    InlineMedia(localId = newState.document.blocks[1].localId, localUrl = URL, mimeType = MIME_TYPE),
                    state.document.blocks[1],
                    state.document.blocks[2]
                )
            )
        )
    }

    @Test
    fun should_insertMedia_inserts_after_media() {
        val range = Range(startBlock = 1, startOffset = 1)
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    InlineMedia(),
                    Paragraph()
                ),
                range = range
            )
        )
        val newMedia = InlineMedia(localUrl = URL, mimeType = MIME_TYPE)
        val newState = state.insertMedia(newMedia)
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    state.document.blocks[1],
                    InlineMedia(localId = newState.document.blocks[2].localId, localUrl = URL, mimeType = MIME_TYPE),
                    state.document.blocks[2]
                )
            )
        )
    }

    @Test
    fun should_updateMedia() {
        val range = Range(startBlock = 1, startOffset = 1)
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    InlineMedia(localUrl = URL, mimeType = MIME_TYPE),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.updateMediaWithAltText(state.document.blocks[1].localId, ALT_TEXT)
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    InlineMedia(
                        localId = newState.document.blocks[1].localId, localUrl = URL, mimeType = MIME_TYPE,
                        altText = ALT_TEXT
                    ),
                    state.document.blocks[2]
                )
            )
        )
    }
}
