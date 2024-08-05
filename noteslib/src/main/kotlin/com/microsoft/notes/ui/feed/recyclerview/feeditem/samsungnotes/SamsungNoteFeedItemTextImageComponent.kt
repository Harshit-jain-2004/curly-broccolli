package com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.View
import com.microsoft.notes.models.Note
import com.microsoft.notes.ui.extensions.hide
import com.microsoft.notes.ui.extensions.highlightKeywords
import com.microsoft.notes.ui.extensions.setPreviewContentAndVisibility

private const val MAX_VISIBLE_IMAGES_COUNT = 1

class SamsungNoteFeedItemTextImageComponent(context: Context, attrs: AttributeSet) :
    SamsungNoteFeedItemImageOnlyComponent(context, attrs) {

    override val maxVisibleImagesCount = MAX_VISIBLE_IMAGES_COUNT

    override fun bindNote(note: Note, keywordsToHighlight: List<String>?, isListLayout: Boolean, isSelectionEnabled: Boolean, isItemSelected: Boolean, isFeedUIRefreshEnabled: Boolean) {
        super.bindNote(note, keywordsToHighlight, isListLayout, isSelectionEnabled, isItemSelected, isFeedUIRefreshEnabled)
        if (isFeedUIRefreshEnabled && !isListLayout && !noteTitle?.text.isNullOrEmpty()) {
            // in grid view, hide notePreview if noteTitle is not empty
            notePreview.hide()
            noteTitle?.maxLines = 1
        } else {
            val documentPreview = SpannableStringBuilder(note.document.bodyPreview)
            if (keywordsToHighlight != null) {
                documentPreview.highlightKeywords(context, keywordsToHighlight, note.color)
            }
            notePreview?.setPreviewContentAndVisibility(documentPreview)
            if (isListLayout && notePreview?.visibility == View.GONE) {
                // in list view, show more lines of title if preview is empty
                noteTitle?.maxLines = 4
            } else {
                noteTitle?.maxLines = 1
            }
        }
        updateAccessibilityLabel(note, isSelectionEnabled, isItemSelected)
    }
}
