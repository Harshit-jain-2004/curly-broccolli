package com.microsoft.notes.ui.noteslist.recyclerview.noteitem

import androidx.recyclerview.widget.RecyclerView

class NoteViewHolder(noteView: NoteItemComponent) : RecyclerView.ViewHolder(noteView) {
    val noteView
        get() = itemView as NoteItemComponent
}
