package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle
import com.microsoft.notes.richtext.scheme.asParagraph
import com.microsoft.notes.threeWayMerge.BlockUpdate
import com.microsoft.notes.threeWayMerge.Diff
import com.microsoft.notes.threeWayMerge.RightToLeftDeletion
import com.microsoft.notes.threeWayMerge.RightToLeftInsertion
import com.microsoft.notes.threeWayMerge.UnorderedListInsertion
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class BlockTest {

    @Test
    fun should_apply_UnorderedListDiff() {
        val paragraph = Paragraph(localId = "foo", content = Content(text = "hello"))
        val diff = UnorderedListInsertion(blockId = "foo")

        val result = paragraph.applyUnorderedListDiff(diff)
        assertThat(result.style.unorderedList, iz(true))
    }

    @Test
    fun should_merge_unordered_lists() {
        val paragraph = Paragraph(localId = "foo", content = Content(text = "hello"))
        val primary = mutableListOf(UnorderedListInsertion(blockId = "foo"))
        val secondary = mutableListOf(UnorderedListInsertion(blockId = "bar"))

        val result = paragraph.mergeUnorderedLists(primary, secondary)
        assertThat(result.style.unorderedList, iz(true))
    }

    @Test
    fun should_apply_RightToLeftDiff() {
        val paragraph = Paragraph(localId = "foo", content = Content(text = "hello"))
        val diff = RightToLeftInsertion(blockId = "foo")

        val insertionResult = paragraph.applyRightToLeftDiff(diff)
        assertThat(insertionResult.style.rightToLeft, iz(true))

        val rtlParagraph = Paragraph(
            localId = "bar", content = Content(text = "hello"),
            style = ParagraphStyle(rightToLeft = true)
        )
        val removalDiff = RightToLeftDeletion(blockId = "bar")
        val removalResult = rtlParagraph.applyRightToLeftDiff(removalDiff)
        assertThat(removalResult.style.rightToLeft, iz(false))
    }

    @Test
    fun should_merge_rightToLeft() {
        val paragraph = Paragraph(localId = "foo", content = Content(text = "hello"))
        val primary = mutableListOf(RightToLeftInsertion(blockId = "foo"))
        val secondary = mutableListOf(RightToLeftInsertion(blockId = "bar"))

        val result = paragraph.mergeRightToLeftDiffs(primary, secondary)
        assertThat(result.style.rightToLeft, iz(true))
    }

    @Test
    fun should_apply_update() {
        val block = Paragraph(localId = "foo", content = Content(text = "hello"))
        val primary = mutableListOf(
            BlockUpdate(
                block = block.copy(
                    content = Content("modified1")
                )
            )
        )
        val secondary = mutableListOf(
            BlockUpdate(
                block = block.copy(
                    content = Content("modified2")
                )
            )
        )

        val result = applyUpdate(block, primary, secondary)
        assertThat(result is Paragraph, iz(true))
        val resultAsParagraph = result.asParagraph()
        assertThat(resultAsParagraph.content.text, iz("modified1"))
    }

    @Test
    fun should_merge_block() {
        val block = Paragraph(localId = "foo", content = Content(text = "hello"))
        val primary = mutableListOf<Diff>(
            BlockUpdate(
                block = block.copy(
                    content = Content("modified1")
                )
            )
        )
        val secondary = mutableListOf<Diff>(
            BlockUpdate(
                block = block.copy(
                    content = Content("modified2")
                )
            )
        )

        val selectionFrom = SelectionFrom.PRIMARY
        val result = merge(
            block, primary = primary, secondary = secondary,
            selectionForm = selectionFrom, selectionStart = null, selectionEnd = null
        )
        assertThat(result.block is Paragraph, iz(true))
        assertThat(result.selectionStart, iz(nullValue()))
        assertThat(result.selectionEnd, iz(nullValue()))

        val resultAsParagraph = result.block.asParagraph()
        assertThat(resultAsParagraph.content.text, iz("hello"))
        // FIXME invest more time in this test
    }
}
