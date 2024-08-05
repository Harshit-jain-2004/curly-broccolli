package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.models.Media
import com.microsoft.notes.models.extensions.updateAltText
import com.microsoft.notes.models.extensions.updateImageDimensions
import com.microsoft.notes.models.extensions.updateLastModified
import com.microsoft.notes.models.extensions.updateLocalUrl
import com.microsoft.notes.models.extensions.updateMimeType
import com.microsoft.notes.models.extensions.updateRemoteId
import com.microsoft.notes.threeWayMerge.Diff
import com.microsoft.notes.threeWayMerge.DiffIndex
import com.microsoft.notes.threeWayMerge.diff.MediaDeletion
import com.microsoft.notes.threeWayMerge.diff.MediaInsertion
import com.microsoft.notes.threeWayMerge.diff.MediaUpdateAltText
import com.microsoft.notes.threeWayMerge.diff.MediaUpdateImageDimensions
import com.microsoft.notes.threeWayMerge.diff.MediaUpdateLastModified
import com.microsoft.notes.threeWayMerge.diff.MediaUpdateLocalUrl
import com.microsoft.notes.threeWayMerge.diff.MediaUpdateMimeType
import com.microsoft.notes.threeWayMerge.diff.MediaUpdateRemoteId
import com.microsoft.notes.threeWayMerge.diff.toMapByMediaLocalId
import com.microsoft.notes.threeWayMerge.diffIndex

fun Media.applyUpdateLastModified(diff: MediaUpdateLastModified): Media = updateLastModified(diff.lastModified)

@Suppress("UnsafeCast")
fun Media.mergeUpdateLastModifiedDiffs(primary: MediaUpdateLastModified?, secondary: MediaUpdateLastModified?): Media {
    if (primary != null) {
        return applyUpdateLastModified(primary)
    }
    if (secondary != null) {
        return applyUpdateLastModified(secondary)
    }
    return this
}

fun Media.applyUpdateImageDimensions(diff: MediaUpdateImageDimensions): Media =
    updateImageDimensions(diff.imageDimensions)

@Suppress("UnsafeCast")
fun Media.mergeUpdateImageDimensionsDiffs(primary: MediaUpdateImageDimensions?, secondary: MediaUpdateImageDimensions?): Media {
    if (primary != null) {
        return applyUpdateImageDimensions(primary)
    }
    if (secondary != null) {
        return applyUpdateImageDimensions(secondary)
    }
    return this
}

fun Media.applyUpdateAltText(diff: MediaUpdateAltText): Media = updateAltText(diff.altText)

@Suppress("UnsafeCast")
fun Media.mergeUpdateAltTextDiffs(primary: MediaUpdateAltText?, secondary: MediaUpdateAltText?): Media {
    if (primary != null) {
        return applyUpdateAltText(primary)
    }
    if (secondary != null) {
        return applyUpdateAltText(secondary)
    }
    return this
}

fun Media.applyUpdateMimeType(diff: MediaUpdateMimeType): Media = updateMimeType(diff.mimeType)

@Suppress("UnsafeCast")
fun Media.mergeUpdateMimeTypeDiffs(primary: MediaUpdateMimeType?, secondary: MediaUpdateMimeType?): Media {
    if (primary != null) {
        return applyUpdateMimeType(primary)
    }
    if (secondary != null) {
        return applyUpdateMimeType(secondary)
    }
    return this
}

fun Media.applyUpdateLocalUrlDiffs(diff: MediaUpdateLocalUrl): Media = updateLocalUrl(diff.localUrl)

@Suppress("UnsafeCast")
fun Media.mergeUpdateLocalUrlDiffs(primary: MediaUpdateLocalUrl?, secondary: MediaUpdateLocalUrl?): Media {
    if (primary != null) {
        return applyUpdateLocalUrlDiffs(primary)
    }
    if (secondary != null) {
        return applyUpdateLocalUrlDiffs(secondary)
    }
    return this
}

fun Media.applyUpdateRemoteIdDiff(diff: MediaUpdateRemoteId): Media = updateRemoteId(diff.remoteId)

@Suppress("UnsafeCast")
fun Media.mergeUpdateRemoteIdDiffs(primary: MediaUpdateRemoteId?, secondary: MediaUpdateRemoteId?): Media {
    if (primary != null) {
        return applyUpdateRemoteIdDiff(primary)
    }
    if (secondary != null) {
        return applyUpdateRemoteIdDiff(secondary)
    }
    return this
}

fun List<Media>.applyMediaDeletion(mediaDeletion: MediaDeletion): List<Media> =
    filter { item -> item.localId != mediaDeletion.localId }

internal fun List<Media>.applyMediaInserts(diffs: List<Diff>, previouslyDeletedIndices: List<Int>): List<Media> {

    fun numberOfIndicesBelowValue(list: List<Int>, value: Int): Int =
        // In web they do a slice with an empty parameter, that returns the same, don't know exactly why
        list.filter { it < value }.size

    var newMedia = this
    for (diff in diffs) {
        if (diff is MediaInsertion) {
            val numberDeletedBefore = numberOfIndicesBelowValue(previouslyDeletedIndices, diff.index)
            val insertionIndex = diff.index - numberDeletedBefore
            val mutableMedia = newMedia.toMutableList()
            mutableMedia.add(insertionIndex, diff.media)
            newMedia = mutableMedia.toList()
        }
    }
    return newMedia
}

@Suppress("UnsafeCast")
fun List<Diff>.getDeleteMediaIndex(): Int = indexOfFirst { it is MediaDeletion }

fun merge(media: Media, primary: DiffIndex, secondary: DiffIndex): Media {
    return media
        .mergeUpdateRemoteIdDiffs(primary.mediaUpdateRemoteId.get(media.localId), secondary.mediaUpdateRemoteId.get(media.localId))
        .mergeUpdateLocalUrlDiffs(primary.mediaUpdateLocalUrl.get(media.localId), secondary.mediaUpdateLocalUrl.get(media.localId))
        .mergeUpdateMimeTypeDiffs(primary.mediaUpdateMimeType.get(media.localId), secondary.mediaUpdateMimeType.get(media.localId))
        .mergeUpdateAltTextDiffs(primary.mediaUpdateAltText.get(media.localId), secondary.mediaUpdateAltText.get(media.localId))
        .mergeUpdateImageDimensionsDiffs(primary.mediaUpdateImageDimensions.get(media.localId), secondary.mediaUpdateImageDimensions.get(media.localId))
        .mergeUpdateLastModifiedDiffs(primary.mediaUpdateLastModified.get(media.localId), secondary.mediaUpdateLastModified.get(media.localId))
}

internal fun MutableList<Diff>.removeDuplicatedMediaInserts(toCompare: List<Diff>): List<Int> {
    val removedBaseIndices = mutableListOf<Int>()
    toCompare.forEach { primaryDiff ->
        if (primaryDiff is MediaInsertion) {
            this.removeAll { secondaryDiff ->
                if (secondaryDiff is MediaInsertion &&
                    secondaryDiff.localId == primaryDiff.localId
                ) {
                    removedBaseIndices.add(secondaryDiff.index)
                    true
                } else {
                    false
                }
            }
        }
    }

    return removedBaseIndices
}

fun merge(base: List<Media>, primary: MutableList<Diff>, secondary: MutableList<Diff>): List<Media> {
    var newMedia = base
    val primaryByLocalId = primary.toMapByMediaLocalId()
    val secondaryByLocalId = secondary.toMapByMediaLocalId()
    val primaryDiffIndex = primary.diffIndex()
    val secondaryDiffIndex = secondary.diffIndex()
    val primaryDeletedIndices = mutableListOf<Int>()
    val secondaryDeletedIndices = mutableListOf<Int>()

    // apply deletions
    base.forEachIndexed loop@{ i, media ->
        val primaryDiffs = primaryByLocalId[media.localId] ?: mutableListOf()
        val secondaryDiffs = secondaryByLocalId[media.localId] ?: mutableListOf()

        val changes = primaryDiffs.isNotEmpty() || secondaryDiffs.isNotEmpty()
        if (!changes) {
            return@loop
        }

        // media deleted in primary
        val primaryMediaDeletion = primaryDiffIndex.mediaDeletes.get(media.localId)
        primaryMediaDeletion?.let {
            primaryDeletedIndices.add(i)
            newMedia = newMedia.applyMediaDeletion(primaryMediaDeletion)
            return@loop
        }

        // media deleted in secondary
        val secondaryMediaDeletionIndex = secondaryDiffs.getDeleteMediaIndex()
        if (secondaryMediaDeletionIndex != -1) {
            val secondaryMediaDeletion = secondaryDiffs[secondaryMediaDeletionIndex] as MediaDeletion
            if (primaryDiffs.isEmpty()) {
                secondaryDeletedIndices.add(i)
                newMedia = newMedia.applyMediaDeletion(secondaryMediaDeletion)
                return@loop
            } else {
                secondaryDiffs.removeAt(secondaryMediaDeletionIndex)
            }
        }
    }

    // apply updates
    newMedia = newMedia.map {
        merge(it, primaryDiffIndex, secondaryDiffIndex)
    }

    // apply inserts
    val removedDiffIndices = secondary.removeDuplicatedMediaInserts(toCompare = primary)
    newMedia = newMedia.applyMediaInserts(
        diffs = secondary,
        previouslyDeletedIndices = primaryDeletedIndices + removedDiffIndices
    )
    newMedia = newMedia.applyMediaInserts(
        diffs = primary,
        previouslyDeletedIndices = secondaryDeletedIndices
    )

    return newMedia
}
