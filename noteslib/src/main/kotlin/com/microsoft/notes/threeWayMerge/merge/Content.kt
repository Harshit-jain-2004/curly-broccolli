package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.threeWayMerge.BlockTextDeletion
import com.microsoft.notes.threeWayMerge.BlockTextInsertion
import com.microsoft.notes.threeWayMerge.Diff
import com.microsoft.notes.threeWayMerge.TextDiffOperation

enum class MergeStrategy { BOTH, PRIMARY, SECONDARY }
data class ContentMerge(val content: Content, val selectionStart: Int?, val selectionEnd: Int?)

@Suppress("UnsafeCast")
internal fun List<Diff>.textDiffTypes(): List<TextDiffOperation> {
    val types = mutableListOf<TextDiffOperation>()
    for (operation in this) {
        if (types.size == 2) break

        when (operation) {
            is BlockTextInsertion, is BlockTextDeletion -> {
                val textOperation = operation as TextDiffOperation
                if (!types.contains(textOperation)) {
                    types.add(textOperation)
                }
            }
        }
    }

    return types
}

fun mergeStrategy(primary: List<Diff>, secondary: List<Diff>): MergeStrategy {
    val primaryTypes = primary.textDiffTypes()
    val secondaryTypes = secondary.textDiffTypes()
    val primaryLength = primaryTypes.size
    val secondaryLength = secondaryTypes.size

    return if (primaryLength == 0 && secondaryLength > 0) {
        MergeStrategy.SECONDARY
    } else if (primaryLength == 0 && secondaryLength == 0) {
        MergeStrategy.BOTH
    } else if (primaryLength == 1 && secondaryLength == 1 &&
        primaryTypes[0]::class == secondaryTypes[0]::class
    ) {
        MergeStrategy.BOTH
    } else {
        MergeStrategy.PRIMARY
    }
}

fun updateSelection(start: Int?, end: Int?, deleteIndices: List<Int>, insertedIndices: List<Int>): Pair<Int?, Int?> {
    var newSelectionStart: Int? = null
    var newSelectionEnd: Int? = null

    start?.apply {
        val offset = newOffsetModifiedByDeletes(
            offset = start,
            previouslyDeletedIndices = deleteIndices, isText = true
        )
        newSelectionStart = newOffsetModifiedByInserts(
            offset = offset,
            previouslyInsertedIndices = insertedIndices, isText = true
        )
    }
    end?.let {
        val offset = newOffsetModifiedByDeletes(
            offset = end,
            previouslyDeletedIndices = deleteIndices, isText = true
        )
        newSelectionEnd = newOffsetModifiedByInserts(
            offset = offset,
            previouslyInsertedIndices = insertedIndices, isText = true
        )
    }

    return Pair(newSelectionStart, newSelectionEnd)
}

fun bothStrategy(
    content: Content,
    primary: MutableList<Diff>,
    secondary: MutableList<Diff>,
    selectionStart: Int?,
    selectionEnd: Int?
): ContentMerge {
    var newText = content.text
    var newSpans = content.spans

    // delete text
    val (newTextAfterApplyDeletesOnce, primaryDeletedIndices) =
        applyTextDeletes(diffs = primary, base = newText)
    val (newTextAfterApplyDeletesTwice, secondaryDeletedIndices) =
        applyTextDeletes(
            diffs = secondary, base = newTextAfterApplyDeletesOnce,
            previouslyDeletedIndices = primaryDeletedIndices
        )

    // insert text
    val (newTextAfterApplyInsertsOnce, primaryInsertedIndices) =
        applyTextInserts(
            diffs = primary, base = newTextAfterApplyDeletesTwice,
            previouslyInsertedIndices = secondaryDeletedIndices
        )
    val (newTextAfterApplyInsertTwice, secondaryInsertedIndices) =
        applyTextInserts(
            diffs = secondary, base = newTextAfterApplyInsertsOnce,
            previouslyInsertedIndices = primaryInsertedIndices,
            previouslyDeletedIndices = primaryDeletedIndices
        )
    newText = newTextAfterApplyInsertTwice

    // delete spans
    newSpans = applySpanDeletes(primary, newSpans)
    newSpans = applySpanDeletes(secondary, newSpans)

    // insert spans

    newSpans = applySpanInserts(
        diffs = primary, spans = newSpans,
        previouslyInsertedIndices = secondaryInsertedIndices,
        previouslyDeletedIndices = secondaryDeletedIndices
    )
    newSpans = applySpanInserts(
        diffs = secondary, spans = newSpans,
        previouslyInsertedIndices = primaryInsertedIndices,
        previouslyDeletedIndices = primaryDeletedIndices
    )
    // update selection
    val newSelection = updateSelection(
        start = selectionStart, end = selectionEnd,
        deleteIndices = secondaryDeletedIndices,
        insertedIndices = secondaryInsertedIndices
    )

    val newContent = content.copyAndNormalizeSpans(text = newText, spans = newSpans)

    return ContentMerge(
        content = newContent, selectionStart = newSelection.first,
        selectionEnd = newSelection.second
    )
}

fun basicStrategy(
    content: Content,
    diffs: MutableList<Diff>,
    selectionStart: Int?,
    selectionEnd: Int?,
    modifySelection: Boolean
): ContentMerge {
    var newText = content.text
    var newSpans = content.spans
    val applyTextDeletes: Pair<String, List<Int>>
    val applyTextInserts: Pair<String, List<Int>>

    applyTextDeletes = applyTextDeletes(diffs, newText)
    applyTextInserts = applyTextInserts(diffs, applyTextDeletes.first)
    newText = applyTextInserts.first

    newSpans = applySpanDeletes(diffs, newSpans)
    newSpans = applySpanInserts(diffs, newSpans)

    val newContent = content.copyAndNormalizeSpans(text = newText, spans = newSpans)

    return if (modifySelection) {
        val newSelection = updateSelection(
            selectionStart, selectionEnd, deleteIndices = applyTextDeletes.second,
            insertedIndices = applyTextInserts.second
        )
        ContentMerge(
            content = newContent, selectionStart = newSelection.first,
            selectionEnd = newSelection.second
        )
    } else {
        ContentMerge(content = newContent, selectionStart = selectionStart, selectionEnd = selectionEnd)
    }
}

fun merge(
    content: Content,
    primary: MutableList<Diff>,
    secondary: MutableList<Diff>,
    selectionStart: Int?,
    selectionEnd: Int?,
    selectionFrom: SelectionFrom
): ContentMerge {

    return when (mergeStrategy(primary, secondary)) {
        MergeStrategy.BOTH ->
            bothStrategy(content, primary, secondary, selectionStart, selectionEnd)
        MergeStrategy.PRIMARY ->
            basicStrategy(
                content, primary, selectionStart, selectionEnd,
                selectionFrom == SelectionFrom.SECONDARY
            )
        MergeStrategy.SECONDARY -> {
            basicStrategy(
                content, secondary, selectionStart, selectionEnd,
                selectionFrom == SelectionFrom.PRIMARY
            )
        }
    }
}
