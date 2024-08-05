package com.microsoft.notes.ui.feed

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.feed.filter.FeedSortCache
import com.microsoft.notes.ui.feed.recyclerview.BaseFeedAdapter
import com.microsoft.notes.ui.feed.recyclerview.FeedAdapter
import com.microsoft.notes.ui.feed.recyclerview.FeedItem
import com.microsoft.notes.ui.feed.recyclerview.FeedLayoutType
import com.microsoft.notes.ui.feed.recyclerview.SelectionTrackerCallback
import com.microsoft.notes.ui.feed.recyclerview.feeditem.NoteReferenceFeedItemComponent
import com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes.SamsungNoteFeedItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.utils.Constants.FEED_LAYOUT_TYPE_PREFERENCE
import com.microsoft.notes.utils.utils.Constants.FEED_LAYOUT_TYPE_PREFERENCE_KEY
import kotlinx.android.synthetic.main.feed_layout.view.*
import kotlinx.android.synthetic.main.sn_feed_component_layout.view.*

class FeedComponent(context: Context, attributeSet: AttributeSet?) : FrameLayout(context, attributeSet) {
    var noteReferenceCallbacks: NoteReferenceFeedItemComponent.Callbacks? = null
    var stickyNoteCallbacks: NoteItemComponent.Callbacks? = null
    var samsungNoteCallbacks: SamsungNoteFeedItemComponent.Callbacks? = null
    var feedListCallbacks: FeedListCallbacks? = null

    val adapter: FeedAdapter
    val selectionTracker: SelectionTracker

    private val firstVisibleItemPosition: Int
        get() {
            val layoutManager = feedRecyclerView.layoutManager

            return when (layoutManager) {
                is GridLayoutManager -> layoutManager.findFirstVisibleItemPosition()
                is LinearLayoutManager -> layoutManager.findFirstVisibleItemPosition()
                is StaggeredGridLayoutManager -> {
                    val viewIds = layoutManager.findFirstVisibleItemPositions(null)
                    viewIds.minOrNull() ?: RecyclerView.NO_POSITION
                }
                else -> RecyclerView.NO_POSITION
            }
        }

    private val lastVisibleItemPosition: Int
        get() {
            val layoutManager = feedRecyclerView.layoutManager

            return when (layoutManager) {
                is GridLayoutManager -> layoutManager.findLastVisibleItemPosition()
                is LinearLayoutManager -> layoutManager.findLastVisibleItemPosition()
                is StaggeredGridLayoutManager -> {
                    val viewIds = layoutManager.findLastVisibleItemPositions(null)
                    viewIds.lastOrNull() ?: RecyclerView.NO_POSITION
                }
                else -> RecyclerView.NO_POSITION
            }
        }

    private val hideSwipeToRefreshSpinnerRunnable = Runnable {
        stopRefreshAnimation()
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.sn_feed_component_layout, this)
        adapter = createAdapter()

        selectionTracker = SelectionTracker(adapter, adapter, NotesLibrary.getInstance().experimentFeatureFlags.multiSelectInActionModeEnabled)
        setUpSwipeToRefresh()
        if (!NotesLibrary.getInstance().experimentFeatureFlags.feedDifferentViewModesEnabled) {
            feedComponent.setPadding(
                resources.getDimensionPixelSize(R.dimen.feed_child_margin),
                0,
                resources.getDimensionPixelSize(R.dimen.feed_child_margin),
                0
            )
        }
    }

    private fun setUpSwipeToRefresh() {
        feedSwipeToRefresh.setOnRefreshListener {
            feedListCallbacks?.onSwipeToRefreshTriggered()
        }
    }

    fun updateFeedItems(items: List<FeedItem>, keywordsToHighlight: List<String>? = null, scrollToTop: Boolean? = null) {
        val shouldFeedListScrollOnSyncDown = scrollToTop ?: isTopItemVisible()
        if (FeedSortCache.dateCriterionSwitched) {
            FeedSortCache.dateCriterionSwitched = false
            adapter.notifyDataSetChanged()
        }
        adapter.updateFeed(items, keywordsToHighlight)
        if (shouldFeedListScrollOnSyncDown) {
            scrollToTop()
        }
    }

    fun stopRefreshAnimation() {
        feedSwipeToRefresh.isRefreshing = false
        feedSwipeToRefresh.removeCallbacks(hideSwipeToRefreshSpinnerRunnable)
    }

    fun setSwipeToRefreshEnabled(isEnabled: Boolean) {
        feedSwipeToRefresh.isEnabled = isEnabled
    }

    fun dismissSwipeToRefresh() {
        feedSwipeToRefresh.post(hideSwipeToRefreshSpinnerRunnable)
    }

    fun ensureFeedItemVisibility(feedItemLocalId: String) {
        val activeItemIndex = adapter.getIndexFromItemLocalId(feedItemLocalId)
        if (activeItemIndex == -1) {
            return
        }

        if (activeItemIndex <= firstVisibleItemPosition || activeItemIndex >= lastVisibleItemPosition) {
            if (NotesLibrary.getInstance().experimentFeatureFlags.feedDifferentViewModesEnabled) {
                feedRecyclerView.layoutManager?.scrollToPosition(activeItemIndex) // TODO: Figure out right API to be used here
            } else {
                (feedRecyclerView.layoutManager as LinearLayoutManager?)?.scrollToPositionWithOffset(activeItemIndex, Int.MIN_VALUE)
            }
        }
    }

    fun getTopVisibleItemIndex(): Int = if (firstVisibleItemPosition >= 0) firstVisibleItemPosition else 0

    fun scrollToTop() {
        feedRecyclerView.post {
            if (NotesLibrary.getInstance().experimentFeatureFlags.feedDifferentViewModesEnabled) {
                feedRecyclerView.layoutManager?.scrollToPosition(0)
            } else {
                (feedRecyclerView.layoutManager as LinearLayoutManager?)?.scrollToPositionWithOffset(
                    0,
                    0
                )
            }
        }
    }

    fun changeFeedLayout(layoutType: FeedLayoutType) {
        // skip if layoutType is same as current layout
        if (getFeedLayout() == layoutType) {
            return
        }
        // Update layout type in shared preferences
        val sharedPrefs = context.getSharedPreferences(FEED_LAYOUT_TYPE_PREFERENCE, Context.MODE_PRIVATE)

        sharedPrefs.edit().putInt(FEED_LAYOUT_TYPE_PREFERENCE_KEY, layoutType.value).apply()
        updateRecyclerViewLayoutManager(layoutType)
    }

    fun getFeedLayout(): FeedLayoutType {
        val sharedPrefs = context.getSharedPreferences(FEED_LAYOUT_TYPE_PREFERENCE, Context.MODE_PRIVATE)
        return FeedLayoutType.fromInt(sharedPrefs.getInt(FEED_LAYOUT_TYPE_PREFERENCE_KEY, FeedLayoutType.GRID_LAYOUT.value))
    }

    private fun isTopItemVisible(): Boolean = firstVisibleItemPosition == 0

    private fun createAdapter(): FeedAdapter {
        val adapter = FeedAdapter(
            feedItems = emptyList(),
            noteCallbacks = object : NoteItemComponent.Callbacks() {
                override fun onNoteItemClicked(note: Note) {
                    handleOnClick(note)
                }

                override fun onNoteItemLongPress(note: Note, view: View) {
                    handleLongPress(note)
                }
            },
            noteReferenceCallbacks = object : NoteReferenceFeedItemComponent.Callbacks {
                override fun onNoteItemClicked(note: NoteReference) {
                    handleOnClick(note)
                }

                override fun onNoteItemLongPress(note: NoteReference, view: View) {
                    handleLongPress(note)
                }
            },
            selectionTrackerCallback = object : SelectionTrackerCallback {
                override fun isItemSelected(item: FeedItem) = selectionTracker.isItemSelected(getLocalId(item))
                override fun isSelectionEnabled() = selectionTracker.isSelectionEnabled()
            },
            samsungNoteCallbacks = object : SamsungNoteFeedItemComponent.Callbacks {
                override fun onNoteItemClicked(note: Note) = handleOnClick(note)
                override fun onNoteItemLongPress(note: Note, view: View) = handleLongPress(note)
            },
            fragmentApi = object : BaseFeedAdapter.FragmentApi {
                override fun isFeedUIRefreshEnabled() = NotesLibrary.getInstance().experimentFeatureFlags.feedDifferentViewModesEnabled
                override fun getFeedLayoutType() = getFeedLayout()
            }
        )
        createLayoutManager()
        feedRecyclerView.adapter = adapter
        return adapter
    }

    private fun createLayoutManager() {
        var layoutPreference = FeedLayoutType.LIST_LAYOUT

        // Get user selected preference layout view if feedDifferentViewModesEnabled FG is enabled
        if (NotesLibrary.getInstance().experimentFeatureFlags.feedDifferentViewModesEnabled) {
            layoutPreference = getFeedLayout()
        }

        updateRecyclerViewLayoutManager(layoutPreference)
    }

    private fun updateRecyclerViewLayoutManager(layoutType: FeedLayoutType) {
        feedRecyclerView.layoutManager = when (layoutType) {
            FeedLayoutType.LIST_LAYOUT -> LinearLayoutManager(context)
            FeedLayoutType.GRID_LAYOUT -> StaggeredGridLayoutManager(2, GridLayoutManager.VERTICAL)
        }
    }

    fun isSelectionEnabled() = selectionTracker.isSelectionEnabled()

    private fun changeSelection(item: Any) {
        selectionTracker.setSelectedItem(getLocalId(item))
        if (selectionTracker.getSelectedItems().isEmpty()) {
            feedListCallbacks?.endActionMode()
        } else {
            feedListCallbacks?.invalidateActionMenu()
        }
    }

    private fun getLocalId(item: Any): String = when (item) {
        is FeedItem.NoteItem -> item.note.localId
        is FeedItem.NoteReferenceItem -> item.noteReference.localId
        is Note -> item.localId
        is NoteReference -> item.localId
        else -> ""
    }

    fun getSelectedItems(): List<FeedItem> {
        val selectedIds = selectionTracker.getSelectedItems()

        // iterate through the list of feed items to filter the items which are selected i.e. which match the localIds of selected items
        return adapter.feedItems.filter { feedItem -> selectedIds.contains(getLocalId(feedItem)) }
    }

    /*
     * In multi-select action mode, we want the user to select the notes only if he can perform actions on it
     * Currently, if the notebook page is not opened locally we cannot perform actions on it.
     * So, this function doesn't executes the callback if we try to select a notebook page in the feed which is opened not locally
     * When an item is selectable we call the callbackIfSelectable function else we show the toast
     * When we don't have noteReferenceCallback or promise returned, we assume that we cannot perform action on the note and show the toast
     * Showing toast is temporary UI change. It will changed very soon
     */
    private fun executeCallbackIfItemSelectable(item: Any, callbackIfSelectable: () -> Unit) {
        if (NotesLibrary.getInstance().experimentFeatureFlags.multiSelectInActionModeEnabled && (item is NoteReference)) {
            if (NotesLibrary.getInstance().isActionsEnabledOnNoteReferences) {
                noteReferenceCallbacks?.canPerformActionOnNote(item)?.then {
                    post {
                        if (!it) recordTelemetryAndShowToast()
                        else callbackIfSelectable()
                    }
                } ?: recordTelemetryAndShowToast()
            } else {
                recordTelemetryAndShowToast()
            }
        } else {
            callbackIfSelectable()
        }
    }

    private fun handleLongPress(item: Any) {
        if (NotesLibrary.getInstance().uiOptionFlags.showActionModeOnFeed && feedListCallbacks?.isActionModeSupported() == true) {
            executeCallbackIfItemSelectable(item) {
                if (isSelectionEnabled()) {
                    changeSelection(item)
                } else {
                    selectionTracker.startSelection(getLocalId(item))
                    feedListCallbacks?.startActionMode()
                }
            }
        }
    }

    private fun handleOnClick(item: Any) {
        if (feedListCallbacks?.isActionModeSupported() == true && isSelectionEnabled()) {
            executeCallbackIfItemSelectable(item) {
                changeSelection(item)
            }
            return
        }
        when (item) {
            is Note -> if (item.document.isSamsungNoteDocument) {
                samsungNoteCallbacks?.onNoteItemClicked(item)
            } else {
                stickyNoteCallbacks?.onNoteItemClicked(item)
            }
            is NoteReference -> noteReferenceCallbacks?.onNoteItemClicked(item)
        }
    }

    open class FeedListCallbacks {
        open fun onSwipeToRefreshTriggered() {}
        open fun startActionMode() {}
        open fun endActionMode() {}
        open fun invalidateActionMenu() {}
        open fun isActionModeSupported(): Boolean = true
    }

    private fun recordTelemetryAndShowToast() {
        NotesLibrary.getInstance().recordTelemetry(EventMarkers.PageNotOpenedInMultiSelectMode)
        NotesLibrary.getInstance().showToast(R.string.not_opened)
    }
}
