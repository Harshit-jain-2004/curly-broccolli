package com.microsoft.notes.ui.feed.filter

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.extensions.hide
import com.microsoft.notes.ui.extensions.show
import com.microsoft.notes.utils.utils.getFeedSortingTextOptionColorStateList
import kotlinx.android.synthetic.main.feed_sorting_criterion_layout.view.*

class FeedSortingCriterionView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    // Logic : There is an indicator next to each sort option which tells the user if the sort option
    //          has been selected, and if so whether the sorting is in ascending order or descending order.
    // STATE                            INDICATOR
    // Option not selected              Hidden
    // Option selected; ascending       Upward arrow
    // Option selected; descending      Downward arrow
    var sortingCriterion: SortingCriterion = SortingCriterion.DATE_MODIFIED
    var sortingState: SortingState = SortingState.DISABLED
        set(value) {
            field = value
            when (value) {
                SortingState.DISABLED -> {
                    sort_state_indicator.hide()
                    sort_criterion_title.isSelected = false
                }
                SortingState.ENABLED_ASCENDING, SortingState.ENABLED_DESCENDING -> {
                    sort_state_indicator.setImageResource(getImageResourceForSortingState(value))
                    sort_state_indicator.show()
                    sort_criterion_title.isSelected = true
                }
            }
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.feed_sorting_criterion_layout, this)
        sortingState = SortingState.DISABLED
        sortingCriterion = SortingCriterion.DATE_MODIFIED
    }

    fun setSortCriterionTitle(title: String) {
        sort_criterion_title.text = title
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && context != null) {
            sort_criterion_title.setTextColor(getFeedSortingTextOptionColorStateList(context))
        }
    }

    private fun getImageResourceForSortingState(sortingState: SortingState): Int {
        val sortArrowUp = NotesLibrary.getInstance().theme.sortFilterBottomSheetThemeOverride?.sortArrowUpDrawable ?: -1
        val sortArrowDown = NotesLibrary.getInstance().theme.sortFilterBottomSheetThemeOverride?.sortArrowDownDrawable ?: -1
        return when (sortingState) {
            SortingState.ENABLED_ASCENDING -> if (sortingCriterion == SortingCriterion.TITLE) sortArrowDown else sortArrowUp
            SortingState.ENABLED_DESCENDING -> if (sortingCriterion == SortingCriterion.TITLE) sortArrowUp else sortArrowDown
            else -> -1
        }
    }
}

enum class SortingState {
    ENABLED_ASCENDING,
    ENABLED_DESCENDING,
    DISABLED;

    fun getContentDescriptionResource(): Int = when (this) {
        ENABLED_ASCENDING -> R.string.sort_applied_ascending_description
        ENABLED_DESCENDING -> R.string.sort_applied_descending_description
        DISABLED -> R.string.sort_disabled_description
    }
}
