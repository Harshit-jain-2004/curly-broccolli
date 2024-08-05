package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Range
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ReplaceRangeTest {
    @Test
    fun should_deleteBlocksAfterFirst_no_ops_on_single_line_selection() {
        val range = Range(
            startBlock = 1, startOffset = 1, endBlock = 1,
            endOffset = 2
        )
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
        val newState = state.deleteBlocksAfterFirst()
        assertThat(newState.document.blocks, iz(state.document.blocks))
    }

    @Test
    fun should_deleteBlocksAfterFirst_merges_next_paragraph() {
        val range = Range(
            startBlock = 1, startOffset = 3, endBlock = 2,
            endOffset = 0
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two"))
                ),
                range = range
            )
        )
        val newState = state.deleteBlocksAfterFirst()
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(localId = state.document.blocks[1].localId, content = Content("onetwo"))
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 3)))
    }

    @Test
    fun should_deleteBlocksAfterFirst_deletes_content_from_next_paragraph() {
        val range = Range(
            startBlock = 1, startOffset = 3, endBlock = 2,
            endOffset = 2
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two"))
                ),
                range = range
            )
        )
        val newState = state.deleteBlocksAfterFirst()
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(localId = state.document.blocks[1].localId, content = Content("oneo"))
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 3)))
    }

    @Test
    fun should_deleteBlocksAfterFirst_deletes_next_paragraph() {
        val range = Range(
            startBlock = 1, startOffset = 3, endBlock = 2,
            endOffset = 3
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two"))
                ),
                range = range
            )
        )
        val newState = state.deleteBlocksAfterFirst()
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    state.document.blocks[1]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 3)))
    }

    @Test
    fun should_deleteBlocksAfterFirst_deletes_paragraph_in_middle() {
        val range = Range(
            startBlock = 1, startOffset = 3, endBlock = 3,
            endOffset = 0
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.deleteBlocksAfterFirst()
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    state.document.blocks[1]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 3)))
    }

    @Test
    fun should_deleteBlocksAfterFirst_deletes_media_in_middle() {
        val range = Range(
            startBlock = 1, startOffset = 3, endBlock = 3,
            endOffset = 0
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    InlineMedia(),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.deleteBlocksAfterFirst()
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    state.document.blocks[1]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 3)))
    }

    @Test
    fun should_deleteBlocksAfterFirst_deletes_media_in_last_block_if_selected() {
        val range = Range(
            startBlock = 1, startOffset = 3, endBlock = 2,
            endOffset = 1
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    InlineMedia(),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.deleteBlocksAfterFirst()
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    state.document.blocks[1],
                    state.document.blocks[3]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 3)))
    }

    @Test
    fun should_deleteBlocksAfterFirst_leaves_media_in_last_block_if_not_selected() {
        val range = Range(
            startBlock = 1, startOffset = 3, endBlock = 2,
            endOffset = 0
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    InlineMedia(),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.deleteBlocksAfterFirst()
        assertThat(newState.document.blocks, iz(state.document.blocks))
        assertThat(newState.document.range, iz(Range(1, 3)))
        assertThat(newState.needsRender, iz(true))
    }

    @Test
    fun should_addExtraLines_no_ops_if_one_line() {
        val range = Range(startBlock = 1, startOffset = 3)
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.addExtraLines(emptyList())
        assertThat(newState.document, iz(state.document))
        assertThat(newState.document.range, iz(Range(1, 3)))
    }

    @Test
    fun should_addExtraLines_inserts_line_in_correct_position() {
        val range = Range(startBlock = 1, startOffset = 3)
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.addExtraLines(listOf("two"))
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    state.document.blocks[1],
                    Paragraph(localId = newState.document.blocks[2].localId, content = Content("two")),
                    state.document.blocks[2]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(2, 3)))
    }

    @Test
    fun should_addExtraLines_inserts_blank_line() {
        val range = Range(startBlock = 1, startOffset = 3)
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("after"))
                ),
                range = range
            )
        )
        val newState = state.addExtraLines(listOf(""))
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    state.document.blocks[1],
                    Paragraph(localId = newState.document.blocks[2].localId, content = Content()),
                    state.document.blocks[2]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(2, 0)))
    }

    @Test
    fun should_addExtraLines_inserts_multiple_lines() {
        val range = Range(startBlock = 1, startOffset = 3)
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.addExtraLines(listOf("two", "three"))
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    state.document.blocks[1],
                    Paragraph(localId = newState.document.blocks[2].localId, content = Content("two")),
                    Paragraph(localId = newState.document.blocks[3].localId, content = Content("three")),
                    state.document.blocks[2]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(3, 5)))
    }

    @Test
    fun should_addExtraLines_splits_in_middle_of_line() {
        val range = Range(startBlock = 1, startOffset = 3)
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one test")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.addExtraLines(listOf("two", "three"))
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(localId = state.document.blocks[1].localId, content = Content("one")),
                    Paragraph(localId = newState.document.blocks[2].localId, content = Content("two")),
                    Paragraph(localId = newState.document.blocks[3].localId, content = Content("three test")),
                    state.document.blocks[2]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(3, 5)))
    }

    @Test
    fun should_replaceFirstLine_inserts_text_in_middle() {
        val range = Range(
            startBlock = 1, startOffset = 4, endBlock = 1,
            endOffset = 4
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one two")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceFirstLine("test ")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(localId = state.document.blocks[1].localId, content = Content("one test two")),
                    state.document.blocks[2]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 9)))
    }

    @Test
    fun should_replaceFirstLine_replaces_single_paragraph_range() {
        val range = Range(
            startBlock = 1, startOffset = 4, endBlock = 1,
            endOffset = 8
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one two three")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceFirstLine("test ")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(localId = state.document.blocks[1].localId, content = Content("one test three")),
                    state.document.blocks[2]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 9)))
    }

    @Test
    fun should_replaceFirstLine_replaces_to_end_on_multi_paragraph_range() {
        val range = Range(
            startBlock = 1, startOffset = 4, endBlock = 2,
            endOffset = 2
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one two three")),
                    Paragraph(content = Content("after")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceFirstLine("test")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(localId = state.document.blocks[1].localId, content = Content("one test")),
                    state.document.blocks[2],
                    state.document.blocks[3]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 8, 2, 2)))
    }

    @Test
    fun should_replaceFirstLine_after_media_inserts_paragraph_after() {
        val range = Range(
            startBlock = 1, startOffset = 1, endBlock = 1,
            endOffset = 1
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    InlineMedia(),
                    Paragraph(content = Content("one two three")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceFirstLine("test")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    state.document.blocks[1],
                    Paragraph(localId = newState.document.blocks[2].localId, content = Content("test")),
                    state.document.blocks[2],
                    state.document.blocks[3]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(2, 4)))
        assertThat(newState.needsRender, iz(true))
    }

    @Test
    fun should_replaceFirstLine_after_media_with_multiline_selection() {
        val range = Range(
            startBlock = 1, startOffset = 1, endBlock = 2,
            endOffset = 8
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    InlineMedia(),
                    Paragraph(content = Content("one two three")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceFirstLine("test")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    state.document.blocks[1],
                    Paragraph(localId = newState.document.blocks[2].localId, content = Content("test")),
                    state.document.blocks[2],
                    state.document.blocks[3]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(2, 4, 3, 8)))
        assertThat(newState.needsRender, iz(true))
    }

    @Test
    fun should_replaceFirstLine_before_media_inserts_paragraph_before() {
        val range = Range(
            startBlock = 1, startOffset = 0, endBlock = 1,
            endOffset = 0
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    InlineMedia(),
                    Paragraph(content = Content("one two three")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceFirstLine("test")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(localId = newState.document.blocks[1].localId, content = Content("test")),
                    state.document.blocks[1],
                    state.document.blocks[2],
                    state.document.blocks[3]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 4, 2, 0)))
        assertThat(newState.needsRender, iz(true))
    }

    @Test
    fun should_replaceFirstLine_with_media_selected_inserts_before_and_keeps_media_selected() {
        val range = Range(
            startBlock = 1, startOffset = 0, endBlock = 1,
            endOffset = 1
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    InlineMedia(),
                    Paragraph(content = Content("one two three")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceFirstLine("test")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(localId = newState.document.blocks[1].localId, content = Content("test")),
                    state.document.blocks[1],
                    state.document.blocks[2],
                    state.document.blocks[3]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 4, 2, 1)))
        assertThat(newState.needsRender, iz(true))
    }

    @Test
    fun should_replaceRange_inserting_single_line() {
        val range = Range(
            startBlock = 1, startOffset = 4, endBlock = 1,
            endOffset = 4
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one two")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceRange("test ")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(state.document.blocks[1].localId, content = Content("one test two")),
                    state.document.blocks[2]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 9)))
    }

    @Test
    fun should_replaceRange_inserting_multiple_lines() {
        val range = Range(
            startBlock = 1, startOffset = 4, endBlock = 1,
            endOffset = 4
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one two")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceRange("hello\nworld ")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(state.document.blocks[1].localId, content = Content("one hello")),
                    Paragraph(newState.document.blocks[2].localId, content = Content("world two")),
                    state.document.blocks[2]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(2, 6)))
    }

    @Test
    fun should_replaceRange_deleting_within_single_paragraph() {
        val range = Range(
            startBlock = 1, startOffset = 4, endBlock = 1,
            endOffset = 8
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one two three")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceRange("")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(state.document.blocks[1].localId, content = Content("one three")),
                    state.document.blocks[2]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 4)))
    }

    @Test
    fun should_replaceRange_deleting_across_multiple_paragraphs() {
        val range = Range(
            startBlock = 1, startOffset = 2, endBlock = 3,
            endOffset = 2
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two")),
                    Paragraph(content = Content("three")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceRange("")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(state.document.blocks[1].localId, content = Content("onree")),
                    state.document.blocks[4]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 2)))
    }

    @Test
    fun should_replaceRange_replacing_single_line_range_with_single_line() {
        val range = Range(
            startBlock = 1, startOffset = 4, endBlock = 1,
            endOffset = 8
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one two three")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceRange("test ")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(state.document.blocks[1].localId, content = Content("one test three")),
                    state.document.blocks[2]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 9)))
    }

    @Test
    fun should_replaceRange_replacing_multi_line_range_with_single_line() {
        val range = Range(
            startBlock = 1, startOffset = 3, endBlock = 3,
            endOffset = 0
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two")),
                    Paragraph(content = Content("three")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceRange(" test ")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(state.document.blocks[1].localId, content = Content("one test three")),
                    state.document.blocks[4]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(1, 9)))
    }

    @Test
    fun should_replaceRange_replacing_single_line_range_with_multiple_lines() {
        val range = Range(
            startBlock = 1, startOffset = 0, endBlock = 1,
            endOffset = 7
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one two three")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceRange("hello\nworld")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(state.document.blocks[1].localId, content = Content("hello")),
                    Paragraph(newState.document.blocks[2].localId, content = Content("world three")),
                    state.document.blocks[2]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(2, 5)))
    }

    @Test
    fun should_replaceRange_replacing_multi_line_range_with_multiple_lines() {
        val range = Range(
            startBlock = 1, startOffset = 0, endBlock = 3,
            endOffset = 5
        )
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two")),
                    Paragraph(content = Content("three")),
                    Paragraph()
                ),
                range = range
            )
        )
        val newState = state.replaceRange("this\nis\na\ntest")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(state.document.blocks[1].localId, content = Content("this")),
                    Paragraph(newState.document.blocks[2].localId, content = Content("is")),
                    Paragraph(newState.document.blocks[3].localId, content = Content("a")),
                    Paragraph(newState.document.blocks[4].localId, content = Content("test")),
                    state.document.blocks[4]
                )
            )
        )
        assertThat(newState.document.range, iz(Range(4, 4)))
    }
}
