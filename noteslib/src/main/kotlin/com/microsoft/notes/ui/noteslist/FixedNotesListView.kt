package com.microsoft.notes.ui.noteslist

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.microsoft.notes.models.Note
import com.microsoft.notes.ui.extensions.getLayoutId
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.ui.theme.ThemedLinearLayout

/**
 * View that displays a fixed number of notes. Callers are expected to filter notes as necessary
 * before passing them into this view. This view is not optimized in the same way RecyclerViews are
 * in the way items are rendered, and so it's only recommended you use this class if not implementing
 * scroll functionality.
 */
class FixedNotesListView(context: Context, attrs: AttributeSet?) : ThemedLinearLayout(context, attrs) {
    private var currentNotes: List<Note>? = null

    var callbacks: NotesListComponent.Callbacks? = null

    fun setNotes(notes: List<Note>) {
        if (currentNotes == notes) {
            return
        }
        currentNotes = notes

        removeAllViews()
        for (note: Note in notes) {
            addNoteInList(note)
        }
        requestLayout()
    }

    private fun addNoteInList(note: Note) {
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(note.getLayoutId(), this, false) as NoteItemComponent
        view.callbacks = object : NoteItemComponent.Callbacks() {
            override fun onNoteItemClicked(note: Note) {
                callbacks?.onNoteClicked(note)
            }

            override fun onNoteItemLongPress(note: Note, view: View) {
                callbacks?.onNoteLongPress(note, view)
            }
        }
        view.bindNote(note = note, showDateTime = true)
        addView(view)
    }

    override fun removeAllViews() {
        repeat(childCount) {
            val child = getChildAt(it)
            when (child) {
                is NoteItemComponent -> child.onRemovingFromParent()
            }
        }
        super.removeAllViews()
    }
}
