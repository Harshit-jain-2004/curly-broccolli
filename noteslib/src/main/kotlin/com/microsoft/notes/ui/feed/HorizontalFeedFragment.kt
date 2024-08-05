package com.microsoft.notes.ui.feed

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.feed.filter.FeedFilters
import com.microsoft.notes.ui.feed.recyclerview.FeedItem
import com.microsoft.notes.ui.feed.recyclerview.FeedLayoutType
import com.microsoft.notes.ui.feed.recyclerview.HorizontalFeedAdapter
import com.microsoft.notes.ui.feed.recyclerview.feeditem.NoteReferenceFeedItemComponent
import com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes.SamsungNoteFeedItemComponent
import com.microsoft.notes.ui.feed.sourcefilter.FeedSourceFilterOption
import com.microsoft.notes.ui.noteslist.NotesListComponent
import com.microsoft.notes.ui.noteslist.UserNotifications
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.FeedItemType
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import kotlinx.android.synthetic.main.feed_layout.*
import kotlinx.android.synthetic.main.horizontal_feed_layout.*

@Suppress("TooManyFunctions")
class HorizontalFeedFragment :
    Fragment(),
    FragmentApi {

    private lateinit var adapter: HorizontalFeedAdapter
    private lateinit var presenter: FeedPresenter
    var noteReferenceCallbacks: NoteReferenceFeedItemComponent.Callbacks? = null

    override fun onStart() {
        super.onStart()
        presenter = FeedPresenter(this)
        presenter.onStart()
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::presenter.isInitialized) {
            presenter.onDestroy()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.horizontal_feed_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = createAdapter()
        horizontalFeedRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        horizontalFeedRecyclerView.adapter = adapter
    }

    private fun createAdapter(): HorizontalFeedAdapter =
        HorizontalFeedAdapter(
            feedItems = emptyList(),
            noteCallbacks = object : NoteItemComponent.Callbacks() {
                override fun onNoteItemClicked(note: Note) {
                    noteItemClickHandling(note)
                }

                override fun onNoteItemLongPress(note: Note, view: View) {}
            },
            noteReferenceCallbacks = object : NoteReferenceFeedItemComponent.Callbacks {
                override fun onNoteItemClicked(note: NoteReference) {
                    noteReferenceCallbacks?.onNoteItemClicked(note)
                    presenter.recordTelemetry(
                        EventMarkers.TappedOnFeedItem,
                        Pair(NotesSDKTelemetryKeys.NoteProperty.FEED_ITEM_TYPE, note.type),
                        Pair(NotesSDKTelemetryKeys.FeedUIProperty.FEED_SELECTED_ITEM_DEPTH, feedComponent?.adapter?.getIndexFromItemLocalId(note.localId).toString())
                    )
                }

                override fun onNoteItemLongPress(note: NoteReference, view: View) {}
            },
            samsungNoteCallbacks = object : SamsungNoteFeedItemComponent.Callbacks {
                override fun onNoteItemClicked(note: Note) {
                    noteItemClickHandling(note)
                }

                override fun onNoteItemLongPress(note: Note, view: View) {}
            }
        )

    private fun noteItemClickHandling(note: Note) {
        NotesListComponent.DefaultNotesListComponentCallbacks.onNoteClicked(note)
        presenter.recordTelemetry(
            EventMarkers.TappedOnFeedItem,
            Pair(
                NotesSDKTelemetryKeys.NoteProperty.FEED_ITEM_TYPE,
                if (note.document.isSamsungNoteDocument)
                    FeedItemType.SamsungNote.name
                else FeedItemType.StickyNote.name
            ),
            Pair(NotesSDKTelemetryKeys.FeedUIProperty.FEED_SELECTED_ITEM_DEPTH, feedComponent?.adapter?.getIndexFromItemLocalId(note.localId).toString())
        )
    }

    override fun updateFeedItems(items: List<FeedItem>, scrollToTop: Boolean?) {
        adapter.updateFeed(items)
    }

    override fun shouldAddTimeHeader() = false

    // not required by horizontal feed
    override fun updateSourceFilterButtonLabel(source: FeedSourceFilterOption) {}
    override fun updateFilterOptions(feedFilters: FeedFilters) {}
    override fun updateTimeHeader() {}
    override fun finishActionMode() {}
    override fun invalidateActionMode() {}
    override fun changeFeedLayout(layoutType: FeedLayoutType) {}
    override fun hideSwipeToRefreshSpinner(source: FeedSourceFilterOption) {}
    override fun updateUserNotificationsUi(userIdToNotificationsMap: Map<String, UserNotifications>) {}
    override fun onRefreshCompleted(errorMessageId: Int?) {}
    override fun displayFilterAndSortPanel() {}
    override fun deleteNoteReferences(noteReferences: List<NoteReference>) {}

    override fun getConnectivityManager(): ConnectivityManager? =
        activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
}
