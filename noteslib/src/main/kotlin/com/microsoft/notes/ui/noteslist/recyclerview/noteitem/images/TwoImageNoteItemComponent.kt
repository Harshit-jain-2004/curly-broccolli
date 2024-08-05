package com.microsoft.notes.ui.noteslist.recyclerview.noteitem.images

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.ImageNoteItemComponent

private const val VISIBLE_IMAGES_COUNT = 2

class TwoImageNoteItemComponent(context: Context, attrs: AttributeSet) : ImageNoteItemComponent(context, attrs) {

    private val noteImage1: ImageView by lazy { findViewById<ImageView>(R.id.noteImage1) }
    private val noteImage2: ImageView by lazy { findViewById<ImageView>(R.id.noteImage2) }
    private val imageContainers: List<ImageView> by lazy { listOf(noteImage1, noteImage2) }

    override fun loadPhotos(note: Note) {
        val mediaList = note.sortedMedia
        val previewMediaList = mediaList.take(VISIBLE_IMAGES_COUNT)
        previewMediaList.forEachIndexed { index, media -> loadPhoto(imageContainers[index], media) }
    }
}
