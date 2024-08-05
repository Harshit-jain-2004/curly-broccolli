package com.microsoft.notes.ui.feed.filter

class FeedFilters {

    var selectedNotesTypeFilters: HashMap<FeedNotesTypeFilterOption, Boolean> = hashMapOf()
    var selectedUserIDs: HashMap<String, Boolean> = hashMapOf()

    init {
        FeedNotesTypeFilterOption.values().forEach {
            selectedNotesTypeFilters.put(it, false)
        }
    }

    fun isAnyAccountFilterSelected(): Boolean {
        selectedUserIDs.forEach {
            if (it.value) {
                return true
            }
        }
        return false
    }

    fun isAnyNotesTypeFilterSelected(): Boolean {
        selectedNotesTypeFilters.forEach {
            if (it.value && (it.key != FeedNotesTypeFilterOption.ALL)) {
                return true
            }
        }
        return false
    }
}

enum class FeedNotesTypeFilterOption {
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
