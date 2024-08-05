package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Range
import com.microsoft.notes.richtext.scheme.asParagraph
import com.microsoft.notes.richtext.scheme.isEmpty
import com.microsoft.notes.richtext.scheme.isParagraph
import com.microsoft.notes.richtext.scheme.size
import com.microsoft.notes.threeWayMerge.BlockDeletion
import com.microsoft.notes.threeWayMerge.BlockInsertion
import com.microsoft.notes.threeWayMerge.Diff
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

typealias Selection = Range

class DocumentTest {

    @Test
    fun should_get_selection_start_value_and_end_null_when_blocks_are_differents() {
        val selection = Selection(startOffset = 0, startBlock = 0, endBlock = 10, endOffset = 1)
        val selectionBlockIds = SelectionBlockIds(startBlockId = "block0", endBlockId = "block1")
        val selectionFrom = SelectionFrom.PRIMARY
        val selectionInfo = SelectionInfo(
            selection = selection, selectionIds = selectionBlockIds,
            selectionFrom = selectionFrom
        )

        val (selectionStart, selectionEnd) = selectionStartAndEnd(selectionInfo = selectionInfo, blockId = "block0")
        assertThat(selectionStart, iz(selection.startOffset))
        assertThat(selectionEnd, iz(nullValue()))
    }

    @Test
    fun should_get_selection_start_value_and_end_value_when_blocks_are_the_same() {
        val selection = Selection(startOffset = 0, startBlock = 0, endBlock = 10, endOffset = 0)
        val selectionBlockIds = SelectionBlockIds(startBlockId = "block0", endBlockId = "block0")
        val selectionFrom = SelectionFrom.PRIMARY
        val selectionInfo = SelectionInfo(
            selection = selection, selectionIds = selectionBlockIds,
            selectionFrom = selectionFrom
        )

        val (selectionStart, selectionEnd) = selectionStartAndEnd(selectionInfo = selectionInfo, blockId = "block0")
        assertThat(selectionStart, iz(selection.startOffset))
        assertThat(selectionEnd, iz(selection.endOffset))
    }

    @Test
    fun should_get_BlockDeletion_index() {
        val block = Paragraph(localId = "block0", content = Content(text = "text"))
        val blockInsertion = BlockInsertion(block = block, index = 0)
        val blockDeletion = BlockDeletion("block0")
        val diffs = listOf<Diff>(blockInsertion, blockDeletion)
        val blockDeletionIndex = diffs.getDeleteBlockIndex()
        assertThat(blockDeletionIndex, iz(1))
    }

    @Test
    fun should_get_BlockDeletion() {
        val block = Paragraph(localId = "block0", content = Content(text = "text"))
        val blockInsertion = BlockInsertion(block = block, index = 0)
        val blockDeletion = BlockDeletion("block0")
        val diffs = listOf<Diff>(blockInsertion, blockDeletion)
        val blockDeletionResult = diffs.getDeleteBlock()
        assertThat(blockDeletionResult, iz(blockDeletion))
    }

    @Test
    fun should_apply_BlockDeletion() {
        val block = Paragraph(localId = "block0", content = Content(text = "text"))
        val document = Document(blocks = listOf(block))
        val blockDeletion = BlockDeletion("block0")
        document.applyBlockDeletion(blockDeletion)
        val newDocument = document.applyBlockDeletion(blockDeletion)
        assertThat(newDocument.isEmpty(), iz(true))
    }

    @Test
    fun should_apply_BlockUpdate() {
        val block1 = Paragraph(localId = "block1", content = Content(text = "text"))
        val document = Document(blocks = listOf(block1))
        val block2 = Paragraph(localId = "block1", content = Content(text = "hello"))
        document.applyBlockUpdate(block2)
        val newDocument = document.applyBlockUpdate(block2)
        with(newDocument.blocks) {
            assertThat(size, iz(1))
            assertThat(component1().isParagraph(), iz(true))
            assertThat(component1().asParagraph(), iz(block2))
        }
    }

    @Test
    fun should_apply_BlockInserts() {
        val block0 = Paragraph(localId = "block0", content = Content(text = "text block0"))
        val document = Document(blocks = listOf(block0))

        val block1 = Paragraph(localId = "block1", content = Content(text = "text block1"))
        val block2 = Paragraph(localId = "block2", content = Content(text = "text block2"))
        val blockInsert1 = BlockInsertion(block = block1, index = 0)
        val blockInsert2 = BlockInsertion(block = block2, index = 2)
        val diffs = listOf(blockInsert1, blockInsert2)

        val newDocument = document.applyBlockInserts(diffs, previouslyDeletedIndices = emptyList())
        assertThat(newDocument.size(), iz(3))
        with(newDocument.blocks) {
            assertThat(component1() as Paragraph, iz(block1))
            assertThat(component2() as Paragraph, iz(block0))
            assertThat(component3() as Paragraph, iz(block2))
        }
    }

    @Test
    fun should_apply_BlockInsertsWithPrimaryBlockDeletes() {
        val block0 = Paragraph(localId = "block0", content = Content(text = "text block0"))
        val block2 = Paragraph(localId = "block2", content = Content(text = "text block2"))
        val document = Document(blocks = listOf(block0, block2))

        val block_0 = Paragraph(localId = "block_0", content = Content(text = "text block_0"))
        val block4 = Paragraph(localId = "block4", content = Content(text = "text block4"))
        val block5 = Paragraph(localId = "block5", content = Content(text = "text block5"))

        val blockInsert_0 = BlockInsertion(block = block_0, index = 0)
        val blockInsert3 = BlockInsertion(block = block4, index = 4)
        val blockInsert4 = BlockInsertion(block = block5, index = 5)
        val diffs = listOf(blockInsert_0, blockInsert3, blockInsert4)

        val newDocument = document.applyBlockInserts(diffs, previouslyDeletedIndices = listOf(1))
        assertThat(newDocument.size(), iz(5))
        with(newDocument.blocks) {
            assertThat(component1() as Paragraph, iz(block_0))
            assertThat(component2() as Paragraph, iz(block0))
            assertThat(component3() as Paragraph, iz(block2))
            assertThat(component4() as Paragraph, iz(block4))
            assertThat(component5() as Paragraph, iz(block5))
        }
    }

    @Test
    fun should_remove_duplicate_inserts_from_anoter_list_so_its_empty() {
        val block1 = Paragraph(localId = "block1", content = Content(text = "text block1"))
        val block2 = Paragraph(localId = "block2", content = Content(text = "text block2"))
        val blockInsert1 = BlockInsertion(block = block1, index = 0)
        val blockInsert2 = BlockInsertion(block = block2, index = 2)
        val diffs = mutableListOf<Diff>(blockInsert1, blockInsert2)
        val other_diffs = listOf(blockInsert1, blockInsert2)

        diffs.removeDuplicatedInserts(other_diffs)
        assertThat(diffs.isEmpty(), iz(true))
    }

    @Test
    fun should_remove_duplicate_inserts_from_anoter_list() {
        val block1 = Paragraph(localId = "block1", content = Content(text = "text block1"))
        val block2 = Paragraph(localId = "block2", content = Content(text = "text block2"))
        val blockInsert1 = BlockInsertion(block = block1, index = 0)
        val blockInsert2 = BlockInsertion(block = block2, index = 2)
        val diffs = mutableListOf<Diff>(blockInsert1, blockInsert2)
        val other_diffs = listOf(blockInsert1)

        diffs.removeDuplicatedInserts(other_diffs)
        assertThat(diffs.size, iz(1))
        assertThat(diffs.component1() as BlockInsertion, iz(blockInsert2))
    }

    @Test
    fun should_replace_all_items() {
        val list = mutableListOf(1, 2, 3, 4, 5)
        val otherList = listOf(6, 7, 8, 9, 0)
        list.replaceAll(otherList)
        assertThat(list, iz(otherList))
    }
}
