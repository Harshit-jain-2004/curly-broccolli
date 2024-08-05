package com.microsoft.notes.ui.shared

import android.net.ConnectivityManager
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.sideeffect.ui.SyncStateUpdates
import com.microsoft.notes.sideeffect.ui.toTelemetryErrorType
import com.microsoft.notes.store.AuthState
import com.microsoft.notes.store.SyncErrorState
import com.microsoft.notes.ui.noteslist.SyncErrorResIds
import com.microsoft.notes.ui.noteslist.toSyncErrorStringIds
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.HostTelemetryKeys
import com.microsoft.notes.utils.logging.ManualSyncRequestStatus
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.utils.logging.SyncActionType
import com.microsoft.notes.utils.utils.exhaustive

abstract class StickyNotesPresenterWithSyncSpinner(private val syncStateUpdate: SyncStateUpdatingApi) : StickyNotesPresenter(), SyncStateUpdates {
    // refresh handling
    /*
    * Following state variables are needed for:
    * - manualSyncRequested: Identifying manual sync request and keep hiding other progress bars which
    * are not dependent on user action. In other words user may not be actively waiting for progress
    * to finish which is happening in case pull down spinner. So we hide other progress bar.
    *
    */
    private var manualSyncRequested = false
    private val userIDList: Set<String> get() =
        with(NotesLibrary.getInstance()) {
            if (experimentFeatureFlags.combinedListForMultiAccountEnabled) {
                getAllUsers()
            } else {
                setOf(currentUserID)
            }
        }

    fun refreshList() {
        recordTelemetry(
            EventMarkers.ManualSyncAction,
            Pair(NotesSDKTelemetryKeys.NoteProperty.ACTION, SyncActionType.START)
        )

        for (userID in userIDList) {
            if (canSyncNotes(userID)) {
                manualSyncRequested = true
                NotesLibrary.getInstance().requestManualSync(userID)
            }
        }
    }

    fun refreshNoteReferencesList() {
        recordTelemetry(EventMarkers.NoteReferencesSyncStarted)

        for (userID in userIDList) {
            if (canSyncNotes(userID)) {
                manualSyncRequested = true
                NotesLibrary.getInstance().requestManualNoteReferencesSync(userID)
            }
        }
    }

    fun refreshSamsungNotesList() {
        recordTelemetry(EventMarkers.SamsungNotesSyncStarted)

        for (userID in userIDList) {
            if (canSyncNotes(userID)) {
                manualSyncRequested = true
                NotesLibrary.getInstance().requestSamsungNoteSync(userID)
            }
        }
    }

    private fun canSyncNotes(userID: String): Boolean {
        if (!isNetworkAvailable()) {
            manualSyncCompleted(ManualSyncRequestStatus.NetworkUnavailable, userID)
            return false
        }

        if (NotesLibrary.getInstance().getAuthState(userID) != AuthState.AUTHENTICATED) {
            manualSyncCompleted(ManualSyncRequestStatus.Unauthenticated, userID)
            return false
        }

        if (NotesLibrary.getInstance().isSyncPaused(userID)) {
            manualSyncCompleted(ManualSyncRequestStatus.SyncPaused, userID)
            return false
        }
        return true
    }

    private fun isNetworkAvailable(): Boolean =
        syncStateUpdate.getConnectivityManager()?.activeNetworkInfo?.isConnected ?: false

    private fun manualSyncCompleted(status: ManualSyncRequestStatus, userID: String) {
        recordTelemetry(
            EventMarkers.ManualSyncAction,
            Pair(HostTelemetryKeys.RESULT, status.toString()),
            Pair(NotesSDKTelemetryKeys.NoteProperty.ACTION, SyncActionType.COMPLETE)
        )

        // sync state is modified if the user is current
        if (userID == NotesLibrary.getInstance().currentUserID || NotesLibrary.getInstance().experimentFeatureFlags.combinedListForMultiAccountEnabled) {
            // client app is notified about sync failures which need to be handled via UiBindings
            when (status) {
                ManualSyncRequestStatus.AutoDiscoverGenericFailure, ManualSyncRequestStatus.EnvironmentNotSupported, ManualSyncRequestStatus.UserNotFoundInAutoDiscover, ManualSyncRequestStatus.NetworkUnavailable, ManualSyncRequestStatus.SyncPaused ->
                    syncStateUpdate.onRefreshCompleted(
                        when (status) {
                            ManualSyncRequestStatus.NetworkUnavailable -> R.string.sn_manual_sync_failed_no_network
                            else -> R.string.sn_manual_sync_failed
                        }
                    )
                ManualSyncRequestStatus.Success, ManualSyncRequestStatus.Unauthenticated, ManualSyncRequestStatus.SyncFailure -> syncStateUpdate.onRefreshCompleted()
            }.exhaustive
        }
    }

    // SyncStateUpdates interface implementation
    override fun remoteNotesSyncStarted(userID: String) {}

    override fun remoteNotesSyncErrorOccurred(errorType: SyncStateUpdates.SyncErrorType, userID: String) {
        runIfActivityIsRunning {
            runOnClientThread {
                if (manualSyncRequested) {
                    manualSyncCompleted(errorType.toTelemetryErrorType(), userID)
                }
                val syncErrorStringIds = toSyncErrorStringIds(errorType)
                syncErrorStringIds?.let { syncStateUpdate.setSyncStatusMessage(it, userID) }
            }
        }
    }

    override fun accountSwitched(syncErrorState: SyncErrorState, userID: String) {
        runIfActivityIsRunning {
            runOnClientThread {

                val syncErrorStringIds = toSyncErrorStringIds(syncErrorState)

                // This will reset any Sync Error UI which was shown earlier
                if (syncErrorStringIds == null) {
                    syncStateUpdate.unsetSyncStatusMessage(userID)
                } else {
                    syncStateUpdate.setSyncStatusMessage(syncErrorStringIds, userID)
                }
            }
        }
    }

    override fun remoteNotesSyncFinished(successful: Boolean, userID: String) {
        runIfActivityIsRunning {
            runOnClientThread {
                if (manualSyncRequested) {
                    manualSyncRequested = false
                    manualSyncCompleted(if (successful) ManualSyncRequestStatus.Success else ManualSyncRequestStatus.SyncFailure, userID)
                }
                if (successful) {
                    syncStateUpdate.unsetSyncStatusMessage(userID)
                }
            }
        }
    }
}

interface SyncStateUpdatingApi {
    fun onRefreshCompleted(errorMessageId: Int? = null)
    fun getConnectivityManager(): ConnectivityManager?
    fun setSyncStatusMessage(errorMessage: SyncErrorResIds, userID: String) {}
    fun unsetSyncStatusMessage(userID: String) {}
}
