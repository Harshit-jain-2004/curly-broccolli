package com.microsoft.notes.ui.search

import android.os.Handler
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.ui.extensions.filterDeletedAndFutureNotes
import com.microsoft.notes.ui.extensions.search
import com.microsoft.notes.ui.noteslist.ScrollTo
import com.microsoft.notes.ui.shared.StickyNotesPresenter
import com.microsoft.notes.utils.logging.EventMarkers

class SearchPresenter(private val fragmentApi: FragmentApi) :
    StickyNotesPresenter() {

    var query: String = ""
        set(value) {
            val previousQuery = field
            field = value
            runSearch()
            if (isNewQuery(previousQuery, value)) {
                recordTelemetry(EventMarkers.SearchInitiated)
            }
        }

    var colorFilter: Color? = null
        set(value) {
            field = value
            runSearch()
        }

    // presenter functions
    fun runSearch() {
        when {
            query.isEmpty() && colorFilter == null -> updateNotesList(emptyList())
            else -> {
                Handler().post {
                    val notes =
                        NotesLibrary.getInstance().currentNotes
                            .filterDeletedAndFutureNotes()
                            .search(query, colorFilter)
                    updateNotesList(notes)
                }
            }
        }
    }

    private fun updateNotesList(notes: List<Note>) {
        runIfActivityIsRunning {
            runOnClientThread {
                fragmentApi.updateNotesCollection(notes, ScrollTo.Top, true)
            }
        }
    }

    private fun isNewQuery(previousQuery: String, newQuery: String): Boolean =
        newQuery.isNotEmpty() && previousQuery.isEmpty()

    // UI bindings
    override fun addUiBindings() {
        // no bindings at the moment
    }

    override fun removeUiBindings() {
        // no bindings at the moment
    }
}

interface FragmentApi {
    fun updateNotesCollection(notesCollection: List<Note>, scrollTo: ScrollTo, notesLoaded: Boolean)
}
