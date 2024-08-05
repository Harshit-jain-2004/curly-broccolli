package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.editor.ParagraphFormattingProperty
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle

fun EditorState.changeParagraphStyle(property: ParagraphFormattingProperty, value: Boolean): EditorState {
    var currentState = this
    (document.range.startBlock..document.range.endBlock).forEach { paraIndex ->
        val para = currentState.document.blocks[paraIndex]
        if (para is Paragraph) {
            currentState = currentState.changeParagraphStyle(para, property, value)
        }
    }
    return currentState
}

fun EditorState.changeParagraphStyle(paragraph: Paragraph, property: ParagraphFormattingProperty, value: Boolean): EditorState {
    val paraIndex = document.blocks.indexOf(paragraph)
    return copy(
        document = document.copy(
            blocks = document.blocks.subList(0, paraIndex) +
                paragraph.changeParagraphStyle(property, value) +
                document.blocks.subList(paraIndex + 1, document.blocks.size)
        )
    )
}

fun Paragraph.changeParagraphStyle(property: ParagraphFormattingProperty, value: Boolean): Paragraph =
    copy(style = style.setProperty(property, value))

fun ParagraphStyle.setProperty(property: ParagraphFormattingProperty, value: Boolean): ParagraphStyle =
    when (property) {
        ParagraphFormattingProperty.BULLETS -> copy(unorderedList = value)
    }
