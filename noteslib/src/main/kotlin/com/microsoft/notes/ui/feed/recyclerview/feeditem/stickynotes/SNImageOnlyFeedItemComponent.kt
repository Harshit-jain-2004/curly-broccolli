package com.microsoft.notes.ui.feed.recyclerview.feeditem.stickynotes

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.extensions.hide
import com.microsoft.notes.ui.extensions.show
import com.microsoft.notes.ui.extensions.toFeedImageCountBackgroundContextColor
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.ImageNoteItemComponent

private const val MAX_VISIBLE_IMAGES_COUNT = 3

open class SNImageOnlyFeedItemComponent(context: Context, attrs: AttributeSet) :
    ImageNoteItemComponent(context, attrs) {

    protected open val maxVisibleImagesCount = MAX_VISIBLE_IMAGES_COUNT
    private val noteImage1: ImageView? by lazy { findViewById<ImageView>(R.id.noteImage1) }
    private val noteImage2: ImageView? by lazy { findViewById<ImageView>(R.id.noteImage2) }
    private val noteImage3: ImageView? by lazy { findViewById<ImageView>(R.id.noteImage3) }
    private val imageCount: TextView by lazy { findViewById<TextView>(R.id.imageCount) }
    private val imageContainers: List<ImageView?> by lazy { listOf(noteImage1, noteImage2, noteImage3) }

    override fun loadPhotos(note: Note) {
        if (note.isVoiceNote) {
            imageContainers.forEachIndexed { _, container -> container?.visibility = View.GONE }
            imageCount.hide()
            return
        }
        val mediaList = note.sortedMedia
        val previewMediaList = mediaList.take(kotlin.math.min(mediaList.size, maxVisibleImagesCount))
        val cntImagesToBeShown = previewMediaList.size
        previewMediaList.forEachIndexed { index, media -> imageContainers[index]?.let { loadPhoto(it, media) } }
        setImageCount(note, mediaList.size - maxVisibleImagesCount)
        imageContainers.forEachIndexed { index, container -> container?.visibility = if (index < cntImagesToBeShown) View.VISIBLE else View.GONE }
    }

    private fun setImageCount(note: Note, count: Int) {
        if (count > 0) {
            imageCount.text = resources.getString(R.string.sn_note_preview_image_count, count)
            imageCount.show()
            (imageCount.background as? GradientDrawable)?.setColor(
                note.color.toFeedImageCountBackgroundContextColor(context)
            )
        } else {
            imageCount.hide()
        }
    }
}
