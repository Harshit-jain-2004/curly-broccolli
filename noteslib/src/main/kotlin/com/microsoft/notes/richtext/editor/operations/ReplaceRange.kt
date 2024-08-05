package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.editor.forceRender
import com.microsoft.notes.richtext.editor.updateRange
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Range
import com.microsoft.notes.richtext.scheme.asParagraph

fun EditorState.replaceRange(text: String): EditorState {
    val lines = text.lines()
    return replaceFirstLine(lines.first()).deleteBlocksAfterFirst().addExtraLines(lines.drop(1))
}

fun EditorState.replaceFirstLine(line: String): EditorState {
    val currentBlock = document.blocks[document.range.startBlock]
    return when (currentBlock) {
        is Paragraph -> {
            val end: Int
            val newRange: Range
            if (document.range.isSingleBlock) {
                end = document.range.endOffset
                newRange = document.range.copy(
                    startOffset = document.range.startOffset + line.length,
                    endOffset = document.range.startOffset + line.length
                )
            } else {
                end = currentBlock.content.text.length
                newRange = document.range.copy(startOffset = document.range.startOffset + line.length)
            }
            replaceText(currentBlock, document.range.startOffset, end, line).updateRange(newRange)
        }
        is InlineMedia -> {
            val newPara = Paragraph(content = Content(line))
            when {
                document.range.startOffset >= 1 -> {
                    val newRange = Range(
                        startBlock = document.range.startBlock + 1,
                        startOffset = line.length,
                        endBlock = document.range.endBlock + 1,
                        endOffset = if (document.range.isSingleBlock) line.length else document.range.endOffset
                    )
                    insertBlockAfter(newPara, currentBlock).updateRange(newRange)
                }
                else -> {
                    val newRange = document.range.copy(
                        startOffset = line.length,
                        endBlock = document.range.endBlock + 1
                    )
                    insertBlockBefore(newPara, currentBlock).updateRange(newRange)
                }
            }.forceRender()
        }
    }
}

fun EditorState.addExtraLines(lines: List<String>): EditorState {
    var currentState = this
    var splitAt = document.range.startOffset
    var splitParaIndex = document.range.startBlock
    lines.forEach {
        val block = currentState.document.blocks[splitParaIndex]
        currentState = currentState.splitParagraph(block.asParagraph(), splitAt)
        splitParaIndex++
        currentState = currentState.replaceText(
            currentState.document.blocks[splitParaIndex]
                .asParagraph(),
            0, 0, it
        )
        splitAt = it.length
    }
    return currentState.updateRange(
        Range(startBlock = splitParaIndex, startOffset = splitAt)
    )
}

fun EditorState.deleteBlocksAfterFirst(): EditorState {
    return document.blocks.foldIndexed(this) { index, currentState, block ->
        when {
            index > document.range.startBlock && index == document.range.endBlock -> when (block) {
                is Paragraph -> currentState.replaceText(block, 0, document.range.endOffset, "")
                    .mergeParagraphIntoPrevious(block.localId)
                is InlineMedia -> when {
                    document.range.endOffset == 1 -> currentState.removeBlock(block)
                    else -> currentState.forceRender()
                }
            }
            index > document.range.startBlock && index < document.range.endBlock -> when (block) {
                is Paragraph -> currentState.replaceText(block, 0, block.content.text.length, "")
                    .mergeParagraphIntoPrevious(block.localId)
                is InlineMedia -> currentState.removeBlock(block)
            }
            else -> currentState
        }
    }.updateRange(document.range.collapseToStart())
}
