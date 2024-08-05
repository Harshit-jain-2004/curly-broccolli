package com.microsoft.notes.store

import com.microsoft.notes.models.Note

fun State.combinedNotesForAllUsers(): List<Note> {
    val notes = mutableListOf<Note>()
    for ((_, userState) in userIDToUserStateMap) {
        notes.addAll(userState.notesList.notesCollection)
    }
    return notes.distinctBy { it.localId }.sortByDocumentModifiedAt()
}

fun State.areNotesLoadedForAnyUser(): Boolean =
    userIDToUserStateMap.any { (_, userState) -> userState.notesList.notesLoaded }

fun State.combinedNotesListForAllUsers(): NotesList =
    NotesList.createNotesList(combinedNotesForAllUsers(), areNotesLoadedForAnyUser())

fun State.combinedSamsungNotesForAllUsers(): List<Note> =
    userIDToUserStateMap.values
        .flatMap { it.samsungNotesList.samsungNotesCollection }
        .distinctBy { it.localId }.sortByDocumentModifiedAt()
