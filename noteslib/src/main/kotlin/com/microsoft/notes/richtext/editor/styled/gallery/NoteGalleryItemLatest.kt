package com.microsoft.notes.richtext.editor.styled.gallery

import android.view.View
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Media
import kotlinx.android.synthetic.main.sn_note_gallery_item_latest.view.*

class NoteGalleryItemLatest(view: View) : NoteGalleryItem.NoteGalleryImage(view) {
    override fun setMedia(
        media: Media,
        selected: Boolean,
        noteColor: Color,
        callback: NoteGalleryAdapter.Callback?
    ) {
        super.setMedia(
            itemView.noteGalleryItemImageContainer,
            itemView.noteGalleryItemImageView, media, selected, noteColor, callback, true
        )
    }

    override fun setSelected(selected: Boolean, noteColor: Color) {
        super.setSelected(itemView.noteGalleryItemOverlay, selected, noteColor)
    }
}
