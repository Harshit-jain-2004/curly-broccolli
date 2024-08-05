package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.threeWayMerge.BlockDeletion
import com.microsoft.notes.threeWayMerge.BlockTextDeletion
import com.microsoft.notes.threeWayMerge.BlockTextInsertion
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ContentTest {

    @Test
    fun should_have_merge_Secondary_strategy_given_no_text_diffs_in_primary() {
        val primaryDiffs = mutableListOf(BlockDeletion(blockId = "bar"))
        val secondaryDiffs = mutableListOf(BlockTextDeletion(start = 0, end = 1, blockId = "foobar"))
        val result = mergeStrategy(primaryDiffs, secondaryDiffs)

        // It should return Secondary strategy
        assertThat(result, iz(MergeStrategy.SECONDARY))
    }

    @Test
    fun should_have_merge_Both_strategy_given_no_text_diffs_whatsoever() {
        val primaryDiffs = mutableListOf(BlockDeletion(blockId = "foo"))
        val secondaryDiffs = mutableListOf(BlockDeletion(blockId = "bar"))
        val result = mergeStrategy(primaryDiffs, secondaryDiffs)

        // It should return Both strategy
        assertThat(result, iz(MergeStrategy.BOTH))
    }

    @Test
    fun should_have_merge_Both_strategy_given_only_one_type_of_text_diffs_in_primary_and_secondary() {
        val primaryDiffs = mutableListOf(BlockTextDeletion(start = 0, end = 1, blockId = "foobar"))
        val secondaryDiffs = mutableListOf(BlockTextDeletion(start = 4, end = 5, blockId = "foobar"))
        val result = mergeStrategy(primaryDiffs, secondaryDiffs)

        // It should return Both strategy
        assertThat(result, iz(MergeStrategy.BOTH))
    }

    @Test
    fun should_have_merge_Both_strategy_given_two_types_of_text_diffs_in_primary_and_secondary() {
        val primaryDiffs = mutableListOf(BlockTextDeletion(start = 0, end = 1, blockId = "foobar"))
        val secondaryDiffs = mutableListOf(BlockTextInsertion(blockId = "foobar", index = 1, text = "hello"))
        val result = mergeStrategy(primaryDiffs, secondaryDiffs)

        // It should return Both strategy
        assertThat(result, iz(MergeStrategy.PRIMARY))
    }

    @Test
    fun should_have_merge_Primary_strategy_given_several_text_diffs_in_both() {
        val primaryDiffs = mutableListOf(
            BlockTextDeletion(start = 0, end = 1, blockId = "foobar"),
            BlockTextInsertion(text = "foo", index = 5, blockId = "foobar"),
            BlockTextInsertion(text = "bar", index = 10, blockId = "foobar")
        )
        val secondaryDiffs = mutableListOf(
            BlockTextDeletion(start = 0, end = 3, blockId = "foobar"),
            BlockTextInsertion(text = "bar", index = 4, blockId = "foobar"),
            BlockTextInsertion(text = "foo", index = 15, blockId = "foobar")
        )
        val result = mergeStrategy(primaryDiffs, secondaryDiffs)

        // It should return Primary strategy
        assertThat(result, iz(MergeStrategy.PRIMARY))
    }
}
