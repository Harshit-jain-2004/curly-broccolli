package com.microsoft.notes.sideeffect.sync

import android.content.Context
import com.microsoft.notes.noteslib.ExperimentFeatureFlags
import com.microsoft.notes.store.SideEffect
import com.microsoft.notes.store.State
import com.microsoft.notes.store.Store
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.AuthAction.LogoutAction
import com.microsoft.notes.store.action.AuthAction.NewAuthTokenAction
import com.microsoft.notes.store.action.AuthenticatedSyncRequestAction
import com.microsoft.notes.store.action.AutoDiscoverAction
import com.microsoft.notes.store.action.CompoundAction
import com.microsoft.notes.store.action.CreationAction.AddNoteAction
import com.microsoft.notes.store.action.DeleteAction.CleanupNotesMarkedAsDeletedAction
import com.microsoft.notes.store.action.PollingAction
import com.microsoft.notes.store.action.ReadAction
import com.microsoft.notes.store.action.ReadAction.DeltaTokenLoadedAction
import com.microsoft.notes.store.action.ReadAction.NotesLoadedAction
import com.microsoft.notes.store.action.SamsungNotesResponseAction
import com.microsoft.notes.store.action.SyncRequestAction
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.action.SyncStateAction
import com.microsoft.notes.store.action.UIAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithColorAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithDocumentAction
import com.microsoft.notes.store.getNoteForNoteLocalId
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.store.getSamsungNotesCollectionForUser
import com.microsoft.notes.sync.AutoDiscoverCache
import com.microsoft.notes.sync.AutoDiscoverCallManager
import com.microsoft.notes.sync.AutoDiscoverErrorCode
import com.microsoft.notes.sync.HttpError
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.threading.ExecutorServices
import com.microsoft.notes.utils.threading.ThreadExecutor
import com.microsoft.notes.utils.threading.ThreadExecutorService
import com.microsoft.notes.utils.utils.Constants
import com.microsoft.notes.utils.utils.UserInfo
import com.microsoft.notes.utils.utils.UserInfoUtils

sealed class AuthState {
    class Unauthenticated : AuthState()
    class AADAuthenticated : AuthState()
}

enum class PollState {
    Waiting,
    PreviousCharacterWasNewLine,
}

data class SyncState(
    val auth: AuthState,
    val deltaTokensLoaded: Boolean,
    val apiHostInitialized: Boolean,
    val pauseSync: (Boolean) -> Unit
) {
    init {
        initPauseState()
    }

    private fun initPauseState() {
        pauseSync(!(deltaTokensLoaded && auth is AuthState.AADAuthenticated && apiHostInitialized))
    }
}

class SyncSideEffectThreadService : ThreadExecutorService(ExecutorServices.syncSideEffect)

const val REALTIME_RECONNECT_DELAY_MAX: Long = 5000 // 5 seconds

class SyncSideEffect(
    val context: Context,
    val store: Store,
    val syncThread: ThreadExecutor? = null,
    val notesLogger: NotesLogger? = null,
    val experimentFeatureFlags: ExperimentFeatureFlags,
    val syncHandlerManager: SyncHandlerManager,
    private val autoDiscoverCallManager: AutoDiscoverCallManager,
    val autoDiscoverCache: AutoDiscoverCache
) : SideEffect(syncThread) {
    init {
        if (experimentFeatureFlags.multiAccountEnabled) {
            // Fetch all the signed in accounts from shared pref and create a userSyncHandler for each
            // This will make sure userSyncHandler always exists before any sync call comes
            val signedInEmails = context.getSharedPreferences(Constants.AUTH_SHARED_PREF_KEY, Context.MODE_PRIVATE)
                .getStringSet(Constants.SIGNED_IN_ACCOUNTS, emptySet())
            if (signedInEmails?.isNotEmpty() == true) {
                signedInEmails.forEach {
                    syncHandlerManager.handleNewUser(
                        UserInfo.EMPTY_USER_INFO.copy(
                            userID = it,
                            userInfoSuffix = UserInfoUtils.getUserInfoSuffix(it, context)
                        )
                    )
                }
            } else {
                syncHandlerManager.handleNewUser(UserInfo.EMPTY_USER_INFO)
            }
        } else {
            syncHandlerManager.handleNewUser(UserInfo.EMPTY_USER_INFO)
        }
    }

    override fun handle(action: Action, state: State) {
        notesLogger?.i(message = "handle SyncSideEffect: ${action.toLoggingIdentifier()}")
        when (action) {
            is CompoundAction -> handleCompoundAction(action, state)
            is NewAuthTokenAction -> handleNewTokenAction(action)
            is LogoutAction -> handleLogoutAction(action)
            is AddNoteAction -> handleAddNoteAction(action)
            is UpdateNoteWithColorAction -> handleUpdateWithColorAction(state, action)
            is UpdateNoteWithDocumentAction -> handleUpdateNoteWithDocumentAction(state, action)
            is AuthenticatedSyncRequestAction -> handleAuthenticatedSyncRequestAction(action)
            is SyncRequestAction -> handleSyncRequestAction(action)
            is SyncResponseAction -> handleSyncResponseAction(action)
            is SamsungNotesResponseAction -> handleSamsungNotesResponseAction(action)
            is NotesLoadedAction -> handleNotesLoadedAction(action)
            is DeltaTokenLoadedAction -> handleDeltaTokensLoadedAction(action)
            is CleanupNotesMarkedAsDeletedAction -> handleCleanupNotesMarkedAsDeletedAction(state, action)
            is PollingAction.Start -> syncHandlerManager.getSyncHandlerForUser(action.userID)?.handleStartPolling()
            is PollingAction.Stop -> syncHandlerManager.getSyncHandlerForUser(action.userID)?.handleStopPolling()
            is UIAction.AccountChanged -> handleAccountChanged(action)
            is AutoDiscoverAction.CacheHostUrl -> handleCacheHostUrl(action)
            is AutoDiscoverAction.SetHostUrl -> handleSetHostUrl(action)
        }
    }

    private fun handleCompoundAction(
        action: CompoundAction,
        state: State
    ) {
        action.actions.forEach { handle(it, state) }
    }

    private fun handleNewTokenAction(action: NewAuthTokenAction) {
        // Creates a new UserSyncHandler or reuses existing one.
        syncHandlerManager.handleNewUser(action.userInfo)

        // call to update the delta tokens post creation of userSynHanlders
        store.dispatch(ReadAction.RetrieveDeltaTokensForAllNoteTypes(action.userInfo))

        // Performs all the tasks on signin
        syncHandlerManager.getSyncHandlerForUser(userID = action.userID)?.login(action.userInfo)
    }

    private fun handleAutoDiscoverForNewToken(userInfo: UserInfo) {
        autoDiscoverCallManager.getNotesClientHostUrl(
            userInfo = userInfo,
            successObserver = { host, fromCache ->
                // No sync success action is dispatched here as it is done by sync layer on successful sync
                if (!fromCache) {
                    store.dispatch(AutoDiscoverAction.CacheHostUrl(host, userInfo.userID))
                }
                store.dispatch(AutoDiscoverAction.SetHostUrl(host, userInfo.userID))
            },
            errorObserver = {
                if (it.error is HttpError) {
                    when (it.error.errorDetails?.error?.code) {
                        AutoDiscoverErrorCode.PROTOCOL_NOT_SUPPORTED.errorCode -> store.dispatch(
                            SyncStateAction.RemoteNotesSyncErrorAction(
                                SyncStateAction
                                    .RemoteNotesSyncErrorAction.SyncErrorType.EnvironmentNotSupported,
                                userInfo.userID
                            )
                        )
                        AutoDiscoverErrorCode.USER_NOT_FOUND.errorCode -> store.dispatch(
                            SyncStateAction.RemoteNotesSyncErrorAction(
                                SyncStateAction
                                    .RemoteNotesSyncErrorAction.SyncErrorType.UserNotFoundInAutoDiscover,
                                userInfo.userID
                            )
                        )
                        else -> store.dispatch(
                            SyncStateAction.RemoteNotesSyncErrorAction(
                                SyncStateAction
                                    .RemoteNotesSyncErrorAction.SyncErrorType.AutoDiscoverGenericFailure,
                                userInfo.userID
                            )
                        )
                    }
                } else {
                    store.dispatch(
                        SyncStateAction.RemoteNotesSyncErrorAction(
                            SyncStateAction
                                .RemoteNotesSyncErrorAction.SyncErrorType.AutoDiscoverGenericFailure,
                            userInfo.userID
                        )
                    )
                }
            }
        )
    }

    private fun handleLogoutAction(action: LogoutAction) {
        syncHandlerManager.handleLogout(action.userID)
    }

    private fun handleAddNoteAction(action: AddNoteAction) {
        store.dispatch(SyncRequestAction.CreateNote(action.note, action.userID))
    }

    private fun handleUpdateWithColorAction(
        state: State,
        action: UpdateNoteWithColorAction
    ) {
        val note = state.getNoteForNoteLocalId(noteId = action.noteLocalId) ?: return
        store.dispatch(SyncRequestAction.UpdateNote(note, action.uiRevision, action.userID))
    }

    private fun handleUpdateNoteWithDocumentAction(
        state: State,
        action: UpdateNoteWithDocumentAction
    ) {
        val note = state.getNoteForNoteLocalId(action.noteLocalId) ?: return
        store.dispatch(SyncRequestAction.UpdateNote(note, action.uiRevision, action.userID))
    }

    private fun handleAuthenticatedSyncRequestAction(action: AuthenticatedSyncRequestAction) {
        val pushSyncRequest = when (action) {
            /*
             * When DB is loaded, we get persisted deltaToken which should be used for first read request.
             * Other AuthenticatedSyncRequestAction, specify delta tokens or are user action and we do not
             * block them.
             */
            is AuthenticatedSyncRequestAction.RemoteChangedDetected,
            is AuthenticatedSyncRequestAction.RemoteNoteReferencesChangedDetected,
            is AuthenticatedSyncRequestAction.SamsungNotesChangedDetected,
            is AuthenticatedSyncRequestAction.MeetingNotesChangedDetected ->
                syncHandlerManager.getSyncHandlerForUser(action.userID)?.syncState?.deltaTokensLoaded
            else -> true
        }
        if (pushSyncRequest == true) {
            syncHandlerManager.getSyncHandlerForUser(action.userID)?.handleAuthSyncRequestAction(action)
        }
    }

    private fun handleSyncRequestAction(action: SyncRequestAction) {
        syncHandlerManager.getSyncHandlerForUser(action.userID)?.handleSyncRequestAction(action)
    }

    private fun handleSyncResponseAction(action: SyncResponseAction) {
        syncHandlerManager.getSyncHandlerForUser(action.userID)?.handleSyncResponseAction(action)
    }

    private fun handleSamsungNotesResponseAction(action: SamsungNotesResponseAction) {
        syncHandlerManager.getSyncHandlerForUser(action.userID)?.handleSamsungNotesResponseAction(action)
    }

    private fun handleNotesLoadedAction(action: NotesLoadedAction) {
        with(action) {
            if (allNotesLoaded) {
                store.dispatch(CleanupNotesMarkedAsDeletedAction(action.userID))
            }

            store.dispatch(UIAction.UpdateFutureNoteUserNotification(action.notesCollection, action.userID))
        }
    }

    private fun handleDeltaTokensLoadedAction(action: DeltaTokenLoadedAction) {
        with(action) {
            syncHandlerManager.getSyncHandlerForUser(userID)?.let { userSyncHandler ->
                userSyncHandler.eventHandler.deltaToken = deltaToken?.let { Token.Delta(it) }
                userSyncHandler.eventHandler.samsungDeltaToken = samsungDeltaToken?.let { Token.Delta(it) }
                userSyncHandler.eventHandler.noteReferencesDeltaToken = noteReferencesDeltaToken?.let { Token.Delta(it) }
                userSyncHandler.syncState = userSyncHandler.syncState.copy(deltaTokensLoaded = true)

                handleAutoDiscoverForNewToken(action.userInfo)
                userSyncHandler.startPollingAndRealtime(action.userID)
            }
        }
    }

    private fun handleCleanupNotesMarkedAsDeletedAction(state: State, action: CleanupNotesMarkedAsDeletedAction) {
        cleanupStickyNotes(state, action)
        cleanupSamsungNotes(state, action)
    }

    private fun cleanupStickyNotes(
        state: State,
        action: CleanupNotesMarkedAsDeletedAction
    ) {
        val markedNotes = state.getNotesCollectionForUser(action.userID).filter { it.isDeleted }
        markedNotes.forEach {
            store.dispatch(
                SyncRequestAction.DeleteNote(
                    it.localId,
                    it.remoteData?.id,
                    action.userID
                )
            )
        }
    }

    private fun cleanupSamsungNotes(
        state: State,
        action: CleanupNotesMarkedAsDeletedAction
    ) {
        val markedNotes = state.getSamsungNotesCollectionForUser(action.userID).filter { it.isDeleted }
        markedNotes.forEach {
            store.dispatch(
                SyncRequestAction.DeleteSamsungNote(
                    it.localId,
                    it.remoteData?.id ?: "",
                    action.userID
                )
            )
        }
    }

    private fun handleAccountChanged(action: UIAction.AccountChanged) {
        // Fetch all notes locally
        store.dispatch(ReadAction.FetchAllNotesAction(action.userID, experimentFeatureFlags.samsungNotesSyncEnabled))

        // Delta Sync
        store.dispatch(AuthenticatedSyncRequestAction.RemoteChangedDetected(action.userID))

        if (experimentFeatureFlags.noteReferencesSyncEnabled) {
            // NoteReferencesSync
            store.dispatch(AuthenticatedSyncRequestAction.RemoteNoteReferencesChangedDetected(action.userID))
        }

        if (experimentFeatureFlags.samsungNotesSyncEnabled) {
            store.dispatch(AuthenticatedSyncRequestAction.SamsungNotesChangedDetected(action.userID))
        }

        if (experimentFeatureFlags.meetingNotesSyncEnabled) {
            store.dispatch(AuthenticatedSyncRequestAction.MeetingNotesChangedDetected(action.userID))
        }

        // ToDo:- Update GlobalPoller's frequency for active account
        // https://github.com/microsoft-notes/notes-android-sdk/issues/827
    }

    private fun handleCacheHostUrl(action: AutoDiscoverAction.CacheHostUrl) {
        autoDiscoverCache.setExpirableHost(action.userID, action.host)
    }

    private fun handleSetHostUrl(action: AutoDiscoverAction.SetHostUrl) {
        syncHandlerManager.getSyncHandlerForUser(action.userID)?.setApiHost(action.host)
    }
}
