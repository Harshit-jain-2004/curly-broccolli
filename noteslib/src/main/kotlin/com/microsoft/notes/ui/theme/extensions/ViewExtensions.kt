package com.microsoft.notes.ui.theme.extensions

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.microsoft.notes.noteslib.NotesThemeOverride
import com.microsoft.notes.richtext.editor.styled.NoteStyledView
import com.microsoft.notes.richtext.editor.styled.SamsungNoteStyledView
import com.microsoft.notes.ui.feed.recyclerview.feeditem.NoteReferenceFeedItemComponent
import com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes.SamsungNoteFeedItemComponent
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.ui.theme.ThemeTag

fun View.setTheme(context: Context, theme: NotesThemeOverride) {
    when (tag as? String) {
        ThemeTag.PRIMARY_BACKGROUND -> setBackgroundColor(ContextCompat.getColor(context, theme.backgroundColor))
        ThemeTag.OPTION_TOOLBAR_BACKGROUND -> {
            setDrawableTint(ContextCompat.getColor(context, theme.optionToolbarBackgroundColor))
        }
        ThemeTag.OPTION_ICON -> {
            setOptionIconColor(ContextCompat.getColor(context, theme.optionIconColor))
            setBackgroundColor(theme)
        }
        ThemeTag.OPTION_TEXT ->
            setOptionTextColor(theme.optionTextColor)
        ThemeTag.OPTION_SECONDARY_TEXT ->
            setOptionTextColor(theme.optionSecondaryTextColor)
        ThemeTag.OPTION_BOTTOM_SHEET_ICON ->
            setOptionIconColor(ContextCompat.getColor(context, theme.optionBottomSheetIconColor))
        ThemeTag.OPTION_BOTTOM_SHEET_DIVIDER ->
            setBackgroundColor(ContextCompat.getColor(context, theme.dividerColor))
        ThemeTag.BOTTOM_SHEET_PRIMARY_TEXT ->
            setOptionTextColor(theme.primaryAppColor)
        ThemeTag.NOTE_CANVAS_LAYOUT ->
            setNoteCanvasColors(theme)
        ThemeTag.FEED_LAYOUT ->
            setBackgroundColor(ContextCompat.getColor(context, theme.feedBackgroundColor))
        ThemeTag.OPTION_ICON_NO_BG -> setOptionIconColor(ContextCompat.getColor(context, theme.optionTextColor))
        ThemeTag.OPTION_TEXT_WITH_DRAWABLE -> {
            setOptionTextColor(theme.optionIconColor)
            setOptionIconColor(ContextCompat.getColor(context, theme.optionIconColor))
            setBackgroundColor(theme)
        }
        ThemeTag.SWIPE_TO_REFRESH -> {
            val swipeView: SwipeRefreshLayout = this as SwipeRefreshLayout
            swipeView.setColorSchemeResources(theme.primaryAppColor)
            swipeView.setProgressBackgroundColorSchemeResource(theme.backgroundColor)
        }
    }
}

private fun View.setDrawableTint(color: Int) {
    DrawableCompat.setTint(getBackground(), color)
}

private fun View.setOptionIconColor(color: Int) {
    when (this) {
        is Button -> {
            setTextColor(color)
            compoundDrawablesRelative.forEach {
                if (it != null) {
                    DrawableCompat.setTint(it, color)
                }
            }
        }
        is ImageButton -> setColorFilter(color)
        is ImageView -> setColorFilter(color)
        is TextView -> {
            for (drawable in compoundDrawablesRelative) {
                drawable?.let { DrawableCompat.setTint(it, color) }
            }
        }
    }
}

private fun View.setOptionTextColor(color: Int) {
    when (this) {
        is TextView -> setTextColor(ContextCompat.getColor(context, color))
    }
}

private fun View.setBackgroundColor(theme: NotesThemeOverride) {
    val backgroundDrawable = theme.optionIconBackgroundDrawable.let {
        ContextCompat.getDrawable(context, it)
    }
    backgroundDrawable?.let { ViewCompat.setBackground(this, it) }
}

private fun View.setNoteCanvasColors(theme: NotesThemeOverride?) {
    val stickyNoteCanvasThemeOverride = theme?.stickyNoteCanvasThemeOverride
    val noteRefCanvasThemeOverride = theme?.noteRefCanvasThemeOverride
    val samsungNoteThemeOverride = theme?.samsungNoteCanvasThemeOverride
    when (this) {
        is NoteItemComponent -> {
            if (themeOverride != stickyNoteCanvasThemeOverride) {
                themeOverride = stickyNoteCanvasThemeOverride
                applyTheme()
            }
        }
        is NoteReferenceFeedItemComponent -> {
            if (themeOverride != noteRefCanvasThemeOverride) {
                themeOverride = noteRefCanvasThemeOverride
                applyTheme()
            }
        }
        is NoteStyledView -> {
            if (themeOverride != stickyNoteCanvasThemeOverride) {
                themeOverride = stickyNoteCanvasThemeOverride
                applyTheme()
            }
        }
        is SamsungNoteFeedItemComponent -> {
            if (themeOverride != samsungNoteThemeOverride) {
                themeOverride = samsungNoteThemeOverride
                applyTheme()
            }
        }
        is SamsungNoteStyledView -> {
            if (themeOverride != samsungNoteThemeOverride) {
                themeOverride = samsungNoteThemeOverride
                applyTheme()
            }
        }
    }
}
