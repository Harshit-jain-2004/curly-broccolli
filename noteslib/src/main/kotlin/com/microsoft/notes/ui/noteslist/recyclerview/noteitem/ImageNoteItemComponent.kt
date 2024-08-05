package com.microsoft.notes.ui.noteslist.recyclerview.noteitem

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.core.view.ViewCompat
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.editor.styled.loadRoundedImageFromUri
import com.microsoft.notes.richtext.editor.styled.toInkContextColor
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.getUrlOrNull
import com.microsoft.notes.ui.extensions.asPreviewSpannable
import com.microsoft.notes.ui.extensions.hide
import com.microsoft.notes.ui.extensions.highlightKeywords
import com.microsoft.notes.ui.extensions.setPreviewContentAndVisibility
import com.microsoft.notes.ui.extensions.show

abstract class ImageNoteItemComponent(context: Context, attrs: AttributeSet) : NoteItemComponent(context, attrs) {

    override fun bindNote(
        note: Note,
        keywordsToHighlight: List<String>?,
        isSelectionEnabled: Boolean,
        isItemSelected: Boolean,
        showDateTime: Boolean,
        showSource: Boolean,
        showSourceText: Boolean,
        isFeedUiRefreshEnabled: Boolean
    ) {
        super.bindNote(
            note, keywordsToHighlight, isSelectionEnabled, isItemSelected, showDateTime,
            showSource, showSourceText, isFeedUiRefreshEnabled
        )
        if (note.hasNoText) {
            noteBody?.text = ""
            noteBody.hide()
        } else {
            val documentPreview = note.document.asPreviewSpannable(context)
            if (keywordsToHighlight != null) {
                documentPreview.highlightKeywords(context, keywordsToHighlight, note.color)
            }
            noteBody?.setPreviewContentAndVisibility(documentPreview)
            noteBody.show()
        }

        if (note.isVoiceNote) {
            // show UX here
            recordingIcon?.visibility = VISIBLE
        }
        loadPhotos(note)
    }

    override fun applyTheme(isSelectionEnabled: Boolean, isItemSelected: Boolean) {
        super.applyTheme(isSelectionEnabled, isItemSelected)
        sourceNote?.let {
            // Apply new ink color
            if (it.isRenderedInkNote) {
                loadPhotos(it)
            }
        }
    }

    override fun prepareSharedElements(markSharedElement: (View, String) -> Unit) {
        super.prepareSharedElements(markSharedElement)
        noteBody?.let { markSharedElement(it as View, "body") }
    }

    override fun clearTransitionNames() {
        super.clearTransitionNames()
        noteBody?.let { ViewCompat.setTransitionName(it as View, "") }
    }

    protected fun loadPhoto(iv: ImageView, media: InlineMedia, note: Note) {
        iv.visibility = View.VISIBLE
        val inkColor = if (note.isRenderedInkNote) {
            note.color.toInkContextColor(context, themeOverride)
        } else {
            null
        }
        iv.loadRoundedImageFromUri(media.getUrlOrNull(), color = inkColor)
    }

    protected fun loadPhoto(iv: ImageView, media: Media) {
        iv.visibility = View.VISIBLE
        iv.loadRoundedImageFromUri(media.localUrl)
    }

    abstract fun loadPhotos(note: Note)
}
