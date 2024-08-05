package com.microsoft.notes.store

import com.microsoft.notes.models.Note

fun State.getSamsungNotesCollectionForUser(userID: String): List<Note> =
    getUserStateForUserID(userID).samsungNotesList.samsungNotesCollection

fun State.addSamsungNotes(samsungNotes: List<Note>, userID: String): State {
    if (samsungNotes.isEmpty()) {
        return this
    }

    val newSamsungNoteLocalIdToUserIDMap = addSamsungNotesToUserIDMap(samsungNotes, userID)
    val newSamsungNotesList =
        getUserStateForUserID(userID).samsungNotesList.addSamsungNotes(samsungNotes)

    return withSamsungNotesListForUser(
        newSamsungNotesList,
        userID
    ).copy(samsungNoteLocalIDToUserIDMap = newSamsungNoteLocalIdToUserIDMap)
}

fun State.addDistinctSamsungNotes(samsungNotes: List<Note>, userID: String): State {
    if (samsungNotes.isEmpty()) {
        return this
    }

    val newSamsungNotesToUserId = addSamsungNotesToUserIDMap(samsungNotes, userID)
    val newSamsungNotesList = getUserStateForUserID(userID).samsungNotesList
        .addDistinctSamsungNotes(samsungNotes)

    return withSamsungNotesListForUser(newSamsungNotesList, userID)
        .copy(samsungNoteLocalIDToUserIDMap = newSamsungNotesToUserId)
}

private fun SamsungNotesList.addDistinctSamsungNotes(samsungNotes: List<Note>): SamsungNotesList {
    if (samsungNotes.isEmpty()) {
        return this
    }
    val newSamsungNotesCollection = (samsungNotesCollection + samsungNotes).distinctBy { it.localId }
    return SamsungNotesList.createSamsungNotesList(
        samsungNotesCollection = newSamsungNotesCollection.sortByDocumentModifiedAt()
    )
}

private fun State.addSamsungNotesToUserIDMap(
    notes: List<Note>,
    userID: String
): Map<String, String> =
    samsungNoteLocalIDToUserIDMap + notes.associate { it.localId to userID }

fun State.replaceSamsungNotes(samsungNotes: List<Note>, userID: String): State {
    if (samsungNotes.isEmpty()) {
        return this
    }
    val newSamsungNotesList =
        getUserStateForUserID(userID).samsungNotesList.replaceNotes(samsungNotes)
    return withSamsungNotesListForUser(samsungNotesList = newSamsungNotesList, userID = userID)
}

fun State.deleteSamsungNotes(notes: List<Note>, userID: String): State {
    if (notes.isEmpty()) {
        return this
    }

    val localIdsToBeDeleted = notes.map { it.localId }.toSet()
    val newSamsungNoteLocalIDToUserIDMap =
        samsungNoteLocalIDToUserIDMap.filter { it.key in localIdsToBeDeleted }

    val updatedUserState = getUserStateForUserID(userID).deleteSamsungNotes(notes)
    return newState(
        userID = userID,
        updatedUserState = updatedUserState,
        updatedSamsungNoteIDToUserIDMap = newSamsungNoteLocalIDToUserIDMap
    )
}

fun State.withSamsungNotesListForUser(samsungNotesList: SamsungNotesList, userID: String): State {
    val updatedUserState = getUserStateForUserID(userID).copy(samsungNotesList = samsungNotesList)
    return newState(userID, updatedUserState)
}

private fun SamsungNotesList.addSamsungNotes(notes: List<Note>): SamsungNotesList {
    if (notes.isEmpty()) {
        return this
    }
    return SamsungNotesList.createSamsungNotesList(
        samsungNotesCollection = (samsungNotesCollection + notes).sortByDocumentModifiedAt()
    )
}

fun State.replaceSamsungNote(replacement: Note): State {
    val userID = userIDForLocalNoteID(replacement.localId)
    val newSamsungNotesList = getUserStateForUserID(userID).samsungNotesList.replaceNote(replacement)
    return withSamsungNotesListForUser(samsungNotesList = newSamsungNotesList, userID = userID)
}

private fun SamsungNotesList.replaceNote(replacement: Note): SamsungNotesList {
    val newSamsungNotesCollection = samsungNotesCollection.map {
        if (it.localId == replacement.localId) {
            replacement
        } else {
            it
        }
    }
    return SamsungNotesList.createSamsungNotesList(
        samsungNotesCollection = newSamsungNotesCollection.sortByDocumentModifiedAt()
    )
}

private fun SamsungNotesList.replaceNotes(notes: List<Note>): SamsungNotesList {
    val idToNoteToBeReplaced = notes.associateBy { it.localId }
    val newSamsungNotesCollection: List<Note> =
        samsungNotesCollection.map { idToNoteToBeReplaced[it.localId] ?: it }
    return SamsungNotesList.createSamsungNotesList(
        samsungNotesCollection =
        newSamsungNotesCollection.sortByDocumentModifiedAt()
    )
}

private fun UserState.deleteSamsungNotes(notes: List<Note>): UserState {
    val newSamsungNotesCollection = samsungNotesList.samsungNotesCollection - notes

    return copy(
        samsungNotesList = SamsungNotesList.createSamsungNotesList(
            samsungNotesCollection = newSamsungNotesCollection.sortByDocumentModifiedAt()
        )
    )
}

fun State.deleteSamsungNoteByLocalId(noteLocalId: String, userID: String): State {
    if (noteLocalId.isEmpty()) {
        return this
    }
    val userStateForUserID = getUserStateForUserID(userID)
    val updatedUserState = userStateForUserID.deleteSamsungNoteByLocalId(noteLocalId)
    val newNoteLocalIDToUserIDMap = noteLocalIDToUserIDMap.toMutableMap()

    newNoteLocalIDToUserIDMap.remove(noteLocalId)
    return newState(
        userID = userID, updatedUserState = updatedUserState,
        updatedNoteIDToUserIDMap = newNoteLocalIDToUserIDMap
    )
}

fun UserState.deleteSamsungNoteByLocalId(noteLocalId: String): UserState {
    val newSamsungNotesCollection = samsungNotesList.samsungNotesCollection.filter { !noteLocalId.contains(it.localId) }
    return copy(
        samsungNotesList = SamsungNotesList.createSamsungNotesList(
            samsungNotesCollection = newSamsungNotesCollection.sortByDocumentModifiedAt()
        )
    )
}

fun State.markSamsungNoteWithLocalIdAsDeleted(localId: String): State {
    val userID = userIDForLocalNoteID(localId)
    val userState = getUserStateForUserID(userID)
    val updatedUserState = userState.copy(
        samsungNotesList = userState.samsungNotesList.setIsDeletedForSamsungNoteWithLocalId(
            localId,
            isDeleted = true
        )
    )
    return newState(userID, updatedUserState)
}
