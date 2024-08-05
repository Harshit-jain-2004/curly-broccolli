package com.microsoft.notes.store.action

import com.microsoft.notes.models.MeetingNote
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.extensions.describe
import com.microsoft.notes.utils.utils.UserInfo

sealed class ReadAction(val userID: String) : Action {

    override fun toLoggingIdentifier(): String {
        val actionType = when (this) {
            is FetchAllNotesAction -> "FetchAllNotesAction"
            is NotesLoadedAction -> "NotesLoadedAction"
            is RetrieveDeltaTokensForAllNoteTypes -> "RetrieveDeltaTokensForAllNoteTypes"
            is DeltaTokenLoadedAction -> "DeltaTokenLoadedAction"
        }

        return "ReadAction.$actionType"
    }

    class FetchAllNotesAction(userID: String, val isSamsungNotesSyncEnabled: Boolean) : ReadAction(userID)

    class NotesLoadedAction(
        val allNotesLoaded: Boolean,
        val notesCollection: List<Note>,
        val samsungNotesCollection: List<Note>,
        val noteReferencesCollection: List<NoteReference>,
        val meetingNotesCollection: List<MeetingNote>,
        userID: String
    ) : ReadAction(userID) {
        override fun toPIIFreeString(): String {
            return "${toLoggingIdentifier()}: allNotesLoaded = $allNotesLoaded" +
                ", notesCollection = ${notesCollection.describe()}" +
                ", samsungNotesCollection = ${samsungNotesCollection.describe()}"
        }
    }

    class RetrieveDeltaTokensForAllNoteTypes(
        val userInfo: UserInfo
    ) : ReadAction(userInfo.userID)

    class DeltaTokenLoadedAction(
        val deltaToken: String?,
        val samsungDeltaToken: String?,
        val noteReferencesDeltaToken: String?,
        val userInfo: UserInfo
    ) : ReadAction(userInfo.userID)
}
