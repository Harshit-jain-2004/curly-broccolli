package com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import com.microsoft.notes.models.Note
import com.microsoft.notes.ui.extensions.highlightKeywords
import com.microsoft.notes.ui.extensions.setPreviewContentAndVisibility

class SamsungNotesFeedItemTextOnlyComponent(context: Context, attrs: AttributeSet) :
    SamsungNoteFeedItemComponent(context, attrs) {
    override fun bindNote(note: Note, keywordsToHighlight: List<String>?, isListLayout: Boolean, isSelectionEnabled: Boolean, isItemSelected: Boolean, isFeedUIRefreshEnabled: Boolean) {
        super.bindNote(note, keywordsToHighlight, isListLayout, isSelectionEnabled, isItemSelected, isFeedUIRefreshEnabled)
        val documentPreview = SpannableStringBuilder(note.document.bodyPreview)
        if (keywordsToHighlight != null) {
            documentPreview.highlightKeywords(context, keywordsToHighlight, note.color)
        }
        notePreview?.setPreviewContentAndVisibility(documentPreview)
        updateAccessibilityLabel(note, isSelectionEnabled, isItemSelected)
    }
}
