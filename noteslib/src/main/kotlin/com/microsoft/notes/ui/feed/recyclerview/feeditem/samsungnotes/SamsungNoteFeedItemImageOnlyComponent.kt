package com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.styled.loadRoundedImageFromUri
import com.microsoft.notes.sideeffect.sync.getSamsungAttachedImagesForHTMLNote
import com.microsoft.notes.ui.extensions.hide
import com.microsoft.notes.ui.extensions.show
import com.microsoft.notes.ui.extensions.toImageCountBackgroundContextColor

private const val MAX_VISIBLE_IMAGES_COUNT = 3
open class SamsungNoteFeedItemImageOnlyComponent(context: Context, attrs: AttributeSet) :
    SamsungNoteFeedItemComponent(context, attrs) {

    protected open val shouldCenterCrop = true
    protected open val maxVisibleImagesCount = MAX_VISIBLE_IMAGES_COUNT
    private val samsungNoteImage1: ImageView? by lazy { findViewById<ImageView>(R.id.samsungNoteImage1) }
    private val samsungNoteImage2: ImageView? by lazy { findViewById<ImageView>(R.id.samsungNoteImage2) }
    private val samsungNoteImage3: ImageView? by lazy { findViewById<ImageView>(R.id.samsungNoteImage3) }
    private val imageCount: TextView? by lazy { findViewById<TextView>(R.id.imageCount) }
    private val imageContainers: List<ImageView?> by lazy { listOf(samsungNoteImage1, samsungNoteImage2, samsungNoteImage3) }

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
        val mediaList = getMedia(note)
        val previewMediaList = mediaList.take(kotlin.math.min(mediaList.size, maxVisibleImagesCount))
        val countImagesToBeShown = previewMediaList.size
        previewMediaList.forEachIndexed { index, media -> imageContainers[index]?.let { loadPhoto(it, media) } }
        setImageCount(note, mediaList.size - maxVisibleImagesCount)
        imageContainers.forEachIndexed { index, container -> container?.visibility = if (index < countImagesToBeShown) View.VISIBLE else View.GONE }
    }

    protected open fun getMedia(note: Note): List<Media> = note.media.getSamsungAttachedImagesForHTMLNote()

    private fun loadPhoto(imageView: ImageView, media: Media) {
        imageView.visibility = View.VISIBLE
        imageView.loadRoundedImageFromUri(media.localUrl, centerCrop = shouldCenterCrop)
    }

    private fun setImageCount(note: Note, count: Int) {
        if (count > 0) {
            imageCount?.text = resources.getString(R.string.sn_note_preview_image_count, count)
            imageCount.show()
            (imageCount?.background as? GradientDrawable)?.setColor(
                note.color.toImageCountBackgroundContextColor(context)
            )
        } else {
            imageCount.hide()
        }
    }
}
