package com.microsoft.notes.richtext.render

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import com.microsoft.notes.platform.extensions.px

class TextLeadingMarginSpan(
    private val marginText: String = "",
    private val indentationLevel: Int = 0
) : LeadingMarginSpan {
    companion object {
        private val INDENTATION_WIDTH = 8.px
        private val DEFAULT_LEADING_MARGIN = 14.px
    }

    fun getIndentationLevel() = indentationLevel

    private val indentationMargin = indentationLevel * INDENTATION_WIDTH

    override fun getLeadingMargin(first: Boolean): Int = DEFAULT_LEADING_MARGIN + indentationMargin

    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        l: Layout
    ) {
        if ((text as Spanned).getSpanStart(this) == start) {
            val rtl = (dir < 0)
            val dx = if (rtl)
                -indentationMargin.toFloat() - DEFAULT_LEADING_MARGIN
            else
                indentationMargin.toFloat()
            val str = if (rtl)
                " .$marginText"
            else
                "$marginText. "
            c.drawText(str, x + dx, baseline.toFloat(), p)
        }
    }
}
