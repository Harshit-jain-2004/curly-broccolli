package com.microsoft.notes.ui.transition.extensions

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout

fun View.nextLayoutListener(listener: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(
        object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                listener()
            }
        })
}

fun View.getLocationOnScreen(): IntArray {
    val position = IntArray(2)
    getLocationOnScreen(position)
    return position
}

fun View.getLocationOnScreenAsPoint(): Point {
    val position = getLocationOnScreen()
    return Point(position[0], position[1])
}

fun View.getBoundsOnScreen(): Rect {
    val position = getLocationOnScreen()
    return Rect(position[0], position[1], position[0] + width, position[1] + height)
}

fun View.setLayoutParamsFrom(other: View, parentPosition: Point) {
    val position = other.getLocationOnScreen()

    val layoutParams = FrameLayout.LayoutParams(layoutParams)
    layoutParams.leftMargin = position[0] - parentPosition.x
    layoutParams.topMargin = position[1] - parentPosition.y
    layoutParams.height = other.height
    layoutParams.width = other.width
    this.layoutParams = layoutParams
}

operator fun Rect.minus(point: IntArray) =
    Rect(left - point[0], top - point[1], right - point[0], bottom - point[1])

operator fun Rect.minus(point: Point) =
    Rect(left - point.x, top - point.y, right - point.x, bottom - point.y)

fun Rect.center() = Point(centerX(), centerY())

operator fun Point.plus(other: Point) = Point(x + other.x, y + other.y)

operator fun Point.minus(other: Point) = Point(x - other.x, y - other.y)

operator fun PointF.minus(other: PointF) = PointF(x - other.x, y - other.y)
