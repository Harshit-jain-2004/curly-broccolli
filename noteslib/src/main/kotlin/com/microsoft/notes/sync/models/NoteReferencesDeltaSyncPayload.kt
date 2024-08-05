package com.microsoft.notes.sync.models
import com.microsoft.notes.sync.JSON

sealed class NoteReferencesDeltaSyncPayload {
    val noteId: String
        get() = when (this) {
            is NonDeleted -> this.note.id
            is Deleted -> this.id
        }

    companion object {
        fun fromJSON(json: JSON): NoteReferencesDeltaSyncPayload? {
            val obj = json as? JSON.JObject
            val reason = obj?.get<JSON.JString>("reason")?.string

            return if (reason == "deleted") {
                val id = obj.get<JSON.JString>("id")?.string
                id?.let { NoteReferencesDeltaSyncPayload.Deleted(id = it) }
            } else {
                RemoteNoteReference.fromJSON(json)?.let { NoteReferencesDeltaSyncPayload.NonDeleted(note = it) }
            }
        }
    }

    data class NonDeleted(val note: RemoteNoteReference) : NoteReferencesDeltaSyncPayload()
    data class Deleted(val id: String) : NoteReferencesDeltaSyncPayload()
}
