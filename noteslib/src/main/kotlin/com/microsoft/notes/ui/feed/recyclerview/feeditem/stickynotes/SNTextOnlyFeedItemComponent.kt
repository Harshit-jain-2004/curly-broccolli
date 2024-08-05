package com.microsoft.notes.ui.feed.recyclerview.feeditem.stickynotes

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat
import com.microsoft.notes.models.Note
import com.microsoft.notes.ui.extensions.asPreviewSpannable
import com.microsoft.notes.ui.extensions.highlightKeywords
import com.microsoft.notes.ui.extensions.setPreviewContentAndVisibility
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent

class SNTextOnlyFeedItemComponent(context: Context, attrs: AttributeSet) : NoteItemComponent(context, attrs) {

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
        val documentPreview = note.document.asPreviewSpannable(context)
        if (keywordsToHighlight != null) {
            documentPreview.highlightKeywords(context, keywordsToHighlight, note.color)
        }
        noteBody?.setPreviewContentAndVisibility(documentPreview)
    }

    override fun prepareSharedElements(markSharedElement: (View, String) -> Unit) {
        super.prepareSharedElements(markSharedElement)
        markSharedElement(noteBody as View, "body")
    }

    override fun clearTransitionNames() {
        super.clearTransitionNames()
        ViewCompat.setTransitionName(noteBody as View, "")
    }
}
