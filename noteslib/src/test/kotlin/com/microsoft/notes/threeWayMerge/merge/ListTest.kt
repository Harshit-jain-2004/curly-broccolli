package com.microsoft.notes.threeWayMerge.merge

import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ListTest {

    @Test
    fun should_remove_specified_indices_from_a_list() {
        var list = mutableListOf(1, 2, 3, 4, 5)
        list.removeAt(listOf(0, 4))
        assertThat(list, iz(listOf(2, 3, 4)))
    }
}
