package com.microsoft.notes.utils.utils

import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Paragraph

fun toParagraphs(textLines: List<String>): List<Paragraph> {
    val paragraphs: MutableList<Paragraph> = ArrayList()
    for (text in textLines) {
        val listOfString = text.replace("\r", "").split("\n")
        for (string in listOfString) {
            paragraphs.add(Paragraph(content = Content(text = string)))
        }
    }
    return paragraphs
}
