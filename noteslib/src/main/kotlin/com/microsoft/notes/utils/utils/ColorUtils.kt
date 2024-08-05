package com.microsoft.notes.utils.utils

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import androidx.annotation.RequiresApi
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R

@RequiresApi(Build.VERSION_CODES.N)
fun getFeedSortingChipOptionColorStateList(context: Context): ColorStateList {
    val states = arrayOf(
        intArrayOf(android.R.attr.state_selected),
        intArrayOf(-android.R.attr.state_selected)
    )
    val colors = intArrayOf(
        context.getColor(NotesLibrary.getInstance().theme.sortFilterBottomSheetThemeOverride?.selectedOptionChipColor ?: 0),
        context.getColor(R.color.chip_unselected_color)
    )
    return ColorStateList(states, colors)
}

@RequiresApi(Build.VERSION_CODES.N)
fun getFeedSortingTextOptionColorStateList(context: Context): ColorStateList {
    val states = arrayOf(
        intArrayOf(android.R.attr.state_selected),
        intArrayOf(-android.R.attr.state_selected)
    )
    val colors = intArrayOf(
        context.getColor(NotesLibrary.getInstance().theme.primaryAppColor),
        context.getColor(R.color.sort_option_unselected_text_color)
    )
    return ColorStateList(states, colors)
}
