@file:Suppress("UNUSED_PARAMETER")

package com.microsoft.notes.sideeffect.persistence.handler

import com.microsoft.notes.models.Media
import com.microsoft.notes.models.RemoteData
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.sideeffect.persistence.extensions.toJson
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceNote
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.models.Note as StoreNote

fun insertNoteIntoDB(storeNote: StoreNote, notesDB: NotesDatabase, notesLogger: NotesLogger? = null) {
    val note = storeNote.toPersistenceNote()
    notesDB.noteDao().insert(note = note)
}

fun updateDocument(noteID: String, document: Document, notesDB: NotesDatabase, notesLogger: NotesLogger? = null) {
    notesDB.noteDao().updateDocument(noteId = noteID, document = document.toJson())
}

fun updateMedia(noteId: String, media: List<Media>, notesDB: NotesDatabase, notesLogger: NotesLogger? = null) {
    notesDB.noteDao().updateMedia(noteId = noteId, media = media.toJson())
}

fun updateRemoteData(
    noteID: String,
    remoteData: RemoteData,
    notesDB: NotesDatabase,
    notesLogger: NotesLogger? = null
) {
    notesDB.noteDao().updateRemoteData(noteId = noteID, remoteData = remoteData.toJson())
}
