package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.editor.utils.removeFile
import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.InlineMedia

fun EditorState.removeBlock(block: Block): EditorState {
    if (block is InlineMedia) {
        block.localUrl?.let { removeFile(it) }
    }
    return copy(document = document.copy(blocks = document.blocks - block))
}

fun EditorState.insertBlockBefore(newBlock: Block, insertBefore: Block): EditorState {
    val blockIndex = document.blocks.indexOf(insertBefore)
    return copy(
        document = document.copy(
            blocks = document.blocks.subList(0, blockIndex) +
                newBlock +
                document.blocks.subList(blockIndex, document.blocks.size)
        )
    )
}

fun EditorState.insertBlockAfter(newBlock: Block, insertAfter: Block): EditorState =
    insertBlockAfter(newBlock, document.blocks.indexOf(insertAfter))

fun EditorState.insertBlockAfter(newBlock: Block, blockIndex: Int): EditorState {
    return copy(
        document = document.copy(
            blocks = document.blocks.subList(0, blockIndex + 1) +
                newBlock +
                document.blocks.subList(blockIndex + 1, document.blocks.size)
        )
    )
}
