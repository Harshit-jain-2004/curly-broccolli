package com.microsoft.notes.ui.note.ink

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.utils.utils.calculateOffsetToCenterAlign
import com.microsoft.notes.utils.utils.calculateScaleFactor

// Scaling the image to fit the height or width (whichever is smaller) or using default scaling,
// then centrally aligning the preview.
class PreviewInkView(context: Context, attrs: AttributeSet) : InkView(context, attrs) {
    private var xOffSet = 0f
    private var yOffSet = 0f

    private var minXOfInkPoints: Float? = null
    private var maxXOfInkPoints: Float? = null
    private var minYOfInkPoints: Float? = null
    private var maxYOfInkPoints: Float? = null

    private var inkWidth: Float? = null
    private var inkHeight: Float? = null

    override fun setDocumentAndUpdateScaleFactor(doc: Document) {
        super.updateDocument(doc, invalidatePathCache = true)

        setupInkDimensions(doc)

        val inkHeight = this.inkHeight
        val inkWidth = this.inkWidth
        this.getScaleFactor = if (inkWidth == null || inkHeight == null) {
            { DEFAULT_SCALE_FACTOR }
        } else {
            {
                calculateScaleFactor(inkWidth, inkHeight, measuredWidth, measuredHeight, DEFAULT_SCALE_FACTOR)
            }
        }
    }

    override var getScaleFactor: (() -> Float)? = null

    override fun onDraw(canvas: Canvas) {
        val inkHeight = this.inkHeight
        val inkWidth = this.inkWidth
        if (minXOfInkPoints != null && minYOfInkPoints != null && inkWidth != null && inkHeight != null) {
            xOffSet = calculateOffsetToCenterAlign(minXOfInkPoints, inkWidth, measuredWidth, getScaleFactorWithDefault())
            yOffSet = calculateOffsetToCenterAlign(minYOfInkPoints, inkHeight, measuredHeight, getScaleFactorWithDefault())
            canvas.translate(xOffSet, yOffSet)
        }
        super.onDraw(canvas)
    }

    private fun setupInkDimensions(doc: Document) {
        maxXOfInkPoints = getMaxXOfInkPoints(doc)?.toFloat()
        minXOfInkPoints = getMinXOfInkPoints(doc)?.toFloat()
        maxYOfInkPoints = getMaxYOfInkPoints(doc)?.toFloat()
        minYOfInkPoints = getMinYOfInkPoints(doc)?.toFloat()

        inkWidth = getInkSize(maxXOfInkPoints, minXOfInkPoints)
        inkHeight = getInkSize(maxYOfInkPoints, minYOfInkPoints)
    }

    private fun getInkSize(maxCoordinate: Float?, minCoordinate: Float?): Float? = if (maxCoordinate != null && minCoordinate != null)
        maxCoordinate - minCoordinate else null
}
