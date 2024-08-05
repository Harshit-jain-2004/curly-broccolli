package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class InsertOrRemoveBlockTest {

    @Test
    fun should_remove_block() {
        val state = EditorState(Document(listOf(Paragraph(), Paragraph(), InlineMedia())))
        val newState = state.removeBlock(state.document.blocks[1])
        assertThat(newState.document.blocks, iz(listOf(state.document.blocks[0], state.document.blocks[2])))
    }

    @Test
    fun should_insert_before_at_beginning() {
        val state = EditorState(Document(listOf(Paragraph(), Paragraph())))
        val newBlock = InlineMedia()
        val newState = state.insertBlockBefore(newBlock, state.document.blocks[0])
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    newBlock, state.document.blocks[0],
                    state.document.blocks[1]
                )
            )
        )
    }

    @Test
    fun should_insert_before_in_middle() {
        val state = EditorState(Document(listOf(Paragraph(), Paragraph())))
        val newBlock = InlineMedia()
        val newState = state.insertBlockBefore(newBlock, state.document.blocks[1])
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0], newBlock,
                    state.document.blocks[1]
                )
            )
        )
    }

    @Test
    fun should_insert_after_at_end() {
        val state = EditorState(Document(listOf(Paragraph(), Paragraph())))
        val newBlock = InlineMedia()
        val newState = state.insertBlockAfter(newBlock, state.document.blocks[1])
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0], state.document.blocks[1],
                    newBlock
                )
            )
        )
    }

    @Test
    fun should_insert_after_in_middle() {
        val state = EditorState(Document(listOf(Paragraph(), Paragraph())))
        val newBlock = InlineMedia()
        val newState = state.insertBlockAfter(newBlock, state.document.blocks[0])
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0], newBlock,
                    state.document.blocks[1]
                )
            )
        )
    }
}
