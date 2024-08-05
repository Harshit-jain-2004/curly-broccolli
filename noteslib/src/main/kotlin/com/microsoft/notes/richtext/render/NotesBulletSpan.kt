package com.microsoft.notes.richtext.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.text.Layout
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import com.microsoft.notes.platform.extensions.px

/*
 * Once we move to Android P, we should use the ability to set the bullet radius which is in the developer
 * preview right now
 */
class NotesBulletSpan(private val indentationLevel: Int = 0) : LeadingMarginSpan {
    private val indentationMargin = (indentationLevel * INDENTATION_WIDTH)

    fun getIndentationLevel() = indentationLevel

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
            val style = p.style
            p.style = Paint.Style.FILL

            val rtl = (dir < 0)
            val dx = getXOffset(rtl).toFloat() + dir

            if (c.isHardwareAccelerated) {
                c.save()
                c.translate(x + dx, (top + bottom) / 2.0f)
                c.drawPath(sBulletPath, p)
                c.restore()
            } else {
                c.drawCircle(
                    x + dx, (top + bottom) / 2.0f,
                    BULLET_RADIUS.toFloat(), p
                )
            }
            p.style = style
        }
    }

    // Checking rtl to draw bullets at correct point in both RTL and LTR languages
    private fun getXOffset(rtl: Boolean): Int {
        return if (rtl) -BULLET_RADIUS - indentationMargin
        else BULLET_RADIUS + indentationMargin
    }

    companion object {
        private val GAP_WIDTH = 8.px
        private val INDENTATION_WIDTH = 8.px
        private val BULLET_RADIUS = 2.px
        private val sBulletPath: Path by lazy {
            val path = Path()
            // Bullet is slightly better to avoid aliasing artifacts on mdpi devices.
            // Fixing radius*1.2f to radius + 1.2f = so that bullets are not truncated to the left. This
            // is an existing issue in Android
            path.addCircle(0.0f, 0.0f, 1.2f + BULLET_RADIUS, Path.Direction.CW)
            path
        }

        val DEFAULT_LEADING_MARGIN = 2 * BULLET_RADIUS + GAP_WIDTH
    }
}
