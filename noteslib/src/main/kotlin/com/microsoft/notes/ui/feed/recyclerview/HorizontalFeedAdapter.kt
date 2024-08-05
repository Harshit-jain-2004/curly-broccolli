package com.microsoft.notes.ui.feed.recyclerview

import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.feed.recyclerview.feeditem.FeedItemViewHolder
import com.microsoft.notes.ui.feed.recyclerview.feeditem.NoteReferenceFeedItemComponent
import com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes.SamsungNoteFeedItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.WrongViewHolderTypeException

class HorizontalFeedAdapter(
    feedItems: List<FeedItem>,
    noteCallbacks: NoteItemComponent.Callbacks,
    noteReferenceCallbacks: NoteReferenceFeedItemComponent.Callbacks,
    samsungNoteCallbacks: SamsungNoteFeedItemComponent.Callbacks
) : BaseFeedAdapter(
    feedItems, noteCallbacks, noteReferenceCallbacks, samsungNoteCallbacks,
    object : FragmentApi {}
) {

    override fun getViewHolderLayoutId(viewType: Int): Int = when (viewType) {
        FeedItemType.TEXT.id -> R.layout.sn_horizontal_note_item_layout_text
        FeedItemType.SINGLE_IMAGE.id -> R.layout.sn_horizontal_note_item_layout_single_image // text not being displayed for any image notes
        FeedItemType.TWO_IMAGE.id -> R.layout.sn_horizontal_note_item_layout_two_image
        FeedItemType.THREE_IMAGE.id -> R.layout.sn_horizontal_note_item_layout_three_image
        FeedItemType.MULTI_IMAGE.id -> R.layout.sn_horizontal_note_item_layout_multi_image
        FeedItemType.INK.id -> R.layout.sn_horizontal_note_item_layout_ink
        FeedItemType.NOTE_REFERENCE.id -> R.layout.sn_note_item_layout_note_reference
        FeedItemType.SAMSUNG_NOTE_OLD_HTML.id -> R.layout.samsung_feed_item_layout
        FeedItemType.SAMSUNG_NOTE_OLD_PREVIEW_IMAGE.id -> R.layout.samsung_feed_item_layout_preview_image
        else -> throw WrongViewHolderTypeException()
    }

    override fun onBindViewHolder(holder: FeedItemViewHolder, position: Int) {
        with(holder.feedItemView) {
            when (this) {
                is NoteItemComponent -> {
                    (feedItems[position] as? FeedItem.NoteItem)?.let {
                        bindNote(
                            note = it.note, keywordsToHighlight = null, isSelectionEnabled = false,
                            isItemSelected = false, showDateTime = false
                        )
                        return
                    }
                    throw IllegalStateException("onBindViewHolder :: Item is not a NoteItemComponent")
                }
                is SamsungNoteFeedItemComponent -> {
                    (feedItems[position] as? FeedItem.NoteItem)?.let {
                        bindNote(
                            note = it.note, keywordsToHighlight = null, isSelectionEnabled = false,
                            isItemSelected = false, isFeedUIRefreshEnabled = false
                        )
                        return
                    }
                    throw IllegalStateException("onBindViewHolder :: Item is not a SamsungNoteFeedItemComponent")
                }
                is NoteReferenceFeedItemComponent -> {
                    (feedItems[position] as? FeedItem.NoteReferenceItem)?.let {
                        bindNote(
                            note = it.noteReference, keywordsToHighlight = null, showSource = true, isSelectionEnabled = false,
                            isItemSelected = false, isFeedUIRefreshEnabled = false
                        )
                        return
                    }
                    throw IllegalStateException("onBindViewHolder :: Item is not an NoteReferenceFeedItemComponent")
                }
                else -> throw WrongViewHolderTypeException()
            }
        }
    }
}
