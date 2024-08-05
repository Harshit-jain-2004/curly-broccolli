package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.richtext.scheme.Stroke
import com.microsoft.notes.threeWayMerge.Diff
import com.microsoft.notes.threeWayMerge.diff.StrokeDeletion
import com.microsoft.notes.threeWayMerge.diff.StrokeInsertion

fun merge(
    baseStrokes: List<Stroke>,
    primaryInkDiffs: MutableList<Diff>,
    secondaryInkDiffs: MutableList<Diff>
): List<Stroke> {
    if (primaryInkDiffs.isEmpty() && secondaryInkDiffs.isEmpty()) return baseStrokes

    val newStrokes = mutableListOf<Stroke>()
    val primaryDeletedStrokeIndices = sortedSetOf<Int>()
    val secondaryDeletedStrokeIndices = sortedSetOf<Int>()
    val primaryInsertedStrokeIndices = sortedSetOf<Int>()

    val primaryDeletedStrokeIds = primaryInkDiffs.mapNotNull {
            inkDiff ->
        if (inkDiff is StrokeDeletion) inkDiff.strokeId else null
    }.toHashSet()

    val secondaryDeletedStrokeIds = secondaryInkDiffs.mapNotNull {
            inkDiff ->
        if (inkDiff is StrokeDeletion) inkDiff.strokeId else null
    }.toHashSet()

    val primaryInsertedStrokeIds = primaryInkDiffs.mapNotNull {
            inkDiff ->
        if (inkDiff is StrokeInsertion) inkDiff.strokeId else null
    }.toHashSet()

    // apply deletions
    for (i in baseStrokes.indices) {
        val stroke = baseStrokes[i]

        if (primaryDeletedStrokeIds.contains(stroke.id)) {
            primaryDeletedStrokeIndices.add(i)
        } else if (secondaryDeletedStrokeIds.contains(stroke.id)) {
            secondaryDeletedStrokeIndices.add(i)
        } else {
            newStrokes.add(stroke)
        }
    }

    // apply primary insertions
    for (insertStroke in primaryInkDiffs.filterIsInstance<StrokeInsertion>()) {
        val modifiedIndex = adjustForDeletes(insertStroke.index, secondaryDeletedStrokeIndices)
        newStrokes.add(modifiedIndex, insertStroke.stroke)
        primaryInsertedStrokeIndices.add(modifiedIndex)
    }

    // apply secondary insertions
    var skippedStrokes = 0
    for (insertStroke in secondaryInkDiffs.filterIsInstance<StrokeInsertion>()) {
        if (primaryInsertedStrokeIds.contains(insertStroke.strokeId)) {
            skippedStrokes++
            continue
        }

        var modifiedIndex = adjustForDeletes(insertStroke.index, primaryDeletedStrokeIndices) - skippedStrokes
        modifiedIndex = adjustForInserts(modifiedIndex, primaryInsertedStrokeIndices)
        newStrokes.add(modifiedIndex, insertStroke.stroke)
    }

    return newStrokes.toList()
}

private fun adjustForInserts(startingIndex: Int, insertedIndices: Set<Int>, moveIfSameIndex: Boolean = true): Int {
    var newIndex = startingIndex
    for (insertedIndex in insertedIndices) {
        if (insertedIndex < newIndex || (moveIfSameIndex && newIndex == insertedIndex))
            newIndex++
        else break
    }
    return newIndex
}

private fun adjustForDeletes(startingIndex: Int, deletedIndices: Set<Int>): Int {
    var newIndex = startingIndex
    for (deletedIndex in deletedIndices) {
        if (deletedIndex < startingIndex) newIndex--
        else break
    }
    return newIndex
}
