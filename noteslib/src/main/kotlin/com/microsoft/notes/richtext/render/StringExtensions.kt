package com.microsoft.notes.richtext.render

import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.generateLocalId

fun String.splitIntoParagraphs(): List<Paragraph> {
    val lines = this.lines()
    return lines.map { Paragraph(localId = generateLocalId(), content = Content(text = it)) }
}

fun String.toDocument(): Document {
    val paragraphs = this.splitIntoParagraphs()
    return Document(paragraphs)
}
