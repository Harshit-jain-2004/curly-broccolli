package com.microsoft.notes.ui.feed.recyclerview.feeditem

import android.content.Context
import android.util.AttributeSet
import com.microsoft.notes.ui.feed.recyclerview.TimeBucket
import com.microsoft.notes.ui.theme.ThemedTextView
import kotlinx.android.synthetic.main.feed_item_time_header.view.*

class TimeHeaderItemComponent(context: Context, attrs: AttributeSet) : ThemedTextView(context, attrs) {

    fun bindHeader(timeBucket: TimeBucket) {
        header.text = timeBucket.getTitle(context)
    }
}
