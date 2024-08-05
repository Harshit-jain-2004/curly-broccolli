package com.microsoft.notes.ui.extensions

import android.content.Context
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.styled.parseMillisToRFC1123String
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.asString
import com.microsoft.notes.richtext.scheme.isMedia
import com.microsoft.notes.richtext.scheme.isParagraph
import com.microsoft.notes.richtext.scheme.mediaList
import com.microsoft.notes.ui.noteslist.recyclerview.NotesListAdapter
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.WrongViewHolderTypeException
import com.microsoft.notes.utils.utils.Constants.NOTE_CHARACTER_LIMIT

fun Note.getStableId(): Long = this.localId.hashCode().toLong()

fun List<Note>.filterDeletedAndFutureNotes(): List<Note> = filter { !it.isDeleted && !it.isFutureNote }

fun List<Note>.search(query: String, color: Color?): List<Note> {
    return filter { note ->
        if (query.isNullOrEmpty()) {
            true
        } else {
            query.parseSearchQuery().all { term ->
                note.title?.contains(term, ignoreCase = true) == true ||
                    // check text and inline media
                    note.document.blocks.any { block ->
                        (
                            block.isParagraph() && (block as Paragraph).content.text.contains(
                                term,
                                ignoreCase = true
                            )
                            ) ||
                            (
                                block.isMedia() && (block as InlineMedia).altText?.contains(
                                    term,
                                    ignoreCase = true
                                ) ?: false
                                )
                    } ||
                    // check media alt text
                    note.media.any { it.altText?.contains(term, ignoreCase = true) ?: false }
            }
        } && if (color == null) {
            true
        } else {
            note.color == color
        }
    }
}

const val SAMSUNG_NOTES_APP_NAME = "SamsungNotes"

fun Note?.isSamsungNote() = this?.document?.isSamsungNoteDocument ?: false

fun Note.getLocalMediaUrls(): List<String> {
    val imageLocalUrlsInlineMedia = this.document.mediaList().asSequence()
        .filter { !it.localUrl.isNullOrBlank() }
        .mapNotNull { it.localUrl }
        .toList()
    val imageLocalUrlsMedia = this.media.asSequence()
        .filter { !it.localUrl.isNullOrBlank() }
        .mapNotNull { it.localUrl }
        .toList()

    return imageLocalUrlsInlineMedia + imageLocalUrlsMedia
}

fun Note.getHasImagesTelemetryValue(): String = media.isNotEmpty().toString()

fun Note.getViewType(): Int {
    return when {
        isInkNote -> NotesListAdapter.NoteItemType.INK.id
        mediaCount == 0 -> NotesListAdapter.NoteItemType.TEXT.id
        mediaCount == 1 -> NotesListAdapter.NoteItemType.SINGLE_IMAGE.id
        mediaCount == 2 -> NotesListAdapter.NoteItemType.TWO_IMAGE.id
        mediaCount == 3 -> NotesListAdapter.NoteItemType.THREE_IMAGE.id
        else -> NotesListAdapter.NoteItemType.MULTI_IMAGE.id
    }
}

fun Note.getLayoutId(): Int {
    return when (getViewType()) {
        NotesListAdapter.NoteItemType.TEXT.id -> R.layout.sn_note_item_layout_text
        NotesListAdapter.NoteItemType.SINGLE_IMAGE.id -> R.layout.sn_note_item_layout_single_image
        NotesListAdapter.NoteItemType.TWO_IMAGE.id -> R.layout.sn_note_item_layout_two_image
        NotesListAdapter.NoteItemType.THREE_IMAGE.id -> R.layout.sn_note_item_layout_three_image
        NotesListAdapter.NoteItemType.MULTI_IMAGE.id -> R.layout.sn_note_item_layout_multi_image
        NotesListAdapter.NoteItemType.INK.id -> R.layout.sn_note_item_layout_ink
        else -> throw WrongViewHolderTypeException()
    }
}

/**
 * There is a 2MB limit on reading rows from SQLite. Also, larger notes generally cause an
 * ANR, so the limit we set is under 2MB.
 */
fun Note.isBelowSizeLimitForStorage(): Boolean {
    @Suppress("UNCHECKED_CAST")
    val paragraphs = document.blocks.filter { it.isParagraph() } as List<Paragraph>
    val textLength = paragraphs.map { it.content.text.length }.sum()
    return textLength < NOTE_CHARACTER_LIMIT
}

// =============== Note Accessibility label =================
fun Note.getAccessibilityLabel(context: Context): String {
    val feedCardImprovementsEnabled = NotesLibrary.getInstance().experimentFeatureFlags.feedCardImprovementsEnabled
    val type = getNoteType(context)
    val isNotePinned = if (feedCardImprovementsEnabled && this.isPinned) context.resources.getString(R.string.feed_card_pinned_label) else ""
    val imagesAltText = getNoteImagesAltText()

    val noteContent = getNoteContent()
    val feedNotePreviewAnnouncementLimit = context.resources.getInteger(R.integer.feed_note_preview_announcement_limit)
    val content: String = when {
        noteContent.length > feedNotePreviewAnnouncementLimit -> noteContent.substring(0, feedNotePreviewAnnouncementLimit)
        else -> noteContent
    }

    val date = if (!feedCardImprovementsEnabled) getNoteDate(context) else ""
    val color = getNoteColor(context)

    val accessibilityLabels = listOf(type, isNotePinned, imagesAltText, content, date, color).filter { it.isNotEmpty() }
    return accessibilityLabels.joinToString(", ")
}

private fun Note.getNoteType(context: Context): String {
    if (isRenderedInkNote || isInkNote) {
        return context.getString(R.string.sn_ink_note)
    }

    if (isMediaListEmpty) {
        return context.getString(R.string.sn_text_note)
    }

    val prefix = if (hasNoText) {
        context.getString(R.string.sn_label_image)
    } else {
        context.getString(R.string.sn_label_text)
    }

    return if (mediaCount > 1) {
        context.getString(R.string.sn_multi_image_note, prefix, mediaCount)
    } else {
        context.getString(R.string.sn_single_image_note, prefix)
    }
}

private fun Note.getNoteImagesAltText(): String {
    if (isMediaListEmpty) {
        return ""
    }

    val altTextsMedia = sortedMedia.asSequence()
        .filter { !it.altText.isNullOrEmpty() }
        .mapNotNull { it.altText }
        .toList()
    val altTextsInlineMedia = document.mediaList().asSequence()
        .filter { !it.altText.isNullOrEmpty() }
        .mapNotNull { it.altText }
        .toList()
    return (altTextsMedia + altTextsInlineMedia).joinToString(", ")
}

private fun Note.getNoteContent(): String = document.asString()

private fun Note.getNoteDate(context: Context): String =
    context.getString(
        R.string.sn_label_date,
        context.parseMillisToRFC1123String(documentModifiedAt)
    )

private fun Note.getNoteColor(context: Context): String = context.getString(color.toColorNameResource())

// =============== Note Accessibility label end =================
