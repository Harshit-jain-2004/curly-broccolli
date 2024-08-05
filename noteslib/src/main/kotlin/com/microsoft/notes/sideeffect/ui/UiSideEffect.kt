package com.microsoft.notes.sideeffect.ui

import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.store.AuthState
import com.microsoft.notes.store.SideEffect
import com.microsoft.notes.store.State
import com.microsoft.notes.store.Store
import com.microsoft.notes.store.SyncErrorState
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.AuthAction
import com.microsoft.notes.store.action.AuthenticatedSyncRequestAction
import com.microsoft.notes.store.action.CompoundAction
import com.microsoft.notes.store.action.DeleteAction.MarkNoteAsDeletedAction
import com.microsoft.notes.store.action.NoteReferenceAction
import com.microsoft.notes.store.action.SamsungNotesResponseAction
import com.microsoft.notes.store.action.SyncRequestAction
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.action.SyncStateAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteMeetingNotesSyncFailedAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteMeetingNotesSyncStartedAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteMeetingNotesSyncSucceededAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteNoteReferencesSyncFailedAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteNoteReferencesSyncStartedAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteNoteReferencesSyncSucceededAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteNotesSyncErrorAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteNotesSyncErrorAction.SyncErrorType
import com.microsoft.notes.store.action.SyncStateAction.RemoteNotesSyncFailedAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteNotesSyncStartedAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteNotesSyncSucceededAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteSamsungNotesSyncFailedAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteSamsungNotesSyncStartedAction
import com.microsoft.notes.store.action.SyncStateAction.RemoteSamsungNotesSyncSucceededAction
import com.microsoft.notes.store.action.UIAction
import com.microsoft.notes.store.action.UpdateAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateTimeReminderAction
import com.microsoft.notes.store.areNotesLoadedForAnyUser
import com.microsoft.notes.store.getNoteForNoteLocalId
import com.microsoft.notes.store.getNoteReferencesCollectionForUser
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.store.getNotesLoadedForUser
import com.microsoft.notes.store.getNotificationsForAllUsers
import com.microsoft.notes.store.getSamsungNotesCollectionForUser
import com.microsoft.notes.store.getUserStateForUserID
import com.microsoft.notes.ui.extensions.isSamsungNote
import com.microsoft.notes.ui.feed.filter.FeedFilters
import com.microsoft.notes.ui.feed.recyclerview.FeedLayoutType
import com.microsoft.notes.ui.feed.sourcefilter.FeedSourceFilterOption
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.threading.ExecutorServices
import com.microsoft.notes.utils.threading.ThreadExecutor
import com.microsoft.notes.utils.threading.ThreadExecutorService
import java.io.File
import java.lang.ref.WeakReference
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList

class UiThreadService : ThreadExecutorService(
    ExecutorServices.uiBindings
)

typealias UiBindingsList = CopyOnWriteArrayList<WeakReference<UiBindings>>

@Suppress("LargeClass")
class UiSideEffect(
    val store: Store,
    val uiThread: ThreadExecutor? = null,
    val notesLogger: NotesLogger? = null,
    val combinedListForMultiAccountEnabled: Boolean = false,
    val samsungNotesSyncEnabled: Boolean = false
) : SideEffect(uiThread) {

    private val uiBindingsList: UiBindingsList = UiBindingsList()

    fun addUiBindings(uiBindings: UiBindings): Boolean {
        val item = uiBindingsList.find { it.get() == uiBindings }
        return item?.let { true } ?: uiBindingsList.add(WeakReference(uiBindings))
    }

    fun removeUiBindings(uiBindings: UiBindings): Boolean {
        val item = uiBindingsList.find { it.get() == uiBindings }
        return item?.let { uiBindingsList.remove(it) } ?: false
    }

    @Suppress("LongMethod")
    override fun handle(action: Action, state: State) {
        when (action) {
            is CompoundAction -> action.actions.forEach { handle(it, state) }
            is AuthAction -> handleAuthAction(action, state)
            is SyncResponseAction.NotAuthorized -> handleNotAuthorized(action.userID)
            is SyncResponseAction.ForbiddenSyncError -> handleForbiddenSyncError(action)
            is SyncRequestAction.DeleteNote -> handleDeleteNote(state, action)
            is SyncRequestAction.DeleteNoteReference -> handleDeleteNoteReference(state, action)
            is SyncRequestAction.DeleteSamsungNote -> handleSamsungDeleteNote(state, action)
            is MarkNoteAsDeletedAction -> handleMarkedNoteDeleted(state, action.isUserTriggered)
            is SyncResponseAction.ServiceUpgradeRequired -> handleServiceUpgradeRequired()
            is SyncStateAction -> handleSyncUpdates(action, state)
            is UIAction.AddNewNote -> handleAddNewNote(action)
            is UIAction.ClearCanvasAction -> handleClearCanvas()
            is UIAction.EditNote -> handleEditNote(action)
            is UIAction.EditSearchNote -> handleEditSearchNote(action)
            is UIAction.EditFeedSearchNote -> handleEditFeedSearchNote(action)
            is UIAction.SwipeToRefreshStarted -> handleSwipeToRefreshStarted()
            is UIAction.SwipeToRefreshCompleted -> handleSwipeToRefreshCompleted()
            is UIAction.FeedSwipeToRefreshStarted -> handleFeedSwipeToRefreshStarted()
            is UIAction.NoteOptionsDismissed -> handleNoteOptionsDismissed()
            is UIAction.NoteOptionsNoteDeleted -> handleNoteOptionsNoteDeleted()
            is UIAction.NoteOptionsSendFeedback -> handleNoteOptionsFeedback()
            is UIAction.NoteOptionsColorPicked -> handleNoteOptionsColorPicked()
            is UIAction.NoteOptionsSearchInNote -> handleNoteOptionsSearchInNote()
            is UIAction.NoteOptionsNoteShared -> handleNoteOptionsNoteShared()
            is UIAction.AddPhotoAction -> handleAddPhoto()
            is UIAction.CaptureNoteAction -> handleCaptureNoteAction()
            is UIAction.OnMicroPhoneButtonClickedAction -> handleOnMicroPhoneButtonClicked()
            is UIAction.OnScanButtonClickedAction -> handleOnScanButtonClicked()
            is UIAction.ImageCompressionCompleted -> handleImageCompressionCompleted(action)
            is UIAction.NoteFirstEdited -> handleNoteFirstEdited()
            is UIAction.AccountChanged -> handleAccountChanged(state, action.userID)
            is UIAction.UpdateFutureNoteUserNotification -> handleFutureNoteNotification(state, action.userID)
            is UIAction.UndoRedoInNotesEditText -> handleUndoRedoInNotesEditText(action.isRedoAction)
            is NoteReferenceAction.ApplyChanges -> if (!action.changes.isEmpty()) handleNoteReferencesUpdated(state, action.userID)
            is NoteReferenceAction.MarkAsDeleted -> handleNoteReferencesUpdated(state, action.userID)
            is NoteReferenceAction.PinNoteReference -> handleNoteReferencesUpdated(state, action.userID)
            is NoteReferenceAction.UnpinNoteReference -> handleNoteReferencesUpdated(state, action.userID)
            is SamsungNotesResponseAction.ApplyChanges -> if (!action.changes.isEmpty()) handleSamsungNotesUpdated(state, action.userID)
            is UIAction.FeedSourceFilterSelected -> handleFeedSourceFilterSelected(action.source)
            is UIAction.ComprehensiveFeedSourceFilterSelected -> handleComprehensiveFeedFilterSelected(state, action.feedFilters, action.scrollToTop)
            is UIAction.FeedNoteOrganiseAction -> handleFeedOrganiseAction(action.note)
            is UIAction.FinishActionModeOnFeed -> handleFinishActionMode()
            is UIAction.InvalidateActionModeOnFeed -> handleInvalidateActionMode()
            is UIAction.ChangeFeedLayout -> handleFeedLayoutChange(action.layoutType)
            is AuthenticatedSyncRequestAction.ManualNoteReferencesSyncRequestAction -> handleManualNoteReferencesSyncRequest(action.userID)
            is AuthenticatedSyncRequestAction.ManualSamsungNotesSyncRequestAction -> handleSamsungNotesSyncRequest(action.userID)
            is UIAction.RequestClientAuthAction -> handleRequestClientAuthAction(action.userID)
            is SyncResponseAction.ApplyChanges -> if (!action.changes.isEmpty()) handleStickyNotesUpdated(state, action.userID)
            is AuthenticatedSyncRequestAction.RemoteChangedDetected,
            is AuthenticatedSyncRequestAction.RemoteNoteReferencesChangedDetected,
            is AuthenticatedSyncRequestAction.MeetingNotesChangedDetected,
            is AuthenticatedSyncRequestAction.SamsungNotesChangedDetected -> {} // these actions don't affect UI
            is SyncResponseAction.MediaDownloaded -> handleMediaDownloaded(state, action.userID, action.noteId)
            is UIAction.DisplayFilterAndSortPanel -> handleDisplayFilterAndSortPanel()
            is UIAction.DeletedMultipleNotes -> handleDeletedMultipleNotes()
            is UpdateAction.PinNotes -> handlePinnedNotesUpdated(state, action.userID)
            is UpdateAction.UnpinNotes -> handlePinnedNotesUpdated(state, action.userID)
            is UpdateTimeReminderAction -> handleReminderUpdated(state, action.userID)
            else -> handleNotesUpdatedForOtherCases(state)
        }
    }

    private fun forEachBinding(fn: (elem: UiBindings?) -> Unit) {
        uiBindingsList.forEach {
            val uiBinding = it.get()
            fn(uiBinding)
        }
    }

    private fun handleAuthAction(action: AuthAction, state: State) {
        forEachBinding {
            if (it is AuthChanges) {
                when (action) {
                    is AuthAction.AccountInfoForIntuneProtection -> it.accountInfoForIntuneProtection(
                        databaseName = action.databaseName,
                        userID = action.userID, accountType = action.accountType
                    )

                    is AuthAction.NewAuthTokenAction,
                    is AuthAction.LogoutAction -> {
                        val userState = state.getUserStateForUserID(userID = action.userID)
                        it.authChanged(auth = userState.authenticationState.authState, userID = action.userID)
                    }
                }
            }

            if (it is UserNotificationsUpdates) {
                when (action) {
                    is AuthAction.NewAuthTokenAction,
                    is AuthAction.ClientAuthFailedAction,
                    is AuthAction.LogoutAction -> it.updateUserNotifications(state.getNotificationsForAllUsers())
                }
            }
        }
    }

    private fun handleNotAuthorized(userID: String) {
        forEachBinding {
            when (it) {
                is AuthChanges -> it.authChanged(auth = AuthState.NOT_AUTHORIZED, userID = userID)
            }
        }
    }

    private fun handleForbiddenSyncError(action: SyncResponseAction.ForbiddenSyncError) {
        val error = when (action) {
            is SyncResponseAction.ForbiddenSyncError.NoMailbox ->
                Notifications.SyncError.NoMailbox(action.errorMessage, action.supportArticle)
            is SyncResponseAction.ForbiddenSyncError.QuotaExceeded ->
                Notifications.SyncError.QuotaExceeded(action.errorMessage, action.supportArticle)
            is SyncResponseAction.ForbiddenSyncError.GenericSyncError ->
                Notifications.SyncError.GenericError(action.errorMessage, action.supportArticle)
        }
        forEachBinding {
            when (it) {
                is Notifications -> it.syncErrorOccurred(error, action.userID)
            }
        }
    }

    private fun handleDeleteNote(state: State, action: SyncRequestAction.DeleteNote) {
        state.getNoteForNoteLocalId(action.localId)?.let {
            deleteFiles(it)
        }

        forEachBinding {
            when (it) {
                is NoteChanges -> it.noteDeleted()
            }
        }
    }

    private fun handleDeleteNoteReference(state: State, action: SyncRequestAction.DeleteNoteReference) {
        // No files to delete for NoteReference as of now
        // Need to be implemented when NoteReference support image previews
    }

    private fun handleSamsungDeleteNote(state: State, action: SyncRequestAction.DeleteSamsungNote) {
        state.getNoteForNoteLocalId(action.localId)?.let {
            deleteSamsungPreviewImages(it)
        }

        forEachBinding {
            when (it) {
                is NoteChanges -> it.noteDeleted()
            }
        }
    }

    private fun handleMarkedNoteDeleted(state: State, isUserTriggered: Boolean) {
        if (isUserTriggered) {
            NotesLibrary.getInstance().showToast(R.string.sn_note_deleted)
        }
        handleStickyNotesUpdated(state, state.currentUserID)
    }

    private fun handleDeletedMultipleNotes() {
        NotesLibrary.getInstance().showToast(R.string.notes_deleted)
    }

    private fun handleServiceUpgradeRequired() {
        forEachBinding {
            when (it) {
                is Notifications -> it.upgradeRequired()
            }
        }
    }

    private fun handleSyncUpdates(action: SyncStateAction, state: State) {
        forEachBinding { binding ->
            if (binding is SyncStateUpdates) {
                handleSyncStateUpdates(action, binding)
            }
            if (binding is NoteReferencesSyncStateUpdates) {
                handleNoteReferencesSyncStateUpdates(action, binding)
            }
            if (binding is SamsungNotesSyncStateUpdates) {
                when (action) {
                    is RemoteSamsungNotesSyncStartedAction -> binding.remoteSamsungNotesSyncStarted(action.userID)
                    is RemoteSamsungNotesSyncFailedAction -> binding.remoteSamsungNotesSyncFinished(
                        successful = false,
                        userID = action.userID
                    )
                    is RemoteSamsungNotesSyncSucceededAction -> binding.remoteSamsungNotesSyncFinished(
                        successful = true,
                        userID = action.userID
                    )
                }
            }
            if (binding is MeetingNotesSyncStateUpdates) {
                when (action) {
                    is RemoteMeetingNotesSyncStartedAction -> binding.remoteMeetingNotesSyncStarted(action.userID)
                    is RemoteMeetingNotesSyncFailedAction -> binding.remoteMeetingNotesSyncFinished(
                        successful = false,
                        userID = action.userID
                    )
                    is RemoteMeetingNotesSyncSucceededAction -> binding.remoteMeetingNotesSyncFinished(
                        successful = true,
                        userID = action.userID
                    )
                }
            }
            if (binding is UserNotificationsUpdates) {
                when (action) {
                    is RemoteNotesSyncErrorAction, is RemoteNotesSyncFailedAction,
                    is RemoteNotesSyncSucceededAction ->
                        binding.updateUserNotifications(state.getNotificationsForAllUsers())
                }
            }
        }
    }

    private fun handleSyncStateUpdates(action: SyncStateAction, binding: SyncStateUpdates) {
        when (action) {
            is RemoteNotesSyncStartedAction -> binding.remoteNotesSyncStarted(action.userID)
            is RemoteNotesSyncErrorAction -> binding.remoteNotesSyncErrorOccurred(
                action.errorType.toSyncStateUpdatesSyncErrorType(),
                action.userID
            )
            is RemoteNotesSyncFailedAction -> binding.remoteNotesSyncFinished(
                successful = false,
                userID = action.userID
            )
            is RemoteNotesSyncSucceededAction -> binding.remoteNotesSyncFinished(
                successful = true,
                userID = action.userID
            )
        }
    }

    private fun handleNoteReferencesSyncStateUpdates(action: SyncStateAction, binding: NoteReferencesSyncStateUpdates) {
        when (action) {
            is RemoteNoteReferencesSyncStartedAction -> binding.remoteNoteReferencesSyncStarted(action.userID)
            is RemoteNoteReferencesSyncFailedAction -> binding.remoteNoteReferencesSyncFinished(
                successful = false,
                userID = action.userID
            )
            is RemoteNoteReferencesSyncSucceededAction -> binding.remoteNoteReferencesSyncFinished(
                successful = true,
                userID = action.userID
            )
        }
    }

    private fun updateSyncErrorUI(syncErrorState: SyncErrorState, userID: String) {
        forEachBinding { binding ->
            if (binding is SyncStateUpdates) {
                binding.accountSwitched(syncErrorState = syncErrorState, userID = userID)
            }
        }
    }

    private fun handleAddNewNote(action: UIAction.AddNewNote) {
        forEachBinding {
            when (it) {
                is NotesList -> it.addNewNoteTapped(action.note)
            }
        }
    }

    private fun handleEditNote(action: UIAction.EditNote) {
        forEachBinding {
            when (it) {
                is NotesList -> it.noteFromListTapped(action.note)
            }
        }
    }

    private fun handleEditSearchNote(action: UIAction.EditSearchNote) {
        forEachBinding {
            when (it) {
                is Search -> it.noteFromSearchTapped(action.note)
            }
        }
    }

    private fun handleEditFeedSearchNote(action: UIAction.EditFeedSearchNote) {
        forEachBinding {
            when (it) {
                is FeedSearch -> it.noteFromFeedSearchTapped(action.note)
            }
        }
    }

    private fun handleSwipeToRefreshStarted() {
        forEachBinding {
            when (it) {
                is NotesList -> it.manualSyncStarted()
            }
        }
    }

    private fun handleSwipeToRefreshCompleted() {
        forEachBinding {
            when (it) {
                is NotesList -> it.manualSyncCompleted()
            }
        }
    }

    private fun handleFeedSwipeToRefreshStarted() {
        forEachBinding {
            when (it) {
                is FeedSwipeToRefreshSync -> it.feedSyncStarted()
            }
        }
    }

    private fun handleNoteOptionsDismissed() {
        forEachBinding {
            when (it) {
                is NoteOptions -> it.noteOptionsDismissed()
            }
        }
    }

    private fun handleClearCanvas() {
        forEachBinding {
            when (it) {
                is InkNoteChanges -> {
                    it.clearCanvas()
                }
            }
        }
    }

    private fun handleNoteOptionsNoteDeleted() {
        forEachBinding {
            when (it) {
                is NoteOptions -> it.noteOptionsNoteDeleted()
            }
        }
    }

    private fun handleNoteOptionsFeedback() {
        forEachBinding {
            when (it) {
                is NoteOptions -> it.noteOptionsSendFeedbackTapped()
            }
        }
    }

    private fun handleNoteOptionsColorPicked() {
        forEachBinding {
            when (it) {
                is NoteOptions -> it.noteOptionsColorPicked()
            }
        }
    }

    private fun handleNoteOptionsSearchInNote() {
        forEachBinding {
            when (it) {
                is NoteOptions -> it.noteOptionsSearchInNote()
            }
        }
    }

    private fun handleNoteOptionsNoteShared() {
        forEachBinding {
            when (it) {
                is NoteOptions -> it.noteOptionsNoteShared()
            }
        }
    }

    private fun handleFinishActionMode() {
        forEachBinding {
            when (it) {
                is ActionModeUpdateForFeed -> it.finishActionMode()
            }
        }
    }

    private fun handleInvalidateActionMode() {
        forEachBinding {
            when (it) {
                is ActionModeUpdateForFeed -> it.invalidateActionMode()
            }
        }
    }

    private fun handleFeedLayoutChange(layoutType: FeedLayoutType) {
        forEachBinding {
            when (it) {
                is FeedLayoutChanges -> it.changeFeedLayout(layoutType)
            }
        }
    }

    private fun handleAddPhoto() {
        forEachBinding {
            when (it) {
                is EditNote -> it.addPhotoTapped()
            }
        }
    }

    private fun handleCaptureNoteAction() {
        forEachBinding {
            when (it) {
                is EditNote -> it.captureNoteTapped()
            }
        }
    }

    private fun handleOnMicroPhoneButtonClicked() {
        forEachBinding {
            when (it) {
                is EditNote -> it.microPhoneButtonTapped()
            }
        }
    }

    private fun handleOnScanButtonClicked() {
        forEachBinding {
            when (it) {
                is EditNote -> it.scanButtonTapped()
            }
        }
    }

    private fun handleImageCompressionCompleted(action: UIAction.ImageCompressionCompleted) {
        forEachBinding {
            when (it) {
                is EditNote -> it.imageCompressionCompleted(action.successful)
            }
        }
    }

    private fun handleNoteFirstEdited() {
        forEachBinding {
            when (it) {
                is EditNote -> it.noteFirstEdited()
            }
        }
    }

    private fun handleNotesUpdatedForOtherCases(state: State) {
        handleStickyNotesUpdated(state, state.currentUserID)
        handleNoteReferencesUpdated(state, state.currentUserID)
        handleSamsungNotesUpdated(state, state.currentUserID)
    }

    private fun handleMediaDownloaded(state: State, userID: String, noteId: String) {
        val note = state.getNoteForNoteLocalId(noteId) ?: return
        if (note.isSamsungNote()) {
            handleSamsungNotesUpdated(state, userID)
        } else {
            handleStickyNotesUpdated(state, userID)
        }
    }

    private fun handlePinnedNotesUpdated(state: State, userID: String) {
        handleSamsungNotesUpdated(state, userID)
        handleStickyNotesUpdated(state, userID)
    }

    private fun handleReminderUpdated(state: State, userID: String) {
        handleStickyNotesUpdated(state, userID)
    }

    private fun handleDisplayFilterAndSortPanel() {
        forEachBinding {
            when (it) {
                is DisplayFilterAndSortPanel -> it.displayFilterAndSortPanel()
            }
        }
    }

    private fun handleAccountChanged(state: State, userID: String) {
        // NotesChanges and UserNotificationsUpdates are inherited by same class,
        // Hence, we are having two if checks instead of if/else.
        forEachBinding {
            if (it is NoteChanges) {
                it.notesUpdated(stickyNotesCollectionsByUser = hashMapOf(userID to state.getNotesCollectionForUser(userID)), notesLoaded = state.getNotesLoadedForUser(userID))
            }
            if (it is UserNotificationsUpdates) {
                it.updateUserNotifications(state.getNotificationsForAllUsers())
            }
            if (it is AuthChanges) {
                it.authChanged(state.getUserStateForUserID(userID).authenticationState.authState, userID)
            }
            if (it is SamsungNoteChanges) {
                it.samsungNotesUpdated(samsungNotesCollectionsByUser = hashMapOf(userID to state.getSamsungNotesCollectionForUser(userID)))
            }
        }
        updateSyncErrorUI(state.getUserStateForUserID(userID).currentSyncErrorState, userID)
    }

    private fun handleFutureNoteNotification(state: State, userID: String) {
        forEachBinding {
            when (it) {
                is UserNotificationsUpdates -> it.updateUserNotifications(state.getNotificationsForAllUsers())
            }
        }
    }

    private fun handleSamsungNotesUpdated(state: State, userID: String) {
        val currentUserID = state.currentUserID
        if (!combinedListForMultiAccountEnabled && userID != currentUserID) {
            return
        }
        forEachBinding {
            when (it) {
                is SamsungNoteChanges -> {
                    val signedInUserIDs: Set<String> = if (combinedListForMultiAccountEnabled) {
                        state.userIDToUserStateMap.keys
                    } else {
                        setOf(currentUserID)
                    }
                    val samsungNotesCollectionsByUser: HashMap<String, List<Note>> = hashMapOf()
                    signedInUserIDs.forEach { samsungNotesCollectionsByUser.put(it, state.getSamsungNotesCollectionForUser(it)) }
                    it.samsungNotesUpdated(samsungNotesCollectionsByUser = samsungNotesCollectionsByUser)
                }
            }
        }
    }

    private fun handleStickyNotesUpdated(state: State, userID: String) {
        val currentUserID = state.currentUserID
        // With combinedListForMultiAccountEnabled false, we only want to notify updates for only
        // actions related to currentUser
        if (!combinedListForMultiAccountEnabled && userID != currentUserID) {
            return
        }
        forEachBinding {
            if (it is NoteChanges) {
                val signedInUserIDs: Set<String> = if (combinedListForMultiAccountEnabled) {
                    state.userIDToUserStateMap.keys
                } else {
                    setOf(currentUserID)
                }
                val stickyNotesCollectionsByUser: HashMap<String, List<Note>> = hashMapOf()
                signedInUserIDs.forEach { stickyNotesCollectionsByUser.put(it, state.getNotesCollectionForUser(it)) }
                val notesLoaded =
                    if (combinedListForMultiAccountEnabled) {
                        state.areNotesLoadedForAnyUser()
                    } else {
                        state.getNotesLoadedForUser(currentUserID)
                    }
                it.notesUpdated(stickyNotesCollectionsByUser = stickyNotesCollectionsByUser, notesLoaded = notesLoaded)
            }
        }
    }

    private fun handleUndoRedoInNotesEditText(isRedoAction: Boolean) {
        forEachBinding {
            when (it) {
                is UndoRedoPerformedInNotesEditText -> {
                    if (isRedoAction) {
                        it.redo()
                    } else {
                        it.undo()
                    }
                }
            }
        }
    }

    private fun handleNoteReferencesUpdated(state: State, userID: String) {
        val currentUserID = state.currentUserID
        if (!combinedListForMultiAccountEnabled && userID != currentUserID) {
            return
        }
        forEachBinding {
            when (it) {
                is NoteReferenceChanges -> {
                    val signedInUserIDs: Set<String> = if (combinedListForMultiAccountEnabled) {
                        state.userIDToUserStateMap.keys
                    } else {
                        setOf(currentUserID)
                    }
                    val noteReferencesCollectionsByUser: HashMap<String, List<NoteReference>> = hashMapOf()
                    signedInUserIDs.forEach { noteReferencesCollectionsByUser.put(it, state.getNoteReferencesCollectionForUser(it)) }
                    it.noteReferencesUpdated(noteReferencesCollectionsByUser = noteReferencesCollectionsByUser)
                }
            }
        }
    }

    private fun handleFeedSourceFilterSelected(source: FeedSourceFilterOption) {
        forEachBinding {
            when (it) {
                is FeedSourceFilterOptions -> it.sourceFilterSelected(source)
            }
        }
    }

    private fun handleComprehensiveFeedFilterSelected(
        state: State,
        feedFilters: FeedFilters,
        scrollToTop: Boolean
    ) {
        forEachBinding {
            when (it) {
                is ComprehensiveFeedFilterOptions -> {
                    val stickyNotesListsByUsers: HashMap<String, List<Note> > = hashMapOf()
                    val samsungNotesListsByUsers: HashMap<String, List<Note> > = hashMapOf()
                    val notesReferenceListsByUsers: HashMap<String, List<NoteReference> > = hashMapOf()

                    feedFilters?.selectedUserIDs?.forEach {
                        stickyNotesListsByUsers.put(it.key, state.getNotesCollectionForUser(it.key))
                        samsungNotesListsByUsers.put(it.key, state.getSamsungNotesCollectionForUser(it.key))
                        notesReferenceListsByUsers.put(it.key, state.getNoteReferencesCollectionForUser(it.key))
                    }

                    it.filtersSelected(stickyNotesListsByUsers, samsungNotesListsByUsers, notesReferenceListsByUsers, feedFilters, scrollToTop)
                }
            }
        }
    }

    private fun handleFeedOrganiseAction(note: Note) {
        forEachBinding {
            when (it) {
                is NoteActionMode -> it.organiseNote(note)
            }
        }
    }

    private fun handleManualNoteReferencesSyncRequest(userID: String) {
        if (!NotesLibrary.getInstance().experimentFeatureFlags.noteReferencesSyncEnabled) {
            forEachBinding {
                when (it) {
                    is NoteReferencesSyncRequest -> it.syncNoteReferences(userID)
                }
            }
        }
    }

    private fun handleSamsungNotesSyncRequest(userID: String) {
        forEachBinding {
            when (it) {
                is SamsungNotesSyncRequest -> it.syncSamsungNotes(userID)
            }
        }
    }

    private fun handleRequestClientAuthAction(userID: String) {
        forEachBinding {
            when (it) {
                is AuthChanges -> it.onRequestClientAuth(userID)
            }
        }
    }

    private fun deleteFiles(note: Note) {
        note.document.blocks.forEach { block ->
            if (block is InlineMedia) {
                block.localUrl?.let { removeFile(it) }
            }
        }
    }

    private fun deleteSamsungPreviewImages(note: Note) =
        note.media.forEach { media -> media.localUrl?.let { removeFile(it) } }

    private fun removeFile(uriString: String) {
        if (uriString.isNotBlank()) {
            val uri = try {
                URI.create(uriString)
            } catch (e: IllegalArgumentException) {
                NotesLibrary.getInstance().log(message = "IllegalArgumentException when creating a file")
                return
            }
            val file = File(uri.path)
            file.delete()
        }
    }

    private fun SyncErrorType.toSyncStateUpdatesSyncErrorType(): SyncStateUpdates.SyncErrorType {
        return when (this) {
            SyncErrorType.NetworkUnavailable -> SyncStateUpdates.SyncErrorType.NetworkUnavailable
            SyncErrorType.Unauthenticated -> SyncStateUpdates.SyncErrorType.Unauthenticated
            SyncErrorType.AutoDiscoverGenericFailure -> SyncStateUpdates.SyncErrorType.AutoDiscoverGenericFailure
            SyncErrorType.EnvironmentNotSupported -> SyncStateUpdates.SyncErrorType.EnvironmentNotSupported
            SyncErrorType.UserNotFoundInAutoDiscover -> SyncStateUpdates.SyncErrorType.UserNotFoundInAutoDiscover
            SyncErrorType.SyncPaused -> SyncStateUpdates.SyncErrorType.SyncPaused
            SyncErrorType.SyncFailure -> SyncStateUpdates.SyncErrorType.SyncFailure
        }
    }
}
