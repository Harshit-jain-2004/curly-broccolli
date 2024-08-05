package com.microsoft.notes.richtext.scheme

import java.io.Serializable
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

enum class DocumentType {
    RICH_TEXT,

    SAMSUNG_NOTE,

    /**
     * Rendered ink is passed from the API as a PNG
     */
    RENDERED_INK,
    /**
     * Ink is passed from the API as a strokes array
     */
    INK,
    FUTURE
}

data class Range(
    val startBlock: Int = 0,
    val startOffset: Int = 0,
    val endBlock: Int = 0,
    val endOffset: Int = 0
) : Serializable {

    constructor(startBlock: Int, startOffset: Int) : this(startBlock, startOffset, startBlock, startOffset)

    val isSingleBlock: Boolean
        get() = startBlock == endBlock

    val isCollapsed: Boolean
        get() = isSingleBlock && startOffset == endOffset

    fun collapseToStart(): Range = copy(endBlock = startBlock, endOffset = startOffset)

    fun collapseToEnd(): Range = copy(startBlock = endBlock, startOffset = endOffset)
}

data class GlobalRange(val startOffset: Int = 0, val endOffset: Int = 0)

data class Document(
    val blocks: List<Block> = emptyList(),
    val strokes: List<Stroke> = emptyList(),
    val range: Range = Range(),
    val type: DocumentType = DocumentType.RICH_TEXT,
    val body: String = "",
    val bodyPreview: String = "",
    val dataVersion: String = ""
) : Serializable {
    val readOnly: Boolean
        get() {
            return when (type) {
                DocumentType.RICH_TEXT -> false
                DocumentType.RENDERED_INK -> true
                DocumentType.INK -> true
                DocumentType.FUTURE -> true
                DocumentType.SAMSUNG_NOTE -> true
            }
        }

    val isRenderedInkDocument: Boolean
        get() = type == DocumentType.RENDERED_INK

    val isInkDocument: Boolean
        get() = type == DocumentType.INK

    val isSamsungNoteDocument: Boolean
        get() = type == DocumentType.SAMSUNG_NOTE

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            val map = (json as? Map<String, Any> ?: return json).toMutableMap()

            if (old < 2 && new >= 2) {
                val blocks: List<Map<String, Any>> = map.get(
                    "blocks"
                ) as? List<Map<String, Any>> ?: emptyList()
                val migrated_blocks = blocks.map { block: Map<String, Any> ->
                    Block.migrate(block, old, new)
                }
                map["blocks"] = migrated_blocks
            }

            return map
        }
    }
}

data class Stroke(val id: String, val points: List<InkPoint>) : Serializable {
    companion object {
        fun migrate(json: Any, old: Int, new: Int): Any = json
    }
}

data class InkPoint(val x: Double, val y: Double, val p: Double) : Serializable {
    companion object {
        fun migrate(json: Any, old: Int, new: Int): Any = json
    }

    /* MotionEvent pressure values can range from 0 (no pressure) to 1 (max pressure).
      This can result in having infinite stroke widths.
      We will map pressure values to a finite set for rendering.
    */
    val mappedPressure: Double = p
        get() = when {
            // lowest value is set as STYLUS_MIN_STROKE_PRESSURE
            field <= 0.3f -> 0.3
            field <= 0.4f -> 0.4
            field <= 0.5f -> 0.5
            field <= 0.6f -> 0.6
            field <= 0.7f -> 0.7
            field <= 0.8f -> 0.8
            field <= 0.9f -> 0.9
            else -> 1.0
        }
}

fun InkPoint.scaleDown(scaleFactor: Float): InkPoint =
    InkPoint(x / scaleFactor, y / scaleFactor, p)

fun InkPoint.scaleUp(scaleFactor: Float): InkPoint =
    InkPoint(x * scaleFactor, y * scaleFactor, p)

fun InkPoint.distanceTo(otherPoint: InkPoint): Double {
    val xDiff = abs(otherPoint.x - this.x)
    val yDiff = abs(otherPoint.y - this.y)
    return sqrt(xDiff.pow(2) + yDiff.pow(2))
}
