package com.microsoft.notes.store.reducer

import com.microsoft.notes.models.Color
import com.microsoft.notes.models.LOCAL_ID
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.RemoteData

val REMOTE_ID = "remoteId"
val CHANGE_KEY = "change_key"

fun dummyNotesCollection(): List<Note> {
    val note1 = Note(
        localId = LOCAL_ID,
        remoteData = RemoteData(
            REMOTE_ID, CHANGE_KEY,
            Note(localId = LOCAL_ID),
            createdAt = "2018-01-31T16:45:05.0000000Z",
            lastModifiedAt = "2018-01-31T16:50:31.0000000Z"
        ),
        documentModifiedAt = 2,
        color = Color.GREEN
    )
    val note2 = Note(
        localId = LOCAL_ID + 2,
        remoteData = RemoteData(
            REMOTE_ID + 2, CHANGE_KEY,
            Note(localId = LOCAL_ID + 2),
            createdAt = "2018-01-31T16:45:05.0000000Z",
            lastModifiedAt = "2018-01-31T16:50:31.0000000Z"
        ),
        documentModifiedAt = 1,
        color = Color.BLUE
    )

    return listOf(note1, note2)
}

fun createNoteLocalIdToUserIDMap(notes: List<Note>, userID: String): Map<String, String> {
    val noteLocalIDToUserIDMap = mutableMapOf<String, String>()
    notes.forEach { noteLocalIDToUserIDMap.put(it.localId, userID) }
    return noteLocalIDToUserIDMap
}

fun createNoteReferenceLocalIdToUserIDMap(notes: List<NoteReference>, userID: String): Map<String, String> {
    val noteReferenceLocalIDToUserIDMap = mutableMapOf<String, String>()
    notes.forEach { noteReferenceLocalIDToUserIDMap.put(it.localId, userID) }
    return noteReferenceLocalIDToUserIDMap
}
