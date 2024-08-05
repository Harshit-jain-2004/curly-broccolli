package com.microsoft.notes.ui.feed.recyclerview.feeditem

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ImageView

// FitXCropYImageView is a custom ImageView designed to fit width and crop extra length of the image drawable in the view space.
// Here, we are using scaletype : MATRIX and a matrix transformation that can translate the image drawable to the view available in onMeasure method.
class FitXCropYImageView : ImageView {

    @SuppressWarnings("UnusedDeclaration")
    constructor(context: Context) : super(context) {
        scaleType = ScaleType.MATRIX
    }

    @SuppressWarnings("UnusedDeclaration")
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        scaleType = ScaleType.MATRIX
    }

    @SuppressWarnings("UnusedDeclaration")
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        scaleType = ScaleType.MATRIX
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val d = drawable
            ?: return
        val drawableRect = RectF(0f, 0f, 0f, 0f)
        val viewRect = RectF(0f, 0f, 0f, 0f)
        val m = Matrix()
        // The below calculations are used to derive the matrix transformation using setRectToRect method.
        val viewWidth = measuredWidth
        val drawableWidth = d.intrinsicWidth
        val drawableHeight = d.intrinsicHeight
        drawableRect.set(0f, 0f, drawableWidth.toFloat(), drawableHeight.toFloat()) // Represents the original image
        val scale = viewWidth.toFloat() / drawableWidth.toFloat()
        val scaledHeight = drawableHeight * scale
        viewRect.set(0f, 0f, viewWidth.toFloat(), scaledHeight)
        m.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER)
        imageMatrix = m

        requestLayout()
    }
}
