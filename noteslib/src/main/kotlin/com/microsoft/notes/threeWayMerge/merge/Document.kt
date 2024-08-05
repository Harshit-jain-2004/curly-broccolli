package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Range
import com.microsoft.notes.threeWayMerge.BlockDeletion
import com.microsoft.notes.threeWayMerge.BlockInsertion
import com.microsoft.notes.threeWayMerge.Diff
import com.microsoft.notes.threeWayMerge.toMapByBlockId

typealias Selection = Range

enum class SelectionFrom { PRIMARY, SECONDARY }
data class SelectionBlockIds(val startBlockId: String?, val endBlockId: String?)
data class SelectionInfo(
    val selection: Selection,
    val selectionIds: SelectionBlockIds,
    val selectionFrom: SelectionFrom
)

fun selectionStartAndEnd(selectionInfo: SelectionInfo, blockId: String): Pair<Int?, Int?> {
    val (selection, selectionIds) = selectionInfo
    var start: Int? = null
    var end: Int? = null
    if (selectionIds.startBlockId == blockId) {
        start = selection.startOffset
    }
    if (selectionIds.endBlockId == blockId) {
        end = selection.endOffset
    }
    return Pair(start, end)
}

@Suppress("UnsafeCast")
fun List<Diff>.getDeleteBlockIndex(): Int = indexOfFirst { it is BlockDeletion }

@Suppress("UnsafeCast")
fun List<Diff>.getDeleteBlock(): BlockDeletion? = find { it is BlockDeletion } as BlockDeletion?

fun Document.applyBlockDeletion(blockDelete: BlockDeletion): Document =
    copy(blocks = blocks.filter { item -> item.localId != blockDelete.blockId })

fun Document.applyBlockUpdate(blockUpdate: Block): Document {
    return copy(
        blocks = blocks.map {
            if (it.localId == blockUpdate.localId) blockUpdate else it
        }
    )
}

internal fun Document.applyBlockInserts(diffs: List<Diff>, previouslyDeletedIndices: List<Int>): Document {

    fun numberOfIndicesBelowValue(list: List<Int>, value: Int): Int {
        // In web they do a slice with an empty parameter, that returns the same, don't know exactly why
        return list.filter { it < value }.size
    }

    var newDocument = this
    for (diff in diffs) {
        if (diff is BlockInsertion) {
            val numberDeletedBefore = numberOfIndicesBelowValue(previouslyDeletedIndices, diff.index)
            val insertionIndex = diff.index - numberDeletedBefore
            val newBlocks = newDocument.blocks.toMutableList()
            newBlocks.add(insertionIndex, diff.block)
            newDocument = newDocument.copy(blocks = newBlocks.toList())
        }
    }
    return newDocument
}

internal fun <T> MutableList<T>.replaceAll(otherList: List<T>) {
    clear()
    addAll(otherList)
}

internal fun MutableList<Diff>.removeDuplicatedInserts(toCompare: List<Diff>): List<Int> {
    val removedBaseIndices = mutableListOf<Int>()
    toCompare.forEach { primaryDiff ->
        if (primaryDiff is BlockInsertion) {
            this.removeAll { secondaryDiff ->
                if (secondaryDiff is BlockInsertion &&
                    secondaryDiff.block.localId == primaryDiff.block.localId
                ) {
                    removedBaseIndices.add(secondaryDiff.index)
                    true
                } else {
                    false
                }
            }
        }
    }

    return removedBaseIndices
}

@Suppress("UnsafeCast", "UnsafeCallOnNullableType", "LongMethod")
fun merge(
    base: Document,
    selectionInfo: SelectionInfo,
    primary: MutableList<Diff>,
    secondary: MutableList<Diff>
): Document {
    var newDocument = base
    val (selection, _, selectionFrom) = selectionInfo
    val primaryByBlockId = primary.toMapByBlockId()
    val secondaryByBlockId = secondary.toMapByBlockId()
    val primaryDeletedIndices = mutableListOf<Int>()
    val secondaryDeletedIndices = mutableListOf<Int>()
    var selectionStartBlockId: String? = null
    var selectionEndBlockId: String? = null
    var selectionStartIndex: Int? = null
    var selectionEndIndex: Int? = null

    fun getSelectionByIndexAndBlock(index: Int?, lastBlock: Block?): Pair<String, Int>? {
        return if (index != null && lastBlock != null) {
            val selectionBlockId = lastBlock.localId
            val selectionIndex = when (lastBlock) {
                is Paragraph -> lastBlock.content.text.length
                else -> 1
            }
            Pair(selectionBlockId, selectionIndex)
        } else {
            null
        }
    }

    fun moveSelectionToEndOfPreviousBlock(previousBlockId: String, start: Int?, end: Int?) {
        val previousBlock = newDocument.blocks.find { it.localId == previousBlockId }

        getSelectionByIndexAndBlock(start, previousBlock)?.let {
            selectionStartBlockId = it.first
            selectionStartIndex = it.second
        }

        getSelectionByIndexAndBlock(end, previousBlock)?.let {
            selectionEndBlockId = it.first
            selectionEndIndex = it.second
        }
    }

    base.blocks.forEachIndexed loop@{ i, block ->
        val (start, end) = selectionStartAndEnd(selectionInfo, blockId = block.localId)
        val primaryDiffs = primaryByBlockId[block.localId] ?: mutableListOf()
        val secondaryDiffs = secondaryByBlockId[block.localId] ?: mutableListOf()

        // no block changes
        val changes = primaryDiffs.isNotEmpty() || secondaryDiffs.isNotEmpty()
        if (!changes) {
            if (start != null) {
                selectionStartBlockId = block.localId
                selectionStartIndex = start
            }
            if (end != null) {
                selectionEndBlockId = block.localId
                selectionEndIndex = end
            }
            return@loop
        }

        // block deleted in primary diffs
        val primaryDeletedBlock = primaryDiffs.getDeleteBlock()
        primaryDeletedBlock?.let {
            primaryDeletedIndices.add(i)
            moveSelectionToEndOfPreviousBlock(it.blockId, start, end)
            newDocument = newDocument.applyBlockDeletion(primaryDeletedBlock)
            return@loop
        }

        // block deleted in secondary diffs
        val secondaryDeleteBlockIndex = secondaryDiffs.getDeleteBlockIndex()
        if (secondaryDeleteBlockIndex != -1) {
            val secondaryDeleteBlock = secondaryDiffs[secondaryDeleteBlockIndex] as BlockDeletion
            if (primaryDiffs.isEmpty()) {
                secondaryDeletedIndices.add(i)
                moveSelectionToEndOfPreviousBlock(secondaryDeleteBlock.blockId, start, end)
                newDocument = newDocument.applyBlockDeletion(secondaryDeleteBlock)
                return@loop
            } else {
                secondaryDiffs.removeAt(secondaryDeleteBlockIndex)
            }
        }

        // content diffs
        val blockMerged = merge(
            block = block, primary = primaryDiffs, secondary = secondaryDiffs,
            selectionStart = start, selectionEnd = end, selectionForm = selectionFrom
        )

        newDocument = newDocument.applyBlockUpdate(blockMerged.block)
        blockMerged.selectionStart?.let {
            selectionStartBlockId = blockMerged.block.localId
            selectionStartIndex = blockMerged.selectionStart
        }
        blockMerged.selectionEnd?.let {
            selectionEndBlockId = blockMerged.block.localId
            selectionEndIndex = blockMerged.selectionEnd
        }
    }

    // Remove any block insertions in the secondary that also exist in the primary but do not exist
    // in the base
    val removedDiffIndices = secondary.removeDuplicatedInserts(toCompare = primary)

    newDocument = newDocument.applyBlockInserts(
        diffs = secondary,
        previouslyDeletedIndices = primaryDeletedIndices + removedDiffIndices
    )
    newDocument = newDocument.applyBlockInserts(
        diffs = primary,
        previouslyDeletedIndices = secondaryDeletedIndices
    )

    val startBlock = when {
        selectionStartBlockId != null ->
            newDocument.blocks.indexOfFirst { it.localId == selectionStartBlockId }
        else -> selection.startBlock
    }
    val endBlock = when {
        selectionEndBlockId != null ->
            newDocument.blocks.indexOfFirst { it.localId == selectionEndBlockId }
        else -> selection.endBlock
    }
    val startIndex = when {
        selectionStartIndex != null -> selectionStartIndex
        else -> selection.startOffset
    }
    val endIndex = when {
        selectionEndIndex != null -> selectionEndIndex
        else -> selection.endOffset
    }

    return newDocument.copy(
        range = Selection(
            startOffset = startIndex!!, endOffset = endIndex!!,
            startBlock = startBlock, endBlock = endBlock
        )
    )
}
