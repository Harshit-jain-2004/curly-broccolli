package com.microsoft.notes.richtext.editor.styled.gallery

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

// RecyclerView in NestedScrollView does not always report scrolling correctly, which in turns stops items from
// being clicked. Issue: https://issuetracker.google.com/issues/66996774
// Potentially resolved with upgrade to support library 27.0.1+, but there are reports of issue still being present.
class NestedRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && scrollState == SCROLL_STATE_SETTLING) {
            parent.requestDisallowInterceptTouchEvent(false)
            if (!canScrollVertically(-1) || !canScrollVertically(1)) {
                stopScroll()
                return false
            }
        }

        return super.onInterceptTouchEvent(event)
    }
}
