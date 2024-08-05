package com.microsoft.notes.store

import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.extensions.findAndMap
import com.microsoft.notes.ui.noteslist.UserNotifications
import com.microsoft.notes.utils.utils.Constants
import java.io.Serializable

enum class AuthState {
    /**
     * User is not authenticated.
     * He might never signed in or logged out.
     */
    UNAUTHENTICATED,
    /**
     * User is signed in and we
     * have a valid token in SDK
     */
    AUTHENTICATED,
    /**
     * User is signed in but we
     * don't have a valid token in SDK
     */
    NOT_AUTHORIZED
}

enum class OutboundSyncState {
    Active,
    Inactive
}

class NotesList private constructor(
    val notesCollection: List<Note> = emptyList(),
    val notesLoaded: Boolean = false
) {
    companion object {
        val EMPTY_LIST = NotesList()
        internal fun createNotesList(
            notesCollection: List<Note>,
            notesLoaded: Boolean
        ): NotesList = NotesList(notesCollection, notesLoaded)

        fun emptyNotesList(): NotesList = EMPTY_LIST
    }

    fun getNote(id: String): Note? = notesCollection.find { it.localId == id }

    private fun sortById(note1: Note, note2: Note): Int {
        val remoteData1 = note1.remoteData
        val remoteData2 = note2.remoteData
        return if (remoteData1 != null && remoteData2 != null) {
            remoteData1.id.compareTo(remoteData2.id)
        } else if (remoteData1 != null && remoteData2 == null) {
            -1
        } else if (remoteData1 == null && remoteData2 != null) {
            1
        } else {
            note1.localId.compareTo(note2.localId)
        }
    }

    fun setIsDeletedForNoteWithLocalId(localId: String, isDeleted: Boolean): NotesList {
        val newNotesCollection = notesCollection.findAndMap(
            find = { it.localId == localId },
            map = { it.copy(isDeleted = isDeleted) }
        )
        return createNotesList(
            notesCollection = newNotesCollection,
            notesLoaded = notesLoaded
        )
    }
}

class SamsungNotesList private constructor(
    val samsungNotesCollection: List<Note> = emptyList()
) {
    companion object {
        val EMPTY_LIST = SamsungNotesList()
        internal fun createSamsungNotesList(
            samsungNotesCollection: List<Note>
        ): SamsungNotesList = SamsungNotesList(samsungNotesCollection)

        fun emptySamsungNotesList(): SamsungNotesList = EMPTY_LIST
    }

    fun getSamsungNote(id: String): Note? = samsungNotesCollection.find { it.localId == id }

    fun setIsDeletedForSamsungNoteWithLocalId(localId: String, isDeleted: Boolean): SamsungNotesList {
        val newSamsungNotesCollection = samsungNotesCollection.findAndMap(
            find = { it.localId == localId },
            map = { it.copy(isDeleted = isDeleted) }
        )
        return createSamsungNotesList(newSamsungNotesCollection)
    }
}

class NoteReferencesList private constructor(
    val noteReferencesCollection: List<NoteReference> = emptyList()
) {
    companion object {
        val EMPTY_LIST = NoteReferencesList()
        internal fun createNoteReferencesList(
            noteReferencesCollection: List<NoteReference>
        ): NoteReferencesList = NoteReferencesList(noteReferencesCollection)

        fun emptyNotesList(): NoteReferencesList = EMPTY_LIST
    }

    fun setIsDeletedForNoteReferenceWithLocalId(localId: String, isDeleted: Boolean): NoteReferencesList {
        val newNoteReferencesCollection = noteReferencesCollection.findAndMap(
            find = { it.localId == localId },
            map = { it.copy(isDeleted = isDeleted) }
        )
        return createNoteReferencesList(newNoteReferencesCollection)
    }
}

data class AuthenticationState(val authState: AuthState = AuthState.UNAUTHENTICATED)

sealed class SyncErrorState : Serializable {
    object None : SyncErrorState()
    object NoMailbox : SyncErrorState()
    object QuotaExceeded : SyncErrorState()
    object GenericError : SyncErrorState()
    object NetworkUnavailable : SyncErrorState()
    object Unauthenticated : SyncErrorState()
    object AutoDiscoverGenericFailure : SyncErrorState()
    object EnvironmentNotSupported : SyncErrorState()
    object UserNotFoundInAutoDiscover : SyncErrorState()
    object UpgradeRequired : SyncErrorState()
}

data class UserState(
    val notesList: NotesList = NotesList.emptyNotesList(),
    val samsungNotesList: SamsungNotesList = SamsungNotesList.emptySamsungNotesList(),
    val noteReferencesList: NoteReferencesList = NoteReferencesList.emptyNotesList(),
    val authenticationState: AuthenticationState = AuthenticationState(),
    val outboundSyncState: OutboundSyncState = OutboundSyncState.Active,
    val currentSyncErrorState: SyncErrorState = SyncErrorState.None,
    val userNotifications: UserNotifications = UserNotifications(),
    val email: String = ""
) {
    companion object {
        val EMPTY_USER_STATE = UserState()
    }
}

data class State(
    internal val userIDToUserStateMap: Map<String, UserState> = mapOf(),
    internal val noteLocalIDToUserIDMap: Map<String, String> = mapOf(),
    internal val samsungNoteLocalIDToUserIDMap: Map<String, String> = mapOf(),
    internal val noteReferenceLocalIDToUserIDMap: Map<String, String> = mapOf(),
    internal val meetingNoteLocalIDToUserIDMap: Map<String, String> = mapOf(),
    val currentUserID: String = Constants.EMPTY_USER_ID
) {
    override fun toString(): String = ""

    fun userIDForLocalNoteID(localNoteID: String): String =
        noteLocalIDToUserIDMap[localNoteID]
            ?: samsungNoteLocalIDToUserIDMap[localNoteID]
            ?: Constants.EMPTY_USER_ID

    fun userIDForLocalNoteReferenceID(localNoteReferenceID: String): String =
        noteReferenceLocalIDToUserIDMap.get(localNoteReferenceID) ?: Constants.EMPTY_USER_ID

    internal fun newState(
        userID: String,
        updatedUserState: UserState,
        updatedNoteIDToUserIDMap: Map<String, String> = noteLocalIDToUserIDMap,
        updatedSamsungNoteIDToUserIDMap: Map<String, String> = samsungNoteLocalIDToUserIDMap,
        newSelectedUserID: String = currentUserID,
        updatedNoteReferenceIDToUserIDMap: Map<String, String> = noteReferenceLocalIDToUserIDMap
    ): State {
        val mutableUserIDMap = userIDToUserStateMap.toMutableMap()
        mutableUserIDMap[userID] = updatedUserState
        return copy(
            userIDToUserStateMap = mutableUserIDMap,
            noteLocalIDToUserIDMap = updatedNoteIDToUserIDMap,
            samsungNoteLocalIDToUserIDMap = updatedSamsungNoteIDToUserIDMap,
            noteReferenceLocalIDToUserIDMap = updatedNoteReferenceIDToUserIDMap,
            currentUserID = newSelectedUserID
        )
    }
}
