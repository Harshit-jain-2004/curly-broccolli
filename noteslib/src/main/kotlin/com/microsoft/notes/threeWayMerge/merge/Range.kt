package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.threeWayMerge.Range

/**
 * Returns true if any number inside of [numbers] is contained in the current [Range]
 *
 * @receiver [Range]
 * @param numbers [List]<[Int]> that contains numbers to check in the current [Range].
 * @return true if any number is in the current [Range] false otherwise.
 */
internal fun Range.containsAny(numbers: List<Int>): Boolean {
    numbers.forEach { number ->
        if (number in start..end) {
            return true
        }
    }
    return false
}

/**
 * Returns true if the [other] is contained in the current [Range].
 * Note: [other] has to be fully contained not just a part of it.
 *
 * ie:
 *      current Range: (4, 6)
 *      other: (4, 6)
 *      result: true
 *
 *      current Range: (3, 6)
 *      other: (5, 7)
 *      result : false
 *
 * @receiver [Range]
 * @param other the given [Range] to check.
 * @result true if is contained, otherwise false.
 */
internal fun Range.contains(other: Range): Boolean = this.start <= other.start && other.end <= this.end

/**
 * Returns a [List]<[Int]> with all the indices that the current [Range] contains.
 * The check is exclusive regarding the end of the [Range]], so it means we don't include the end of the [Range]
 * in the result.
 *
 * ie:
 *      current Range: (4, 6)
 *      result: listOf(4, 5) --> we exclude the end of the range (6)
 *
 *      current Range: (0, 5)
 *      result: listOf(0, 1, 2, 3, 4) --> we exclude the end of the range (5)
 *
 * @receiver [Range]
 * @return [List]<[Int]> with the different indices (positions) from the [Range]
 */
internal fun Range.indices(): List<Int> {
    val indices = mutableListOf<Int>()
    val (rangeStart, rangeEnd) = this
    indices += rangeStart until rangeEnd
    return indices
}

/**
 * Given a [Set]<[Int]> we return a [List]<[Range]> with the different [Range] we can build given the current
 * [Set]. Since end of the [Range] is exclusive we have to increase in one the end of the [Range].
 * ie:
 *      current Set: setOf(0, 1, 3, 4, 5, 8, 9)
 *      result: (0, 2), (3, 6), (8, 10)
 *
 *      current Set: setOf(0, 1, 2, 5)
 *      result: (0, 3), (5, 6)
 *
 * @receiver [Set]<[Int]>
 * @result [List]<[Range]> with the generated ranges given the current [Set]
 */
internal fun Set<Int>.toRanges(): List<Range> {
    val sorted = this.sorted()
    val ranges = mutableListOf<Range>()
    var startAndLastSeen: Pair<Int, Int>? = null

    for (n in sorted) {
        startAndLastSeen = when (startAndLastSeen) {
            null -> Pair(n, n)
            else -> {
                val (start, lastSeen) = startAndLastSeen
                if (lastSeen != n - 1) {
                    ranges.add(Range(start, lastSeen + 1))
                    Pair(n, n)
                } else {
                    Pair(start, n)
                }
            }
        }
    }
    if (startAndLastSeen != null) {
        ranges.add(Range(startAndLastSeen.first, startAndLastSeen.second + 1))
    }

    return ranges
}

/**
 * Returns a new [Range] from the current [Range] given a [remove] [Range].
 *
 * ie:
 *      current Range: (5, 15)
 *      remove: (0, 4)
 *      result: (5, 15) --> remove is not contained in the current range
 *
 *      current Range: (5, 15)
 *      remove: (3, 6)
 *      result: (6, 15) --> remove is just contained at the beginning of the current range.
 *
 *      current Range: (5, 15)
 *      remove: (10, 12)
 *      result: (5, 10) --> See that we just have one new Range that dismisses the rest of the range (12, 15)
 *
 * @receiver [Range]
 * @param remove the [Range] to be removed from the current [Range]
 * @result a new [Range] where [remove] has been removed from current [Range] if found.
 */
internal fun Range.remove(remove: Range): Range {
    val (rangeStart, rangeEnd) = this
    val (removeStart, removeEnd) = remove
    if (rangeEnd < removeStart) return this
    if (rangeStart >= removeEnd) return this

    if (this.contains(remove) && rangeStart != removeStart) {
        return this.remove(Range(removeStart, rangeEnd + removeEnd))
    }

    val indices = HashSet(this.indices())
    val removeIndices = HashSet(remove.indices())
    val afterSubtraction = indices.subtract(removeIndices).sorted()
    val start = afterSubtraction.firstOrNull() ?: 0
    val end = start + afterSubtraction.size
    return Range(start, end)
}

/**
 * Return a [Range] where its [start] and [end] have been modified by [previouslyInsertedIndices].
 *
 * ie:
 *      start: 5
 *      end: 10
 *      previouslyInsertedIndices: listOf(1, 2, 3, 4, 5, 6, 7)
 *      result: Range(9, 14)
 *      We had 4 indices inserted before or start offset (1, 2, 3, 4) so we have increased in 4 our start and end.
 *
 * @param start
 * @param end
 * @param previouslyInsertedIndices indices that were inserted previously
 * @return [Range] with start and end based in given [start] and [end] plus [previouslyInsertedIndices]
 */
internal fun rangeModifiedByInserts(start: Int, end: Int, previouslyInsertedIndices: List<Int>): Range {
    val sorted = previouslyInsertedIndices.sorted()
    var adjustment = 0

    run loop@{
        sorted.forEachIndexed { _, index ->
            if (index < start) {
                adjustment += 1
            } else {
                return@loop
            }
        }
    }
    return Range(start + adjustment, end + adjustment)
}

/**
 * Return a [Range] where its [start] and [end] have been modified by [previouslyDeletedIndices].
 *
 * ie:
 *      start: 5
 *      end: 10
 *      previouslyDeletedIndices: listOf(1, 2, 3, 4, 5, 6, 7)
 *      result: Range(1, 6)
 *      We had 4 indices deleted before or start offset (1, 2, 3, 4) so we have decreased in 4 our start and end.
 *
 * @param start
 * @param end
 * @param previouslyDeletedIndices indices that were deleted previously
 * @return [Range] with start and end based in given [start] and [end] plus [previouslyDeletedIndices]
 */
internal fun rangeModifiedByDeletes(start: Int, end: Int, previouslyDeletedIndices: List<Int>): Range {
    val adjustment = previouslyDeletedIndices.count { it < start }
    return Range(start - adjustment, end - adjustment)
}
