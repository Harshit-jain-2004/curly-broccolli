package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.threeWayMerge.BlockTextDeletion
import com.microsoft.notes.threeWayMerge.BlockTextInsertion
import com.microsoft.notes.threeWayMerge.Diff
import com.microsoft.notes.threeWayMerge.Range

/**
 * Returns a [Pair]<[String], [List]<[Int]>> with the text and the indices where the text will be inserted.
 * <We apply [diffs] to [base]. Diffs have to be [BlockTextInsertion] or will be no used.
 * We have [previouslyDeletedIndices] and [previouslyInsertedIndices] so offset are modified regarding these
 * two parameters.
 *
 * ie:
 *      diffs: listOf(BlockTextInsertion("blockId", " black", 3))
 *      base: "the cat"
 *      previouslyDeletedIndices: emptyList()
 *      previouslyInsertedIndices: emptyList()
 *      result: Pair("the black cat", listOf(3, 4, 5, 6, 7, 8))
 *
 *      We have applied a [BlockTextInsertion] with the text " black" into our [base], since [BlockTextInsertion]
 *      has its index as 3 we are going to insert its text in the position 3 of base, so the resulted text will be
 *      "the black cat" and the list of indices where " black" is going to be inserted is (3, 4, 5, 6, 7, 8).
 *
 *      diffs: listOf(BlockTextInsertion("blockId", " fish", 12))
 *      base: "the black cat eats"
 *      previouslyDeletedIndices: emptyList()
 *      previouslyInsertedIndices: listOf(3, 4, 5, 6, 7, 8)
 *      result: Pair("the black cat eats fish", listOf(18, 19, 20, 21, 22))
 *      Our [BlockTextInsertion] said we should insert fish in the position 12, but we have
 *      [previouslyInsertedIndices] with (3, 4, 5, 6, 7, 8) since all of these indices are below the position
 *      where we want to insert "fish" (12) we have to move the index insertion 6 positions (3, 4, 5, 6, 7, 8)
 *      so then we will insert the text "fish" in the position 18 and its list of indices will be (18, 19, 20,
 *      21, 22), having the final result we have.
 *
 * @param diffs
 * @param base
 * @param previouslyDeletedIndices
 * @param previouslyInsertedIndices
 * @return [Pair]<[String], [List]<[Int]>> with the new result text after insertion and the indices affected
 * where we inserted the new text.
 */
fun applyTextInserts(
    diffs: MutableList<Diff>,
    base: String,
    previouslyDeletedIndices: List<Int> = emptyList(),
    previouslyInsertedIndices: List<Int> = emptyList()
): Pair<String, List<Int>> {

    val newInsertedIndices = mutableListOf<Int>()
    val diffIndicesToDelete = mutableListOf<Int>()
    var newText = base

    diffs.forEachIndexed loop@{ index, diff ->
        if (diff !is BlockTextInsertion) return@loop

        val (_, textToInsert, location) = diff
        val (deleteModifiedStart, _) = rangeModifiedByDeletes(location, location, previouslyDeletedIndices)
        val deleteModifiedEnd = deleteModifiedStart + textToInsert.length - 1
        if (Range(deleteModifiedStart, deleteModifiedEnd).containsAny(previouslyInsertedIndices)) {
            diffIndicesToDelete.add(index)
            return@loop
        }
        val (insertModifiedStart, _) = rangeModifiedByInserts(
            deleteModifiedStart, deleteModifiedEnd,
            previouslyInsertedIndices
        )

        // We don't know the root cause of this.
        // This is a safety check
        if (insertModifiedStart in 0..newText.length) {
            newText = newText.substring(0, insertModifiedStart) + diff.text + newText.substring(insertModifiedStart)
            val insertModifiedEnd = insertModifiedStart + textToInsert.length - 1
            newInsertedIndices.insertRange(Range(insertModifiedStart, insertModifiedEnd))
            diffIndicesToDelete.add(index)
        }
    }

    diffs.removeAt(diffIndicesToDelete)
    return Pair(newText, newInsertedIndices)
}

/**
 * Returns a [Pair]<[String], [List]<[Int]>> with the text and the indices where the text will be deleted.
 * <We apply [diffs] to [base]. Diffs have to be [BlockTextDeletion] or will be no used.
 * We have [previouslyDeletedIndices] so offset are modified regarding this parameters.
 *
 * ie:
 *      diffs: listOf(BlockTextDeletion(blockId = "blockId", start = 4, end = 9))
 *      base: "the black cat"
 *      previouslyDeletedIndices: emptyList()
 *      result: Pair("the cat", listOf(4, 5, 6, 7, 8, 9))
 *
 *      We have applied a [BlockTextInsertion] with the text " black" into our [base], since [BlockTextInsertion]
 *      has its index as 3 we are going to insert its text in the position 3 of base, so the resulted text will be
 *      "the black cat" and the list of indices where " black" is going to be inserted is (3, 4, 5, 6, 7, 8).
 *
 *      diffs: listOf(BlockTextInsertion("blockId", " fish", 12))
 *      base: "the black cat eats"
 *      previouslyDeletedIndices: emptyList()
 *      previouslyInsertedIndices: listOf(3, 4, 5, 6, 7, 8)
 *      result: Pair("the black cat eats fish", listOf(18, 19, 20, 21, 22))
 *      Our [BlockTextInsertion] said we should insert fish in the position 12, but we have
 *      [previouslyInsertedIndices] with (3, 4, 5, 6, 7, 8) since all of these indices are below the position
 *      where we want to insert "fish" (12) we have to move the index insertion 6 positions (3, 4, 5, 6, 7, 8)
 *      so then we will insert the text "fish" in the position 18 and its list of indices will be (18, 19, 20,
 *      21, 22), having the final result we have.
 *
 * @param diffs
 * @param base
 * @param previouslyDeletedIndices
 * @param previouslyInsertedIndices
 * @return [Pair]<[String], [List]<[Int]>> with the new result text after insertion and the indices affected
 * where we inserted the new text.
 */
fun applyTextDeletes(
    diffs: MutableList<Diff>,
    base: String,
    previouslyDeletedIndices: List<Int> = emptyList()
): Pair<String, List<Int>> {
    val newDeletedIndices = mutableListOf<Int>()
    val diffIndicesToDelete = mutableListOf<Int>()
    var newText = base
    diffs.forEachIndexed loop@{ index, diff ->
        if (diff !is BlockTextDeletion) return@loop

        val (_, start, end) = diff
        if (Range(start, end).containsAny(previouslyDeletedIndices)) {
            diffIndicesToDelete.add(index)
            return@loop
        }
        val (modifiedDeletedRangeStart, modifiedDeletedRangeEnd) = rangeModifiedByDeletes(
            start, end, previouslyDeletedIndices
        )

        // This is a safety check to avoid apps to crash.
        if (modifiedDeletedRangeEnd in 0 until newText.length && modifiedDeletedRangeStart >= 0) {
            newText = newText.substring(0, modifiedDeletedRangeStart) + newText.substring(modifiedDeletedRangeEnd + 1)
            newDeletedIndices.insertRange(Range(modifiedDeletedRangeStart, modifiedDeletedRangeEnd))
            diffIndicesToDelete.add(index)
        }
    }
    diffs.removeAt(diffIndicesToDelete)
    return Pair(newText, newDeletedIndices)
}
