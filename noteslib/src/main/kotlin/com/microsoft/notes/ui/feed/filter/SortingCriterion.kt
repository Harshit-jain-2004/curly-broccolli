package com.microsoft.notes.ui.feed.filter

import com.microsoft.notes.noteslib.R

enum class SortingCriterion {
    DATE_MODIFIED,
    DATE_CREATED,
    TITLE;

    fun getLabelResource(): Int {
        return when (this) {
            DATE_MODIFIED -> R.string.sort_by_modification_date_title
            DATE_CREATED -> R.string.sort_by_created_date_option
            TITLE -> R.string.sort_alphabetically_title
        }
    }
}
