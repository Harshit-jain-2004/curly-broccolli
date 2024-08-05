package com.microsoft.notes.richtext.scheme

import android.os.Build
import android.text.Html
import android.text.Html.FROM_HTML_MODE_COMPACT
import android.text.Spanned

const val NEW_LINE_CHAR = '\n'
const val NEW_LINE_CHAR_AS_STR = "$NEW_LINE_CHAR"

fun Document.paragraphListCount(): Int =
    blocks.count { it is Paragraph }

fun Document.mediaListCount(): Int =
    blocks.count { it is InlineMedia }

fun Document.size(): Int =
    blocks.size

fun Document.isEmpty(): Boolean =
    blocks.isEmpty() || blocks.size == 1 && blocks[0].isEmpty()

fun Document.mediaList(): List<InlineMedia> =
    blocks.filter(Block::isMedia).map(Block::asMedia)

fun Document.paragraphList(): List<Paragraph> =
    blocks.filter(Block::isParagraph).map(Block::asParagraph)

fun Document.asString(): String =
    if (isSamsungNoteDocument) {
        ""
    } else {
        paragraphList().joinToString(separator = NEW_LINE_CHAR_AS_STR) { it.content.text }
    }

fun getSpannedFromHtml(bodyHtml: String?): Spanned =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(bodyHtml, FROM_HTML_MODE_COMPACT)
    } else {
        Html.fromHtml(bodyHtml)
    }

fun Block.isMedia(): Boolean =
    this is InlineMedia

fun Block.isParagraph(): Boolean =
    this is Paragraph

fun Block.asParagraph(): Paragraph =
    this as? Paragraph ?: throw ClassCastException("Trying to cast a Block to Paragraph")

fun Block.asMedia(): InlineMedia =
    this as? InlineMedia ?: throw ClassCastException("Trying to cast a Block to InlineMedia")

fun Block.isEmpty(): Boolean =
    this is Paragraph && content.text.isEmpty()

fun InlineMedia.getUrlOrNull(): String? =
    if (!localUrl.isNullOrEmpty()) localUrl
    else if (localUrl.isNullOrEmpty() && !remoteUrl.isNullOrEmpty()) remoteUrl
    else null

fun Paragraph.removeBullet(): Paragraph = copy(style = style.copy(unorderedList = false))

fun Paragraph.addBullet(): Paragraph = copy(style = style.copy(unorderedList = true))

fun Paragraph.setAsRightToLeft(): Paragraph = copy(style = style.copy(rightToLeft = true))

fun Paragraph.setAsLeftToRight(): Paragraph = copy(style = style.copy(rightToLeft = false))
