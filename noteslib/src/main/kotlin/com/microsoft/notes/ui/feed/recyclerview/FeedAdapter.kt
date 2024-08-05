package com.microsoft.notes.ui.feed.recyclerview

import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.feed.recyclerview.feeditem.FeedItemViewHolder
import com.microsoft.notes.ui.feed.recyclerview.feeditem.NoteReferenceFeedItemComponent
import com.microsoft.notes.ui.feed.recyclerview.feeditem.TimeHeaderItemComponent
import com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes.SamsungNoteFeedItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.WrongViewHolderTypeException

/**
 * Used by both Horizontal and vertical feed lists
 */
class FeedAdapter(
    feedItems: List<FeedItem>,
    noteCallbacks: NoteItemComponent.Callbacks,
    noteReferenceCallbacks: NoteReferenceFeedItemComponent.Callbacks,
    samsungNoteCallbacks: SamsungNoteFeedItemComponent.Callbacks,
    val selectionTrackerCallback: SelectionTrackerCallback,
    fragmentApi: FragmentApi
) : BaseFeedAdapter(
    feedItems, noteCallbacks, noteReferenceCallbacks,
    samsungNoteCallbacks, fragmentApi
) {

    private val feedCardImprovementsEnabled = NotesLibrary.getInstance().experimentFeatureFlags.feedCardImprovementsEnabled

    override fun getViewHolderLayoutId(viewType: Int): Int = when (viewType) {
        FeedItemType.TEXT.id -> R.layout.sn_note_item_layout_text
        FeedItemType.SINGLE_IMAGE.id -> R.layout.sn_note_item_layout_single_image
        FeedItemType.TWO_IMAGE.id -> R.layout.sn_note_item_layout_two_image
        FeedItemType.THREE_IMAGE.id -> R.layout.sn_note_item_layout_three_image
        FeedItemType.MULTI_IMAGE.id -> R.layout.sn_note_item_layout_multi_image
        FeedItemType.INK.id -> R.layout.sn_note_item_layout_ink
        FeedItemType.NOTE_REFERENCE.id -> R.layout.sn_note_item_layout_note_reference
        FeedItemType.NOTE_REFERENCE_UI_REFRESH.id -> R.layout.sn_note_item_layout_note_reference_ui_refresh
        FeedItemType.NOTE_REFERENCE_UI_REFRESH_GRID.id -> R.layout.sn_note_item_layout_note_reference_ui_refresh_grid
        FeedItemType.SAMSUNG_NOTE_OLD_HTML.id -> R.layout.samsung_feed_item_layout
        FeedItemType.SAMSUNG_NOTE_OLD_PREVIEW_IMAGE.id -> R.layout.samsung_feed_item_layout_preview_image
        FeedItemType.SAMSUNG_NOTE_PREVIEW_IMAGE_ONLY.id -> R.layout.samsung_feed_item_layout_preview_image_ui_refresh
        FeedItemType.SAMSUNG_NOTE_PREVIEW_IMAGE_ONLY_GRID.id -> R.layout.samsung_feed_item_layout_preview_image_grid_ui_refresh
        FeedItemType.SAMSUNG_NOTE_IMAGE_ONLY.id -> R.layout.samsung_feed_item_layout_image_only
        FeedItemType.SAMSUNG_NOTE_IMAGE_ONLY_GRID.id -> R.layout.samsung_feed_item_layout_image_only_grid
        FeedItemType.SAMSUNG_NOTE_TEXT_ONLY.id -> R.layout.samsung_feed_item_layout_text_only
        FeedItemType.SAMSUNG_NOTE_TEXT_ONLY_GRID.id -> R.layout.samsung_feed_item_layout_text_only_grid
        FeedItemType.SAMSUNG_NOTE_TEXT_IMAGE.id -> R.layout.samsung_feed_item_layout_text_image
        FeedItemType.SAMSUNG_NOTE_TEXT_IMAGE_GRID.id -> R.layout.samsung_feed_item_layout_text_image_grid
        FeedItemType.TIME_HEADER.id -> R.layout.feed_item_time_header
        FeedItemType.SN_IMAGE_ONLY.id -> R.layout.sn_note_feed_item_layout_image_only
        FeedItemType.SN_IMAGE_ONLY_GRID.id -> R.layout.sn_note_feed_item_layout_image_only_grid
        FeedItemType.SN_TEXT_ONLY.id -> R.layout.sn_note_feed_item_layout_text_only
        FeedItemType.SN_TEXT_IMAGE.id -> R.layout.sn_note_feed_item_layout_text_image
        FeedItemType.SN_TEXT_ONLY_GRID.id -> R.layout.sn_note_feed_item_layout_text_only_grid
        FeedItemType.SN_TEXT_IMAGE_GRID.id -> R.layout.sn_note_feed_item_layout_text_image_grid
        FeedItemType.SN_INK.id -> R.layout.sn_note_feed_item_layout_ink
        FeedItemType.SN_INK_GRID.id -> R.layout.sn_note_feed_item_layout_ink_grid
        else -> throw WrongViewHolderTypeException()
    }

    override fun onBindViewHolder(holder: FeedItemViewHolder, position: Int) {
        with(holder.feedItemView) {
            when (this) {
                is NoteItemComponent -> {
                    (feedItems[position] as? FeedItem.NoteItem)?.let {
                        bindNote(
                            note = it.note, keywordsToHighlight = keywordsToHighlight, isSelectionEnabled = selectionTrackerCallback.isSelectionEnabled(),
                            isItemSelected = selectionTrackerCallback.isItemSelected(it),
                            showDateTime = fragmentApi.isFeedUIRefreshEnabled() && !feedCardImprovementsEnabled,
                            showSource = fragmentApi.isFeedUIRefreshEnabled() && !feedCardImprovementsEnabled,
                            showSourceText = fragmentApi.isFeedUIRefreshEnabled() &&
                                fragmentApi.getFeedLayoutType() == FeedLayoutType.LIST_LAYOUT,
                            isFeedUiRefreshEnabled = fragmentApi.isFeedUIRefreshEnabled()
                        )
                        return
                    }
                    throw IllegalStateException("onBindViewHolder :: Item is not a NoteItemComponent")
                }
                is SamsungNoteFeedItemComponent -> {
                    (feedItems[position] as? FeedItem.NoteItem)?.let {
                        bindNote(
                            note = it.note, keywordsToHighlight = keywordsToHighlight, isSelectionEnabled = selectionTrackerCallback.isSelectionEnabled(),
                            isListLayout = fragmentApi.getFeedLayoutType() != FeedLayoutType.GRID_LAYOUT,
                            isItemSelected = selectionTrackerCallback.isItemSelected(it),
                            isFeedUIRefreshEnabled = fragmentApi.isFeedUIRefreshEnabled()
                        )
                        return
                    }
                    throw IllegalStateException("onBindViewHolder :: Item is not a SamsungNoteFeedItemComponent")
                }
                is NoteReferenceFeedItemComponent -> {
                    (feedItems[position] as? FeedItem.NoteReferenceItem)?.let {
                        bindNote(
                            note = it.noteReference, keywordsToHighlight = keywordsToHighlight, showSource = true, isSelectionEnabled = selectionTrackerCallback.isSelectionEnabled(),
                            isListLayout = fragmentApi.getFeedLayoutType() != FeedLayoutType.GRID_LAYOUT,
                            isItemSelected = selectionTrackerCallback.isItemSelected(it),
                            isFeedUIRefreshEnabled = fragmentApi.isFeedUIRefreshEnabled()
                        )
                        return
                    }
                    throw IllegalStateException("onBindViewHolder :: Item is not an NoteReferenceFeedItemComponent")
                }
                is TimeHeaderItemComponent -> {
                    (feedItems[position] as? FeedItem.TimeHeaderItem)?.let {
                        it.timeBucket?.let { it1 -> bindHeader(it1) }
                    }
                }
                else -> throw WrongViewHolderTypeException()
            }
        }
    }
}

interface SelectionTrackerCallback {
    fun isItemSelected(item: FeedItem): Boolean
    fun isSelectionEnabled(): Boolean
}
