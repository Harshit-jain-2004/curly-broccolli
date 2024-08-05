package com.microsoft.notes.ui.search

import android.os.Handler
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.ui.extensions.filterDeletedAndFutureNotes
import com.microsoft.notes.ui.extensions.filterDeletedNoteReferences
import com.microsoft.notes.ui.extensions.search
import com.microsoft.notes.ui.feed.recyclerview.FeedItem
import com.microsoft.notes.ui.feed.recyclerview.collateNotes
import com.microsoft.notes.ui.shared.StickyNotesPresenter
import com.microsoft.notes.utils.logging.EventMarkers

class FeedSearchPresenter(private val fragmentApi: FeedSearchFragmentApi) :
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

    // presenter functions
    fun runSearch() {
        when {
            query.isEmpty() -> updateNotesList(emptyList())
            else -> {
                Handler().post {
                    val stickyNotes = NotesLibrary.getInstance().currentNotes
                        .filterDeletedAndFutureNotes()
                        .search(query, null)

                    var samsungNotesList: List<Note> = emptyList()
                    if (NotesLibrary.getInstance().experimentFeatureFlags.samsungNotesSyncEnabled) {
                        samsungNotesList = NotesLibrary.getInstance().currentSamsungNotes
                            .filterDeletedAndFutureNotes().search(this.query, null)
                    }

                    val noteReferenceList: List<NoteReference> = NotesLibrary.getInstance().currentNoteReferences.filterDeletedNoteReferences().search(this.query)
                    updateNotesList(collateNotes(stickyNotes = stickyNotes, samsungNotes = samsungNotesList, noteReferences = noteReferenceList))
                }
            }
        }
    }

    private fun updateNotesList(notes: List<FeedItem>) {
        runIfActivityIsRunning {
            runOnClientThread {
                fragmentApi.updateNotesCollection(notes)
            }
        }
    }

    private fun isNewQuery(previousQuery: String, newQuery: String): Boolean =
        newQuery.isNotEmpty() && previousQuery.isEmpty() && newQuery == previousQuery

    // UI bindings
    override fun addUiBindings() {
        // no bindings at the moment
    }

    override fun removeUiBindings() {
        // no bindings at the moment
    }
}

interface FeedSearchFragmentApi {
    fun updateNotesCollection(notesCollection: List<FeedItem>)
}
