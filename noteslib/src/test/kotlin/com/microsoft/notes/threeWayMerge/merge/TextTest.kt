package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.threeWayMerge.BlockTextDeletion
import com.microsoft.notes.threeWayMerge.BlockTextInsertion
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class TextTest {

    @Test
    fun should_apply_text_inserts_given_simplest_case() {
        val base = "the cat"
        val insertDiff = BlockTextInsertion("blockId", " black", 3)
        val (textResult, indicesResult) = applyTextInserts(mutableListOf(insertDiff), base)
        assertThat(textResult, iz("the black cat"))
        assertThat(indicesResult, iz(listOf(3, 4, 5, 6, 7, 8)))
    }

    @Test
    fun should_apply_text_inserts_given_previous_applied_insert() {
        val base = "the black cat eats"
        val previousInserts = listOf(3, 4, 5, 6, 7, 8)
        val insertDiff = BlockTextInsertion("blockId", " fish", 12)
        val (textResult, indicesResult) = applyTextInserts(
            diffs = mutableListOf(insertDiff), base = base, previouslyInsertedIndices = previousInserts
        )
        assertThat(textResult, iz("the black cat eats fish"))
        assertThat(indicesResult, iz(listOf(18, 19, 20, 21, 22)))
    }

    @Test
    fun should_apply_text_inserts_given_previous_applied_delete() {
        val base = "the cat eats"
        val previousDeletes = listOf(3, 4, 5, 6, 7, 8)
        val insertDiff = BlockTextInsertion("blockId", " fish", 18)
        val (textResult, indicesResult) = applyTextInserts(
            diffs = mutableListOf(insertDiff), base = base, previouslyDeletedIndices = previousDeletes
        )
        assertThat(textResult, iz("the cat eats fish"))
        assertThat(indicesResult, iz(listOf(12, 13, 14, 15, 16)))
    }

    @Test
    fun should_apply_text_inserts_given_previous_bad_applied_insert() {
        val base = "the black cat eats"
        val previousInserts = listOf(3, 4, 5, 6, 7, 8)
        val insertDiff = BlockTextInsertion("blockId", " fish", 13)
        val (textResult, indicesResult) = applyTextInserts(
            diffs = mutableListOf(insertDiff), base = base, previouslyInsertedIndices = previousInserts
        )
        assertThat(textResult, iz("the black cat eats"))
        assertThat(indicesResult, iz(listOf()))
    }

    @Test
    fun should_not_apply_text_inserts_given_previous_bad_applied_inserts() {
        val base = "the cat eats"
        val previousInserts = listOf(3, 3, 4, 4, 5, 5, 6, 6, 7, 8)
        val insertDiff = BlockTextInsertion("blockId", " fish", 18)
        val (textResult, indicesResult) = applyTextInserts(
            diffs = mutableListOf(insertDiff), base = base, previouslyInsertedIndices = previousInserts
        )
        assertThat(textResult, iz("the cat eats"))
        assertThat(indicesResult, iz(listOf()))
    }

    @Test
    fun should_not_apply_text_inserts_given_invalid_block_text_insertion_index() {
        val base = "the cat eats"
        val previousInserts = listOf(3)
        val insertDiff = BlockTextInsertion("blockId", " fish", -1)
        val (textResult, indicesResult) = applyTextInserts(
            diffs = mutableListOf(insertDiff), base = base, previouslyInsertedIndices = previousInserts
        )
        assertThat(textResult, iz("the cat eats"))
        assertThat(indicesResult, iz(listOf()))
    }

    @Test
    fun should_not_apply_text_inserts_given_invalid_block_text_insertion_index_exceeds_string_length() {
        val base = "the cat eats"
        val previousInserts = listOf(3)
        val insertDiff = BlockTextInsertion("blockId", " fish", base.length)
        val (textResult, indicesResult) = applyTextInserts(
            diffs = mutableListOf(insertDiff), base = base, previouslyInsertedIndices = previousInserts
        )
        assertThat(textResult, iz("the cat eats"))
        assertThat(indicesResult, iz(listOf()))
    }

    @Test
    fun should_delete_text_given_simplest_case() {
        // Delete "black" from "the black cat"
        val base = "the black cat"
        val deleteDiff = BlockTextDeletion(blockId = "blockId", start = 4, end = 9)
        val (textResult, indicesResult) = applyTextDeletes(diffs = mutableListOf(deleteDiff), base = base)
        assertThat(textResult, iz("the cat"))
        assertThat(indicesResult, iz(listOf(4, 5, 6, 7, 8, 9)))
    }

    @Test
    fun should_delete_text_given_previous_applied_deletes() {
        // Delete " cat" from "black cat"
        val base = "black cat"
        val previousDeletes = listOf(0, 1, 2, 3)
        val deleteDiff = BlockTextDeletion(blockId = "blockId", start = 9, end = 12)
        val (textResult, indicesResult) = applyTextDeletes(
            diffs = mutableListOf(deleteDiff), base = base,
            previouslyDeletedIndices = previousDeletes
        )
        assertThat(textResult, iz("black"))
        assertThat(indicesResult, iz(listOf(5, 6, 7, 8)))
    }

    @Test
    fun should_delete_text_given_previous_multiple_deletes() {
        // Delete "the " and " cat" from "the black cat"
        val base = "the black cat"
        val deleteDiff1 = BlockTextDeletion(blockId = "blockId", start = 0, end = 3)
        val deleteDiff2 = BlockTextDeletion(blockId = "blockId", start = 9, end = 12)
        val (textResult, indicesResult) = applyTextDeletes(
            diffs = mutableListOf(deleteDiff2, deleteDiff1),
            base = base
        )
        assertThat(textResult, iz("black"))
        assertThat(indicesResult, iz(listOf(9, 10, 11, 12, 0, 1, 2, 3)))
    }

    @Test
    fun should_not_delete_text_given_previous_bad_delete() {
        val base = "the black cat"
        val badDeleteDiff = BlockTextDeletion(blockId = "blockId", start = 0, end = 20)
        val (textResult, indicesResult) = applyTextDeletes(
            diffs = mutableListOf(badDeleteDiff),
            base = base
        )
        assertThat(textResult, iz("the black cat"))
        assertThat(indicesResult, iz(listOf()))
    }

    @Test
    fun should_not_delete_text_given_previous_bad_delete_invalid_end_index() {
        val base = "the black cat"
        val badDeleteDiff = BlockTextDeletion(blockId = "blockId", start = 12, end = 13)
        val (textResult, indicesResult) = applyTextDeletes(
            diffs = mutableListOf(badDeleteDiff),
            base = base
        )
        assertThat(textResult, iz("the black cat"))
        assertThat(indicesResult, iz(listOf()))
    }

    @Test
    fun should_not_delete_text_given_previous_bad_delete_invalid_start_index() {
        val base = "the black cat"
        val badDeleteDiff = BlockTextDeletion(blockId = "blockId", start = -1, end = 0)
        val (textResult, indicesResult) = applyTextDeletes(
            diffs = mutableListOf(badDeleteDiff),
            base = base
        )
        assertThat(textResult, iz("the black cat"))
        assertThat(indicesResult, iz(listOf()))
    }

    @Test
    fun should_not_delete_text_given_previous_bad_delete_invalid_delete_length() {
        val base = "the black cat"
        val badDeleteDiff = BlockTextDeletion(blockId = "blockId", start = 0, end = base.length + 1)
        val (textResult, indicesResult) = applyTextDeletes(
            diffs = mutableListOf(badDeleteDiff),
            base = base
        )
        assertThat(textResult, iz("the black cat"))
        assertThat(indicesResult, iz(listOf()))
    }

    @Test
    fun should_not_delete_text_given_previous_bad_delete_first_index() {
        val base = "the black cat"
        val badDeleteDiff = BlockTextDeletion(blockId = "blockId", start = -1, end = base.length + 1)
        val (textResult, indicesResult) = applyTextDeletes(
            diffs = mutableListOf(badDeleteDiff),
            base = base
        )
        assertThat(textResult, iz("the black cat"))
        assertThat(indicesResult, iz(listOf()))
    }
}
