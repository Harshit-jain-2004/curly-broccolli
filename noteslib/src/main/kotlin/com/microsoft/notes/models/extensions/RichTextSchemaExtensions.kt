package com.microsoft.notes.models.extensions

import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.asMedia
import com.microsoft.notes.richtext.scheme.asParagraph

fun Block.isMedia(): Boolean =
    this is InlineMedia

fun Block.isParagraph(): Boolean =
    this is Paragraph

fun List<Block>.justMedia(): List<InlineMedia> =
    filter { it.isMedia() }.map { it.asMedia() }

fun List<Block>.justParagraph(): List<Paragraph> =
    filter { it.isParagraph() }.map { it.asParagraph() }

fun Document.justMedia(): List<InlineMedia> =
    blocks.justMedia()

fun Document.justParagraph(): List<Paragraph> =
    blocks.justParagraph()
