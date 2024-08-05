package com.microsoft.notes.ui.noteslist.recyclerview.noteitem.images

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.extensions.hide
import com.microsoft.notes.ui.extensions.show
import com.microsoft.notes.ui.extensions.toImageCountBackgroundContextColor
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.ImageNoteItemComponent

private const val MAX_VISIBLE_IMAGES_COUNT = 4

class MultiImageNoteItemComponent(context: Context, attrs: AttributeSet) : ImageNoteItemComponent(context, attrs) {

    private val noteImage1: ImageView by lazy { findViewById<ImageView>(R.id.noteImage1) }
    private val noteImage2: ImageView by lazy { findViewById<ImageView>(R.id.noteImage2) }
    private val noteImage3: ImageView by lazy { findViewById<ImageView>(R.id.noteImage3) }
    private val noteImage4: ImageView by lazy { findViewById<ImageView>(R.id.noteImage4) }
    private val imageCount: TextView by lazy { findViewById<TextView>(R.id.imageCount) }
    private val imageContainers: List<ImageView> by lazy { listOf(noteImage1, noteImage2, noteImage3, noteImage4) }

    override fun loadPhotos(note: Note) {
        val mediaList = note.sortedMedia
        val previewMediaList = mediaList.take(MAX_VISIBLE_IMAGES_COUNT)
        previewMediaList.forEachIndexed { index, media -> loadPhoto(imageContainers[index], media) }
        setImageCount(note, mediaList.size - MAX_VISIBLE_IMAGES_COUNT)
    }

    private fun setImageCount(note: Note, count: Int) {
        if (count > 0) {
            imageCount.text = resources.getString(R.string.sn_note_preview_image_count, count)
            imageCount.show()
            (imageCount.background as? GradientDrawable)?.setColor(
                note.color.toImageCountBackgroundContextColor(context)
            )
        } else {
            imageCount.hide()
        }
    }
}
