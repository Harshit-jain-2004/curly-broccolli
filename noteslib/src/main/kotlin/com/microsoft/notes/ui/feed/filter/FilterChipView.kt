package com.microsoft.notes.ui.feed.filter

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.chip.Chip
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.utils.accessibility.changeActionOfView
import com.microsoft.notes.utils.utils.getFeedSortingChipOptionColorStateList
import com.microsoft.notes.utils.utils.getFeedSortingTextOptionColorStateList

object FilterChipView {
    fun createFilterChip(
        context: Context?,
        label: String,
        isClickable: Boolean = true,
        isChecked: Boolean = false,
        isCloseIconVisible: Boolean = false,
        onCloseListener: () -> Unit = {}
    ): Chip? {
        context?.let {
            val individualFilterView: View = LayoutInflater.from(context).inflate(R.layout.sn_feed_filter_chip, null, false)
            val individualFilterChip: Chip = individualFilterView.findViewById(R.id.individual_filter_chip)

            individualFilterChip.text = label
            individualFilterChip.isClickable = isClickable
            individualFilterChip.isChecked = isChecked
            individualFilterChip.isCloseIconVisible = isCloseIconVisible
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                individualFilterChip.chipBackgroundColor = getFeedSortingChipOptionColorStateList(context)
                individualFilterChip.setTextColor(getFeedSortingTextOptionColorStateList(context))
            }
            individualFilterChip.setOnCloseIconClickListener {
                onCloseListener()
            }
            return individualFilterChip
        }
        return null
    }

    fun createSelectedFilterView(
        context: Context?,
        label: String,
        onCloseListener: () -> Unit = {}
    ): View? {
        context?.let {
            val selectedFilter: View = LayoutInflater.from(context).inflate(R.layout.selected_filter_view, null, false)
            val selectedFilterTextView: TextView = selectedFilter.findViewById(R.id.selected_filter_title)
            val selectedFilterCloseView: ImageView = selectedFilter.findViewById(R.id.selected_filter_close_option)

            selectedFilterTextView.text = label
            selectedFilterCloseView.setOnClickListener {
                onCloseListener()
            }
            selectedFilter.contentDescription = it.resources.getString(R.string.filter_chip_content_description, label)
            changeActionOfView(selectedFilter, it.resources.getString(R.string.action_dismiss))
            selectedFilter.setOnClickListener {
                val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                if (accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled)
                    onCloseListener()
            }
            return selectedFilter
        }
        return null
    }
}
