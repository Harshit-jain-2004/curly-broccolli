package com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes

import android.content.Context
import android.util.AttributeSet

private const val MAX_VISIBLE_IMAGES_COUNT = 1

class SamsungNoteFeedItemImageOnlyGridComponent(context: Context, attrs: AttributeSet) :
    SamsungNoteFeedItemImageOnlyComponent(context, attrs) {
    override val maxVisibleImagesCount = MAX_VISIBLE_IMAGES_COUNT
}
