package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.addBullet
import com.microsoft.notes.richtext.scheme.removeBullet
import com.microsoft.notes.richtext.scheme.setAsLeftToRight
import com.microsoft.notes.richtext.scheme.setAsRightToLeft
import com.microsoft.notes.threeWayMerge.BlockUpdate
import com.microsoft.notes.threeWayMerge.Diff
import com.microsoft.notes.threeWayMerge.RightToLeftDeletion
import com.microsoft.notes.threeWayMerge.RightToLeftInsertion
import com.microsoft.notes.threeWayMerge.RightToLeftOperation
import com.microsoft.notes.threeWayMerge.UnorderedListDeletion
import com.microsoft.notes.threeWayMerge.UnorderedListInsertion
import com.microsoft.notes.threeWayMerge.UnorderedListOperation

data class BlockMerge(val block: Block, val selectionStart: Int?, val selectionEnd: Int?)

fun Paragraph.applyUnorderedListDiff(diff: UnorderedListOperation): Paragraph {
    return when (diff) {
        is UnorderedListDeletion -> removeBullet()
        is UnorderedListInsertion -> addBullet()
    }
}

@Suppress("UnsafeCast")
fun Paragraph.mergeUnorderedLists(primary: List<Diff>, secondary: List<Diff>): Paragraph {
    val primaryDiff = primary.find { it is UnorderedListOperation }
    if (primaryDiff != null) {
        return applyUnorderedListDiff(primaryDiff as UnorderedListOperation)
    }
    val secondaryDiff = secondary.find { it is UnorderedListOperation }
    if (secondaryDiff != null) {
        return applyUnorderedListDiff(secondaryDiff as UnorderedListOperation)
    }
    return this
}

fun Paragraph.applyRightToLeftDiff(diff: RightToLeftOperation): Paragraph {
    return when (diff) {
        is RightToLeftDeletion -> setAsLeftToRight()
        is RightToLeftInsertion -> setAsRightToLeft()
    }
}

@Suppress("UnsafeCast")
fun Paragraph.mergeRightToLeftDiffs(primary: List<Diff>, secondary: List<Diff>): Paragraph {
    val primaryDiff = primary.find { it is RightToLeftOperation }
    if (primaryDiff != null) {
        return applyRightToLeftDiff(primaryDiff as RightToLeftOperation)
    }
    val secondaryDiff = secondary.find { it is RightToLeftOperation }
    if (secondaryDiff != null) {
        return applyRightToLeftDiff(secondaryDiff as RightToLeftOperation)
    }
    return this
}

@Suppress("UnsafeCast")
fun applyUpdate(block: Block, primary: List<Diff>, secondary: List<Diff>): Block {
    val primaryDiff = primary.find { it is BlockUpdate }
    if (primaryDiff != null) {
        return (primaryDiff as BlockUpdate).block
    }

    val secondaryDiff = secondary.find { it is BlockUpdate }
    if (secondaryDiff != null) {
        return (secondaryDiff as BlockUpdate).block
    }

    return block
}

fun merge(
    block: Block,
    primary: MutableList<Diff>,
    secondary: MutableList<Diff>,
    selectionStart: Int?,
    selectionEnd: Int?,
    selectionForm: SelectionFrom
): BlockMerge {
    var newSelectionStart = selectionStart
    var newSelectionEnd = selectionEnd
    val merged = when (block) {
        is Paragraph -> {
            val paragraph = block.mergeUnorderedLists(primary, secondary).mergeRightToLeftDiffs(primary, secondary)
            val content = merge(paragraph.content, primary, secondary, selectionStart, selectionEnd, selectionForm)
            newSelectionStart = content.selectionStart
            newSelectionEnd = content.selectionEnd
            paragraph.copy(content = content.content)
        }
        else -> applyUpdate(block, primary, secondary)
    }
    return BlockMerge(block = merged, selectionStart = newSelectionStart, selectionEnd = newSelectionEnd)
}
