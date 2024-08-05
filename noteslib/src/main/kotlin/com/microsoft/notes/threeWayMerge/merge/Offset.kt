package com.microsoft.notes.threeWayMerge.merge

/**
 * Returns a new offset value [Int] given how many items ([previouslyDeletedIndices])
 * where deleted before the given [offset], ie:
 *      offset: 5
 *      previouslyDeletedIndices: listOf(0, 4, 7, 10)
 *      result: 3
 *      Since offset was 5 we have 2 items deleted before it (0 and 4) so the new offset is 3 (5 - 2 items)
 *
 * @param offset the offset where we will start counting from
 * @param previouslyDeletedIndices a [List]<[Int]> containing previously deleted indices
 * @param isText [true] if is a text from [Content] or a [Span]
 */
internal fun newOffsetModifiedByDeletes(
    offset: Int,
    previouslyDeletedIndices: List<Int>,
    isText: Boolean = false
): Int {
    val adjustment = indicesBeforeOffset(offset, previouslyDeletedIndices, isText)
    return offset - adjustment
}

/**
 * Returns a new offset value [Int] given how many items ([previouslyInsertedIndices]) where inserted before the
 * given [offset]
 * ie:
 *      offset: 5
 *      previouslyDeletedIndices: listOf(0, 2, 10)
 *      result: 7
 *      Since offset was 5 we have 2 items inserted before it (0 and 2) so the new offset is 7 (5 + 2 items)
 *
 * @param offset the offset where we will start counting from
 * @param previouslyInsertedIndices a [List]<[Int]> containing previously inserted indices
 * @param isText [true] if is a text from [Content] or a [Span]
 */
internal fun newOffsetModifiedByInserts(
    offset: Int,
    previouslyInsertedIndices: List<Int>,
    isText: Boolean = false
): Int {
    val adjustment = indicesBeforeOffset(offset, previouslyInsertedIndices, isText)
    return offset + adjustment
}

/**
 * We count how many [values] we have before the given [offset].
 * @param offset the index where we will start counting from
 * @param values number of values (inserted or deleted) we have.
 * @param isText true if is a text from [com.microsoft.notes.richtext.scheme.Content] false if otherwise.
 * @result the number [Int] of items that are before the given offset.
 */
private fun indicesBeforeOffset(offset: Int, values: List<Int>, isText: Boolean): Int {
    val valuesBeforeSorted = values.sorted()
    var numberLessThanOffset = 0

    run loop@{
        for (value in valuesBeforeSorted) {
            numberLessThanOffset += when (isText) {
                // TODO: Discuss with raymond why we need to check for > 0 but web does not
                true -> if (value <= offset && offset > 0) 1 else return@loop
                false -> if (value < offset) 1 else return@loop
            }
        }
    }

    return numberLessThanOffset
}
