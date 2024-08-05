package com.microsoft.notes.threeWayMerge.diff

import com.microsoft.notes.models.ImageDimensions
import com.microsoft.notes.models.Media
import com.microsoft.notes.threeWayMerge.Diff
import com.microsoft.notes.threeWayMerge.idMap

sealed class MediaDiff : Diff() {
    abstract val localId: String
}

data class MediaDeletion(override val localId: String) : MediaDiff()
data class MediaInsertion(override val localId: String, val media: Media, val index: Int) : MediaDiff()
data class MediaUpdateRemoteId(override val localId: String, val remoteId: String) : MediaDiff()
data class MediaUpdateLocalUrl(override val localId: String, val localUrl: String) : MediaDiff()
data class MediaUpdateMimeType(override val localId: String, val mimeType: String) : MediaDiff()
data class MediaUpdateAltText(override val localId: String, val altText: String?) : MediaDiff()
data class MediaUpdateLastModified(override val localId: String, val lastModified: Long) : MediaDiff()
data class MediaUpdateImageDimensions(
    override val localId: String,
    val imageDimensions: ImageDimensions
) : MediaDiff()

/**
 * It will return a [Map]<[String], [Media]> with tuples of ->  <Media localid, Media itself>.
 * @param media the list of [Media] that will be used to map into [Map<String, Media>]
 * @result [Map<String, Media>] being [String] the localId of the [Media] and [Media] the [Media] itself.
 */
fun idToMediaMap(media: List<Media>): Map<String, Media> {
    val id = { item: Media -> item.localId }
    return idMap(media, id)
}

internal fun MutableList<Diff>.toMapByMediaLocalId(): Map<String, MutableList<Diff>> {
    val mapped = mutableMapOf<String, MutableList<Diff>>()
    this.forEach { diff ->
        val localId = when (diff) {
            is MediaDiff -> diff.localId
            else -> null
        }
        localId?.let {
            val listOfDiffs = mapped[it] ?: mutableListOf()
            listOfDiffs.add(diff)
            mapped[it] = listOfDiffs
        }
    }
    return mapped
}

fun mediaDiffs(base: Media, target: Media): List<Diff> {
    val diffs = mutableListOf<Diff>()
    if (base != target) {
        // always keep remoteId
        if (!base.hasRemoteId && target.hasRemoteId) {
            target.remoteId?.let {
                diffs.add(MediaUpdateRemoteId(base.localId, it))
            }
        }
        // always keep localUrl
        if (!base.hasLocalUrl && target.hasLocalUrl) {
            target.localUrl?.let {
                diffs.add(MediaUpdateLocalUrl(base.localId, it))
            }
        }
        // always keep image dimensions
        if (!base.hasImageDimensions && target.hasImageDimensions) {
            target.imageDimensions?.let {
                diffs.add(MediaUpdateImageDimensions(base.localId, it))
            }
        }

        if (base.mimeType != target.mimeType) {
            diffs.add(MediaUpdateMimeType(base.localId, target.mimeType))
        }

        if (base.altText != target.altText) {
            diffs.add(MediaUpdateAltText(base.localId, target.altText))
        }

        if (base.lastModified < target.lastModified) {
            diffs.add(MediaUpdateLastModified(base.localId, target.lastModified))
        }
    }
    return diffs
}
