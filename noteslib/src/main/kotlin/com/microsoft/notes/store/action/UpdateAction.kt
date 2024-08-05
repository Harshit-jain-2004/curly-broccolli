package com.microsoft.notes.store.action

import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.Reminder
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Range

sealed class UpdateAction(val userID: String) : Action {
    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is PinNotes -> "PinNotes"
            is UnpinNotes -> "UnpinNotes"
            is UpdateActionWithId -> "UpdateActionWithId"
        }
        return "UpdateAction.$actionType"
    }

    class PinNotes(val toPinNotes: List<Note>, userID: String) : UpdateAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: changes = $toPinNotes}"
    }

    class UnpinNotes(val toUnpinNotes: List<Note>, userID: String) : UpdateAction(userID) {
        override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: changes = $toUnpinNotes}"
    }

    sealed class UpdateActionWithId(userID: String) : UpdateAction(userID) {
        abstract val noteLocalId: String

        override fun toLoggingIdentifier(): String {
            val actionType = when (this) {
                is UpdateDocumentRange -> "UpdateDocumentRange"
                is UpdateNoteWithDocumentAction -> "UpdateNoteWithDocumentAction"
                is UpdateNoteWithColorAction -> "UpdateNoteWithColorAction"
                is UpdateNoteWithAddedMediaAction -> "UpdateNoteWithAddedMediaAction"
                is UpdateNoteWithRemovedMediaAction -> "UpdateNoteWithRemovedMediaAction"
                is UpdateNoteWithUpdateMediaAltTextAction -> "UpdateNoteWithUpdateMediaAltTextAction"
                is UpdateTimeReminderAction -> "UpdateTimeReminderAction"
            }

            return "UpdateActionWithId.$actionType"
        }

        class UpdateDocumentRange(
            override val noteLocalId: String,
            val newRange: Range,
            val documentModifiedAt: Long = System.currentTimeMillis(),
            userID: String
        ) : UpdateActionWithId(userID) {
            override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: noteId = $noteLocalId"
        }

        class UpdateNoteWithDocumentAction(
            override val noteLocalId: String,
            val updatedDocument: Document,
            val uiRevision: Long,
            val documentModifiedAt: Long = System.currentTimeMillis(),
            userID: String
        ) : UpdateActionWithId(userID) {
            override fun toPIIFreeString(): String =
                "${toLoggingIdentifier()}: noteLocalId = $noteLocalId, uiRevision = $uiRevision"
        }

        class UpdateNoteWithColorAction(
            override val noteLocalId: String,
            val color: Color,
            val uiRevision: Long,
            userID: String
        ) : UpdateActionWithId(userID) {
            override fun toPIIFreeString(): String =
                "${toLoggingIdentifier()}: noteLocalId = $noteLocalId, uiRevision = $uiRevision"
        }

        class UpdateNoteWithAddedMediaAction(
            override val noteLocalId: String,
            val media: Media,
            val uiRevision: Long,
            userID: String
        ) : UpdateActionWithId(userID) {
            override fun toPIIFreeString(): String =
                "${toLoggingIdentifier()}: noteLocalId = $noteLocalId, uiRevision = $uiRevision"
        }

        class UpdateNoteWithRemovedMediaAction(
            override val noteLocalId: String,
            val media: Media,
            val uiRevision: Long,
            userID: String
        ) : UpdateActionWithId(userID) {
            override fun toPIIFreeString(): String =
                "${toLoggingIdentifier()}: noteLocalId = $noteLocalId, uiRevision = $uiRevision"
        }

        class UpdateNoteWithUpdateMediaAltTextAction(
            override val noteLocalId: String,
            val mediaLocalId: String,
            val altText: String?,
            val uiRevision: Long,
            userID: String
        ) : UpdateActionWithId(userID) {
            override fun toPIIFreeString(): String =
                "${toLoggingIdentifier()}: noteLocalId = $noteLocalId, uiRevision = $uiRevision"
        }

        class UpdateTimeReminderAction(
            override val noteLocalId: String,
            val timeReminder: Reminder.TimeReminder,
            userID: String,
            val uiRevision: Long
        ) : UpdateActionWithId(userID) {
            override fun toPIIFreeString(): String = "${toLoggingIdentifier()}: noteId = $noteLocalId"
        }
    }
}
