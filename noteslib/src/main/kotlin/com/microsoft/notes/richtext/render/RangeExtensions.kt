package com.microsoft.notes.richtext.render

import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.GlobalRange
import com.microsoft.notes.richtext.scheme.Range

fun Range.toGlobalRange(document: Document): GlobalRange {
    var globalStart = -1
    var globalEnd = -1
    var currentOffset = 0
    for (index in document.blocks.indices) {
        if (index == startBlock)
            globalStart = currentOffset + startOffset
        if (index == endBlock) {
            globalEnd = currentOffset + endOffset
            break
        }

        currentOffset += document.blocks[index].cursorPlaces()
    }

    if (globalStart < 0)
        globalStart = currentOffset - 1
    if (globalEnd < 0)
        globalEnd = currentOffset - 1

    return GlobalRange(globalStart, globalEnd)
}
