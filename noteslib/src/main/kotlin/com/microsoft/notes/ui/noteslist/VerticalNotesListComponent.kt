package com.microsoft.notes.ui.noteslist

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.noteslist.recyclerview.NotesListAdapter
import com.microsoft.notes.ui.noteslist.recyclerview.VerticalNotesListAdapter
import kotlinx.android.synthetic.main.sn_vertical_notes_list_component_layout.view.*

class VerticalNotesListComponent(context: Context, attributeSet: AttributeSet?) :
    NotesListComponent(context, attributeSet) {

    var swipeToRefreshEnabled: Boolean
        get() {
            return notesSwipeToRefreshView.isEnabled
        }
        set(value) {
            if (value != notesSwipeToRefreshView.isEnabled) {
                notesSwipeToRefreshView.isEnabled = value
            }
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.sn_vertical_notes_list_component_layout, this)
        setupAdapter()
        setupSwipeToRefresh()
    }

    fun isRefreshing() = notesSwipeToRefreshView.isRefreshing

    fun stopRefreshAnimation() {
        notesSwipeToRefreshView.isRefreshing = false
    }

    private fun setupSwipeToRefresh() {
        notesSwipeToRefreshView.setOnRefreshListener {
            callbacks?.onSwipeToRefresh()
        }
    }

    override fun createNotesListAdapter(): NotesListAdapter {
        return VerticalNotesListAdapter(
            notes = emptyList(),
            getExpandedPosition = { expandedPositionForReturnTransition.value },
            onNoteClick = { callbacks?.onNoteClicked(it) },
            onNoteLongPress = { note, view -> callbacks?.onNoteLongPress(note, view) }
        )
    }

    override fun createLayoutManager(): LinearLayoutManager = LinearLayoutManager(context)
}
