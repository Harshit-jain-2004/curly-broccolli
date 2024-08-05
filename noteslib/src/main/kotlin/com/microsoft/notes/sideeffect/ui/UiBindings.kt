package com.microsoft.notes.sideeffect.ui

import com.microsoft.notes.models.AccountType
import com.microsoft.notes.models.MeetingNote
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.store.AuthState
import com.microsoft.notes.store.SyncErrorState
import com.microsoft.notes.ui.feed.filter.FeedFilters
import com.microsoft.notes.ui.feed.recyclerview.FeedLayoutType
import com.microsoft.notes.ui.feed.sourcefilter.FeedSourceFilterOption
import com.microsoft.notes.ui.noteslist.UserNotifications
import java.io.Serializable
import java.net.URL

interface UiBindings

interface AuthChanges : UiBindings {
    fun authChanged(auth: AuthState, userID: String)
    fun accountInfoForIntuneProtection(databaseName: String, userID: String, accountType: AccountType)
    fun onRequestClientAuth(userID: String)
}

interface InkNoteChanges : UiBindings {
    fun clearCanvas()
}

interface NoteChanges : UiBindings {
    fun notesUpdated(stickyNotesCollectionsByUser: HashMap<String, List<Note>>, notesLoaded: Boolean)
    fun noteDeleted()
}

interface Notifications : UiBindings {
    sealed class SyncError : Serializable {
        data class NoMailbox(val errorMessageResId: Int, val supportUrl: URL?) : SyncError()
        data class QuotaExceeded(val errorMessageResId: Int, val supportUrl: URL?) : SyncError()
        data class GenericError(val errorMessageResId: Int, val supportUrl: URL?) : SyncError()
    }

    fun upgradeRequired()
    fun syncErrorOccurred(error: SyncError, userID: String)
}

interface UserNotificationsUpdates : UiBindings {
    fun updateUserNotifications(userIdToNotificationsMap: Map<String, UserNotifications>)
}

/**
 * This binding is informational-only, the client app does not need to take any user-facing action.
 * For user-facing errors which need to be handled, see AuthChanges and Notifications UIBindings.
 */
interface SyncStateUpdates : UiBindings {
    enum class SyncErrorType {
        NetworkUnavailable,
        Unauthenticated,
        AutoDiscoverGenericFailure,
        EnvironmentNotSupported,
        UserNotFoundInAutoDiscover,
        SyncPaused,
        SyncFailure
    }

    /**
     * Called when sync starts.
     */
    fun remoteNotesSyncStarted(userID: String)

    /**
     * Called if sync cannot proceed at the moment.
     * remoteNotesSyncFinished() will be called if sync operation completes at a later point.
     */
    fun remoteNotesSyncErrorOccurred(errorType: SyncErrorType, userID: String)

    /**
     * Called when sync finishes.
     */
    fun remoteNotesSyncFinished(successful: Boolean, userID: String)

    /**
     * Called when account is switched.
     */
    fun accountSwitched(syncErrorState: SyncErrorState, userID: String)
}

interface NoteReferencesSyncStateUpdates : UiBindings {
    /**
     * Called when note reference sync starts.
     */
    fun remoteNoteReferencesSyncStarted(userID: String)

    /**
     * Called when note reference sync finishes.
     */
    fun remoteNoteReferencesSyncFinished(successful: Boolean, userID: String)
}

interface SamsungNotesSyncStateUpdates : UiBindings {
    /**
     * Called when samsung notes sync starts.
     */
    fun remoteSamsungNotesSyncStarted(userID: String)

    /**
     * Called when samsung notes sync finishes.
     */
    fun remoteSamsungNotesSyncFinished(successful: Boolean, userID: String)
}

interface MeetingNotesSyncStateUpdates : UiBindings {
    /**
     * Called when meeting notes sync starts.
     */
    fun remoteMeetingNotesSyncStarted(userID: String)

    /**
     * Called when samsung notes sync finishes.
     */
    fun remoteMeetingNotesSyncFinished(successful: Boolean, userID: String)
}

interface NotesList : UiBindings {
    fun noteFromListTapped(note: Note)
    fun addNewNoteTapped(note: Note)
    fun manualSyncStarted()
    fun manualSyncCompleted()
}

interface Search : UiBindings {
    fun noteFromSearchTapped(note: Note)
}

interface FeedSearch : UiBindings {
    fun noteFromFeedSearchTapped(note: Note)
}

interface NoteOptions : UiBindings {
    fun noteOptionsDismissed()
    fun noteOptionsSendFeedbackTapped()
    fun noteOptionsNoteDeleted()
    fun noteOptionsColorPicked()
    fun noteOptionsSearchInNote()
    fun noteOptionsNoteShared()
}

interface NoteActionMode : UiBindings {
    fun organiseNote(note: Note)
}

interface EditNote : UiBindings {
    fun addPhotoTapped()
    fun microPhoneButtonTapped()
    fun imageCompressionCompleted(successful: Boolean)
    fun noteFirstEdited()
    fun captureNoteTapped()
    fun scanButtonTapped()
}

interface ActionModeUpdateForFeed : UiBindings {
    /*
     * this function gives control to the client to finishActionMode when required
     */
    fun finishActionMode()
    fun invalidateActionMode()
}

interface NoteReferenceChanges : UiBindings {
    fun noteReferencesUpdated(noteReferencesCollectionsByUser: HashMap<String, List<NoteReference>>)
}

interface SamsungNoteChanges : UiBindings {
    fun samsungNotesUpdated(samsungNotesCollectionsByUser: HashMap<String, List<Note>>)
}

interface MeetingNoteChanges : UiBindings {
    fun meetingNotesUpdated(meetingNotesCollectionsByUser: HashMap<String, List<MeetingNote>>)
}

interface FeedSourceFilterOptions : UiBindings {
    fun sourceFilterSelected(source: FeedSourceFilterOption)
}

interface ComprehensiveFeedFilterOptions : UiBindings {
    fun filtersSelected(
        stickyNotesListsByUsers: HashMap<String, List<Note>>,
        samsungNotesListsByUsers: HashMap<String, List<Note>>,
        notesReferenceListsByUsers: HashMap<String, List<NoteReference>>,
        feedFilters: FeedFilters,
        scrollToTop: Boolean
    )
}

interface NoteReferencesSyncRequest : UiBindings {
    fun syncNoteReferences(userID: String)
}

interface SamsungNotesSyncRequest : UiBindings {
    fun syncSamsungNotes(userID: String)
}

interface MeetingNotesSyncRequest : UiBindings {
    fun syncMeetingNotes(userID: String)
}

interface FeedSwipeToRefreshSync : UiBindings {
    fun feedSyncStarted()
    fun feedSyncCompleted()
}

interface FeedLayoutChanges : UiBindings {
    fun changeFeedLayout(layoutType: FeedLayoutType)
}

interface DisplayFilterAndSortPanel : UiBindings {
    fun displayFilterAndSortPanel()
}

interface UndoRedoPerformedInNotesEditText : UiBindings {
    fun undo()
    fun redo()
}
