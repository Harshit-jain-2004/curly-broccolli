package com.microsoft.notes.models.extensions

import com.microsoft.notes.models.ImageDimensions
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.NoteReferenceMedia

fun List<Media>.updateMediaWithRemoteId(mediaLocalId: String, mediaRemoteId: String): List<Media> {
    return findAndMap(
        find = { it.localId == mediaLocalId },
        map = {
            it.copy(remoteId = mediaRemoteId)
        }
    )
}

fun List<Media>.updateMediaWithLocalUrl(mediaRemoteId: String, localUrl: String, mimeType: String): List<Media> {
    return findAndMap(
        find = { it.remoteId == mediaRemoteId },
        map = {
            it.copy(localUrl = localUrl, mimeType = mimeType)
        }
    )
}

fun List<NoteReferenceMedia>.updateNoteReferenceMediaWithLocalUrl(mediaId: String, localUrl: String): List<NoteReferenceMedia> {
    return findAndMap(
        find = { it.mediaID == mediaId },
        map = {
            it.copy(localImageUrl = localUrl)
        }
    )
}

fun Media.updateRemoteId(remoteId: String): Media = copy(remoteId = remoteId)

fun Media.updateLocalUrl(localUrl: String): Media = copy(localUrl = localUrl)

fun Media.updateMimeType(mimeType: String): Media = copy(mimeType = mimeType)

fun Media.updateAltText(altText: String?): Media = copy(altText = altText)

fun Media.updateImageDimensions(imageDimensions: ImageDimensions): Media = copy(imageDimensions = imageDimensions)

fun Media.updateLastModified(lastModified: Long): Media = copy(lastModified = lastModified)

fun List<Media>.sortedByLastModified(): List<Media> = sortedBy { it.lastModified }.reversed()
