package com.microsoft.notes.sampleapp.com.microsoft.notes.sampleapp.noteslist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.sampleapp.R
import com.microsoft.notes.sideeffect.ui.NoteChanges
import com.microsoft.notes.ui.extensions.filterDeletedAndFutureNotes
import com.microsoft.notes.ui.noteslist.NotesListComponent
import kotlinx.android.synthetic.main.fixed_notes_list_layout.*

class FixedNotesListFragment : Fragment(), NoteChanges {
    companion object {
        private const val MAX_NOTES = 3
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fixed_notes_list_layout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupNotesList()
    }

    private fun setupNotesList() {
        fixedNotesList.callbacks = NotesListComponent.DefaultNotesListComponentCallbacks
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
        val filteredNotesCollection = NotesLibrary.getInstance().currentNotes
                .filterDeletedAndFutureNotes().take(MAX_NOTES)
        fixedNotesList?.setNotes(filteredNotesCollection)
    }

    /** Note changes **/
    override fun noteDeleted() { }

    override fun notesUpdated(stickyNotesCollectionsByUser: HashMap<String, List<Note>>, notesLoaded:Boolean) {
        val stickyNotesCollection: MutableList<Note> = mutableListOf()
        stickyNotesCollectionsByUser.forEach {
            stickyNotesCollection.addAll(it.value)
        }
        activity?.runOnUiThread {
            fixedNotesList?.setNotes(stickyNotesCollection.filterDeletedAndFutureNotes().take(MAX_NOTES))
        }
    }
}