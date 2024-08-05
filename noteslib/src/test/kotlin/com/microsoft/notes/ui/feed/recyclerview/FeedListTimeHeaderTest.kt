package com.microsoft.notes.ui.feed.recyclerview

import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.GregorianCalendar
import org.hamcrest.CoreMatchers.`is` as iz

class FeedListTimeHeaderTest {

    @Test
    fun `addTimeHeaders test`() {
        val currentTimeMillis = GregorianCalendar(2020, 5, 17).timeInMillis // Wednesday

        val feedItems: List<FeedItem> = listOf(
            createNoteReferenceFeedItem("0", GregorianCalendar(2020, 5, 20, 23, 0)),
            createNoteFeedItem("1", GregorianCalendar(2020, 5, 17, 2, 0)),
            createNoteFeedItem("2", GregorianCalendar(2020, 5, 16, 4, 0)),
            createNoteReferenceFeedItem("3", GregorianCalendar(2020, 5, 15, 15, 1)),
            createNoteFeedItem("4", GregorianCalendar(2020, 5, 12, 23, 0)),
            createNoteReferenceFeedItem("5", GregorianCalendar(2020, 5, 10, 23, 0)),
            createNoteReferenceFeedItem("6", GregorianCalendar(2020, 5, 3, 23, 0)),
            createNoteReferenceFeedItem("7", GregorianCalendar(2020, 5, 2, 23, 0)),
            createNoteReferenceFeedItem("8", GregorianCalendar(2020, 4, 23, 23, 0)),
            createNoteReferenceFeedItem("9", GregorianCalendar(2020, 4, 12, 23, 0)),
            createNoteReferenceFeedItem("10", GregorianCalendar(2020, 2, 23, 23, 0)),
            createNoteReferenceFeedItem("11", GregorianCalendar(2020, 0, 12, 23, 0)),
            createNoteReferenceFeedItem("12", GregorianCalendar(2019, 11, 23, 23, 0)),
            createNoteReferenceFeedItem("13", GregorianCalendar(2018, 5, 12, 23, 0))
        )

        val expected = listOf(
            feedItems[0],
            feedItems[1],
            createTimeBucketItem(TimeBucket.Yesterday(0L)),
            feedItems[2],
            createTimeBucketItem(TimeBucket.ThisWeek(0L)),
            feedItems[3],
            createTimeBucketItem(TimeBucket.LastWeek(0L)),
            feedItems[4],
            feedItems[5],
            createTimeBucketItem(TimeBucket.ThisMonth(0L)),
            feedItems[6],
            feedItems[7],
            createTimeBucketItem(TimeBucket.LastMonth(0L)),
            feedItems[8],
            feedItems[9],
            createTimeBucketItem(TimeBucket.Month(2020, 2)),
            feedItems[10],
            createTimeBucketItem(TimeBucket.Month(2020, 0)),
            feedItems[11],
            createTimeBucketItem(TimeBucket.Month(2019, 11)),
            feedItems[12],
            createTimeBucketItem(TimeBucket.Year(2018)),
            feedItems[13]
        )
        assertThat(addTimeHeaders(feedItems, currentTimeMillis).map { it.id }, iz(expected.map { it.id }))
    }

    private fun createNoteFeedItem(id: String, calendar: GregorianCalendar) =
        FeedItem.NoteItem(
            Note(
                localId = id,
                documentModifiedAt = calendar.timeInMillis
            )
        )

    private fun createNoteReferenceFeedItem(id: String, calendar: GregorianCalendar) =
        FeedItem.NoteReferenceItem(
            NoteReference(
                localId = id,
                lastModifiedAt = calendar.timeInMillis
            )
        )

    private fun createTimeBucketItem(timeBucket: TimeBucket) = FeedItem.TimeHeaderItem(timeBucket)
}
