package com.microsoft.notes.sideeffect.sync

import com.microsoft.notes.models.Changes
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteUpdate
import com.microsoft.notes.models.generateLocalId
import com.microsoft.notes.sideeffect.sync.mapper.toStoreNote
import com.microsoft.notes.sync.models.DeltaSyncPayload
import com.microsoft.notes.sync.models.RemoteNote

fun changesForFullSync(localNotes: List<Note>, remoteNotes: List<RemoteNote>): ChangesResult {
    val notesToBeDeleted = localNotes.toMutableList()
    val result = remoteNotes.fold(ChangesResult()) { changes, remoteNote ->
        val mergeStrategy = mergeStrategy(remoteNote, notesToBeDeleted)
        changes.append(mergeStrategy)
    }

    return result.append(deleted = notesToBeDeleted.filter { it.remoteData != null })
}

fun changesForDeltaSync(
    localNotes: List<Note>,
    deltaSyncPayloads: List<DeltaSyncPayload>,
    uiBaseRevisions: Map<String, Long>
): ChangesResult {
    return deltaSyncPayloads.fold(ChangesResult()) { changes, deltaSyncPayload ->
        when (deltaSyncPayload) {
            is DeltaSyncPayload.NonDeleted -> {
                val mergeStrategy = mergeStrategy(
                    deltaSyncPayload.note, localNotes.toMutableList(),
                    uiBaseRevisions
                )
                changes.append(mergeStrategy)
            }
            is DeltaSyncPayload.Deleted -> {
                val matchedLocalNote = localNotes.find { it.remoteData?.id == deltaSyncPayload.id }
                if (matchedLocalNote != null) {
                    changes.append(deleted = matchedLocalNote)
                } else {
                    changes
                }
            }
        }
    }
}

private fun mergeStrategy(
    remoteNote: RemoteNote,
    localNotes: MutableList<Note>,
    uiBaseRevisions: Map<String, Long>? = null
): MergeStrategy {
    val matchedLocalNote: Note? = localNotes.remove {
        if (it.remoteData != null) {
            it.remoteData?.id == remoteNote.id
        } else {
            it.localId == remoteNote.createdWithLocalId
        }
    }

    return if (matchedLocalNote != null) {
        val uiRevision = uiBaseRevisions?.get(matchedLocalNote.localId) ?: matchedLocalNote.uiRevision
        Replace(
            NoteUpdate(
                noteFromServer = remoteNote.toStoreNote(matchedLocalNote, uiRevision),
                uiRevision = uiRevision
            )
        )
    } else {
        Create(remoteNote.toStoreNote(generateLocalId()))
    }
}

sealed class MergeStrategy
data class Create(val note: Note) : MergeStrategy()
data class Replace(val note: NoteUpdate) : MergeStrategy()

data class ChangesResult(val changes: Changes = Changes()) {

    fun with(changes: Changes): ChangesResult = ChangesResult(changes)

    fun append(mergeStrategy: MergeStrategy): ChangesResult =
        when (mergeStrategy) {
            is Create -> with(changes.appendToCreate(mergeStrategy.note))
            is Replace -> with(changes.appendToReplace(mergeStrategy.note))
        }

    fun append(deleted: Note): ChangesResult =
        with(changes.appendToDelete(deleted))

    fun append(deleted: List<Note>): ChangesResult =
        with(changes.appendToDelete(deleted))
}

fun <T> MutableList<T>.remove(predicate: (T) -> Boolean): T? {
    val index = indexOfFirst(predicate)
    return if (index >= 0) removeAt(index) else null
}
