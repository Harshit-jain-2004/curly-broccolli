package com.microsoft.notes.sampleapp.noteslist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.sampleapp.R
import com.microsoft.notes.sideeffect.ui.NoteChanges
import com.microsoft.notes.store.combinedNotesForAllUsers
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.store.State
import com.microsoft.notes.ui.extensions.filterDeletedAndFutureNotes
import com.microsoft.notes.ui.noteslist.NotesListComponent
import com.microsoft.notes.ui.noteslist.ScrollTo
import kotlinx.android.synthetic.main.horizontal_notes_list_layout.*

/**
 * A fragment to test out horizontal notes list
 * Here we will consume the HorizontalNotesListComponent from SDK.
 * This can be used as an example how an app can consume the list from SDK independently and include it in their
 * own fragment
 */
class HorizontalNotesListFragment: Fragment(), NoteChanges {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.horizontal_notes_list_layout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupNotesList()
    }

    private fun setupNotesList() {
        notesList.callbacks = NotesListComponent.DefaultNotesListComponentCallbacks
    }

    override fun onStart() {
        super.onStart()
        NotesLibrary.getInstance().addUiBindings(this)
    }

    override fun onStop() {
        super.onStop()
        NotesLibrary.getInstance().removeUiBindings(this)
    }

    override fun onResume() {
        super.onResume()
        val filteredNotesCollection = NotesLibrary.getInstance().currentNotes.filterDeletedAndFutureNotes()
        notesList?.updateNotesCollection(filteredNotesCollection, ScrollTo.NoScroll)
    }

    /** Note changes **/
    override fun noteDeleted() { }

    override fun notesUpdated(stickyNotesCollectionsByUser: HashMap<String, List<Note>>, notesLoaded:Boolean) {
        val stickyNotesCollection: MutableList<Note> = mutableListOf()
        stickyNotesCollectionsByUser.forEach {
            stickyNotesCollection.addAll(it.value)
        }
        activity?.runOnUiThread {
            notesList.updateNotesCollection(stickyNotesCollection.filterDeletedAndFutureNotes(), ScrollTo.NoScroll)
        }
    }
}