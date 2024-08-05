package com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.editor.styled.loadRoundedImageFromUri
import com.microsoft.notes.sideeffect.sync.getSamsungMediaForPreviewImage
import com.microsoft.notes.ui.extensions.hide
import kotlinx.android.synthetic.main.samsung_feed_item_layout_preview_image.view.*

class SamsungNoteItemPreviewImageComponent(context: Context, attrs: AttributeSet) :
    SamsungNoteFeedItemComponent(context, attrs) {

    override fun bindNote(
        note: Note,
        keywordsToHighlight: List<String>?,
        isListLayout: Boolean,
        isSelectionEnabled: Boolean,
        isItemSelected: Boolean,
        isFeedUIRefreshEnabled: Boolean
    ) {
        super.bindNote(note, keywordsToHighlight, isListLayout, isSelectionEnabled, isItemSelected, isFeedUIRefreshEnabled)
        loadPhotos(note)
    }

    private fun loadPhotos(note: Note) {
        val media = note.media.getSamsungMediaForPreviewImage()
        if (media == null) {
            samsungNotePreviewImage.hide()
            return
        }

        with(samsungNotePreviewImage) {
            visibility = View.VISIBLE
            loadRoundedImageFromUri(media.localUrl, centerCrop = false)
        }
    }
}
