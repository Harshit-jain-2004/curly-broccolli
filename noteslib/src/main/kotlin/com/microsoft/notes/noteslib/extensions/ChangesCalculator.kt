package com.microsoft.notes.noteslib.extensions

import com.microsoft.notes.models.Changes
import com.microsoft.notes.models.MeetingNote
import com.microsoft.notes.models.MeetingNoteChanges
import com.microsoft.notes.models.MeetingNoteUpdate
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.NoteReferenceChanges
import com.microsoft.notes.models.NoteReferenceUpdate
import com.microsoft.notes.models.NoteUpdate
import com.microsoft.notes.models.generateLocalId
import com.microsoft.notes.notesReference.models.NoteRefSourceId
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.sideeffect.sync.mapper.toStoreMeetingNote
import com.microsoft.notes.sideeffect.sync.mapper.toStoreNote
import com.microsoft.notes.sideeffect.sync.mapper.toStoreNoteReference
import com.microsoft.notes.sync.models.DeltaSyncPayload
import com.microsoft.notes.sync.models.NoteReferencesDeltaSyncPayload
import com.microsoft.notes.sync.models.RemoteMeetingNote
import com.microsoft.notes.sync.models.RemoteNote
import com.microsoft.notes.sync.models.RemoteNoteReference
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.utils.utils.parseISO8601StringToMillis

fun calculateNoteReferencesListChanges(localNotes: List<NoteReference>, remoteNotes: List<NoteReference>): NoteReferenceChanges {
    var changes = NoteReferenceChanges()
    val localIdToRemoteNotesMap = remoteNotes.map { it.localId to it }.toMap().toMutableMap()

    localNotes.forEach { localNote ->

        localIdToRemoteNotesMap[localNote.localId]?.let { remoteNote ->
            if (remoteNote.lastModifiedAt > localNote.lastModifiedAt) {
                changes = changes.appendToReplace(NoteReferenceUpdate(remoteNote))
            }
            localIdToRemoteNotesMap.remove(localNote.localId)
        } ?: run {
            changes = changes.appendToDelete(localNote)
        }
    }

    localIdToRemoteNotesMap.forEach {
        changes = changes.appendToCreate(it.value)
    }

    return changes
}

fun calculateNoteReferencesChanges(
    localNoteReferences: List<NoteReference>,
    remoteNoteReferences:
        List<RemoteNoteReference>
):
    NoteReferenceChanges {
    var changes = NoteReferenceChanges()
    val idToRemoteNoteReferencesMap = remoteNoteReferences.map { it.id to it }.toMap().toMutableMap()

    localNoteReferences.forEach { localNoteReference ->

        idToRemoteNoteReferencesMap[localNoteReference.remoteId]?.let { remoteNoteReference ->
            changes = changes.appendToReplace(
                NoteReferenceUpdate(
                    remoteNoteReference.toStoreNoteReference
                    (
                        localNoteReferenceId = localNoteReference.localId, pageLocalId = localNoteReference.pageLocalId,
                        sectionLocalId = localNoteReference.sectionLocalId, isDeleted = localNoteReference.isDeleted,
                        isMediaPresent = localNoteReference.isMediaPresent, isPinned = localNoteReference.isPinned, pinnedAt = localNoteReference.pinnedAt, localNoteReferenceMedia = localNoteReference?.media
                    )
                )
            )
            idToRemoteNoteReferencesMap.remove(localNoteReference.remoteId)
        } ?: run {
            changes = changes.appendToDelete(localNoteReference)
        }
    }

    idToRemoteNoteReferencesMap.forEach {
        changes = changes.appendToCreate(
            it.value.toStoreNoteReference(
                localNoteReferenceId = generateLocalId(),
                pageLocalId = null, sectionLocalId = null, isDeleted = false, isPinned = false, pinnedAt = null, localNoteReferenceMedia = null
            )
        )
    }

    return changes
}

fun calculateMeetingNotesChanges(
    localMeetingNotes: List<MeetingNote>,
    remoteMeetingNotes: List<RemoteMeetingNote>
): MeetingNoteChanges {
    var changes = MeetingNoteChanges()
    val idToRemoteMeetingNotesMap = remoteMeetingNotes.map { it.id to it }.toMap().toMutableMap()

    localMeetingNotes.forEach { localMeetingNote ->

        idToRemoteMeetingNotesMap[localMeetingNote.remoteId]?.let { remoteMeetingNote ->
            if (remoteMeetingNote.lastModifiedTime > localMeetingNote.lastModifiedTime) {
                changes = changes.appendToReplace(
                    MeetingNoteUpdate(
                        remoteMeetingNote.toStoreMeetingNote
                        (localMeetingNoteId = localMeetingNote.localId)
                    )
                )
            }
            idToRemoteMeetingNotesMap.remove(localMeetingNote.remoteId)
        } ?: run {
            changes = changes.appendToDelete(localMeetingNote)
        }
    }

    idToRemoteMeetingNotesMap.forEach {
        changes = changes.appendToCreate(it.value.toStoreMeetingNote(localMeetingNoteId = generateLocalId()))
    }
    return changes
}

fun calculateNoteReferencesDeltaSyncChanges(
    localNoteReferences: List<NoteReference>,
    deltaSyncPayloads: List<NoteReferencesDeltaSyncPayload>
): NoteReferenceChanges {
    return deltaSyncPayloads.fold(NoteReferenceChanges()) { changes, noteReferenceDeltaSyncPayload ->
        when (noteReferenceDeltaSyncPayload) {
            is NoteReferencesDeltaSyncPayload.Deleted -> {
                val matchedLocalNote = localNoteReferences.find { it.remoteId == noteReferenceDeltaSyncPayload.noteId }
                if (matchedLocalNote != null) {
                    changes.appendToDelete(matchedLocalNote)
                } else {
                    changes
                }
            }
            is NoteReferencesDeltaSyncPayload.NonDeleted -> {
                val matchedLocalNote = localNoteReferences.find { it.remoteId == noteReferenceDeltaSyncPayload.noteId }
                if (matchedLocalNote != null) {
                    changes.appendToReplace(
                        NoteReferenceUpdate(
                            noteReferenceDeltaSyncPayload.note.toStoreNoteReference
                            (
                                localNoteReferenceId = matchedLocalNote.localId, pageLocalId = matchedLocalNote.pageLocalId,
                                sectionLocalId = matchedLocalNote.sectionLocalId, isDeleted = matchedLocalNote.isDeleted,
                                isMediaPresent = matchedLocalNote.isMediaPresent, isPinned = matchedLocalNote.isPinned, pinnedAt = matchedLocalNote.pinnedAt, localNoteReferenceMedia = matchedLocalNote.media
                            )
                        )
                    )
                } else {
                    changes.appendToCreate(
                        noteReferenceDeltaSyncPayload.note.toStoreNoteReference(
                            localNoteReferenceId = generateLocalId(),
                            pageLocalId = null, sectionLocalId = null, isDeleted = false, isPinned = false, pinnedAt = null, localNoteReferenceMedia = null
                        )
                    )
                }
            }
        }
    }
}

fun calculateSamsungNotesDeltaSyncChanges(
    localSamsungNotes: List<Note>,
    deltaSyncPayloads: List<DeltaSyncPayload>
): Changes = deltaSyncPayloads.fold(Changes()) { changes, samsungDeltaSyncPayload ->
    when (samsungDeltaSyncPayload) {
        is DeltaSyncPayload.Deleted -> {
            val matchedLocalNote = localSamsungNotes.find { it.remoteData?.id == samsungDeltaSyncPayload.noteId }
            if (matchedLocalNote != null) {
                changes.appendToDelete(matchedLocalNote)
            } else {
                changes
            }
        }
        is DeltaSyncPayload.NonDeleted -> {
            val matchedLocalNote = localSamsungNotes.find { it.remoteData?.id == samsungDeltaSyncPayload.noteId }
            if (matchedLocalNote != null) {
                changes.appendToReplace(
                    NoteUpdate(samsungDeltaSyncPayload.note.toStoreNote(matchedLocalNote))
                )
            } else {
                changes.appendToCreate(
                    samsungDeltaSyncPayload.note.toStoreNote(generateLocalId())
                )
            }
        }
    }
}

fun calculateNoteReferencesDeltaSyncChangesForHybrid(
    localNoteReferences: List<NoteReference>,
    deltaSyncPayloads: List<NoteReferencesDeltaSyncPayload>,
    notesLogger: NotesLogger?
): NoteReferenceChanges {
    return deltaSyncPayloads.fold(NoteReferenceChanges()) { changes, noteReferenceDeltaSyncPayload ->
        when (noteReferenceDeltaSyncPayload) {
            is NoteReferencesDeltaSyncPayload.Deleted -> {
                val matchedLocalNote = localNoteReferences.find { it.remoteId == noteReferenceDeltaSyncPayload.noteId }
                if (matchedLocalNote != null) {
                    NotesLibrary.getInstance().deleteCachedImagesForDeletedNoteReference(matchedLocalNote.media)
                    changes.appendToDelete(matchedLocalNote)
                } else {
                    changes
                }
            }
            is NoteReferencesDeltaSyncPayload.NonDeleted -> {
                val matchedLocalNote = getLocalNoteForRemoteNoteIfPresent(noteReferenceDeltaSyncPayload.note, localNoteReferences)
                if (matchedLocalNote != null) {
                    when {
                        shouldUpdateLocalNote(matchedLocalNote, noteReferenceDeltaSyncPayload.note) -> {
                            changes.appendToReplace(
                                NoteReferenceUpdate(
                                    noteReferenceDeltaSyncPayload.note.toStoreNoteReference
                                    (
                                        localNoteReferenceId = matchedLocalNote.localId, pageLocalId = matchedLocalNote.pageLocalId,
                                        sectionLocalId = matchedLocalNote.sectionLocalId, isDeleted = matchedLocalNote.isDeleted,
                                        isMediaPresent = matchedLocalNote.isMediaPresent, isPinned = matchedLocalNote.isPinned, pinnedAt = matchedLocalNote.pinnedAt, localNoteReferenceMedia = matchedLocalNote.media
                                    )
                                )
                            )
                        }
                        matchedLocalNote.isLocalOnlyPage -> {
                            if (notesLogger != null) {
                                logFeedSyncLatency(notesLogger, noteReferenceDeltaSyncPayload.note)
                            }
                            changes.appendToReplace(NoteReferenceUpdate(matchedLocalNote.copy(isLocalOnlyPage = false)))
                        }
                        else -> {
                            changes
                        }
                    }
                } else {
                    changes.appendToCreate(
                        noteReferenceDeltaSyncPayload.note.toStoreNoteReference(
                            localNoteReferenceId = generateLocalId(),
                            pageLocalId = null, sectionLocalId = null, isDeleted = false, isPinned = false, pinnedAt = null, localNoteReferenceMedia = null
                        )
                    )
                }
            }
        }
    }
}

fun calculateNoteReferencesChangesForHybrid(
    localNoteReferences: List<NoteReference>,
    remoteNoteReferences:
        List<RemoteNoteReference>
):
    NoteReferenceChanges {
    var changes = NoteReferenceChanges()
    /* Creating a map with page full source Id as key */
    val sourceIdToRemoteNoteReferencesMap = remoteNoteReferences.map { it.metaData.id.fullSourceId to it }.toMap().toMutableMap()

    localNoteReferences.forEach { localNote ->
        val remoteNote = getRemoteNoteForLocalNoteIfPresent(localNote, sourceIdToRemoteNoteReferencesMap)
        if (remoteNote != null) {
            if (shouldUpdateLocalNote(localNote, remoteNote)) {
                /* Updating the localNote because LMT of remoteNote is >= LMT of localNote*/
                changes = changes.appendToReplace(
                    NoteReferenceUpdate(
                        remoteNote.toStoreNoteReference(
                            localNoteReferenceId = localNote.localId,
                            pageLocalId = localNote.pageLocalId, sectionLocalId = localNote.sectionLocalId, isDeleted = localNote.isDeleted,
                            isMediaPresent = localNote.isMediaPresent, isPinned = localNote.isPinned, pinnedAt = localNote.pinnedAt, localNoteReferenceMedia = localNote.media
                        )
                    )
                )
            } else if (localNote.isLocalOnlyPage) { /* Setting localOnlyPage flag to false after corresponding remoteNote is received from the server in case of LMT of remoteNote less than localNote */
                changes = changes.appendToReplace(NoteReferenceUpdate(localNote.copy(isLocalOnlyPage = false)))
            }
            sourceIdToRemoteNoteReferencesMap.remove(remoteNote.metaData.id.fullSourceId)
        } else {
            if (!localNote.isLocalOnlyPage) { /* Deleting the localNote if isLocalOnlyPage flag is set to false as we didn't receive corresponding remoteNote from the server */
                changes = changes.appendToDelete(localNote)
            }
        }
    }

    sourceIdToRemoteNoteReferencesMap.forEach {
        changes = changes.appendToCreate(
            it.value.toStoreNoteReference(
                localNoteReferenceId = generateLocalId(),
                pageLocalId = null, sectionLocalId = null, isDeleted = false, isPinned = false, pinnedAt = null, localNoteReferenceMedia = null
            )
        )
    }

    return changes
}

fun calculateSamsungNoteChanges(
    localSamsungNotes: List<Note>,
    remoteSamsungNotes: List<RemoteNote>
): Changes {
    var changes = Changes()
    val remoteIdToRemoteSamsungNote = remoteSamsungNotes.associateBy { it.id }.toMutableMap()

    localSamsungNotes.forEach { localSamsungNote ->
        remoteIdToRemoteSamsungNote[localSamsungNote.remoteData?.id]?.let { remoteSamsungNote ->
            // TODO 17-Dec-20 gopalsa: Remove comparing LMT, as it's not reliable to check for changes in all params
            if (parseISO8601StringToMillis(remoteSamsungNote.lastModifiedAt) > localSamsungNote.documentModifiedAt) {
                changes = changes.appendToReplace(
                    NoteUpdate(remoteSamsungNote.toStoreNote(localSamsungNote))
                ) // uiRevision is not applicable
            }
            remoteIdToRemoteSamsungNote.remove(localSamsungNote.remoteData?.id)
        } ?: run {
            changes = changes.appendToDelete(localSamsungNote)
        }
    }

    remoteIdToRemoteSamsungNote.forEach {
        changes = changes.appendToCreate(it.value.toStoreNote(generateLocalId()))
    }

    return changes
}

/* Returns the corresponding Remote Note for the local note from the map if present */
private fun getRemoteNoteForLocalNoteIfPresent(localNote: NoteReference, remoteNotesMap: Map<String, RemoteNoteReference>): RemoteNoteReference? {
    when (localNote.pageSourceId) {
        is NoteRefSourceId.FullSourceId -> return remoteNotesMap[localNote.pageSourceId.fullSourceId] /* Remote Note is returned for the local note by matching the full/Partial source Ids, null if no match */
        is NoteRefSourceId.PartialSourceId -> {
            remoteNotesMap.forEach {
                if (localNote.pageSourceId.isSameId(it.value.metaData.id, it.value.metaData.webUrl)) {
                    return it.value
                }
            }
            return null
        }
        else -> return null
    }
}

private fun getLocalNoteForRemoteNoteIfPresent(remoteNote: RemoteNoteReference, localNoteReferences: List<NoteReference>): NoteReference? {
    localNoteReferences.forEach {
        if (it.pageSourceId != null && it.pageSourceId.isSameId(remoteNote.metaData.id, remoteNote.metaData.webUrl)) {
            return it
        }
    }
    return null
}

/* Returns a boolean if the localNote has to be updated or not */
private fun shouldUpdateLocalNote(localNote: NoteReference, remoteNote: RemoteNoteReference): Boolean {
    return (
        remoteNote.metaData.lastModified /* Returns true if the lastModified of remoteNote is greater than or equal to localNote. We are keeping the equality condition as */
            >= localNote.lastModifiedAt
        ) /* well since LMT can remain same even when other fields would have changed like sectionName, nbName */
}

private fun logFeedSyncLatency(notesLogger: NotesLogger, remoteNote: RemoteNoteReference) {
    notesLogger.recordTelemetry(
        EventMarkers.NoteReferenceSyncLatency,
        Pair(NotesSDKTelemetryKeys.FeedProperty.SYNC_LATENCY, (System.currentTimeMillis() - remoteNote.metaData.lastModified).toString())
    )
}
