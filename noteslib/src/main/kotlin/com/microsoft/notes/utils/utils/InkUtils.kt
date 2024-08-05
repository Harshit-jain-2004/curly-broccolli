package com.microsoft.notes.utils.utils

fun calculateScaleFactor(
    inkWidth: Float,
    inkHeight: Float,
    measuredWidth: Int,
    measuredHeight: Int,
    defaultScaleFactor: Float
): Float {
    val scaleForXFit = measuredWidth / inkWidth
    val scaleForYFit = measuredHeight / inkHeight
    return minOf(scaleForXFit, scaleForYFit, defaultScaleFactor)
}

// Calculates Offset needed to move the canvas so that
// the inkNote starts from the start of the view
fun calculateOffSetToMoveToOrigin(
    minCordOfInkPoints: Float?,
    scaleFactor: Float
): Float {
    if (minCordOfInkPoints == null)
        return 0f
    return -minCordOfInkPoints * scaleFactor
}

// Calculates Offset needed to center align the inkNote by moving the inkNote to start of view
// and distributing the remaining space equally on both sides
fun calculateOffsetToCenterAlign(
    minCordOfInkPoints: Float?,
    inkSize: Float?,
    measuredSize: Int,
    scaleFactor: Float
): Float {
    if (minCordOfInkPoints == null || inkSize == null)
        return 0f
    val inkSizeAfterScale = inkSize * scaleFactor
    val offsetToZero = calculateOffSetToMoveToOrigin(minCordOfInkPoints, scaleFactor)
    val offsetToCenterAlign = (measuredSize - inkSizeAfterScale) / 2
    return offsetToZero + offsetToCenterAlign
}
