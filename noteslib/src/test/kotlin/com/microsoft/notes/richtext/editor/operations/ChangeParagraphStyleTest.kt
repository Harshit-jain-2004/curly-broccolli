package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.editor.ParagraphFormattingProperty
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle
import com.microsoft.notes.richtext.scheme.Range
import com.microsoft.notes.richtext.scheme.asParagraph
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ChangeParagraphStyleTest {

    @Test
    fun should_set_bullets_to_true() {
        val style = ParagraphStyle()
        val newStyle = style.setProperty(ParagraphFormattingProperty.BULLETS, true)
        assertThat(newStyle, iz(ParagraphStyle(unorderedList = true)))
    }

    @Test
    fun should_set_bullets_to_false() {
        val style = ParagraphStyle(unorderedList = true)
        val newStyle = style.setProperty(ParagraphFormattingProperty.BULLETS, false)
        assertThat(newStyle, iz(ParagraphStyle()))
    }

    @Test
    fun should_changeParagraphStyle_on_specified_paragraph() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two")),
                    Paragraph(content = Content("three"))
                )
            )
        )
        val newState = state.changeParagraphStyle(
            state.document.blocks[1].asParagraph(),
            ParagraphFormattingProperty.BULLETS, true
        )
        assertThat(newState.document.blocks[0], iz(state.document.blocks[0]))
        assertThat(newState.document.blocks[2], iz(state.document.blocks[2]))
        with(newState.document.blocks[1].asParagraph()) {
            assertThat(style, iz(ParagraphStyle(unorderedList = true)))
        }
    }

    @Test
    fun should_changeParagraphStyle_on_single_paragraph_selection() {
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two")),
                    Paragraph(content = Content("three"))
                ),
                range = Range(startBlock = 1, startOffset = 1, endBlock = 1, endOffset = 1)
            )
        )
        val newState = state.changeParagraphStyle(ParagraphFormattingProperty.BULLETS, true)
        assertThat(newState.document.blocks[0], iz(state.document.blocks[0]))
        assertThat(newState.document.blocks[2], iz(state.document.blocks[2]))
        with(newState.document.blocks[1].asParagraph()) {
            assertThat(style, iz(ParagraphStyle(unorderedList = true)))
        }
    }

    @Test
    fun should_changeParagraphStyle_on_multi_paragraph_selection() {
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two")),
                    Paragraph(content = Content("three"))
                ),
                range = Range(startBlock = 0, startOffset = 1, endBlock = 2, endOffset = 1)
            )
        )
        val newState = state.changeParagraphStyle(ParagraphFormattingProperty.BULLETS, true)
        with(newState.document.blocks[0].asParagraph()) {
            assertThat(style, iz(ParagraphStyle(unorderedList = true)))
        }
        with(newState.document.blocks[1].asParagraph()) {
            assertThat(style, iz(ParagraphStyle(unorderedList = true)))
        }
        with(newState.document.blocks[2].asParagraph()) {
            assertThat(style, iz(ParagraphStyle(unorderedList = true)))
        }
    }
}
