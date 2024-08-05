package com.microsoft.notes.ui.noteslist.recyclerview

import androidx.recyclerview.widget.DiffUtil
import com.microsoft.notes.models.Note

class NotesDiffCallback(
    private val oldList: List<Note>,
    private val newList: List<Note>,
    private val oldKeywordsToHighlight: List<String>?,
    private val newKeywordsToHighlight: List<String>?
) :
    DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldKeywordsToHighlight == newKeywordsToHighlight &&
            oldList[oldItemPosition].localId == newList[newItemPosition].localId
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}
