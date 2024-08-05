package com.microsoft.notes.sync.models

import com.microsoft.notes.sync.JSON

sealed class DeltaSyncPayload {
    val noteId: String
        get() = when (this) {
            is NonDeleted -> this.note.id
            is Deleted -> this.id
        }

    companion object {
        fun fromJSON(json: JSON): DeltaSyncPayload? {
            val obj = json as? JSON.JObject
            val reason = obj?.get<JSON.JString>("reason")?.string

            return if (reason == "deleted") {
                val id = obj.get<JSON.JString>("id")?.string
                id?.let { DeltaSyncPayload.Deleted(id = it) }
            } else {
                RemoteNote.fromJSON(json)?.let { DeltaSyncPayload.NonDeleted(note = it) }
            }
        }
    }

    data class NonDeleted(val note: RemoteNote) : DeltaSyncPayload()
    data class Deleted(val id: String) : DeltaSyncPayload()
}
