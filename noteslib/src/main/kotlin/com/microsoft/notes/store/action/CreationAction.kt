package com.microsoft.notes.store.action

import com.microsoft.notes.models.Note

sealed class CreationAction(val userID: String) : Action {

    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is AddNoteAction -> "AddNoteAction"
        }

        return "CreationAction.$actionType"
    }

    class AddNoteAction(val note: Note, userID: String) : CreationAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: noteId = ${note.localId}"
    }
}
