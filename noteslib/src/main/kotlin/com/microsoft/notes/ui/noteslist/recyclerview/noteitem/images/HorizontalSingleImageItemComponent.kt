package com.microsoft.notes.ui.noteslist.recyclerview.noteitem.images

import android.content.Context
import android.util.AttributeSet
import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.scheme.mediaList
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.ImageNoteItemComponent
import kotlinx.android.synthetic.main.sn_horizontal_note_item_layout_single_image.view.*

class HorizontalSingleImageItemComponent(context: Context, attrs: AttributeSet) :
    ImageNoteItemComponent(context, attrs) {
    override fun loadPhotos(note: Note) {
        val inlineMedia = note.document.mediaList().firstOrNull()
        val media = note.media.firstOrNull()
        when {
            inlineMedia != null -> loadPhoto(noteImage, inlineMedia, note)
            media != null -> loadPhoto(noteImage, media)
        }
    }
}
