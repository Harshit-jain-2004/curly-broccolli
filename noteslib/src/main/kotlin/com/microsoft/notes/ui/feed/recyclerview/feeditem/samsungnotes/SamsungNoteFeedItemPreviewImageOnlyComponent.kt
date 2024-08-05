package com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes

import android.content.Context
import android.util.AttributeSet
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.sideeffect.sync.getSamsungMediaForPreviewImage

private const val MAX_VISIBLE_IMAGES_COUNT = 1

open class SamsungNoteFeedItemPreviewImageOnlyComponent(context: Context, attrs: AttributeSet) :
    SamsungNoteFeedItemImageOnlyComponent(context, attrs) {

    override val maxVisibleImagesCount = MAX_VISIBLE_IMAGES_COUNT
    override val shouldCenterCrop = false

    override fun getMedia(note: Note): List<Media> {
        val previewImage: Media? = note.media.getSamsungMediaForPreviewImage()
        return if (previewImage == null) {
            // no preview image, return empty list
            listOf()
        } else {
            listOf(previewImage)
        }
    }
}
