package com.microsoft.notes.ui.feed.recyclerview

import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.richtext.scheme.DocumentType
import com.microsoft.notes.richtext.scheme.asParagraph

private const val MAX_CHARS_CONSIDERED_IN_LINE = 300

sealed class FeedItem(val id: String) {
    class NoteItem(val note: Note) : FeedItem(note.localId) {
        override fun getLastModifiedTime() = note.documentModifiedAt
        override fun getCreatedTime() = note.localCreatedAt
        override fun getTitle(): String? {
            val title = when (note.document.type) {
                DocumentType.SAMSUNG_NOTE -> {
                    var title = note.title?.trim()
                    if (title.isNullOrEmpty())
                        title = note.document.bodyPreview.trim().take(MAX_CHARS_CONSIDERED_IN_LINE)
                    return if (title.isNullOrEmpty()) null else title
                }
                DocumentType.RICH_TEXT -> note.document.blocks.firstOrNull()?.asParagraph()?.content?.text?.trim()?.take(MAX_CHARS_CONSIDERED_IN_LINE)
                else -> null
            }
            return if (title.isNullOrEmpty()) null else title
        }

        override fun getPinnedTime(): Long? = note.pinnedAt
    }

    class NoteReferenceItem(val noteReference: NoteReference) : FeedItem(noteReference.localId) {
        override fun getLastModifiedTime() = noteReference.lastModifiedAt
        override fun getCreatedTime() = noteReference.createdAt
        override fun getTitle(): String? {
            var title = noteReference.title?.trim()
            if (title.isNullOrEmpty())
                title = noteReference.previewText.trim().take(MAX_CHARS_CONSIDERED_IN_LINE)
            return if (title.isNullOrEmpty()) null else title
        }
        override fun getPinnedTime(): Long? = noteReference.pinnedAt
    }

    class TimeHeaderItem(timeBucket: TimeBucket) : FeedItem(timeBucket.id) {
        override fun getLastModifiedTime() = -1L
        override fun getCreatedTime() = -1L
        override fun getTitle() = null
        init {
            this.timeBucket = timeBucket
        }
        override fun getPinnedTime(): Long? = null
    }

    fun getStableId() = this.id.hashCode().toLong()
    abstract fun getLastModifiedTime(): Long
    abstract fun getCreatedTime(): Long
    abstract fun getTitle(): String?
    abstract fun getPinnedTime(): Long?
    var timeBucket: TimeBucket? = null
}
