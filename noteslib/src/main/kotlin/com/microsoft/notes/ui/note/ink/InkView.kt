package com.microsoft.notes.ui.note.ink

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.DocumentType
import com.microsoft.notes.richtext.scheme.InkPoint
import com.microsoft.notes.richtext.scheme.Stroke
import com.microsoft.notes.utils.utils.calculateScaleFactor

open class InkView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    protected companion object {
        const val DEFAULT_SCALE_FACTOR = 4f
        const val STROKE_WIDTH_MAX = 10f
        const val ERASER_THRESHOLD = 48
    }

    private var canvasPaint: Paint = Paint(Paint.DITHER_FLAG)

    protected var document: Document = Document(type = DocumentType.INK)
        private set

    val inkPaint: Paint = Paint()

    open var getScaleFactor: (() -> Float)? = null
    protected fun getScaleFactorWithDefault() = getScaleFactor?.let { it() } ?: DEFAULT_SCALE_FACTOR

    private val pathCache = PathCache(object : PathCache.Callback {
        override fun getStrokeList() = document.strokes
        override fun getScaleFactor() = getScaleFactorWithDefault()
        override fun getStrokeWidthFactor() = getStrokeWidth()
    })

    init {
        inkPaint.isAntiAlias = true
        inkPaint.style = Paint.Style.STROKE
        inkPaint.strokeJoin = Paint.Join.ROUND
        inkPaint.strokeCap = Paint.Cap.ROUND
    }

    open fun setDocumentAndUpdateScaleFactor(doc: Document) {
        updateDocument(doc)

        // For each InkView, we only wish to set the scale getter exactly once, as we want to avoid the scale
        // changing as new strokes are added. A getter is used over a value as measuredWidth and measuredHeight
        // may be 0 here as the View is not visible yet
        if (getScaleFactor == null) {
            val maxXOfInkPoints = getMaxXOfInkPoints(doc)
            val maxYOfInkPoints = getMaxYOfInkPoints(doc)
            getScaleFactor = if (maxXOfInkPoints == null || maxYOfInkPoints == null) {
                { DEFAULT_SCALE_FACTOR }
            } else {
                {
                    calculateScaleFactor(maxXOfInkPoints.toFloat(), maxYOfInkPoints.toFloat(), measuredWidth, measuredHeight, DEFAULT_SCALE_FACTOR)
                }
            }
        }
    }

    protected fun updateDocument(doc: Document, invalidatePathCache: Boolean = true) {
        document = doc
        if (invalidatePathCache) {
            pathCache.invalidate()
        }
    }

    private fun getStrokeWidth(): Float = (getScaleFactorWithDefault() / DEFAULT_SCALE_FACTOR) * STROKE_WIDTH_MAX

    protected fun getMaxXOfInkPoints(doc: Document): Double? =
        doc.strokes.map { stroke -> stroke.points.map { pt -> pt.x } }.flatten().maxOrNull()

    protected fun getMaxYOfInkPoints(doc: Document): Double? =
        doc.strokes.map { stroke -> stroke.points.map { pt -> pt.y } }.flatten().maxOrNull()

    protected fun getMinXOfInkPoints(doc: Document): Double? =
        doc.strokes.map { stroke -> stroke.points.map { pt -> pt.x } }.flatten().minOrNull()

    protected fun getMinYOfInkPoints(doc: Document): Double? =
        doc.strokes.map { stroke -> stroke.points.map { pt -> pt.y } }.flatten().minOrNull()

    override fun onDraw(canvas: Canvas) {
        pathCache.getPathMap().forEach {
            inkPaint.strokeWidth = it.key
            canvas.drawPath(it.value, inkPaint)
        }
    }

    protected fun drawStrokePoints(strokePoints: List<InkPoint>, canvas: Canvas, scaleFactor: Float) {
        val strokeWidth = getStrokeWidth()

        if (strokePoints.size == 1) {
            val x = strokePoints.first().x.toFloat() * scaleFactor
            val y = strokePoints.first().y.toFloat() * scaleFactor

            inkPaint.strokeWidth = strokePoints.first().mappedPressure.toFloat() * strokeWidth
            canvas.drawPoint(x, y, inkPaint)
        } else if (strokePoints.isNotEmpty()) {
            val strokesToDraw = mutableListOf<Pair<InkPoint, InkPoint>>()
            val pointsToDraw = mutableListOf<InkPoint>()

            for (i in 1 until strokePoints.size) {
                val currentPoint = strokePoints[i]
                val previousPoint = strokePoints[i - 1]

                if (currentPoint.x == previousPoint.x && currentPoint.y == previousPoint.y) {
                    pointsToDraw.add(currentPoint)
                } else {
                    strokesToDraw.add(Pair(previousPoint, currentPoint))
                }
            }

            val pointsToDrawGroupedByP = pointsToDraw.distinct().groupBy { it.mappedPressure }
            pointsToDrawGroupedByP.forEach {
                inkPaint.strokeWidth = it.key.toFloat() * strokeWidth
                val flatPoints = mutableListOf<Float>()
                it.value.forEach {
                    flatPoints.add(it.x.toFloat() * scaleFactor)
                    flatPoints.add(it.y.toFloat() * scaleFactor)
                }
                canvas.drawPoints(flatPoints.toFloatArray(), inkPaint)
            }

            val strokesToDrawGroupedByP = strokesToDraw.distinct().groupBy { it.second.mappedPressure }
            strokesToDrawGroupedByP.forEach {
                inkPaint.strokeWidth = it.key.toFloat() * strokeWidth
                val flatStrokes = mutableListOf<Float>()
                it.value.forEach {
                    flatStrokes.add(it.first.x.toFloat() * scaleFactor)
                    flatStrokes.add(it.first.y.toFloat() * scaleFactor)
                    flatStrokes.add(it.second.x.toFloat() * scaleFactor)
                    flatStrokes.add(it.second.y.toFloat() * scaleFactor)
                }
                canvas.drawLines(flatStrokes.toFloatArray(), inkPaint)
            }
        }
    }

    protected fun updatePathCache(stroke: Stroke) {
        pathCache.addStroke(stroke)
    }

    // Maintains Path objects for strokes mapped against different stroke widths
    class PathCache(private val callback: Callback) {
        private val pathCache = HashMap<Float, Path>()
        private var scaleFactor: Float = 1.0f
        private var strokeWidthFactor: Float = 1.0f
        private var isInvalid = true

        fun invalidate() {
            isInvalid = true
        }

        fun getPathMap(): Map<Float, Path> {
            if (isInvalid) {
                createPathCache()
            }
            return pathCache
        }

        private fun createPathCache() {
            scaleFactor = callback.getScaleFactor()
            strokeWidthFactor = callback.getStrokeWidthFactor()

            pathCache.clear()
            callback.getStrokeList().forEach { addStroke(it) }
            isInvalid = false
        }

        fun addStroke(stroke: Stroke) {
            if (stroke.points.size == 1) {
                val x = stroke.points.first().x.toFloat() * scaleFactor
                val y = stroke.points.first().y.toFloat() * scaleFactor
                val strokeWidth = stroke.points.first().mappedPressure.toFloat() * strokeWidthFactor

                val path = getPath(strokeWidth)
                path.moveTo(x, y)
                path.lineTo(x, y)
            } else if (stroke.points.isNotEmpty()) {
                var strokeWidth = stroke.points.first().mappedPressure.toFloat() * strokeWidthFactor
                var path = getPath(strokeWidth)
                path.moveTo(
                    stroke.points.first().x.toFloat() * scaleFactor,
                    stroke.points.first().y.toFloat() * scaleFactor
                )

                for (i in 1 until stroke.points.size) {
                    val currentPoint = stroke.points[i]
                    val previousPoint = stroke.points[i - 1]

                    if (previousPoint.mappedPressure != currentPoint.mappedPressure) {
                        strokeWidth = currentPoint.mappedPressure.toFloat() * strokeWidthFactor
                        path = getPath(strokeWidth)
                        path.moveTo(
                            previousPoint.x.toFloat() * scaleFactor,
                            previousPoint.y.toFloat() * scaleFactor
                        )
                    }

                    path.lineTo(currentPoint.x.toFloat() * scaleFactor, currentPoint.y.toFloat() * scaleFactor)
                }
            }
        }

        private fun getPath(strokeWidth: Float): Path {
            var path = pathCache[strokeWidth]
            if (path == null) {
                path = Path()
                pathCache[strokeWidth] = path
            }
            return path
        }

        interface Callback {
            fun getStrokeList(): List<Stroke>
            fun getScaleFactor(): Float
            fun getStrokeWidthFactor(): Float
        }
    }
}
