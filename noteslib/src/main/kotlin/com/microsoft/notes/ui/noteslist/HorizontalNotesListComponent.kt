package com.microsoft.notes.ui.noteslist

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.noteslist.recyclerview.HorizontalNotesListAdapter
import com.microsoft.notes.ui.noteslist.recyclerview.NotesListAdapter

class HorizontalNotesListComponent(context: Context, attributeSet: AttributeSet?) :
    NotesListComponent(context, attributeSet) {
    constructor(context: Context) : this(context, null)
    init {
        LayoutInflater.from(context).inflate(R.layout.sn_horizontal_notes_list_component_layout, this)
        setupAdapter()
    }

    override fun createNotesListAdapter(): NotesListAdapter {
        return HorizontalNotesListAdapter(
            notes = emptyList(),
            onNoteClick = { callbacks?.onNoteClicked(it) },
            onNoteLongPress = { note, view -> callbacks?.onNoteLongPress(note, view) }
        )
    }

    override fun createLayoutManager(): LinearLayoutManager =
        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
}
