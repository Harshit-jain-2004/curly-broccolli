package com.microsoft.notes.sync

import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidDeleteMedia
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidDeleteNote
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidUpdateMediaAltText
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidUpdateNote
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidUploadMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.CreateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteNoteReference
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteSamsungNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DownloadMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.GetNoteForMerge
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.NoteReferencesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.SamsungNotesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.Sync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateMediaAltText
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UploadMedia
import com.microsoft.notes.sync.models.Token

fun squash(queue: PriorityQueue, latestItem: ApiRequestOperation) {
    when (latestItem) {
        is Sync -> squashReads(queue, latestItem)
        is NoteReferencesSync -> squashReads(queue, latestItem)
        is SamsungNotesSync -> squashReads(queue, latestItem)
        is UpdateNote -> squashUpdates(queue, latestItem)
        is DeleteNote -> squashWritesOnDelete(queue, latestItem)
        is DeleteNoteReference -> squashWritesOnNoteReferenceDelete(queue, latestItem)
        is DeleteSamsungNote -> squashWritesOnSamsungNoteDelete(queue, latestItem)
        is DeleteMedia -> squashMediaUpdatesOnDelete(queue, latestItem)
        is UpdateMediaAltText -> squashMediaAltTextUpdates(queue, latestItem)
        is GetNoteForMerge -> outdatedMergeFetches(queue, latestItem)
        is InvalidUpdateNote -> squashInvalidUpdates(queue, latestItem)
        is InvalidDeleteNote -> squashInvalidDeletes(queue, latestItem)
    }
}

/*
 * Inbound Sync request squashing handling:
 * - A new full sync requests squashes all existing requests except:
     - existing full sync is in progress, neither remove the existing item nor push the new sync request
     - existing delta sync is in progress, don't remove this item but push the latest full sync request
 * - A new delta sync requests squashes all existing requests except:
     - existing item is a full sync, neither remove the existing item nor push the new sync request
     - existing delta item is in progress, neither remove the existing item nor push the new sync request
 *
 * Why use lock()?
 * In delta sync request case there may be already a full sync request in queue. Above logic requires us
 * determine this and modify the queue. Following logic requires two separate operations on queue hence
 * we need to obtain a lock and perform two operations atomically.
 */
private fun squashReads(queue: PriorityQueue, latestItem: ApiRequestOperation) {
    when (latestItem) {
        is Sync,
        is NoteReferencesSync,
        is SamsungNotesSync -> {}
        else -> throw IllegalArgumentException("Unsupported Operation ${latestItem.type}")
    }
    val isLatestItemFullSync = getDeltaTokenForSyncOperation(latestItem) == null
    queue.synchronized {
        var pushLatestItem = true
        /*
         * Following logic will remove already pushed request as well which will be pushed again based
         * on whether full sync is requested or not
         */
        queue.removeIf { item ->
            when (latestItem.type == item.type) {
                true -> {
                    val isFullSyncRequest = getDeltaTokenForSyncOperation(item) == null
                    when (isLatestItemFullSync) {
                        true -> {
                            if (item.isProcessing) {
                                if (isFullSyncRequest) {
                                    pushLatestItem = false
                                }
                                false
                            } else true
                        }
                        false -> {
                            if (isFullSyncRequest || item.isProcessing) {
                                pushLatestItem = false
                            }
                            !(isFullSyncRequest || item.isProcessing)
                        }
                    }
                }
                else -> false
            }
        }

        if (pushLatestItem) {
            queue.push(latestItem)
        }
    }
}

private fun squashUpdates(queue: PriorityQueue, latestItem: UpdateNote) {
    queue.removeIf { item ->
        val notSameOperation = item.uniqueId != latestItem.uniqueId
        val isUpdateForSameNote = item is UpdateNote && item.note.id == latestItem.note.id
        val isInvalidUpdateForSameNote = item is InvalidUpdateNote && item.note.id == latestItem.note.id
        notSameOperation && (isUpdateForSameNote || isInvalidUpdateForSameNote)
    }
}

private fun squashWritesOnDelete(queue: PriorityQueue, latestItem: DeleteNote) {
    var removeLatestDeleteRequest = false
    queue.removeIf { item ->
        when (item) {
            is CreateNote ->
                if (item.note.id == latestItem.localId && !item.isProcessing) {
                    removeLatestDeleteRequest = true
                    true
                } else {
                    false
                }
            is UpdateNote -> item.note.id == latestItem.localId
            is UploadMedia -> item.note.id == latestItem.localId
            is DownloadMedia -> item.note.id == latestItem.localId
            is DeleteMedia -> item.localNoteId == latestItem.localId
            is DeleteNote -> item.uniqueId != latestItem.uniqueId && item.localId == latestItem.localId
            is UpdateMediaAltText -> item.note.id == latestItem.localId
            is GetNoteForMerge -> item.note.id == latestItem.localId
            is InvalidDeleteNote -> item.localId == latestItem.localId
            is InvalidUpdateNote -> item.note.id == latestItem.localId
            is InvalidUploadMedia -> item.note.id == latestItem.localId
            is InvalidDeleteMedia -> item.noteLocalId == latestItem.localId
            is InvalidUpdateMediaAltText -> item.note.id == latestItem.localId
            else -> false
        }
    }
    if (removeLatestDeleteRequest) queue.remove(latestItem)
}

private fun squashWritesOnNoteReferenceDelete(queue: PriorityQueue, latestItem: DeleteNoteReference) {
    queue.removeIf { item ->
        when (item) {
            is DeleteNoteReference -> item.uniqueId != latestItem.uniqueId && item.localId == latestItem.localId
            else -> false
        }
    }
}

private fun squashWritesOnSamsungNoteDelete(queue: PriorityQueue, latestItem: DeleteSamsungNote) {
    queue.removeIf { item ->
        when (item) {
            is DownloadMedia -> item.note.id == latestItem.localId
            is DeleteSamsungNote -> item.uniqueId != latestItem.uniqueId && item.localId == latestItem.localId
            else -> false
        }
    }
}

private fun squashMediaUpdatesOnDelete(queue: PriorityQueue, latestItem: DeleteMedia) {
    queue.removeIf { item ->
        when (item) {
            is UploadMedia -> item.mediaLocalId == latestItem.localMediaId
            is InvalidUploadMedia -> item.mediaLocalId == latestItem.localMediaId
            is InvalidDeleteMedia -> item.mediaLocalId == latestItem.localMediaId
            is UpdateMediaAltText -> item.localMediaId == latestItem.localMediaId
            is InvalidUpdateMediaAltText -> item.mediaLocalId == latestItem.localMediaId
            else -> false
        }
    }
}

private fun squashMediaAltTextUpdates(queue: PriorityQueue, latestItem: UpdateMediaAltText) {
    queue.removeIf { item ->
        val notSameOperation = item.uniqueId != latestItem.uniqueId
        val isUpdateForSameMedia = item is UpdateMediaAltText && item.localMediaId == latestItem.localMediaId
        val isInvalidUpdateForSameMedia = item is InvalidUpdateMediaAltText && item.mediaLocalId == latestItem.localMediaId
        notSameOperation && (isUpdateForSameMedia || isInvalidUpdateForSameMedia)
    }
}

private fun outdatedMergeFetches(queue: PriorityQueue, latestItem: GetNoteForMerge) {
    queue.removeIf { item ->
        when (item) {
            is GetNoteForMerge -> {
                item.note.id == latestItem.note.id && item.uniqueId != latestItem.uniqueId
            }
            else -> false
        }
    }
}

private fun squashInvalidUpdates(queue: PriorityQueue, latestItem: InvalidUpdateNote) {
    queue.removeIf { item ->
        val notSameOperation = item.uniqueId != latestItem.uniqueId
        val isInvalidUpdateForSameNote = item is InvalidUpdateNote && item.note.id == latestItem.note.id
        notSameOperation && isInvalidUpdateForSameNote
    }
}

private fun squashInvalidDeletes(queue: PriorityQueue, latestItem: InvalidDeleteNote) {
    var removeLatestDeleteRequest = false
    queue.removeIf { item ->
        when (item) {
            is CreateNote ->
                if (item.note.id == latestItem.localId && !item.isProcessing) {
                    removeLatestDeleteRequest = true
                    true
                } else {
                    false
                }
            is InvalidDeleteNote ->
                item.uniqueId != latestItem.uniqueId &&
                    item.localId == latestItem.localId
            is DeleteNote -> item.localId == latestItem.localId
            else -> false
        }
    }
    if (removeLatestDeleteRequest) queue.remove(latestItem)
}

private fun getDeltaTokenForSyncOperation(operation: ApiRequestOperation): Token.Delta? {
    return when (operation) {
        is Sync -> operation.deltaToken
        is NoteReferencesSync -> operation.deltaToken
        is SamsungNotesSync -> operation.deltaToken
        else -> throw IllegalArgumentException("Unsupported Operation ${operation.type}")
    }
}
