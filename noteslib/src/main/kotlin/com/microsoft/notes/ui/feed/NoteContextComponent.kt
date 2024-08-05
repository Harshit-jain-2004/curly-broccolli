package com.microsoft.notes.ui.feed

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.NotesThemeOverride
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.scheme.NoteContext
import com.microsoft.notes.ui.theme.ThemedTextView
import com.microsoft.notes.utils.utils.ImageUtils

class NoteContextComponent(context: Context, attrs: AttributeSet) : ThemedTextView(context, attrs) {
    fun showNoteContext(noteContext: NoteContext, themeOverride: NotesThemeOverride.StickyNoteCanvasThemeOverride?) {
        if (NotesLibrary.getInstance().experimentFeatureFlags.enableContextInNotes) {
            if (noteContext.displayName.isNotBlank()) {
                text = noteContext.displayName
                visibility = View.VISIBLE
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (themeOverride != null) R.color.pinned_note_text_color_dark
                        else R.color.sn_metadata_color_charcoal
                    )
                )

                val drawableIcon: Drawable? = if (!noteContext.hostIcon.isNullOrBlank())
                    ImageUtils.getBitmapFromBase64(noteContext.hostIcon)?.let {
                        BitmapDrawable(context.resources, it)
                    } else null
                val size = context.resources.getDimensionPixelSize(R.dimen.sn_context_icon_size)
                drawableIcon?.setBounds(0, 0, size, size)
                setCompoundDrawables(drawableIcon, null, null, null)
            } else {
                visibility = View.GONE
            }
        }
    }
}
