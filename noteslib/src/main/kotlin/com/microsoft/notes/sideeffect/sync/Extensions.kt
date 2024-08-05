package com.microsoft.notes.sideeffect.sync

import com.microsoft.notes.ui.extensions.isSamsungNote
import com.microsoft.notes.models.Media as StoreMedia
import com.microsoft.notes.models.Note as StoreNote

internal fun List<StoreNote>.processMediaNeedingDownload(
    enqueueDownload: (note: StoreNote, mediaRemoteId: String, mimeType: String) -> Unit
) {
    for (note in this) {
        val mediaNeedDownload = note.mediaNeedingDownload()
        mediaNeedDownload.forEach {
            val remoteId = it.remoteId ?: return
            enqueueDownload(note, remoteId, it.mimeType)
        }
    }
}

const val SAMSUNG_PREVIEW_IMAGE_LOCAL_ID = "samsungpreview"

internal fun StoreNote.mediaNeedingDownload(): List<StoreMedia> =
    if (isSamsungNote()) {
        // As of now media with id "samsungpreview" is always the first in the list
        // Re-download every time there is a modification, despite localUrl != null
        // because preview image indicates note content for samsung notes
        listOfNotNull(this.media.getSamsungMediaForPreviewImage()) +
            this.media.filter {
                it.localId != SAMSUNG_PREVIEW_IMAGE_LOCAL_ID &&
                    it.localUrl == null && it.remoteId != null
            }
    } else {
        this.media.filter { it.localUrl == null && it.remoteId != null }
    }

fun List<StoreMedia>.getSamsungMediaForPreviewImage() = firstOrNull { it.localId == SAMSUNG_PREVIEW_IMAGE_LOCAL_ID }

fun List<StoreMedia>.getSamsungAttachedImagesForHTMLNote() = filter { it.localId != SAMSUNG_PREVIEW_IMAGE_LOCAL_ID && it.hasRemoteId }
