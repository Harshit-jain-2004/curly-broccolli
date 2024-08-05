package com.microsoft.notes.ui.noteslist.recyclerview

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.microsoft.notes.models.Note
import com.microsoft.notes.ui.extensions.getStableId
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteViewHolder

abstract class NotesListAdapter(
    protected var notes: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongPress: (Note, View) -> Unit
) :
    RecyclerView.Adapter<NoteViewHolder>() {

    internal var keywordsToHighlight: List<String>? = null

    protected val itemCallbacks = object : NoteItemComponent.Callbacks() {
        override fun onNoteItemClicked(note: Note) {
            onNoteClick(note)
        }

        override fun onNoteItemLongPress(note: Note, view: View) {
            onNoteLongPress(note, view)
        }
    }

    init {
        setHasStableIds(true)
    }

    enum class NoteItemType(val id: Int) {
        TEXT(0),
        SINGLE_IMAGE(1),
        TWO_IMAGE(2),
        THREE_IMAGE(3),
        MULTI_IMAGE(4),
        INK(5)
    }

    override fun getItemViewType(position: Int): Int {
        with(notes[position]) {
            return when {
                this.isInkNote -> NoteItemType.INK.id
                isMediaListEmpty -> NoteItemType.TEXT.id
                this.mediaCount == 1 -> NoteItemType.SINGLE_IMAGE.id
                this.mediaCount == 2 -> NoteItemType.TWO_IMAGE.id
                this.mediaCount == 3 -> NoteItemType.THREE_IMAGE.id
                else -> NoteItemType.MULTI_IMAGE.id
            }
        }
    }

    override fun getItemCount(): Int = notes.size

    fun getItem(position: Int): Note = notes[position]

    fun getItemPosition(note: Note): Int = notes.indexOf(note)

    override fun getItemId(position: Int): Long =
        getItem(position).getStableId()

    fun updateNotesCollection(notesCollection: List<Note>, keywordsToHighlight: List<String>?) {
        val oldNotes = notes
        notes = notesCollection
        val oldKeywordsToHighlight = this.keywordsToHighlight
        this.keywordsToHighlight = keywordsToHighlight
        applyDiff(oldNotes, notes, oldKeywordsToHighlight, keywordsToHighlight)
    }

    private fun applyDiff(
        oldNotes: List<Note>,
        newNotesCollection: List<Note>,
        oldKeywordsToHighlight: List<String>?,
        newKeywordsToHighlight: List<String>?
    ) {
        val diffResult = DiffUtil.calculateDiff(
            NotesDiffCallback(
                oldNotes, newNotesCollection,
                oldKeywordsToHighlight, newKeywordsToHighlight
            )
        )
        diffResult.dispatchUpdatesTo(this)
    }
}
