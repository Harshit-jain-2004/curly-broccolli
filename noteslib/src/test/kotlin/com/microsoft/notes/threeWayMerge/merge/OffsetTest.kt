package com.microsoft.notes.threeWayMerge.merge

import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class OffsetTest {

    @Test
    fun should_lessen_the_offset_by_the_number_of_indices_deleted_before_it() {
        val deletedIndices = listOf(0, 4, 7, 10)
        val newStart = newOffsetModifiedByDeletes(5, deletedIndices)
        assertThat(newStart, iz(3))
    }

    @Test
    fun should_increase_the_offset_by_the_number_of_indices_inserted_before_it() {
        val insertedIndices = listOf(0, 2, 10)
        val newStart = newOffsetModifiedByInserts(5, insertedIndices)
        assertThat(newStart, iz(7))
    }
}
