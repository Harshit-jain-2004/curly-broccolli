package com.microsoft.notes.ui.feed.sourcefilter

enum class FeedSourceFilterOption {
    ALL,
    STICKY_NOTES,
    ONENOTE_PAGES,
    SAMSUNG_NOTES
    ;

    override fun toString(): String = when (this) {
        ALL -> "All"
        STICKY_NOTES -> "Sticky notes"
        ONENOTE_PAGES -> "OneNote pages"
        SAMSUNG_NOTES -> "Samsung Notes"
    }
}
