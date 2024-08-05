package com.microsoft.notes.ui.transition

import android.graphics.Rect
import android.view.View

class DynamicTransitionParams {
    var duration: Long = 0L
    var slideUpDistance: Int = 0
    var slideDownDistance: Int = 0
    var containerBounds: Rect = Rect()
    var slideDownOffset: Int = 0

    fun setFromViews(noteView: View, listView: View) {
        val noteViewPos = IntArray(2)
        val listViewPos = IntArray(2)
        noteView.getLocationOnScreen(noteViewPos)
        listView.getLocationOnScreen(listViewPos)

        slideUpDistance = listViewPos[1] - noteViewPos[1]
        slideDownDistance = listViewPos[1] + listView.height + slideDownOffset - (noteViewPos[1] + noteView.height)
        containerBounds = Rect(
            listViewPos[0],
            listViewPos[1],
            listViewPos[0] + listView.width,
            listViewPos[1] + listView.height + slideDownOffset
        )
        val magnitude: Float = maxOf(
            Math.abs(slideUpDistance),
            slideDownDistance
        ).toFloat() / listView.height
        duration = Math.round(MIN_DURATION + maxOf(magnitude, 0f) * EXTRA_DURATION).toLong()
    }
}
