package com.microsoft.notes.ui.feed.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.editor.styled.shouldRenderAsHTML
import com.microsoft.notes.sideeffect.sync.getSamsungAttachedImagesForHTMLNote
import com.microsoft.notes.ui.extensions.isSamsungNote
import com.microsoft.notes.ui.feed.SelectionTracker
import com.microsoft.notes.ui.feed.recyclerview.feeditem.FeedItemViewHolder
import com.microsoft.notes.ui.feed.recyclerview.feeditem.NoteReferenceFeedItemComponent
import com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes.SamsungNoteFeedItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent

/**
 * Used by both Horizontal and vertical feed lists
 */
abstract class BaseFeedAdapter(
    var feedItems: List<FeedItem>,
    val noteCallbacks: NoteItemComponent.Callbacks,
    val noteReferenceCallbacks: NoteReferenceFeedItemComponent.Callbacks,
    val samsungNoteCallbacks: SamsungNoteFeedItemComponent.Callbacks,
    val fragmentApi: FragmentApi
) : RecyclerView.Adapter<FeedItemViewHolder>(), SelectionTracker.Callbacks {

    internal var keywordsToHighlight: List<String>? = null

    init {
        setHasStableIds(true)
    }

    fun updateFeed(newFeedItems: List<FeedItem>, newKeywordsToHighlight: List<String>? = null) {
        val oldFeedItems = feedItems
        val oldKeywordsToHighlight = keywordsToHighlight
        keywordsToHighlight = newKeywordsToHighlight
        feedItems = newFeedItems
        applyDiff(oldFeedItems, newFeedItems, oldKeywordsToHighlight, keywordsToHighlight)
    }

    private fun applyDiff(oldFeedItems: List<FeedItem>, newFeedItems: List<FeedItem>, oldKeywordsToHighlight: List<String>?, newKeywordsToHighlight: List<String>?) {
        val diffResult = DiffUtil.calculateDiff(FeedItemsDiffCallback(oldFeedItems, newFeedItems, oldKeywordsToHighlight, newKeywordsToHighlight))
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        with(feedItems[position]) {
            return when (this) {
                is FeedItem.NoteItem ->
                    // Supporting only note-as-bitmap for samsung notes
                    if (note.isSamsungNote()) getViewTypeForSamsungNote(this.note)
                    else getViewTypeForNote(this.note)
                is FeedItem.NoteReferenceItem -> getViewTypeForNoteReference()
                is FeedItem.TimeHeaderItem -> FeedItemType.TIME_HEADER
            }.id
        }
    }

    override fun getItemId(position: Int): Long =
        feedItems[position].getStableId()

    private fun getViewTypeForSamsungNote(note: Note): FeedItemType {
        return if (fragmentApi.isFeedUIRefreshEnabled()) {
            val isGridLayout = fragmentApi.getFeedLayoutType() == FeedLayoutType.GRID_LAYOUT
            val htmlRenderingIsEnabled = shouldRenderAsHTML(note)
            val hasAttachedMedia: Boolean = note.media.getSamsungAttachedImagesForHTMLNote().isNotEmpty()
            val hasNoText: Boolean = note.hasNoBodyPreview && note.title.isNullOrEmpty()

            when (htmlRenderingIsEnabled) {
                false ->
                    if (isGridLayout) FeedItemType.SAMSUNG_NOTE_PREVIEW_IMAGE_ONLY_GRID
                    else FeedItemType.SAMSUNG_NOTE_PREVIEW_IMAGE_ONLY
                true ->
                    when {
                        hasAttachedMedia && hasNoText ->
                            if (isGridLayout) FeedItemType.SAMSUNG_NOTE_IMAGE_ONLY_GRID
                            else FeedItemType.SAMSUNG_NOTE_IMAGE_ONLY
                        hasAttachedMedia ->
                            if (isGridLayout) FeedItemType.SAMSUNG_NOTE_TEXT_IMAGE_GRID
                            else FeedItemType.SAMSUNG_NOTE_TEXT_IMAGE
                        else ->
                            if (isGridLayout) FeedItemType.SAMSUNG_NOTE_TEXT_ONLY_GRID
                            else FeedItemType.SAMSUNG_NOTE_TEXT_ONLY
                    }
            }
        } else {
            when (shouldRenderAsHTML(note)) {
                true -> FeedItemType.SAMSUNG_NOTE_OLD_HTML
                false -> FeedItemType.SAMSUNG_NOTE_OLD_PREVIEW_IMAGE
            }
        }
    }

    private fun getViewTypeForNoteReference(): FeedItemType {
        return if (fragmentApi.isFeedUIRefreshEnabled()) {
            val isGridLayout = fragmentApi.getFeedLayoutType() == FeedLayoutType.GRID_LAYOUT
            if (isGridLayout)
                FeedItemType.NOTE_REFERENCE_UI_REFRESH_GRID
            else
                FeedItemType.NOTE_REFERENCE_UI_REFRESH
        } else {
            FeedItemType.NOTE_REFERENCE
        }
    }

    private fun getViewTypeForNote(note: Note): FeedItemType {
        return if (fragmentApi.isFeedUIRefreshEnabled()) {
            val isGridLayout = fragmentApi.getFeedLayoutType() == FeedLayoutType.GRID_LAYOUT
            when {
                note.isInkNote -> if (isGridLayout) FeedItemType.SN_INK_GRID else FeedItemType.SN_INK
                note.isMediaListEmpty -> if (isGridLayout) FeedItemType.SN_TEXT_ONLY_GRID else FeedItemType.SN_TEXT_ONLY
                note.hasNoText -> if (isGridLayout) FeedItemType.SN_IMAGE_ONLY_GRID else FeedItemType.SN_IMAGE_ONLY
                else -> if (isGridLayout) FeedItemType.SN_TEXT_IMAGE_GRID else FeedItemType.SN_TEXT_IMAGE
            }
        } else {
            when {
                note.isInkNote -> FeedItemType.INK
                note.isMediaListEmpty -> FeedItemType.TEXT
                note.mediaCount == 1 -> FeedItemType.SINGLE_IMAGE
                note.mediaCount == 2 -> FeedItemType.TWO_IMAGE
                note.mediaCount == 3 -> FeedItemType.THREE_IMAGE
                else -> FeedItemType.MULTI_IMAGE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedItemViewHolder {
        val layoutId = getViewHolderLayoutId(viewType)

        // TODO Is there a cleaner + more compile-safe way of typing?
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        when (view) {
            is NoteItemComponent -> view.callbacks = noteCallbacks
            is NoteReferenceFeedItemComponent -> view.callbacks = noteReferenceCallbacks
            is SamsungNoteFeedItemComponent -> view.callbacks = samsungNoteCallbacks
        }

        return FeedItemViewHolder(view)
    }

    abstract fun getViewHolderLayoutId(viewType: Int): Int

    override fun getItemCount(): Int = feedItems.size

    override fun getIndexFromItemLocalId(itemId: String?): Int {
        var index = -1
        if (itemId == null) {
            return index
        }
        for (i in 0 until this.itemCount) {
            if (itemId == this.feedItems.get(i).id) {
                index = i
                break
            }
        }
        return index
    }

    enum class FeedItemType(val id: Int) {
        TEXT(0),
        SINGLE_IMAGE(1),
        TWO_IMAGE(2),
        THREE_IMAGE(3),
        MULTI_IMAGE(4),
        INK(5),
        NOTE_REFERENCE(6),
        TIME_HEADER(7),
        SAMSUNG_NOTE_OLD_HTML(8),
        SAMSUNG_NOTE_OLD_PREVIEW_IMAGE(9),
        SN_IMAGE_ONLY(10),
        SN_IMAGE_ONLY_GRID(11),
        SN_TEXT_ONLY(12),
        SN_TEXT_IMAGE(13),
        SN_TEXT_ONLY_GRID(14),
        SN_TEXT_IMAGE_GRID(15),
        SN_INK(16),
        SN_INK_GRID(17),
        NOTE_REFERENCE_UI_REFRESH(18),
        NOTE_REFERENCE_UI_REFRESH_GRID(19),
        SAMSUNG_NOTE_PREVIEW_IMAGE_ONLY(20),
        SAMSUNG_NOTE_PREVIEW_IMAGE_ONLY_GRID(21),
        SAMSUNG_NOTE_IMAGE_ONLY(22),
        SAMSUNG_NOTE_IMAGE_ONLY_GRID(23),
        SAMSUNG_NOTE_TEXT_ONLY(24),
        SAMSUNG_NOTE_TEXT_ONLY_GRID(25),
        SAMSUNG_NOTE_TEXT_IMAGE(26),
        SAMSUNG_NOTE_TEXT_IMAGE_GRID(27)
    }

    interface FragmentApi {
        fun isFeedUIRefreshEnabled(): Boolean = false
        fun getFeedLayoutType(): FeedLayoutType = FeedLayoutType.LIST_LAYOUT
    }
}
