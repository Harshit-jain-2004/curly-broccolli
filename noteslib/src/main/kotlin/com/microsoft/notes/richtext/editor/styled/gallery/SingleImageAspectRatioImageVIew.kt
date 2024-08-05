package com.microsoft.notes.richtext.editor.styled.gallery

import android.content.Context
import android.util.AttributeSet
import com.microsoft.notes.richtext.editor.utils.getScreenHeight
import kotlin.math.roundToInt

class SingleImageAspectRatioImageVIew(context: Context, attrs: AttributeSet) : AspectRatioImageView(context, attrs) {
    companion object {
        // 80% of full note is 0.67 of full display.
        const val MAXIMUM_IMAGE_HEIGHT_PERCENTAGE_OF_SCREEN = 0.67
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val height = measuredWidth * aspectRatioHeight / aspectRatioWidth

        val parentHeight = (getScreenHeight() * MAXIMUM_IMAGE_HEIGHT_PERCENTAGE_OF_SCREEN).roundToInt()
        val newHeight: Int =
            if (height > parentHeight && parentHeight > 0)
                parentHeight
            else
                height
        setMeasuredDimension(measuredWidth, newHeight)
    }
}
