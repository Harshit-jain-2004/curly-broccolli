package com.microsoft.notes.threeWayMerge

import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.scheme.NoteMetadata
import com.microsoft.notes.threeWayMerge.diff.MediaDeletion
import com.microsoft.notes.threeWayMerge.diff.MediaInsertion
import com.microsoft.notes.threeWayMerge.diff.MediaUpdateAltText
import com.microsoft.notes.threeWayMerge.diff.MediaUpdateImageDimensions
import com.microsoft.notes.threeWayMerge.diff.MediaUpdateLastModified
import com.microsoft.notes.threeWayMerge.diff.MediaUpdateLocalUrl
import com.microsoft.notes.threeWayMerge.diff.MediaUpdateMimeType
import com.microsoft.notes.threeWayMerge.diff.MediaUpdateRemoteId
import com.microsoft.notes.threeWayMerge.diff.inkDiff
import com.microsoft.notes.threeWayMerge.merge.SelectionBlockIds
import com.microsoft.notes.threeWayMerge.merge.SelectionFrom
import com.microsoft.notes.threeWayMerge.merge.SelectionInfo
import com.microsoft.notes.threeWayMerge.merge.merge
import java.util.Comparator

data class DiffIndex(
    val updateColor: ColorUpdate?,
    val blockUpdates: MutableMap<String, BlockUpdate>,
    val blockDeletes: MutableMap<String, BlockDeletion>,
    val spanInserts: MutableMap<String, SpanInsertion>,
    val mediaDeletes: MutableMap<String, MediaDeletion>,
    val mediaInserts: MutableMap<String, MediaInsertion>,
    val mediaUpdateRemoteId: MutableMap<String, MediaUpdateRemoteId>,
    val mediaUpdateLocalUrl: MutableMap<String, MediaUpdateLocalUrl>,
    val mediaUpdateMimeType: MutableMap<String, MediaUpdateMimeType>,
    val mediaUpdateAltText: MutableMap<String, MediaUpdateAltText>,
    val mediaUpdateImageDimensions: MutableMap<String, MediaUpdateImageDimensions>,
    val mediaUpdateLastModified: MutableMap<String, MediaUpdateLastModified>
)

@Suppress("UnsafeCast")
internal fun List<Diff>.sorted(): List<Diff> {
    val comparator = Comparator<Diff> { a, b ->
        val isABlockTextDeletion = a is BlockTextDeletion
        val isBBlockTextDeletion = b is BlockTextDeletion
        val isASpanDeletion = a is SpanDeletion
        val isBSpanDeletion = b is SpanDeletion

        if (isABlockTextDeletion && !isBBlockTextDeletion) -1
        else if (!isABlockTextDeletion && isBBlockTextDeletion) 1
        else if (isABlockTextDeletion && isBBlockTextDeletion)
            (b as BlockTextDeletion).start - (a as BlockTextDeletion).start
        else if (isASpanDeletion && !isBSpanDeletion) -1
        else if (!isASpanDeletion && isBSpanDeletion) 1
        else if (isASpanDeletion && isBSpanDeletion)
            (b as SpanDeletion).span.start - (a as SpanDeletion).span.start
        else 0
    }
    return sortedWith(comparator)
}

internal fun List<Diff>.diffIndex(): DiffIndex {
    var diffIndex = DiffIndex(
        updateColor = null, blockUpdates = mutableMapOf(),
        blockDeletes = mutableMapOf(), spanInserts = mutableMapOf(),
        mediaInserts = mutableMapOf(), mediaDeletes = mutableMapOf(),
        mediaUpdateRemoteId = mutableMapOf(), mediaUpdateLocalUrl = mutableMapOf(),
        mediaUpdateMimeType = mutableMapOf(), mediaUpdateAltText = mutableMapOf(),
        mediaUpdateImageDimensions = mutableMapOf(), mediaUpdateLastModified = mutableMapOf()
    )

    forEach { diff ->
        when (diff) {
            is ColorUpdate -> diffIndex = diffIndex.copy(updateColor = diff)
            is BlockUpdate -> diffIndex.blockUpdates.put(diff.block.localId, diff)
            is BlockDeletion -> diffIndex.blockDeletes.put(diff.blockId, diff)
            is SpanInsertion -> diffIndex.spanInserts.put(diff.blockId, diff)
            is MediaInsertion -> diffIndex.mediaInserts.put(diff.localId, diff)
            is MediaDeletion -> diffIndex.mediaDeletes.put(diff.localId, diff)
            is MediaUpdateRemoteId -> diffIndex.mediaUpdateRemoteId.put(diff.localId, diff)
            is MediaUpdateLocalUrl -> diffIndex.mediaUpdateLocalUrl.put(diff.localId, diff)
            is MediaUpdateMimeType -> diffIndex.mediaUpdateMimeType.put(diff.localId, diff)
            is MediaUpdateAltText -> diffIndex.mediaUpdateAltText.put(diff.localId, diff)
            is MediaUpdateImageDimensions -> diffIndex.mediaUpdateImageDimensions.put(diff.localId, diff)
            is MediaUpdateLastModified -> diffIndex.mediaUpdateLastModified.put(diff.localId, diff)
        }
    }
    return diffIndex
}

internal fun DiffIndex.mergeUpdates(secondary: DiffIndex): List<Diff> {
    val diffs = mutableListOf<Diff>()
    // ignore left-hand updates if deleted or right-hand block is updated
    secondary.blockUpdates.forEach { (id, value) ->
        if (blockDeletes[id] != null && blockUpdates[id] != null && spanInserts[id] != null) {
            diffs.add(value)
        }
    }
    // accept all right-hand block updates
    diffs.addAll(blockUpdates.values)

    return diffs
}

/**
 * Merges two sets of diffs into one. Prefers primary changes on collisions.
 */
internal fun List<Diff>.mergeDiffs(secondary: List<Diff>): List<Diff> {
    val diffs = mutableListOf<Diff>()
    val primaryIndex = this.diffIndex()
    val secondaryIndex = secondary.diffIndex()

    val color = primaryIndex.updateColor ?: secondaryIndex.updateColor
    color?.let { diffs.add(it) }

    diffs.addAll(primaryIndex.mergeUpdates(secondaryIndex))
    return diffs
}

internal fun Note.applyBlockUpdates(blockUpdates: List<BlockUpdate>): Note {
    var note = this
    blockUpdates.forEach { blockUpdate ->
        note = note.copy(
            document = note.document.copy(
                blocks = note.document.blocks.map {
                    if (it.localId == blockUpdate.block.localId) {
                        blockUpdate.block
                    } else {
                        it
                    }
                }
            )
        )
    }
    return note
}

/**
 * Applies content diffs to a note
 */
internal fun Note.patch(diffs: List<Diff>): Note {
    var note = this
    val diffIndex = diffs.diffIndex()
    diffIndex.updateColor?.let {
        note = note.copy(color = it.color)
    }
    note = note.applyBlockUpdates(diffIndex.blockUpdates.values.toList())

    return note
}

internal fun Note.indexToId(index: Int): String? {
    return if (index in 0 until this.document.blocks.size) {
        document.blocks[index].localId
    } else {
        null
    }
}

internal fun Note.selectionInfo(secondary: Note, selectionFrom: SelectionFrom): SelectionInfo {
    val selectionSource = if (selectionFrom == SelectionFrom.PRIMARY) this else secondary
    val wantedSelection = selectionSource.document.range

    return SelectionInfo(
        selection = wantedSelection,
        selectionIds = SelectionBlockIds(
            startBlockId = selectionSource.indexToId(wantedSelection.startBlock),
            endBlockId = selectionSource.indexToId(wantedSelection.endBlock)
        ),
        selectionFrom = selectionFrom
    )
}

fun threeWayMerge(
    base: Note,
    primary: Note,
    secondary: Note,
    selectionFrom: SelectionFrom = SelectionFrom.PRIMARY
): Note {
    if (base.isRenderedInkNote && primary.isRenderedInkNote && secondary.isRenderedInkNote) {
        return primary
    }

    if (base.isInkNote && primary.isInkNote && secondary.isInkNote) {
        val primaryInkDiffs = inkDiff(base, primary)
        val secondaryInkDiffs = inkDiff(base, secondary)

        var colorUpdate = primaryInkDiffs.filterIsInstance<ColorUpdate>().firstOrNull()
        if (colorUpdate == null) {
            colorUpdate = secondaryInkDiffs.filterIsInstance<ColorUpdate>().firstOrNull()
        }

        val updatedColor = colorUpdate?.color ?: base.color
        val updatedStrokes = merge(
            baseStrokes = base.document.strokes,
            primaryInkDiffs = primaryInkDiffs.toMutableList(),
            secondaryInkDiffs = secondaryInkDiffs.toMutableList()
        )
        val updatedDocument = base.document.copy(strokes = updatedStrokes)

        return base.copy(
            color = updatedColor,
            document = updatedDocument,
            documentModifiedAt = maxOf(primary.documentModifiedAt, secondary.documentModifiedAt)
        )
    }

    val primaryDiffs = diff(base, primary).sorted()
    val secondaryDiffs = diff(base, secondary).sorted()

    // document merge
    val selectionInfo = primary.selectionInfo(secondary, selectionFrom)
    val updatedDocument = merge(
        base = base.document,
        selectionInfo = selectionInfo,
        primary = primaryDiffs.toMutableList(),
        secondary = secondaryDiffs.toMutableList()
    )

    val updatedMedia = merge(base = base.media, primary = primaryDiffs.toMutableList(), secondary = secondaryDiffs.toMutableList())

    // note attribute merge
    val noteDiffs = primaryDiffs.mergeDiffs(secondaryDiffs)

    // metadata merge
    val updatedMetadata = mergeMetadata(primary.metadata, secondary.metadata)

    val documentModifiedAt = maxOf(primary.documentModifiedAt, secondary.documentModifiedAt)
    return base.copy(document = updatedDocument)
        .patch(noteDiffs)
        .copy(
            documentModifiedAt = documentModifiedAt,
            media = updatedMedia,
            metadata = updatedMetadata
        )
}

// currently we will choose whoever has a non-null context among primary and secondary
// if two non-null contexts favor the primary; context deletion not supported
fun mergeMetadata(primary: NoteMetadata, secondary: NoteMetadata): NoteMetadata {
    val mergedContext = primary.context ?: secondary.context
    val mergedReminder = primary.reminder ?: secondary.reminder
    return NoteMetadata(context = mergedContext, reminder = mergedReminder)
}

fun noteWithNewType(base: Note, primary: Note, secondary: Note): Note {
    val different = if (primary.document.type != base.document.type) { primary } else { secondary }
    return different.copy(uiShadow = different)
}

fun canThreeWayMerge(base: Note, primary: Note, secondary: Note): Boolean {
    val isPrimarySame = primary.document.type == base.document.type
    val isSecondarySame = secondary.document.type == base.document.type
    return isPrimarySame && isSecondarySame
}
