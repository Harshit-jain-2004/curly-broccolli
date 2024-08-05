package com.microsoft.notes.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.extensions.parseSearchQuery
import com.microsoft.notes.ui.feed.FeedComponent
import com.microsoft.notes.ui.feed.recyclerview.FeedItem
import com.microsoft.notes.ui.feed.recyclerview.feeditem.NoteReferenceFeedItemComponent
import com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes.SamsungNoteFeedItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.ui.shared.StickyNotesFragment
import kotlinx.android.synthetic.main.feed_search_fragment_layout.*

open class FeedSearchFragment :
    StickyNotesFragment(),
    FeedSearchFragmentApi {
    var noteReferenceCallbacks: NoteReferenceFeedItemComponent.Callbacks? = null

    init {
        allowEnterTransitionOverlap = true
        allowReturnTransitionOverlap = true
    }

    private val presenter: FeedSearchPresenter by lazy {
        FeedSearchPresenter(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.feed_search_fragment_layout, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        feed_search_component.feedListCallbacks = object : FeedComponent.FeedListCallbacks() {
            override fun isActionModeSupported(): Boolean = false
        }

        feed_search_component.noteReferenceCallbacks = object : NoteReferenceFeedItemComponent.Callbacks {
            override fun onNoteItemClicked(note: NoteReference) {
                noteReferenceCallbacks?.onNoteItemClicked(note)
                // telemetry
            }
            override fun onNoteItemLongPress(note: NoteReference, view: View) {}
        }

        feed_search_component.stickyNoteCallbacks = object : NoteItemComponent.Callbacks() {
            override fun onNoteItemClicked(note: Note) {
                NotesLibrary.getInstance().sendEditFeedSearchNoteAction(note)
                // telemetry
            }

            override fun onNoteItemLongPress(note: Note, view: View) {}
        }

        feed_search_component.samsungNoteCallbacks = object : SamsungNoteFeedItemComponent.Callbacks {
            override fun onNoteItemClicked(note: Note) {
                NotesLibrary.getInstance().sendEditFeedSearchNoteAction(note)
                // telemetry
            }

            override fun onNoteItemLongPress(note: Note, view: View) {}
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
        presenter.runSearch()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    // function to call to start the search
    fun setSearchText(newText: String) {
        presenter.query = newText
    }

    // ---- FragmentApi ----//
    override fun updateNotesCollection(notesCollection: List<FeedItem>) {
        feed_search_component.updateFeedItems(notesCollection, presenter.query.parseSearchQuery())
    }
}
