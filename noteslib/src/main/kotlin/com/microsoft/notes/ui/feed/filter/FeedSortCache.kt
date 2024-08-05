package com.microsoft.notes.ui.feed.filter

import android.content.Context
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.SortingProperty.PREVIOUS_SORTING_CRITERION
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.SortingProperty.PREVIOUS_SORTING_STATE
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.SortingProperty.SELECTED_SORTING_CRITERION
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.SortingProperty.SELECTED_SORTING_STATE
import com.microsoft.notes.utils.utils.Constants

object FeedSortCache {
    private var locallyCachedPreferredSortSelection = Pair(SortingCriterion.DATE_MODIFIED, SortingState.ENABLED_DESCENDING)
        set(value) {
            if (field != value) {
                if ((field.first != value.first) &&
                    (
                        field.first == SortingCriterion.DATE_CREATED ||
                            value.first == SortingCriterion.DATE_CREATED
                        )
                ) {
                    dateCriterionSwitched = true
                }
                NotesLibrary.getInstance().recordTelemetry(
                    EventMarkers.SortSelectionUpdated,
                    Pair(PREVIOUS_SORTING_CRITERION, field.first.toString()),
                    Pair(SELECTED_SORTING_CRITERION, value.first.toString()),
                    Pair(PREVIOUS_SORTING_STATE, field.second.toString()),
                    Pair(SELECTED_SORTING_STATE, value.second.toString())
                )
                field = value
            }
        }
    var dateCriterionSwitched = false

    fun resetCachedPreferredSortSelection(context: Context) {
        locallyCachedPreferredSortSelection = Pair(SortingCriterion.DATE_MODIFIED, SortingState.ENABLED_DESCENDING)
        updatePreferredSortSelection(context, locallyCachedPreferredSortSelection)
    }

    fun fetchPreferredSortSelection(context: Context?): Pair<SortingCriterion, SortingState> {
        context?.let {
            val sharedPrefs = context.getSharedPreferences(Constants.PREFERRED_SORT_SELECTION, Context.MODE_PRIVATE)
            val preferredSortingCriterionOrdinal = sharedPrefs.getInt(
                Constants.PREFERRED_SORT_CRITERION_KEY,
                locallyCachedPreferredSortSelection.first.ordinal
            )
            val preferredSortingStateOrdinal = sharedPrefs.getInt(
                Constants.PREFERRED_SORT_STATE_KEY,
                locallyCachedPreferredSortSelection.second.ordinal
            )

            locallyCachedPreferredSortSelection = Pair(
                SortingCriterion.values().get(preferredSortingCriterionOrdinal),
                SortingState.values().get(preferredSortingStateOrdinal)
            )
        }
        return locallyCachedPreferredSortSelection
    }

    fun updatePreferredSortSelection(context: Context?, selectedSort: Pair<SortingCriterion, SortingState>) {
        context?.let {
            val sharedPrefs = context.getSharedPreferences(Constants.PREFERRED_SORT_SELECTION, Context.MODE_PRIVATE)
            var writeSucceed = sharedPrefs?.edit()?.putInt(Constants.PREFERRED_SORT_CRITERION_KEY, selectedSort.first.ordinal)?.commit()
                ?: false
            if (writeSucceed)
                writeSucceed = sharedPrefs?.edit()?.putInt(Constants.PREFERRED_SORT_STATE_KEY, selectedSort.second.ordinal)?.commit()
                    ?: false
            if (writeSucceed)
                locallyCachedPreferredSortSelection = selectedSort
        }
    }
}
