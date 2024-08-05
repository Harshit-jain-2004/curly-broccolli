package com.microsoft.notes.richtext.editor.styled.gallery

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.microsoft.notes.noteslib.R

open class AspectRatioImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {
    protected var aspectRatioHeight: Int = 1
    protected var aspectRatioWidth: Int = 1

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.AspectRatioImageView, 0, 0)
            .apply {
                try {
                    aspectRatioHeight = getInteger(R.styleable.AspectRatioImageView_aspectRatioHeight, 1)
                    aspectRatioWidth = getInteger(R.styleable.AspectRatioImageView_aspectRatioWidth, 1)
                } finally {
                    recycle()
                }
            }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        val height = width * aspectRatioHeight / aspectRatioWidth
        setMeasuredDimension(width, height)
    }
}
