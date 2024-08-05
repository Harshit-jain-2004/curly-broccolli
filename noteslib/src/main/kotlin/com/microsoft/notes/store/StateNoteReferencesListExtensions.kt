package com.microsoft.notes.store

import com.microsoft.notes.models.NoteReference

fun State.getNoteReferencesCollectionForUser(userID: String): List<NoteReference> =
    getUserStateForUserID(userID).noteReferencesList.noteReferencesCollection

fun State.addNoteReferences(noteReferences: List<NoteReference>, userID: String): State {
    if (noteReferences.isEmpty()) {
        return this
    }

    val newNoteReferencesToUserIDMap = addNoteReferencesToUserIDMap(noteReferences, userID)
    val newNoteReferencesList = getUserStateForUserID(userID).noteReferencesList.addNoteReferences(noteReferences)

    return withNoteReferencesListForUser(newNoteReferencesList, userID)
        .copy(noteReferenceLocalIDToUserIDMap = newNoteReferencesToUserIDMap)
}

fun State.addDistinctNoteReferences(noteReferences: List<NoteReference>, userID: String): State {
    if (noteReferences.isEmpty()) {
        return this
    }

    val newNoteReferencesToUserIDMap = addNoteReferencesToUserIDMap(noteReferences, userID)
    val newNoteReferencesList = getUserStateForUserID(userID).noteReferencesList
        .addDistinctNoteReferences(noteReferences)

    return withNoteReferencesListForUser(newNoteReferencesList, userID)
        .copy(noteReferenceLocalIDToUserIDMap = newNoteReferencesToUserIDMap)
}

fun State.deleteNoteReferenceByLocalId(noteReferenceLocalId: String, userID: String): State {
    if (noteReferenceLocalId.isEmpty()) {
        return this
    }

    val updatedUserState = getUserStateForUserID(userID).deleteNoteReferenceByLocalId(noteReferenceLocalId)
    val newNoteReferenceLocalIDToUserIDMap = noteReferenceLocalIDToUserIDMap.toMutableMap()

    newNoteReferenceLocalIDToUserIDMap.remove(noteReferenceLocalId)
    return newState(
        userID = userID,
        updatedUserState = updatedUserState,
        updatedNoteReferenceIDToUserIDMap = newNoteReferenceLocalIDToUserIDMap
    )
}

fun State.replaceNoteReference(replacement: NoteReference, userID: String): State {
    val newNotesList = getUserStateForUserID(userID).noteReferencesList.replaceNote(replacement)
    return withNoteReferencesListForUser(noteReferencesList = newNotesList, userID = userID)
}

fun State.replaceNoteReferences(noteReferences: List<NoteReference>, userID: String): State {
    if (noteReferences.isEmpty()) {
        return this
    }
    val newList = getUserStateForUserID(userID).noteReferencesList.replaceNotes(noteReferences)
    return withNoteReferencesListForUser(noteReferencesList = newList, userID = userID)
}

fun State.pinOrUnpinNoteReferences(noteReferences: List<NoteReference>, userID: String, setIsPinned: Boolean): State {
    if (noteReferences.isEmpty()) {
        return this
    }
    val newList = getUserStateForUserID(userID).noteReferencesList.setIsPinned(noteReferences, setIsPinned)
    return withNoteReferencesListForUser(noteReferencesList = newList, userID = userID)
}

fun State.deleteNoteReferences(noteReferences: List<NoteReference>, userID: String): State {
    if (noteReferences.isEmpty()) {
        return this
    }

    val newLocalIDToUserIDMap = noteReferenceLocalIDToUserIDMap.toMutableMap()
    noteReferences.forEach { newLocalIDToUserIDMap.remove(it.localId) }

    val updatedUserState = getUserStateForUserID(userID).deleteNoteReferences(noteReferences)
    return newState(
        userID = userID, updatedUserState = updatedUserState,
        updatedNoteReferenceIDToUserIDMap = newLocalIDToUserIDMap
    )
}

fun State.markNoteReferenceWithLocalIdAsDeleted(localId: String): State {
    val userID = userIDForLocalNoteReferenceID(localId)
    val userState = getUserStateForUserID(userID)
    val updatedUserState = userState.copy(
        noteReferencesList = userState.noteReferencesList.setIsDeletedForNoteReferenceWithLocalId(
            localId,
            isDeleted = true
        )
    )
    return newState(userID, updatedUserState)
}

fun State.markNoteReferencesAsDeleted(noteReferences: List<NoteReference>, userID: String): State {
    if (noteReferences.isEmpty()) {
        return this
    }
    val newList = getUserStateForUserID(userID).noteReferencesList.markAsDeleted(noteReferences)
    return withNoteReferencesListForUser(noteReferencesList = newList, userID = userID)
}

fun State.combinedNoteReferencesForAllUsers(): List<NoteReference> {
    val noteReferences = mutableListOf<NoteReference>()
    for ((_, userState) in userIDToUserStateMap) {
        noteReferences.addAll(userState.noteReferencesList.noteReferencesCollection)
    }
    return noteReferences.distinctBy { it.localId }.sortByLastModifiedAt()
}

private fun State.addNoteReferencesToUserIDMap(
    noteReferences: List<NoteReference>,
    userID: String
): Map<String, String> {
    val newNoteReferenceLocalIdToUserIdMap = noteReferenceLocalIDToUserIDMap.toMutableMap()
    noteReferences.forEach { newNoteReferenceLocalIdToUserIdMap.put(it.localId, userID) }
    return newNoteReferenceLocalIdToUserIdMap
}

private fun NoteReferencesList.setIsPinned(notesToBeUpdated: List<NoteReference>, setIsPinned: Boolean): NoteReferencesList {
    val idsToBeMarked = notesToBeUpdated.map { it.localId }.toSet()
    val newCollection = noteReferencesCollection.map {
        if (it.localId in idsToBeMarked) {
            it.copy(isPinned = setIsPinned, pinnedAt = if (setIsPinned) System.currentTimeMillis() else null)
        } else {
            it
        }
    }
    return NoteReferencesList.createNoteReferencesList(noteReferencesCollection = newCollection.sortByLastModifiedAt())
}

private fun NoteReferencesList.addNoteReferences(noteReferences: List<NoteReference>): NoteReferencesList {
    if (noteReferences.isEmpty()) {
        return this
    }
    return NoteReferencesList.createNoteReferencesList(
        noteReferencesCollection = (noteReferencesCollection + noteReferences).sortByLastModifiedAt()
    )
}

private fun NoteReferencesList.addDistinctNoteReferences(noteReferences: List<NoteReference>): NoteReferencesList {
    if (noteReferences.isEmpty()) {
        return this
    }
    val newNotesCollection = (noteReferencesCollection + noteReferences).distinctBy { it.localId }
    return NoteReferencesList.createNoteReferencesList(
        noteReferencesCollection = newNotesCollection.sortByLastModifiedAt()
    )
}

fun State.withNoteReferencesListForUser(noteReferencesList: NoteReferencesList, userID: String): State {
    val updatedUserState = getUserStateForUserID(userID).copy(noteReferencesList = noteReferencesList)
    return newState(userID, updatedUserState)
}

private fun List<NoteReference>.sortByLastModifiedAt(): List<NoteReference> =
    sortedWith(compareByDescending { it.lastModifiedAt })

private fun UserState.deleteNoteReferenceByLocalId(noteReferenceLocalId: String): UserState {
    val newNotesCollection = noteReferencesList.noteReferencesCollection.filter {
        !noteReferenceLocalId.contains(it.localId)
    }
    return copy(
        noteReferencesList = NoteReferencesList.createNoteReferencesList(noteReferencesCollection = newNotesCollection)
    )
}

private fun NoteReferencesList.replaceNote(replacement: NoteReference): NoteReferencesList {
    val newCollection = noteReferencesCollection.map {
        if (it.localId == replacement.localId) {
            replacement
        } else {
            it
        }
    }
    return NoteReferencesList.createNoteReferencesList(noteReferencesCollection = newCollection.sortByLastModifiedAt())
}

private fun NoteReferencesList.replaceNotes(notes: List<NoteReference>): NoteReferencesList {
    val newCollection = notes.fold(noteReferencesCollection) { notesCollection, replacement ->
        notesCollection.map {
            if (it.localId == replacement.localId) {
                replacement
            } else {
                it
            }
        }
    }
    return NoteReferencesList.createNoteReferencesList(noteReferencesCollection = newCollection.sortByLastModifiedAt())
}

private fun NoteReferencesList.markAsDeleted(notesToBeMarked: List<NoteReference>): NoteReferencesList {
    val idsToBeMarked = notesToBeMarked.map { it.localId }.toSet()
    val newCollection = noteReferencesCollection.map {
        if (it.localId in idsToBeMarked) {
            it.copy(isDeleted = true, isLocalOnlyPage = false)
        } else {
            it
        }
    }
    return NoteReferencesList.createNoteReferencesList(noteReferencesCollection = newCollection.sortByLastModifiedAt())
}

private fun UserState.deleteNoteReferences(noteReferences: List<NoteReference>): UserState {
    val newCollection = noteReferencesList.noteReferencesCollection.minus(noteReferences)

    return copy(
        noteReferencesList = NoteReferencesList.createNoteReferencesList(noteReferencesCollection = newCollection)
    )
}
