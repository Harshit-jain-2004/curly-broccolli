package com.microsoft.notes.ui.feed.recyclerview

import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.ui.feed.filter.FeedSortCache
import com.microsoft.notes.ui.feed.filter.SortingCriterion
import com.microsoft.notes.ui.feed.filter.SortingState
import java.util.Locale

fun collateNotes(
    stickyNotes: List<Note> = emptyList(),
    samsungNotes: List<Note> = emptyList(),
    noteReferences: List<NoteReference> = emptyList()
): List<FeedItem> {
    val pinnedNotesEnabled: Boolean = NotesLibrary.getInstance().experimentFeatureFlags.pinnedNotesEnabled
    val notesCollection = if (pinnedNotesEnabled) (stickyNotes + samsungNotes).filter { !it.isEmpty }.filter { !it.isPinned }.map { FeedItem.NoteItem(it) } else (stickyNotes + samsungNotes).filter { !it.isEmpty }.map { FeedItem.NoteItem(it) }
    val noteReferencesCollection = if (pinnedNotesEnabled) noteReferences.filter { !it.isPinned }.map { FeedItem.NoteReferenceItem(it) } else noteReferences.map { FeedItem.NoteReferenceItem(it) }
    val collection = notesCollection + noteReferencesCollection
    val feedSelectedSort = FeedSortCache.fetchPreferredSortSelection(null)
    val pinnedNotesCollection = if (pinnedNotesEnabled)(stickyNotes + samsungNotes).filter { it.isPinned }.map { FeedItem.NoteItem(it) } else emptyList()
    val pinnedNoteReferencesCollection = if (pinnedNotesEnabled)noteReferences.filter { it.isPinned }.map { FeedItem.NoteReferenceItem(it) } else emptyList()

    // Sort ascending based on sorting criterion (created time/last modified time/title)
    // Default sorting criterion is last modified time

    return (pinnedNotesCollection + pinnedNoteReferencesCollection).sortedWith(compareByDescending { it.getPinnedTime() }) + collection.sortedWith(
        if (feedSelectedSort.second == SortingState.ENABLED_ASCENDING) {
            when (feedSelectedSort.first) {
                SortingCriterion.DATE_MODIFIED -> compareBy { it.getLastModifiedTime() }
                SortingCriterion.DATE_CREATED -> compareBy { it.getCreatedTime() }
                SortingCriterion.TITLE -> compareBy<FeedItem, String?>(nullsLast(), { it.getTitle()?.toLowerCase(Locale.getDefault()) }).thenByDescending { it.getLastModifiedTime() }
            }
        } else {
            when (feedSelectedSort.first) {
                SortingCriterion.DATE_MODIFIED -> compareByDescending { it.getLastModifiedTime() }
                SortingCriterion.DATE_CREATED -> compareByDescending { it.getCreatedTime() }
                SortingCriterion.TITLE -> compareByDescending<FeedItem, String?>(nullsLast(), { it.getTitle()?.toLowerCase(Locale.getDefault()) }).thenByDescending { it.getLastModifiedTime() }
            }
        }
    )
}

// expects a list 'feedItems' sorted by lastModifiedTime descending
fun addTimeHeaders(feedItems: List<FeedItem>, currentTimeMillis: Long = System.currentTimeMillis()): List<FeedItem> {
    val bucketsIterator = TimeBucket.getAllBuckets(currentTimeMillis = currentTimeMillis, nMonth = 10).iterator()
    var currentBucket: TimeBucket = TimeBucket.Empty()

    val bucketedFeedItems = mutableListOf<FeedItem>()

    var isFirstHeaderItem = true
    for (item in feedItems) {
        if (!currentBucket.contains(item.getLastModifiedTime())) {
            currentBucket = getNextBucket(item.getLastModifiedTime(), bucketsIterator)
            if (!(isFirstHeaderItem || currentBucket is TimeBucket.Future)) {
                bucketedFeedItems.add(FeedItem.TimeHeaderItem(currentBucket))
            }
            if (isFirstHeaderItem) isFirstHeaderItem = false
        }
        item.timeBucket = currentBucket
        bucketedFeedItems.add(item)
    }
    return bucketedFeedItems
}
