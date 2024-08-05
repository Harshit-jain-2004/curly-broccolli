package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.threeWayMerge.Range
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class RangeTest {

    @Test
    fun should_say_when_range_is_contained() {
        val TESTS = listOf(
            Triple(Range(4, 6), Range(4, 6), true),
            Triple(Range(4, 6), Range(5, 6), true),
            Triple(Range(4, 6), Range(3, 4), false),
            Triple(Range(3, 6), Range(3, 4), true),
            Triple(Range(3, 6), Range(4, 5), true),
            Triple(Range(3, 6), Range(4, 7), false),
            Triple(Range(3, 6), Range(5, 6), true),
            Triple(Range(3, 6), Range(0, 3), false),
            Triple(Range(3, 6), Range(2, 3), false),
            Triple(Range(3, 6), Range(2, 4), false),
            Triple(Range(3, 6), Range(2, 12), false),
            Triple(Range(3, 6), Range(5, 7), false),
            Triple(Range(3, 6), Range(6, 7), false),
            Triple(Range(3, 6), Range(10, 12), false)
        )

        TESTS.forEach { test ->
            val container = test.first
            val other = test.second
            assertThat(container.contains(other), iz(test.third))
        }
    }

    @Test
    fun should_return_boolean_if_range_intersects_with_indices() {
        val TESTS = listOf(
            Triple(Range(4, 6), listOf(0, 5, 7), true),
            Triple(Range(4, 6), listOf(0), false),
            Triple(Range(0, 5), listOf(7, 8, 9), false)
        )

        TESTS.forEach { test ->
            val range = test.first
            val indices = test.second
            assertThat(range.containsAny(indices), iz(test.third))
        }
    }

    @Test
    fun should_convert_a_range_to_a_list_of_indices() {
        val TESTS = listOf(
            Pair(Range(0, 0), emptyList()),
            Pair(Range(0, 1), listOf(0)),
            Pair(Range(4, 6), listOf(4, 5)),
            Pair(Range(4, 5), listOf(4)),
            Pair(Range(5, 15), listOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 14))
        )

        TESTS.forEach { test ->
            val result = test.first.indices()
            assertThat(result, iz(test.second))
        }
    }

    @Test
    fun should_convert_a_set_of_indices_into_a_list_of_ranges() {
        val TESTS = listOf(
            Pair(setOf(0, 1, 3, 4, 5, 8, 9), listOf(Range(0, 2), Range(3, 6), Range(8, 10))),
            Pair(setOf(0, 1, 2, 5), listOf(Range(0, 3), Range(5, 6)))
        )

        TESTS.forEach { test ->
            val result: List<Range> = test.first.toRanges()
            assertThat(result, iz(test.second))
        }
    }

    @Test
    fun should_remove_range() {
        val TARGET = Range(5, 15)
        val TESTS = listOf(
            Triple(TARGET, Range(0, 4), TARGET),
            Triple(TARGET, Range(0, 5), TARGET),
            Triple(TARGET, Range(15, 20), TARGET),
            Triple(TARGET, Range(5, 10), Range(10, 15)),
            Triple(TARGET, Range(10, 15), Range(5, 10)),
            Triple(TARGET, Range(0, 20), Range(0, 0)),
            Triple(TARGET, Range(10, 12), Range(5, 10))
        )

        TESTS.forEach { test ->
            val result = test.first.remove(test.second)
            assertThat(result, iz(test.third))
        }
    }

    @Test
    fun should_modify_range_by_previously_inserted_indices() {
        val start = 5
        val end = 10
        val previouslyInsertedIndices = listOf(1, 2, 3, 4, 5, 6, 7)
        val result = rangeModifiedByInserts(start, end, previouslyInsertedIndices)

        assertThat(result.start, iz(9))
        assertThat(result.end, iz(14))
    }

    @Test
    fun should_modify_range_by_previously_deleted_indices() {
        val start = 5
        val end = 10
        val previouslyDeletedIndices = listOf(1, 2, 3, 4, 5, 6, 7)
        val result = rangeModifiedByDeletes(start, end, previouslyDeletedIndices)

        assertThat(result.start, iz(1))
        assertThat(result.end, iz(6))
    }
}
