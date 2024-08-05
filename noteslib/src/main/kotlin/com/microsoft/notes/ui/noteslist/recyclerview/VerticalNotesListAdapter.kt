package com.microsoft.notes.ui.noteslist.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteViewHolder
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.WrongViewHolderTypeException

class VerticalNotesListAdapter(
    notes: List<Note>,
    private val getExpandedPosition: () -> Int,
    onNoteClick: (Note) -> Unit,
    onNoteLongPress: (Note, View) -> Unit
) : NotesListAdapter(notes, onNoteClick, onNoteLongPress) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val layoutId = when (viewType) {
            NoteItemType.TEXT.id -> R.layout.sn_note_item_layout_text
            NoteItemType.SINGLE_IMAGE.id -> R.layout.sn_note_item_layout_single_image
            NoteItemType.TWO_IMAGE.id -> R.layout.sn_note_item_layout_two_image
            NoteItemType.THREE_IMAGE.id -> R.layout.sn_note_item_layout_three_image
            NoteItemType.MULTI_IMAGE.id -> R.layout.sn_note_item_layout_multi_image
            NoteItemType.INK.id -> R.layout.sn_note_item_layout_ink
            else -> throw WrongViewHolderTypeException()
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false) as NoteItemComponent
        view.callbacks = itemCallbacks
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holderNote: NoteViewHolder, position: Int) {
        with(holderNote.noteView) {
            bindNote(note = notes[position], keywordsToHighlight = keywordsToHighlight, showDateTime = true)
            when {
                position == getExpandedPosition() -> prepareSharedElements(ViewCompat::setTransitionName)
                position < getExpandedPosition() -> setRootTransitionName("up")
                position > getExpandedPosition() -> setRootTransitionName("down")
            }
        }
    }
}
