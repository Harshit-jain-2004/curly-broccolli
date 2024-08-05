package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph

fun EditorState.insertMedia(media: InlineMedia): EditorState {
    val blockToInsertAt = document.blocks[document.range.endBlock]
    return when (blockToInsertAt) {
        is Paragraph -> {
            when {
                blockToInsertAt.content.text.isEmpty() -> {
                    insertBlockBefore(media, blockToInsertAt).removeBlock(blockToInsertAt)
                }
                document.range.endOffset == 0 -> {
                    insertBlockBefore(media, blockToInsertAt)
                }
                document.range.endOffset == blockToInsertAt.content.text.length -> {
                    insertBlockAfter(media, blockToInsertAt)
                }
                else -> {
                    splitParagraph(blockToInsertAt, document.range.endOffset).insertBlockAfter(
                        media,
                        document.range.endBlock
                    )
                }
            }
        }
        is InlineMedia -> {
            when (document.range.endOffset) {
                0 -> insertBlockBefore(media, blockToInsertAt)
                else -> insertBlockAfter(media, blockToInsertAt)
            }
        }
    }
}

fun EditorState.updateMediaWithAltText(localId: String, altText: String): EditorState {
    return copy(
        document = document.copy(
            blocks = document.blocks.map {
                if (it.localId == localId && it is InlineMedia) {
                    it.copy(altText = altText)
                } else {
                    it
                }
            }
        )
    )
}
