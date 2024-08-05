package com.microsoft.notes.ui.search

import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.extensions.getHasImagesTelemetryValue
import com.microsoft.notes.ui.extensions.parseSearchQuery
import com.microsoft.notes.ui.noteslist.NotesListComponent
import com.microsoft.notes.ui.noteslist.ScrollTo
import com.microsoft.notes.ui.noteslist.placeholder.NotesListPlaceholderHelper
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteViewHolder
import com.microsoft.notes.ui.shared.StickyNotesFragment
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import kotlinx.android.synthetic.main.sn_search_fragment_layout.*

open class SearchFragment :
    StickyNotesFragment(),
    FragmentApi,
    SearchColorPicker.NoteColorPickerListener {

    init {
        allowEnterTransitionOverlap = true
        allowReturnTransitionOverlap = true
    }

    private val presenter: SearchPresenter by lazy {
        SearchPresenter(this)
    }

    private val placeholderHelper by lazy { NotesListPlaceholderHelper() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.sn_search_fragment_layout, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupColorButtons()
        setupNotesList()
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

        // This is used to prevent empty state flicker when resuming search UI with non-default options preselected
        notesList.hidePlaceholder()
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

    private fun setupNotesList() {
        notesList.callbacks = object : NotesListComponent.Callbacks() {
            override val noteForReturnTransition: Note?
                get() = getCurrentNote()

            override fun onNoteClicked(note: Note) {
                presenter.recordTelemetry(
                    EventMarkers.SearchResultSelected,
                    Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES, note.getHasImagesTelemetryValue())
                )
                presenter.recordTelemetry(
                    EventMarkers.NoteViewed,
                    Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES, note.getHasImagesTelemetryValue()),
                    Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_LOCAL_ID, note.localId)
                )
                NotesLibrary.getInstance().sendEditSearchNoteAction(note)
            }
        }
        placeholderHelper.setNotesList(notesList)
    }

    private fun setupColorButtons() {
        colorPicker.setListener(this)
        colorPicker.clearColorSelection()
    }

    override fun onColorSelected(color: Color?) {
        presenter.colorFilter = color
    }

    private fun updateNotesListAndColorButtonsVisibility() {
        if ((presenter.query.isNotEmpty() || presenter.colorFilter != null) && notesList == null) {
            setupNotesList()
        }
    }

    fun setSearchText(newText: String) {
        presenter.query = newText
    }

    fun clearColorPickerSelection() {
        colorPicker.clearColorSelection()
    }

    /**
     * See NotesListComponent::setPlaceholder for documentation
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
        placeholderHelper.setPlaceholder(
            image, imageContentDescription,
            title, titleContentDescription, titleStyle,
            subtitle, subtitleContentDescription, subtitleStyle
        )
    }

    // ---- Transitions ----//
    fun findCurrentNoteView(): NoteViewHolder? = getCurrentNote()?.let { notesList.findNoteView(it) }

    fun findNotesList(): NotesListComponent? = notesList

    // ---- FragmentApi ----//
    override fun updateNotesCollection(notesCollection: List<Note>, scrollTo: ScrollTo, notesLoaded: Boolean) {
        updateNotesListAndColorButtonsVisibility()
        notesList?.updateNotesCollection(notesCollection, scrollTo, presenter.query.parseSearchQuery())
    }
}
