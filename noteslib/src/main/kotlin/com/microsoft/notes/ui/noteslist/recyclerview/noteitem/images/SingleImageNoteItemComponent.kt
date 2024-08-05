package com.microsoft.notes.ui.noteslist.recyclerview.noteitem.images

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.scheme.mediaList
import com.microsoft.notes.ui.extensions.hide
import com.microsoft.notes.ui.extensions.show
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.ImageNoteItemComponent
import kotlinx.android.synthetic.main.sn_note_item_layout_single_image.view.*

open class SingleImageNoteItemComponent(context: Context, attrs: AttributeSet) :
    ImageNoteItemComponent(context, attrs) {

    override fun loadPhotos(note: Note) {
        val inlineMedia = note.document.mediaList().firstOrNull()
        val media = note.media.firstOrNull()
        if (inlineMedia == null && media == null) {
            noteImage_3_2.hide()
            noteImage_16_9.hide()
            return
        }

        val imageView = getImageView(note)
        when {
            inlineMedia != null -> loadPhoto(imageView, inlineMedia, note)
            media != null -> loadPhoto(imageView, media)
        }
    }

    private fun getImageView(note: Note): ImageView {
        return if (note.hasNoText) {
            noteImage_3_2.show()
            noteImage_16_9.hide()
            noteImage_3_2
        } else {
            noteImage_3_2.hide()
            noteImage_16_9.show()
            noteImage_16_9
        }
    }
}
