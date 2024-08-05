package com.microsoft.notes.store.action

import com.microsoft.notes.utils.utils.toNullabilityIdentifierString

sealed class DeleteAction(val userID: String) : Action {

    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is DeleteAllNotesAction -> "DeleteAllNotesAction"
            is MarkNoteAsDeletedAction -> "MarkNoteAsDeletedAction"
            is MarkNoteReferenceAsDeletedAction -> "MarkNoteReferenceAsDeletedAction"
            is MarkSamsungNoteAsDeletedAction -> "MarkSamsungNoteAsDeletedAction"
            is UnmarkNoteAsDeletedAction -> "UnmarkNoteAsDeletedAction"
            is CleanupNotesMarkedAsDeletedAction -> "CleanupNotesMarkedAsDeletedAction"
        }

        return "DeleteAction.$actionType"
    }

    class DeleteAllNotesAction(userID: String) : DeleteAction(userID)
    class MarkNoteAsDeletedAction(val localId: String, val remoteId: String?, userID: String, val isUserTriggered: Boolean = false) :
        DeleteAction(userID) {
        override fun toPIIFreeString(): String {
            return "${toLoggingIdentifier()}: noteId = $localId" +
                ", remoteId = ${toNullabilityIdentifierString(remoteId)}"
        }
    }

    class MarkNoteReferenceAsDeletedAction(val localId: String, val remoteId: String?, userID: String, val isUserTriggered: Boolean = false) :
        DeleteAction(userID) {
        override fun toPIIFreeString(): String {
            return "${toLoggingIdentifier()}: noteId = $localId" +
                ", remoteId = ${toNullabilityIdentifierString(remoteId)}"
        }
    }

    class MarkSamsungNoteAsDeletedAction(val localId: String, val remoteId: String?, userID: String) :
        DeleteAction(userID) {
        override fun toPIIFreeString(): String {
            return "${toLoggingIdentifier()}: noteId = $localId" +
                ", remoteId = ${toNullabilityIdentifierString(remoteId)}"
        }
    }

    class UnmarkNoteAsDeletedAction(val localId: String, val remoteId: String?, userID: String) :
        DeleteAction(userID) {
        override fun toPIIFreeString(): String {
            return "${toLoggingIdentifier()}: noteId = $localId" +
                ", remoteId = ${toNullabilityIdentifierString(remoteId)}"
        }
    }

    class CleanupNotesMarkedAsDeletedAction(userID: String) : DeleteAction(userID)
}
