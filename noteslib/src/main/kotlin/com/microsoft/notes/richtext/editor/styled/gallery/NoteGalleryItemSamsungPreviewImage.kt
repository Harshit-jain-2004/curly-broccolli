package com.microsoft.notes.richtext.editor.styled.gallery

import android.view.View
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Media
import kotlinx.android.synthetic.main.samsung_gallery_item_preview_image.view.*
import kotlinx.android.synthetic.main.sn_note_gallery_item_single_image.view.noteGalleryItemImageContainer
import kotlinx.android.synthetic.main.sn_note_gallery_item_single_image.view.noteGalleryItemOverlay

class NoteGalleryItemSamsungPreviewImage(view: View) : NoteGalleryItem.NoteGalleryImage(view) {
    override fun setMedia(
        media: Media,
        selected: Boolean,
        noteColor: Color,
        callback: NoteGalleryAdapter.Callback?
    ) {
        super.setMedia(
            itemView.noteGalleryItemImageContainer,
            itemView.samsungNoteBodyPreviewImage, media, selected, noteColor, callback, false
        )
    }

    override fun setSelected(selected: Boolean, noteColor: Color) {
        super.setSelected(itemView.noteGalleryItemOverlay, selected, noteColor)
    }
}
