package com.microsoft.notes.ui.feed.recyclerview.feeditem.stickynotes

import android.content.Context
import android.util.AttributeSet

private const val MAX_VISIBLE_IMAGES_COUNT = 1

class SNImageOnlyFeedGridItemComponent(context: Context, attrs: AttributeSet) :
    SNImageOnlyFeedItemComponent(context, attrs) {

    override val maxVisibleImagesCount = MAX_VISIBLE_IMAGES_COUNT
}
