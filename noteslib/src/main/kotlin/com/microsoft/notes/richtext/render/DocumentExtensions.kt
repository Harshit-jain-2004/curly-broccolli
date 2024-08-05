package com.microsoft.notes.richtext.render

import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Range

fun Document.blocksSelected(selStart: Int, selEnd: Int): Range {
    val selStartInternal = if (selStart <= selEnd) selStart else selEnd
    val selEndInternal = if (selStart > selEnd) selStart else selEnd
    var count = 0
    var startBlock: Int = -1
    var selStartBlock: Int = -1
    var endBlock: Int = -1
    var selEndBlock: Int = -1
    blocks.forEachIndexed { index, item ->
        if (startBlock == -1)
            selStartBlock = selStartInternal - count
        if (endBlock == -1)
            selEndBlock = selEndInternal - count

        when (item) {
            is Paragraph -> {
                count += item.cursorPlaces()
                if (selStartInternal < count && startBlock == -1) {
                    startBlock = index
                }
                if (selEndInternal < count && endBlock == -1) {
                    endBlock = index
                }
            }
            is InlineMedia -> {
                count += item.cursorPlaces()
                if (count > selStartInternal && startBlock == -1) {
                    startBlock = index
                }
                if (count > selEndInternal && endBlock == -1) {
                    endBlock = index
                }
            }
        }
        if (startBlock >= 0 && endBlock >= 0) {
            return Range(
                startBlock = startBlock, startOffset = selStartBlock,
                endBlock = endBlock, endOffset = selEndBlock
            )
        }
    }
    if (startBlock < 0) {
        startBlock = blocks.lastIndex
        selStartBlock = if (blocks.isEmpty()) startBlock else blocks.last().size()
    }
    if (endBlock < 0) {
        endBlock = blocks.lastIndex
        selEndBlock = if (blocks.isEmpty()) endBlock else blocks.last().size()
    }
    return Range(startBlock, selStartBlock, endBlock, selEndBlock)
}

fun Document.addBlock(block: Block): Document = Document(blocks + block)
