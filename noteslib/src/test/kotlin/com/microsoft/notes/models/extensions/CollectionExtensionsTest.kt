package com.microsoft.notes.models.extensions

import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class CollectionExtensionsTest {

    @Test
    fun should_return_first() {
        val dummyList = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val element = dummyList.firstOrDefault(predicate = { it % 4 == 0 }, default = 10)

        assertThat(element, iz(4))
    }

    @Test
    fun should_return_default() {
        val dummyList = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val element = dummyList.firstOrDefault(predicate = { it % 12 == 0 }, default = 10)

        assertThat(element, iz(10))
    }

    @Test
    fun should_find_and_map() {
        val dummyList = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val dummyListMapped = dummyList.findAndMap(find = { it % 2 == 0 }, map = { it * 10 })

        assertThat(dummyListMapped, CoreMatchers.hasItem(20))
    }

    @Test
    fun should_not_find_and_not_map() {
        val dummyList = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val dummyListMapped = dummyList.findAndMap(find = { it % 12 == 0 }, map = { it * 10 })

        assertThat(dummyListMapped, not(CoreMatchers.hasItem(20)))
    }
}
