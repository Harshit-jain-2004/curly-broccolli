package com.microsoft.notes.ui.feed.recyclerview

import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import org.junit.BeforeClass
import org.junit.Test

class FeedItemDiffCallBackTest {

    companion object {
        private lateinit var feedListNew: List<FeedItem>
        private lateinit var feedListOld: List<FeedItem>

        @BeforeClass @JvmStatic
        fun `setUp test`() {
            feedListOld = listOf<FeedItem>(
                createFeedNoteItem("Note1", "Text"), createFeedNoteItem("Note2", "small text"),
                createFeedNoteReferenceItem("Note3", "How are you?"), createFeedNoteReferenceItem("Note4", "This note"),
                FeedItem.TimeHeaderItem(TimeBucket.ThisMonth(0)), FeedItem.TimeHeaderItem(TimeBucket.ThisWeek(0)),
                createFeedNoteItem("Note5", "Text"), createFeedNoteItem("Note5", "Changed text"),
                createFeedNoteReferenceItem("Note6", "text"), createFeedNoteReferenceItem("Note6", "Changed text")
            )

            feedListNew = feedListOld.toList() // copy is made
        }

        private fun createFeedNoteItem(localId: String, text: String): FeedItem.NoteItem {
            val content = Content(text = text)
            val paragraph = Paragraph(localId = localId, content = content)
            val document = Document(listOf(paragraph))
            val note = Note(localId = localId, document = document, localCreatedAt = 0, documentModifiedAt = 0)
            return FeedItem.NoteItem(note)
        }

        private fun createFeedNoteReferenceItem(localId: String, content: String): FeedItem.NoteReferenceItem {
            val noteReference = NoteReference(
                localId = localId,
                title = content,
                type = "source",
                lastModifiedAt = 0,
                createdAt = 0
            )
            return FeedItem.NoteReferenceItem(noteReference)
        }
    }

    @Test
    fun `checkDiffForNote test`() {
        val feedItemDiff = FeedItemsDiffCallback(feedListOld, feedListNew, null, null)
        // same item comparison
        assert(feedItemDiff.areContentsTheSame(0, 0))
        assert(feedItemDiff.areContentsTheSame(2, 2))
        assert(feedItemDiff.areContentsTheSame(4, 4))

        // comparison between same type, different values
        assert(!feedItemDiff.areContentsTheSame(0, 1))
        assert(!feedItemDiff.areContentsTheSame(2, 3))
        assert(!feedItemDiff.areContentsTheSame(4, 5))

        // comparison between different types
        assert(!feedItemDiff.areContentsTheSame(0, 2))
        assert(!feedItemDiff.areContentsTheSame(0, 4))
        assert(!feedItemDiff.areContentsTheSame(2, 4))

        // note and noteReference, same id different text
        assert(!feedItemDiff.areContentsTheSame(6, 7))
        assert(!feedItemDiff.areContentsTheSame(8, 9))
    }

    @Test
    fun `check diff for search test`() {
        val feedItemDiff1 = FeedItemsDiffCallback(feedListOld, feedListNew, listOf("text", "note"), listOf("text", "note"))

        assert(feedItemDiff1.areItemsTheSame(0, 0)) // same item
        assert(!feedItemDiff1.areItemsTheSame(0, 1)) // same type

        val feedItemDiff2 = FeedItemsDiffCallback(feedListOld, feedListNew, listOf("text", "note"), null)

        assert(!feedItemDiff2.areItemsTheSame(0, 0)) // same item
        assert(!feedItemDiff2.areItemsTheSame(0, 1)) // same type

        val feedItemDiff3 = FeedItemsDiffCallback(feedListOld, feedListNew, listOf("text", "note"), listOf("text", "note", "bug"))

        assert(!feedItemDiff3.areItemsTheSame(0, 0)) // same item
        assert(!feedItemDiff3.areItemsTheSame(0, 1)) // same type
    }
}
