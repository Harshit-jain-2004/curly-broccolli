package com.microsoft.notes.ui.feed.sourcefilter

import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.ui.shared.StickyNotesPresenter

class FeedSourceFilterPresenter(private val fragmentApi: FragmentApi) :
    StickyNotesPresenter() {

    fun updateSourceFilter(source: FeedSourceFilterOption) {
        NotesLibrary.getInstance().sendFeedSourceFilterSelectedAction(source)
    }

    // UI bindings
    override fun addUiBindings() {
    }

    override fun removeUiBindings() {
    }
}
