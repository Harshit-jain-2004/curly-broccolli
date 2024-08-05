package com.microsoft.notes.ui.feed.filter

import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.ui.shared.StickyNotesPresenter

class FeedComprehensiveFilterPresenter(private val fragmentApi: FragmentApi) :
    StickyNotesPresenter() {

    fun updateComprehensiveFilter(
        feedFilters: FeedFilters,
        scrollToTop: Boolean
    ) {
        NotesLibrary.getInstance().sendComprehensiveFeedFilterSelectedAction(feedFilters, scrollToTop)
    }

    // UI bindings
    override fun addUiBindings() {
    }

    override fun removeUiBindings() {
    }
}
