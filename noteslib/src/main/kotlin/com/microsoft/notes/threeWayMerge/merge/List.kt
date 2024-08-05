package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.threeWayMerge.Range

internal fun <T> MutableList<T>.removeAt(indices: List<Int>) {
    val sortedIndices = indices.sorted().toMutableList()
    var removedCount = 0
    sortedIndices.forEachIndexed { _, index ->
        val position = index - removedCount
        this.removeAt(position)
        removedCount += 1
    }
}

internal fun MutableList<Int>.insertRange(range: Range) {
    this += range.start..range.end
}
