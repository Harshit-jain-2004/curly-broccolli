package com.microsoft.notes.ui.noteslist

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableString
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.scheme.DocumentType
import com.microsoft.notes.ui.extensions.getHasImagesTelemetryValue
import com.microsoft.notes.ui.extensions.getStableId
import com.microsoft.notes.ui.noteslist.recyclerview.NotesListAdapter
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteViewHolder
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.utils.logging.SNMarker
import com.microsoft.notes.utils.logging.SNMarkerConstants
import kotlinx.android.synthetic.main.sn_notes_list_component_layout.view.*

abstract class NotesListComponent(context: Context, attributeSet: AttributeSet?) : FrameLayout(
    context,
    attributeSet
) {
    companion object {
        const val EXPAND_FROM_TOP = -1
    }

    var notesCollection: List<Note> = emptyList()
        private set

    lateinit var notesAdapter: NotesListAdapter
    var callbacks: Callbacks? = null

    val recyclerViewID: Int
        get() = R.id.notesRecyclerView

    private var savedState: Parcelable? = null

    // This value is used by the NotesListAdapter to set default names on the item views when we are returning
    // to the notes list from the details view, so that way the appropriate views are either part of the shared
    // element default. We lazily compute its value whenever we are starting this default, and otherwise its
    // value is meaningless.
    protected var expandedPositionForReturnTransition: Lazy<Int> = lazy { findExpandedPositionFromNotesList() }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())
        bundle.putParcelable("state", notesRecyclerView.layoutManager?.onSaveInstanceState())
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var superState: Parcelable? = null
        state?.let {
            if (it is Bundle) {
                savedState = it.getParcelable("state")
                superState = it.getParcelable("superState")
            }
        }

        superState?.let {
            super.onRestoreInstanceState(it)
            return
        }

        super.onRestoreInstanceState(state)
    }

    fun setupAdapter() {
        notesAdapter = createNotesListAdapter()
        val layoutManager = createLayoutManager()
        notesRecyclerView.layoutManager = layoutManager
        notesRecyclerView.adapter = notesAdapter
    }

    fun findNoteView(note: Note): NoteViewHolder? =
        notesRecyclerView.findViewHolderForItemId(note.getStableId()) as NoteViewHolder?

    fun prepareForNewNoteTransition() {
        setTransitionSlideDirections(EXPAND_FROM_TOP)
    }

    fun prepareForEditNoteTransition(note: Note, txn: FragmentTransaction): NoteItemComponent? {
        val vh = notesRecyclerView.findViewHolderForItemId(note.getStableId()) as NoteViewHolder? ?: return null

        setTransitionSlideDirections(vh.layoutPosition)
        vh.noteView.prepareSharedElements { view, name ->
            view.transitionName = name
            txn.addSharedElement(view, name)
        }
        return vh.noteView
    }

    fun prepareForReturnTransition() {
        expandedPositionForReturnTransition = lazy { findExpandedPositionFromNotesList() }
    }

    private fun setTransitionSlideDirections(position: Int) {
        for (i: Int in 0..notesAdapter.itemCount) {
            val viewHolder = notesRecyclerView.findViewHolderForLayoutPosition(i) as NoteViewHolder?
            viewHolder?.noteView?.let {
                when {
                    i < position -> it.setRootTransitionName("up")
                    i > position -> it.setRootTransitionName("down")
                }
            }
        }
    }

    private fun findExpandedPositionFromNotesList(): Int {
        val currentNote = callbacks?.noteForReturnTransition ?: return EXPAND_FROM_TOP
        return if (currentNote.isEmpty) {
            EXPAND_FROM_TOP
        } else {
            val currentNoteId = currentNote.localId
            notesCollection.indexOfFirst { it.localId == currentNoteId }
        }
    }

    private fun filterRenderedInkNotes(notesCollection: List<Note>): List<Note> {
        val filteredNotes: ArrayList<Note> = ArrayList()
        for (note in notesCollection) {
            if (note.document.type != DocumentType.RENDERED_INK) {
                filteredNotes.add(note)
            }
        }
        return filteredNotes
    }

    fun updateNotesCollection(notesCollection: List<Note>, scrollTo: ScrollTo, keywordsToHighlight: List<String>? = null) {

        if (NotesLibrary.getInstance().uiOptionFlags.hideRenderedInkNotesWhenInkEnabled &&
            NotesLibrary.getInstance().experimentFeatureFlags.inkEnabled
        ) {
            this.notesCollection = filterRenderedInkNotes(notesCollection)
        } else {
            this.notesCollection = notesCollection
        }

        notesAdapter.updateNotesCollection(this.notesCollection, keywordsToHighlight)
        when (scrollTo) {
            is ScrollTo.Bottom -> scrollToPosition(position = notesCollection.size - 1, offset = 20)
            is ScrollTo.Top -> scrollToPosition(position = 0)
            is ScrollTo.Custom -> scrollToPosition(position = scrollTo.position)
            is ScrollTo.NoScroll -> Unit
        }

        savedState?.let {
            notesRecyclerView.layoutManager?.onRestoreInstanceState(savedState)
            savedState = null
        }

        if (notesCollection.isEmpty()) {
            notesListPlaceholder.showPlaceholder()
        } else {
            notesListPlaceholder.visibility = View.GONE
            SNMarker.logMarker(SNMarkerConstants.NotesFetchUIEnd)
        }
    }

    fun hidePlaceholder() {
        notesListPlaceholder.visibility = View.GONE
    }

    fun scrollToPosition(position: Int, offset: Int = Int.MIN_VALUE) {
        (notesRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, offset)
    }

    fun ensureCurrentEditNoteVisibility(note: Note?) {
        val position = if (note != null) {
            notesAdapter.getItemPosition(note)
        } else {
            Int.MIN_VALUE
        }

        val firstVisiblePosition = (notesRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val lastVisiblePosition = (notesRecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        if (position >= 0 && (position < firstVisiblePosition || position > lastVisiblePosition)) {
            scrollToPosition(position)
        }
    }

    fun findRecyclerView(): RecyclerView = notesRecyclerView

    fun setBottomPadding(bottomPadding: Int) {
        findRecyclerView().apply {
            setPaddingRelative(
                paddingStart, paddingTop,
                paddingEnd, bottomPadding
            )
        }
    }

    /**
     * Sets the placeholder for an empty state.
     * All values are optional, UI will adjust accordingly to only show the elements provided.
     * If image is used, image content description should be set.
     * Content descriptions for title and subtitle are not mandatory, but can be provided if needed.
     */
    fun setPlaceholder(
        image: Int? = null,
        imageContentDescription: String? = null,
        title: SpannableString? = null,
        titleContentDescription: String? = null,
        titleStyle: Int? = null,
        subtitle: SpannableString? = null,
        subtitleContentDescription: String? = null,
        subtitleStyle: Int? = null
    ) {
        notesListPlaceholder.setPlaceholder(
            image, imageContentDescription,
            title, titleContentDescription, titleStyle,
            subtitle, subtitleContentDescription, subtitleStyle
        )
        updateNotesCollection(notesCollection, ScrollTo.NoScroll, notesAdapter.keywordsToHighlight)
    }

    abstract fun createNotesListAdapter(): NotesListAdapter
    abstract fun createLayoutManager(): LinearLayoutManager

    open class Callbacks {
        open val noteForReturnTransition: Note? get() = null
        open fun onNoteClicked(note: Note) {}
        open fun onNoteLongPress(note: Note, view: View) {}
        open fun onSwipeToRefresh() {}
        open fun onNoteOrganise(note: Note) {}
    }

    /**
     * TODO This is not a good way, think more on this and discuss with others
     * Option 1: Make sendEditNotesListComponentNoteAction() in NotesLibrary public and expect clients to call it and
     * record telemetry as well?
     * Option 2: Unify the EditNote, EditSearchNote and EditNotesListComponentNote actions into single one and
     * ListComponent dispatches them internally and also records telemetry.
     * Option 3: Handling clicks is Client's responsibility. We only record telemetry.
     *
     */
    object DefaultNotesListComponentCallbacks : NotesListComponent.Callbacks() {
        override fun onNoteClicked(note: Note) {
            NotesLibrary.getInstance().sendEditNoteAction(note)
            NotesLibrary.getInstance().recordTelemetry(
                EventMarkers.NoteViewed,
                Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES, note.getHasImagesTelemetryValue()),
                Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_LOCAL_ID, note.localId)
            )
        }

        override fun onNoteOrganise(note: Note) {
            NotesLibrary.getInstance().sendFeedNoteOrganiseAction(note)
        }
    }
}
