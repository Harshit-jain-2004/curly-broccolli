package com.microsoft.notes.store

import com.microsoft.notes.models.Note
import com.microsoft.notes.ui.extensions.isSamsungNote

fun State.getNoteForNoteLocalId(noteId: String): Note? {
    val userID = userIDForLocalNoteID(noteId)
    val userStateForUserID = getUserStateForUserID(userID)
    return userStateForUserID.notesList.getNote(noteId) ?: userStateForUserID.samsungNotesList.getSamsungNote(noteId)
}

fun State.getNotesCollectionForUser(userID: String): List<Note> = getUserStateForUserID(userID).notesList.notesCollection

fun State.addNotes(notes: List<Note>, userID: String): State {
    if (notes.isEmpty()) {
        return this
    }

    val newNotesToUserIDMap = addNotesToUserIDMap(notes, userID)
    val newNotesList = getUserStateForUserID(userID).notesList.addNotes(notes)

    return withNotesListForUser(newNotesList, userID).copy(noteLocalIDToUserIDMap = newNotesToUserIDMap)
}

fun State.addDistinctNotes(notes: List<Note>, userID: String): State {
    if (notes.isEmpty()) {
        return this
    }

    val newNotesToUserIDMap = addNotesToUserIDMap(notes, userID)
    val newNotesList = getUserStateForUserID(userID).notesList.addDistinctNotes(notes)

    return withNotesListForUser(newNotesList, userID).copy(noteLocalIDToUserIDMap = newNotesToUserIDMap)
}

private fun State.addNotesToUserIDMap(notes: List<Note>, userID: String): Map<String, String> {
    val newNoteLocalIdToUserIdMap = noteLocalIDToUserIDMap.toMutableMap()
    notes.forEach { newNoteLocalIdToUserIdMap.put(it.localId, userID) }
    return newNoteLocalIdToUserIdMap
}

internal fun List<Note>.sortByDocumentModifiedAt(): List<Note> {
    return sortedWith(
        comparator = Comparator { note1, note2 ->
            when {
                note1.documentModifiedAt > note2.documentModifiedAt -> -1
                note1.documentModifiedAt == note2.documentModifiedAt -> sortById(note1, note2)
                else -> 1
            }
        }
    )
}

fun State.pinOrUnpinNotes(notes: List<Note>, userID: String, setIsPinned: Boolean): State {
    if (notes.isEmpty()) {
        return this
    }
    val newList = getUserStateForUserID(userID).notesList.setIsPinned(notes, setIsPinned)
    return withNotesListForUser(newList, userID = userID)
}

private fun NotesList.setIsPinned(notesToBeUpdated: List<Note>, setIsPinned: Boolean): NotesList {
    val idsToBeMarked = notesToBeUpdated.map { it.localId }.toSet()
    val newCollection = notesCollection.map {
        if (it.localId in idsToBeMarked) {
            it.copy(isPinned = setIsPinned, pinnedAt = if (setIsPinned) System.currentTimeMillis() else null)
        } else {
            it
        }
    }
    return NotesList.createNotesList(newCollection.sortByDocumentModifiedAt(), notesLoaded)
}

private fun sortById(note1: Note, note2: Note): Int {
    val remoteData1 = note1.remoteData
    val remoteData2 = note2.remoteData
    return if (remoteData1 != null && remoteData2 != null) {
        remoteData1.id.compareTo(remoteData2.id)
    } else if (remoteData1 != null && remoteData2 == null) {
        -1
    } else if (remoteData1 == null && remoteData2 != null) {
        1
    } else {
        note1.localId.compareTo(note2.localId)
    }
}

fun State.replaceNote(replacement: Note): State {
    if (replacement.isSamsungNote()) {
        return replaceSamsungNote(replacement)
    }
    val userID = userIDForLocalNoteID(replacement.localId)
    val newNotesList = getUserStateForUserID(userID).notesList.replaceNote(replacement)
    return withNotesListForUser(notesList = newNotesList, userID = userID)
}

fun State.replaceNotes(notes: List<Note>, userID: String): State {
    if (notes.isEmpty()) {
        return this
    }
    val newNotesList = getUserStateForUserID(userID).notesList.replaceNotes(notes)
    return withNotesListForUser(notesList = newNotesList, userID = userID)
}

fun State.deleteNotes(notes: List<Note>, userID: String): State {
    if (notes.isEmpty()) {
        return this
    }

    val newNoteLocalIDToUserIDMap = noteLocalIDToUserIDMap.toMutableMap()
    notes.forEach { newNoteLocalIDToUserIDMap.remove(it.localId) }

    val updatedUserState = getUserStateForUserID(userID).deleteNotes(notes)
    return newState(userID = userID, updatedUserState = updatedUserState, updatedNoteIDToUserIDMap = newNoteLocalIDToUserIDMap)
}

fun State.deleteNoteByLocalId(noteLocalId: String, userID: String): State {
    if (noteLocalId.isEmpty()) {
        return this
    }

    val updatedUserState = getUserStateForUserID(userID).deleteNoteByLocalId(noteLocalId)
    val newNoteLocalIDToUserIDMap = noteLocalIDToUserIDMap.toMutableMap()

    newNoteLocalIDToUserIDMap.remove(noteLocalId)
    return newState(
        userID = userID, updatedUserState = updatedUserState,
        updatedNoteIDToUserIDMap = newNoteLocalIDToUserIDMap
    )
}

fun State.deleteAllNotes(): State {
    val newUserIdToUserStateMap = userIDToUserStateMap.toMutableMap()
    userIDToUserStateMap.forEach { (userID, userState) ->
        newUserIdToUserStateMap.put(
            userID,
            UserState(
                notesList = NotesList.createNotesList(
                    notesLoaded =
                    userState.notesList.notesLoaded,
                    notesCollection = emptyList()
                )
            )
        )
    }
    return copy(userIDToUserStateMap = newUserIdToUserStateMap, noteLocalIDToUserIDMap = emptyMap())
}

fun State.deleteAllNotesForUserID(userID: String): State {
    val userState = getUserStateForUserID(userID)
    val newUserIdToUserStateMap = userIDToUserStateMap.toMutableMap()
    newUserIdToUserStateMap[userID] = UserState(
        notesList = NotesList.createNotesList(
            notesLoaded =
            userState.notesList.notesLoaded,
            notesCollection = emptyList()
        )
    )
    val newNoteLocalIDToUserIDMap = noteLocalIDToUserIDMap.filter { it.value != userID }

    return copy(userIDToUserStateMap = newUserIdToUserStateMap, noteLocalIDToUserIDMap = newNoteLocalIDToUserIDMap)
}

fun State.markNoteWithLocalIdAsDeleted(localId: String): State {
    val userID = userIDForLocalNoteID(localId)
    val userState = getUserStateForUserID(userID)
    val updatedUserState = userState.copy(
        userState.notesList.setIsDeletedForNoteWithLocalId(
            localId,
            isDeleted = true
        )
    )
    return newState(userID, updatedUserState)
}

fun State.unmarkNoteWithLocalIdAsDeleted(localId: String): State {
    val userID = userIDForLocalNoteID(localId)
    val userState = getUserStateForUserID(userID)
    val updatedUserState = userState.copy(
        userState.notesList.setIsDeletedForNoteWithLocalId(
            localId,
            isDeleted = false
        )
    )
    return newState(userID, updatedUserState)
}

fun State.withNotesListForUser(notesList: NotesList, userID: String): State {
    val updatedUserState = getUserStateForUserID(userID).copy(notesList = notesList)
    return newState(userID, updatedUserState)
}

fun State.withNotesLoaded(newNotesLoaded: Boolean, userID: String): State {
    val userState = getUserStateForUserID(userID)
    if (newNotesLoaded != userState.notesList.notesLoaded) {
        val updatedUserState = userState.copy(
            notesList = NotesList.createNotesList(
                notesCollection = userState.notesList.notesCollection,
                notesLoaded = newNotesLoaded
            )
        )
        return newState(userID, updatedUserState)
    }
    return this
}

fun State.getNotesLoadedForUser(userID: String): Boolean =
    getUserStateForUserID(userID).notesList.notesLoaded

internal fun State.addUserState(userState: UserState, userID: String): State {
    val newNoteLocalIDToUserIDMap = addNotesToUserIDMap(userState.notesList.notesCollection, userID)
    val newUserIDToUserStateMap = userIDToUserStateMap.toMutableMap()
    newUserIDToUserStateMap.put(userID, userState)
    return copy(
        userIDToUserStateMap = newUserIDToUserStateMap,
        noteLocalIDToUserIDMap =
        newNoteLocalIDToUserIDMap
    )
}

private fun NotesList.addNotes(notes: List<Note>): NotesList {
    if (notes.isEmpty()) {
        return this
    }
    return NotesList.createNotesList(
        notesCollection = (notesCollection + notes).sortByDocumentModifiedAt(),
        notesLoaded = notesLoaded
    )
}

private fun NotesList.addDistinctNotes(notes: List<Note>): NotesList {
    if (notes.isEmpty()) {
        return this
    }

    val newNotesCollection = (notesCollection + notes).distinctBy { it.localId }
    return NotesList.createNotesList(
        newNotesCollection.sortByDocumentModifiedAt(),
        notesLoaded = notesLoaded
    )
}

private fun NotesList.replaceNote(replacement: Note): NotesList {
    val newNotesCollection = notesCollection.map {
        if (it.localId == replacement.localId) {
            replacement
        } else {
            it
        }
    }
    return NotesList.createNotesList(
        notesCollection = newNotesCollection
            .sortByDocumentModifiedAt(),
        notesLoaded = notesLoaded
    )
}

private fun NotesList.replaceNotes(notes: List<Note>): NotesList {
    val newNotesCollection = notes.fold(notesCollection) { notesCollection, replacement ->
        notesCollection.map {
            if (it.localId == replacement.localId) {
                replacement
            } else {
                it
            }
        }
    }
    return NotesList.createNotesList(
        notesCollection = newNotesCollection.sortByDocumentModifiedAt(),
        notesLoaded = notesLoaded
    )
}

private fun UserState.deleteNoteByLocalId(noteLocalId: String): UserState {
    val newNotesCollection = notesList.notesCollection.filter { !noteLocalId.contains(it.localId) }
    return copy(
        notesList = NotesList.createNotesList(
            notesCollection = newNotesCollection.sortByDocumentModifiedAt(),
            notesLoaded = notesList.notesLoaded
        )
    )
}

private fun UserState.deleteNotes(notes: List<Note>): UserState {
    val newNotesCollection = notesList.notesCollection.minus(notes)

    return copy(
        notesList = NotesList.createNotesList(
            notesCollection = newNotesCollection
                .sortByDocumentModifiedAt(),
            notesLoaded = notesList.notesLoaded
        )
    )
}
