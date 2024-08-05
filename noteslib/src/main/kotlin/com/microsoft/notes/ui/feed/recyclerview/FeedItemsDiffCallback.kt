package com.microsoft.notes.ui.feed.recyclerview

import androidx.recyclerview.widget.DiffUtil

class FeedItemsDiffCallback(
    private val oldList: List<FeedItem>,
    private val newList: List<FeedItem>,
    private val oldKeywordsToHighlight: List<String>? = null,
    private val newKeywordsToHighlight: List<String>? = null
) :
    DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        compareKeywords() && oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        when (oldList[oldItemPosition]) {
            is FeedItem.NoteItem -> {
                (newList[newItemPosition] is FeedItem.NoteItem) && (oldList[oldItemPosition] as FeedItem.NoteItem).note == (newList[newItemPosition] as FeedItem.NoteItem).note
            }
            is FeedItem.NoteReferenceItem -> {
                (newList[newItemPosition] is FeedItem.NoteReferenceItem) && (oldList[oldItemPosition] as FeedItem.NoteReferenceItem).noteReference == (newList[newItemPosition] as FeedItem.NoteReferenceItem).noteReference
            }
            is FeedItem.TimeHeaderItem -> {
                (newList[newItemPosition] is FeedItem.TimeHeaderItem) && (oldList[oldItemPosition] as FeedItem.TimeHeaderItem).id == (newList[newItemPosition] as FeedItem.TimeHeaderItem).id
            }
        }

    private fun compareKeywords(): Boolean = newKeywordsToHighlight == oldKeywordsToHighlight // ignoring order of keywords here as it is a rare scenario
}
