package com.microsoft.notes.threeWayMerge.diff

import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.scheme.Stroke
import com.microsoft.notes.threeWayMerge.ColorUpdate
import com.microsoft.notes.threeWayMerge.Diff
import com.microsoft.notes.threeWayMerge.idMap

sealed class StrokeDiff : Diff() {
    abstract val strokeId: String
}

data class StrokeDeletion(override val strokeId: String) : StrokeDiff()
data class StrokeInsertion(override val strokeId: String, val stroke: Stroke, val index: Int) : StrokeDiff()

/**
 * Given a list of strokes, returns a mapping of stroke IDs to the strokes themselves
 */
fun idToStrokeMap(strokes: List<Stroke>): Map<String, Stroke> {
    val id = { stroke: Stroke -> stroke.id }
    return idMap(strokes, id)
}

fun inkDiff(base: Note, target: Note): List<Diff> {
    val diffs = mutableListOf<Diff>()

    val baseStrokes = base.document.strokes
    val targetStrokes = target.document.strokes

    val baseIdToStrokeMap = idToStrokeMap(baseStrokes)
    val targetIdToStrokeMap = idToStrokeMap(targetStrokes)

    if (base.color != target.color) diffs.add(ColorUpdate(target.color))

    baseIdToStrokeMap.keys.forEach {
        if (!targetIdToStrokeMap.containsKey(it)) diffs.add(StrokeDeletion(it))
    }

    targetStrokes.forEachIndexed { index, targetStroke ->
        if (baseIdToStrokeMap[targetStroke.id] == null) {
            diffs.add(StrokeInsertion(targetStroke.id, targetStroke, index))
        }
    }

    return diffs
}
