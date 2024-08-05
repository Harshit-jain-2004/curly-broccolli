package com.microsoft.notes.store.action

import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.NoteReferenceChanges
import com.microsoft.notes.models.NoteReferenceMedia
import com.microsoft.notes.utils.utils.toNullabilityIdentifierString

sealed class NoteReferenceAction(val userID: String) : Action {

    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is ApplyChanges -> "ApplyChanges"
            is MarkAsDeleted -> "MarkAsDeleted"
            is PinNoteReference -> "pinNoteReference"
            is UnpinNoteReference -> "unpinNoteReference"
            is UpdateNoteReferenceMedia -> "UpdateNoteReferenceMedia"
        }

        return "NoteReferenceAction.$actionType"
    }

    class ApplyChanges(val changes: NoteReferenceChanges, userID: String, val deltaToken: String?, val isLocalChange: Boolean = false) :
        NoteReferenceAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: changes = $changes" +
            ", deltaToken = ${toNullabilityIdentifierString(deltaToken)}"
    }

    class MarkAsDeleted(val toMarkAsDeleted: List<NoteReference>, userID: String) : NoteReferenceAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: changes = $toMarkAsDeleted"
    }

    class PinNoteReference(val toPinNoteReferences: List<NoteReference>, userID: String) : NoteReferenceAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: changes = $toPinNoteReferences}"
    }

    class UnpinNoteReference(val toUnpinNoteReferences: List<NoteReference>, userID: String) : NoteReferenceAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: changes = $toUnpinNoteReferences}"
    }

    class UpdateNoteReferenceMedia(val noteReference: NoteReference, userID: String, val media: List<NoteReferenceMedia>?) : NoteReferenceAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: changes = $noteReference}"
    }
}
