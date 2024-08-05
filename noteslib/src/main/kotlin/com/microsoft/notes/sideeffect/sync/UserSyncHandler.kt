package com.microsoft.notes.sideeffect.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.webkit.MimeTypeMap
import com.microsoft.notes.models.AccountType
import com.microsoft.notes.models.Changes
import com.microsoft.notes.noteslib.GlobalPoller
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.platform.extensions.isNetworkConnected
import com.microsoft.notes.platform.files.AtomicFileBackup
import com.microsoft.notes.sideeffect.sync.mapper.toSyncNote
import com.microsoft.notes.store.Store
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.AuthenticatedSyncRequestAction
import com.microsoft.notes.store.action.SamsungNotesResponseAction
import com.microsoft.notes.store.action.SyncRequestAction
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.action.UIAction
import com.microsoft.notes.store.getNoteForNoteLocalId
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.sync.ApiError
import com.microsoft.notes.sync.ApiPromise
import com.microsoft.notes.sync.ApiRequestOperation
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidDeleteMedia
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidDeleteNote
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidUpdateMediaAltText
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidUpdateNote
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidUploadMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.CreateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteNoteReference
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteSamsungNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DownloadMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.GetNoteForMerge
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.MeetingNotesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.NoteReferencesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.SamsungNotesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.Sync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateMediaAltText
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UploadMedia
import com.microsoft.notes.sync.ApiResult
import com.microsoft.notes.sync.CorrelationVector
import com.microsoft.notes.sync.HttpError401
import com.microsoft.notes.sync.NetworkedSdk
import com.microsoft.notes.sync.NotesClientHost
import com.microsoft.notes.sync.OutboundQueue
import com.microsoft.notes.sync.OutboundQueueApiRequestOperationHandler
import com.microsoft.notes.sync.PriorityQueue
import com.microsoft.notes.sync.REALTIME_API_VERSION_V1
import com.microsoft.notes.sync.REALTIME_API_VERSION_V2
import com.microsoft.notes.sync.SdkManager
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.JsonParser
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.utils.logging.SyncActionType
import com.microsoft.notes.utils.utils.Constants
import com.microsoft.notes.utils.utils.UserInfo
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import com.microsoft.notes.models.Note as StoreNote

class UserSyncHandler(
    val context: Context,
    val store: Store,
    val outboundQueue: OutboundQueue,
    val eventHandler: ApiResponseEventHandler,
    val outboundRequestHandler: OutboundQueueApiRequestOperationHandler,
    val notesLogger: NotesLogger? = null,
    var realtimeEnabled: Boolean,
    val createSdkManager: (UserInfo) -> SdkManager,
    apiHostInitialized: Boolean
) {

    private var globalPoller: GlobalPoller? = null

    private var realtimeReconnectDelay = 0L
    private var lastShoulderTapTime = 0L
    private val realtimeSessionId = outboundQueue.correlationVector.correlationVectorBase

    private val freSyncingKey = Constants.FEED_FRE_SYNC_PREFERENCE_KEY + "_" + eventHandler.userID
    private val freSyncCompletedString = "FRESyncCompleted"
    private val freSamsungNotesSyncCompletedString = "FRESamsungNotesSyncCompleted"
    private val feedFreSyncPreferences: SharedPreferences = context.getSharedPreferences(Constants.FEED_FRE_SYNC_PREFERENCE, Context.MODE_PRIVATE)

    companion object {
        private const val LOG_TAG = "UserSyncHandler"
        private const val MINIMUM_TIME_WINDOW_BEFORE_NEXT_SYNC = 250

        // Realtime Connection Response
        private const val REALTIME_CONNECTION_KEEP_ALIVE_EVENT_PREFIX = ":"
        private const val REALTIME_CONNECTION_VALID_DATA_EVENT_PREFIX = "data:"
        private const val REALTIME_CONNECTION_JSON_SOURCE_KEY = "source"
        private const val REALTIME_CONNECTION_RESPONSE_NOTES = "notes"
        private const val REALTIME_CONNECTION_RESPONSE_CONNECTED_NOTES = "connectednotes"
        private const val REALTIME_CONNECTION_RESPONSE_NOTEREFERENCES = "notereferences"
    }

    var syncState = SyncState(
        auth = AuthState.Unauthenticated(),
        deltaTokensLoaded = false,
        apiHostInitialized = apiHostInitialized,
        pauseSync = { pauseOutboundQueue = it }
    )

    private val deltaToken: Token.Delta?
        get() = eventHandler.deltaToken

    private val samsungDeltaToken: Token.Delta?
        get() = eventHandler.samsungDeltaToken

    private val noteReferencesDeltaToken: Token.Delta?
        get() = eventHandler.noteReferencesDeltaToken

    private val noteReferencesSyncEnabled
        get() = NotesLibrary.getInstance().experimentFeatureFlags.noteReferencesSyncEnabled
    private val samsungNotesSyncEnabled
        get() = NotesLibrary.getInstance().experimentFeatureFlags.samsungNotesSyncEnabled

    private var meetingNotesSyncEnabled = false

    private val feedFREFastSyncEnabled
        get() = NotesLibrary.getInstance().experimentFeatureFlags.feedFREFastSyncEnabled
    fun login(userInfo: UserInfo) {
        outboundRequestHandler.sdkManager = createSdkManager(userInfo)
        eventHandler.userID = userInfo.userID
        meetingNotesSyncEnabled = NotesLibrary.getInstance().experimentFeatureFlags.meetingNotesSyncEnabled && userInfo.accountType == AccountType.ADAL
        outboundQueue.meetingNotesSyncEnabled = meetingNotesSyncEnabled

        syncState = syncState.copy(auth = AuthState.AADAuthenticated())
        notesLogger?.recordTelemetry(
            EventMarkers.SyncSessionAction,
            Pair(NotesSDKTelemetryKeys.NoteProperty.ACTION, SyncActionType.START)
        )

        // Polling would start once delta tokens are loaded
    }

    fun logout() {
        outboundQueue.reset()
        outboundQueue.clearFeedFRESyncSharedPreferences()
        eventHandler.reset()
        outboundRequestHandler.reset()

        syncState = syncState.copy(auth = AuthState.Unauthenticated())
        notesLogger?.recordTelemetry(
            EventMarkers.SyncSessionAction,
            Pair(NotesSDKTelemetryKeys.NoteProperty.ACTION, SyncActionType.STOP)
        )
        outboundRequestHandler.sdkManager = createSdkManager(UserInfo.EMPTY_USER_INFO)
    }

    fun setApiHost(apiHost: NotesClientHost) {
        outboundRequestHandler.sdkManager.notes.host = apiHost
        syncState = syncState.copy(apiHostInitialized = true)
    }

    var pauseOutboundQueue: Boolean
        get() = outboundQueue.isPaused
        set(pause) {
            outboundQueue.isPaused = pause
            if (!pause) {
                triggerSyncRequests()
            }
        }

    private fun triggerSyncRequests() {
        store.dispatch(AuthenticatedSyncRequestAction.RemoteChangedDetected(eventHandler.userID))
        if (noteReferencesSyncEnabled) {
            store.dispatch(AuthenticatedSyncRequestAction.RemoteNoteReferencesChangedDetected(eventHandler.userID))
        }
        if (samsungNotesSyncEnabled) {
            store.dispatch(AuthenticatedSyncRequestAction.SamsungNotesChangedDetected(eventHandler.userID))
        }
        if (meetingNotesSyncEnabled) {
            store.dispatch(
                AuthenticatedSyncRequestAction.MeetingNotesChangedDetected(
                    eventHandler.userID
                )
            )
        }
    }

    fun handleAuthSyncRequestAction(action: AuthenticatedSyncRequestAction) {
        notesLogger?.d(message = "authSyncRequestAction: ${action.toLoggingIdentifier()}")
        when (action) {
            is AuthenticatedSyncRequestAction.RemoteChangedDetected -> {
                val operation = Sync(deltaToken)
                outboundQueue.push(operation)
            }
            is AuthenticatedSyncRequestAction.ManualSyncRequestAction -> {
                val operation = Sync(deltaToken)
                outboundQueue.push(operation)
            }
            is AuthenticatedSyncRequestAction.ForceFullSyncRequestAction -> {
                // removing delta token to ensure Full Sync happens once
                eventHandler.resetNotesDeltaToken()
                val operation = Sync(deltaToken = null)
                outboundQueue.push(operation)
            }
            is AuthenticatedSyncRequestAction.RemoteNoteReferencesChangedDetected -> {
                val operation = NoteReferencesSync(noteReferencesDeltaToken)
                outboundQueue.push(operation)
            }
            is AuthenticatedSyncRequestAction.ManualNoteReferencesSyncRequestAction -> {
                if (noteReferencesSyncEnabled) {
                    val operation = NoteReferencesSync(noteReferencesDeltaToken)
                    outboundQueue.push(operation)
                }
            }
            is AuthenticatedSyncRequestAction.ManualMeetingNotesSyncRequestAction -> {
                if (meetingNotesSyncEnabled) {
                    val operation = ApiRequestOperation.ValidApiRequestOperation.MeetingNotesSync()
                    outboundQueue.push(operation)
                }
            }
            is AuthenticatedSyncRequestAction.ManualSamsungNotesSyncRequestAction ->
                outboundQueue.push(SamsungNotesSync(deltaToken = samsungDeltaToken))
            is AuthenticatedSyncRequestAction.SamsungNotesChangedDetected ->
                outboundQueue.push(SamsungNotesSync(deltaToken = samsungDeltaToken))
            is AuthenticatedSyncRequestAction.MeetingNotesChangedDetected ->
                outboundQueue.push(MeetingNotesSync())
        }
    }

    fun handleSyncRequestAction(action: SyncRequestAction) {
        notesLogger?.d(message = "syncRequestAction: ${action.toLoggingIdentifier()}")
        when (action) {
            is SyncRequestAction.CreateNote -> {
                val note = action.note.toSyncNote()
                val operation = CreateNote(note)
                outboundQueue.push(operation)
            }
            is SyncRequestAction.UpdateNote -> {
                outboundQueue.push(updateNoteOperation(action))
            }
            is SyncRequestAction.DeleteNote -> {
                outboundQueue.push(deleteNoteOperation(action))
            }
            is SyncRequestAction.DeleteNoteReference -> {
                outboundQueue.push(deleteNoteReferenceOperation(action))
            }
            is SyncRequestAction.DeleteSamsungNote -> {
                outboundQueue.push(deleteSamsungNoteOperation(action))
            }
            is SyncRequestAction.UploadMedia -> {
                outboundQueue.push(uploadMediaOperation(action))
            }
            is SyncRequestAction.DeleteMedia -> {
                outboundQueue.push(deleteMediaOperation(action))
            }
            is SyncRequestAction.UpdateMediaAltText -> {
                outboundQueue.push(updateMediaAltTextOperation(action))
            }
        }
    }

    fun handleSamsungNotesResponseAction(action: SamsungNotesResponseAction) {
        when (action) {
            is SamsungNotesResponseAction.ApplyChanges -> {
                notesLogger?.i(
                    message = "Samsung Notes Apply changes Create: ${action.changes.toCreate.count()}, " +
                        "Replace: ${action.changes.toReplace.count()}, " +
                        "Delete: ${action.changes.toDelete.count()}"
                )
                downloadMedia(action.changes)

                if (feedFREFastSyncEnabled) {
                    /*
                    * If this is the response of a FRE sync, add it to shared preference and trigger full sync for all note types
                    * We are pushing the full sync for all after each full sync FRE completion because we don't know which request would be the last.
                    * If this is not the last FRE full sync, it would be discarded in the queue push logic. Please check the push function logic.
                    * */
                    var item = freSamsungNotesSyncCompletedString
                    val freValue = feedFreSyncPreferences.getString(freSyncingKey, null)
                    val freValues = freValue?.split(',')
                    if (freValues?.contains(item) != true) {
                        if (freValue != null) {
                            item = "$freValue,$item"
                        }
                        feedFreSyncPreferences.edit().putString(freSyncingKey, item).apply()

                        triggerSyncRequests()
                    }
                }
            }
        }
    }

    private fun downloadMedia(changes: Changes) {
        fun enqueueDownload(note: StoreNote, mediaRemoteId: String, mimeType: String) {
            val syncNote = note.toSyncNote()
            val operation = DownloadMedia(
                note = syncNote, mediaRemoteId = mediaRemoteId, mimeType = mimeType
            )
            outboundQueue.push(operation)
        }
        changes.toCreate.processMediaNeedingDownload(::enqueueDownload)
        changes.toReplace.map { it.noteFromServer }.processMediaNeedingDownload(::enqueueDownload)
    }

    @Suppress("LongMethod")
    fun handleSyncResponseAction(action: SyncResponseAction) {
        notesLogger?.d(message = "syncResponseAction: ${action.toLoggingIdentifier()}")
        when (action) {
            is SyncResponseAction.ApplyChanges -> {
                notesLogger?.i(
                    message = "Create: ${action.changes.toCreate.count()}, " +
                        "Replace: ${action.changes.toReplace.count()}, " +
                        "Delete: ${action.changes.toDelete.count()}"
                )

                downloadMedia(action.changes)

                if (feedFREFastSyncEnabled) {
                    /*
                    * If this is the response of a FRE sync, add it to shared preference and trigger full sync for all note types
                    * We are pushing the full sync for all after each full sync FRE completion because we don't know which request would be the last.
                    * If this is not the last FRE full sync, it would be discarded in the queue push logic. Please check the push function logic.
                    * */
                    var item = freSyncCompletedString
                    val freValue = feedFreSyncPreferences.getString(freSyncingKey, null)
                    val freValues = freValue?.split(',')
                    if (freValues?.contains(item) != true) {
                        if (freValue != null) {
                            item = "$freValue,$item"
                        }
                        feedFreSyncPreferences.edit().putString(freSyncingKey, item).apply()

                        triggerSyncRequests()
                    }
                }
                store.dispatch(
                    UIAction.UpdateFutureNoteUserNotification(
                        store.state.getNotesCollectionForUser(action.userID),
                        action.userID
                    )
                )
            }
            is SyncResponseAction.MediaUploaded -> {
                notesLogger?.d(message = "updating note with MediaUploaded")
                val note = store.state.getNoteForNoteLocalId(action.noteId) ?: return
                val syncNote = note.toSyncNote()
                // uploading media changes changeKey on remote note, retrieve and merge note
                val operation = GetNoteForMerge(syncNote, note.uiRevision)
                notesLogger?.d(message = "operation with MediaUploaded")
                outboundQueue.push(operation)
            }
            is SyncResponseAction.MediaDeleted -> {
                notesLogger?.d(message = "updating note with MediaDeleted")
                val note = store.state.getNoteForNoteLocalId(action.noteId) ?: return
                val syncNote = note.toSyncNote()
                // deleting media changes changeKey on remote note, retrieve and merge note
                val operation = GetNoteForMerge(syncNote, note.uiRevision)
                notesLogger?.d(message = "operation with MediaDeleted")
                outboundQueue.push(operation)
            }
            is SyncResponseAction.MediaAltTextUpdated -> {
                notesLogger?.d(message = "updating note with MediaAltTextUpdated")
                val note = store.state.getNoteForNoteLocalId(action.noteId) ?: return

                val mediaWithUpdatedAltText = note.media.find { action.media.localId == it.localId }
                    ?: return
                val updatedMedia = note.media - mediaWithUpdatedAltText + action.media.copy(
                    localUrl = mediaWithUpdatedAltText.localUrl
                )

                val changedNote = note.copy(
                    remoteData = note.remoteData?.copy(changeKey = action.changeKey),
                    media = updatedMedia
                )

                val syncNote = changedNote.toSyncNote()
                val operation = UpdateNote(syncNote, note.uiRevision)
                notesLogger?.d(message = "operation with MediaAltTextUpdated")
                outboundQueue.push(operation)
            }
            is SyncResponseAction.ApplyConflictResolution -> {
                val note = store.state.getNoteForNoteLocalId(action.noteLocalId) ?: return
                val syncNote = note.toSyncNote()
                val operation = UpdateNote(syncNote, note.uiRevision)
                outboundQueue.push(operation)
            }
            is SyncResponseAction.InvalidateClientCache -> {
                outboundQueue.reset()
                outboundQueue.isPaused = false
                val operation = Sync(deltaToken = null)
                outboundQueue.push(operation)
                if (noteReferencesSyncEnabled) {
                    outboundQueue.push(NoteReferencesSync(deltaToken = null))
                }
                if (samsungNotesSyncEnabled) {
                    outboundQueue.push(SamsungNotesSync(deltaToken = null))
                }
            }
            is SyncResponseAction.InvalidateNoteReferencesClientCache -> {
                outboundQueue.clearNoteReferencesOperation()
                outboundQueue.isPaused = false
                outboundQueue.push(NoteReferencesSync(deltaToken = null))
            }
            is SyncResponseAction.InvalidateSamsungNotesClientCache -> {
                outboundQueue.clearSamsungNotesOperationsFromQueue()
                outboundQueue.isPaused = false
                outboundQueue.push(SamsungNotesSync(deltaToken = null))
            }
            is SyncResponseAction.NotAuthorized -> {
                notesLogger?.recordTelemetry(
                    EventMarkers.SyncSessionAction,
                    Pair(NotesSDKTelemetryKeys.NoteProperty.ACTION, SyncActionType.STOP)
                )
            }
        }
    }

    fun startPollingAndRealtime(userID: String) {
        val feedRealtimeSyncEnabled = NotesLibrary.getInstance().experimentFeatureFlags.feedRealTimeEnabled
        if (globalPoller == null) {
            globalPoller = GlobalPoller(
                realtimeEnabled, noteReferencesSyncEnabled, samsungNotesSyncEnabled, meetingNotesSyncEnabled,
                feedRealtimeSyncEnabled, userID
            ) { newAction ->
                store.dispatch(newAction)
            }

            if (!NotesLibrary.getInstance().startPollingDisabledOnNewToken) {
                handleStartPolling()
            }
        }

        when (realtimeEnabled) {
            true -> startRealtimeForNotes(outboundRequestHandler.sdkManager.notes)
        }
    }

    private fun startRealtimeForNotes(notesSdk: NetworkedSdk.NotesSdk) {
        notesLogger?.recordTelemetry(EventMarkers.SyncRealtimeAction, Pair(NotesSDKTelemetryKeys.NoteProperty.ACTION, SyncActionType.START))
        startLongPollForNotes(notesSdk).onComplete {
            handleRealtimeResultForNotes(it, notesSdk)
            notesLogger?.recordTelemetry(EventMarkers.SyncRealtimeAction, Pair(NotesSDKTelemetryKeys.NoteProperty.ACTION, SyncActionType.END))
        }
    }

    private fun startLongPollForNotes(notesSdk: NetworkedSdk.NotesSdk): ApiPromise<Unit> {
        notesLogger?.i(message = "Starting realtime connection")
        when (context.isNetworkConnected()) {
            true -> {
                notesLogger?.i(message = "Network connection looks active for realtime")
                return notesSdk.longPoll(notesSdk.hostRelativeApiRealtimeUrl, realtimeSessionId, getOnNewDataImplForNotes(notesSdk))
            }
            else -> {
                notesLogger?.e(message = "No network connection for realtime")
                return ApiPromise.of(ApiError.NetworkError(Exception()))
            }
        }
    }

    private fun getOnNewDataImplForNotes(notesSdk: NetworkedSdk.NotesSdk) = when (notesSdk.apiVersion) {
        REALTIME_API_VERSION_V1 -> onNewDataImplV1ForNotes(notesSdk)
        REALTIME_API_VERSION_V2 -> onNewDataImplV2ForNotes(notesSdk)
        else -> onNewDataImplV1ForNotes(notesSdk)
    }

    private fun onNewDataImplV1ForNotes(notesSdk: NetworkedSdk.NotesSdk): (Char) -> Unit {
        var pollState = PollState.Waiting
        return { char ->
            realtimeReconnectDelay = 0
            pollState = when (char) {
                '\n' -> {
                    when (pollState) {
                        PollState.Waiting -> PollState.PreviousCharacterWasNewLine
                        PollState.PreviousCharacterWasNewLine -> {
                            onShoulderTap(notesSdk.userInfo.userID)
                            PollState.Waiting
                        }
                    }
                }
                else -> PollState.Waiting
            }
        }
    }

    private fun onNewDataImplV2ForNotes(notesSdk: NetworkedSdk.NotesSdk): (Char) -> Unit {
        var pollState = PollState.Waiting
        var currentBlock = ""

        return { char ->
            realtimeReconnectDelay = 0

            // a valid event block is terminated by two newline chars
            // we expect few stay alive events as well starting with ':' and terminated by SINGLE newline char
            pollState = when (char) {
                '\n' -> {
                    when (pollState) {
                        PollState.Waiting -> PollState.PreviousCharacterWasNewLine
                        PollState.PreviousCharacterWasNewLine -> {
                            getNoteTypeFromRealTimeEvent(currentBlock)?.let {
                                onShoulderTap(notesSdk.userInfo.userID, it)
                            }
                            PollState.Waiting
                        }
                    }
                }
                else -> {
                    if (pollState == PollState.PreviousCharacterWasNewLine) {
                        currentBlock = ""
                    }
                    currentBlock += char
                    PollState.Waiting
                }
            }
        }
    }

    private fun getNoteTypeFromRealTimeEvent(eventBlock: String): RealTimeNotificationSourceType? {
        if (eventBlock.startsWith(REALTIME_CONNECTION_KEEP_ALIVE_EVENT_PREFIX)) {
            // keep alive event...ignore
            return null
        }
        if (!eventBlock.startsWith(REALTIME_CONNECTION_VALID_DATA_EVENT_PREFIX)) {
            // unexpected event
            notesLogger?.recordTelemetry(EventMarkers.RealTimeSyncUnexpectedEvent)
            return null
        }
        var src: String? = null
        try {
            val obj = JSONObject(
                eventBlock.substring(
                    REALTIME_CONNECTION_VALID_DATA_EVENT_PREFIX.length,
                    eventBlock.length
                )
            )
            src = obj.getString(REALTIME_CONNECTION_JSON_SOURCE_KEY)
        } catch (e: JSONException) {
            notesLogger?.d(
                LOG_TAG,
                "Json exception error. Message: ${e.message} cause: ${e.cause} " +
                    "type: ${e::class.java.canonicalName} "
            )
            notesLogger?.recordTelemetry(
                EventMarkers.SyncJsonError,
                Pair(
                    NotesSDKTelemetryKeys.SyncProperty.JSON_PARSER,
                    JsonParser.JsonParserException.toString() + " type: ${e::class.java.canonicalName} "
                ),
                Pair(NotesSDKTelemetryKeys.SyncProperty.IS_REALTIME, true.toString())
            )
            return null
        }
        return when (src) {
            REALTIME_CONNECTION_RESPONSE_NOTES -> RealTimeNotificationSourceType.Notes
            REALTIME_CONNECTION_RESPONSE_CONNECTED_NOTES -> RealTimeNotificationSourceType.ConnectedNotes
            REALTIME_CONNECTION_RESPONSE_NOTEREFERENCES -> RealTimeNotificationSourceType.Notereferences
            else -> null
        }
    }

    private fun handleRealtimeResultForNotes(result: ApiResult<Unit>, notesSdk: NetworkedSdk.NotesSdk) {
        return when (result) {
            is ApiResult.Success -> handleRealtimeDisconnectForNotes(notesSdk)
            is ApiResult.Failure -> handleRealtimeErrorForNotes(result, notesSdk)
        }
    }

    private fun handleRealtimeDisconnectForNotes(notesSdk: NetworkedSdk.NotesSdk) {
        notesLogger?.i(message = "Realtime connection lost")
        retryRealtimeForNotes(notesSdk)
    }

    private fun handleRealtimeErrorForNotes(result: ApiResult.Failure<Unit>, notesSdk: NetworkedSdk.NotesSdk) {
        notesLogger?.e(message = "Realtime connection ended in error")
        when (result.error) {
            is HttpError401 -> {
                // trigger sync and let sync response handler deal with 401
                store.dispatch(AuthenticatedSyncRequestAction.RemoteChangedDetected(notesSdk.userInfo.userID))
            }
            else -> retryRealtimeForNotes(notesSdk)
        }
    }

    private fun retryRealtimeForNotes(notesSdk: NetworkedSdk.NotesSdk) {
        realtimeReconnectDelay = minOf(realtimeReconnectDelay + 500, REALTIME_RECONNECT_DELAY_MAX)
        notesLogger?.i(message = "Realtime reconnecting in " + realtimeReconnectDelay + " ms")
        ApiPromise.delay(realtimeReconnectDelay).andThen {
            startRealtimeForNotes(notesSdk)
        }
    }

    private fun onShoulderTap(userID: String, notificationSourceType: RealTimeNotificationSourceType = RealTimeNotificationSourceType.Notes) {
        val now = System.currentTimeMillis()
        val timeSince = now - lastShoulderTapTime
        if (timeSince >= MINIMUM_TIME_WINDOW_BEFORE_NEXT_SYNC) {
            notesLogger?.i(message = "Realtime message received, triggering sync")
            val changeDetectedAction: Action? = when (notificationSourceType) {
                RealTimeNotificationSourceType.Notes -> AuthenticatedSyncRequestAction.RemoteChangedDetected(userID)
                RealTimeNotificationSourceType.ConnectedNotes -> {
                    if (samsungNotesSyncEnabled) AuthenticatedSyncRequestAction.SamsungNotesChangedDetected(userID) else null
                }
                RealTimeNotificationSourceType.Notereferences -> {
                    if (noteReferencesSyncEnabled) AuthenticatedSyncRequestAction.RemoteNoteReferencesChangedDetected(userID) else null
                }
            }
            changeDetectedAction?.let { store.dispatch(it) }
            lastShoulderTapTime = now
        }
    }

    fun handleStartPolling() {
        if (globalPoller?.isPollingRunning() == false) {
            globalPoller?.startPolling()
        }
    }

    fun handleStopPolling() {
        if (globalPoller?.isPollingRunning() == true) {
            globalPoller?.stopPolling()
        }
    }

    private enum class RealTimeNotificationSourceType {
        Notes,
        ConnectedNotes,
        Notereferences
    }
}

fun deleteNoteOperation(action: SyncRequestAction.DeleteNote): ApiRequestOperation {
    val remoteId = action.remoteId
    return if (remoteId == null) {
        InvalidDeleteNote(action.localId)
    } else {
        DeleteNote(action.localId, remoteId)
    }
}

fun deleteNoteReferenceOperation(action: SyncRequestAction.DeleteNoteReference): ApiRequestOperation =
    DeleteNoteReference(action.localId, action.remoteId)

fun deleteSamsungNoteOperation(action: SyncRequestAction.DeleteSamsungNote): ApiRequestOperation =
    DeleteSamsungNote(action.localId, action.remoteId)

fun updateNoteOperation(action: SyncRequestAction.UpdateNote): ApiRequestOperation {
    val note = action.note.toSyncNote()
    return if (note.remoteData == null) {
        InvalidUpdateNote(note, action.uiRevision)
    } else {
        UpdateNote(note, action.uiRevision)
    }
}

fun uploadMediaOperation(action: SyncRequestAction.UploadMedia): ApiRequestOperation {
    val note = action.note.toSyncNote()
    return if (note.remoteData == null) {
        InvalidUploadMedia(
            note = note, mediaLocalId = action.mediaLocalId, localUrl = action.localUrl,
            mimeType = action.mimeType
        )
    } else {
        UploadMedia(
            note = note, mediaLocalId = action.mediaLocalId, localUrl = action.localUrl,
            mimeType = action.mimeType
        )
    }
}

fun deleteMediaOperation(action: SyncRequestAction.DeleteMedia): ApiRequestOperation {
    val noteLocalId = action.localNoteId
    val noteRemoteId = action.remoteNoteId
    val mediaLocalId = action.mediaLocalId
    val mediaRemoteId = action.mediaRemoteId

    return if (noteRemoteId == null || mediaRemoteId == null) {
        InvalidDeleteMedia(noteLocalId, mediaLocalId)
    } else {
        DeleteMedia(noteLocalId, noteRemoteId, mediaLocalId, mediaRemoteId)
    }
}

fun updateMediaAltTextOperation(action: SyncRequestAction.UpdateMediaAltText): ApiRequestOperation {
    val note = action.note.toSyncNote()
    val mediaLocalId = action.mediaLocalId
    val mediaRemoteId = action.mediaRemoteId
    val uiRevision = action.note.uiRevision

    return if (note.remoteData?.id == null || mediaRemoteId == null) {
        InvalidUpdateMediaAltText(note, mediaLocalId, action.altText, uiRevision)
    } else {
        UpdateMediaAltText(note, mediaLocalId, mediaRemoteId, action.altText, uiRevision)
    }
}

class UserSyncHandlerBuilder(
    val context: Context,
    val rootDirectory: File,
    val store: Store,
    val notesLogger: NotesLogger?,
    val isDebugMode: Boolean,
    val userInfo: UserInfo,
    val realtimeEnabled: Boolean,
    val createSdkManager: (UserInfo) -> SdkManager,
    val apiHostInitialized: Boolean
) {

    companion object {
        private const val LOG_TAG = "UserSyncHandlerBuilder"
    }

    private val correlationVector by lazy {
        CorrelationVector { byteArray ->
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        }
    }

    fun build(): UserSyncHandler {
        val notesFilesDir = File(rootDirectory, Constants.NOTES_FOLDER_NAME)
        if (!notesFilesDir.exists()) {
            notesFilesDir.mkdirs()
        }

        val newNotesFilesDir = if (userInfo.userInfoSuffix.isNotEmpty()) File(notesFilesDir, userInfo.userInfoSuffix) else
            notesFilesDir

        if (!newNotesFilesDir.exists()) {
            newNotesFilesDir.mkdirs()
        }

        val apiRequestOperationHandler = OutboundQueueApiRequestOperationHandler(createSdkManager(userInfo))

        val notesImagesDir = File(newNotesFilesDir, Constants.NOTES_IMAGES_FOLDER_NAME)
        if (!notesImagesDir.exists()) {
            notesImagesDir.mkdirs()
        }

        val apiResponseEventHandler = ApiResponseEventHandler(
            store = store,
            createFile = { fileName ->
                File(notesImagesDir, fileName)
            },
            mimeTypeToFileExtension = this::mimeTypeToFileExtension,
            decodeBase64 = { encodedData ->
                Base64.decode(encodedData, Base64.DEFAULT)
            },
            userID = userInfo.userID
        )

        val backingQueue = PriorityQueue(
            queue = emptyList(),
            createBackupFile = { fileName ->
                AtomicFileBackup(newNotesFilesDir, fileName, notesLogger)
            },
            notesLogger = notesLogger
        )

        val outboundQueue = OutboundQueue(
            context = context,
            userID = userInfo.userID,
            backingQueue = backingQueue,
            apiRequestOperationHandler = apiRequestOperationHandler,
            apiResponseEventHandler = apiResponseEventHandler,
            notesLogger = notesLogger,
            sleep = { amount -> ApiPromise.task { Thread.sleep(amount) } },
            isDebugMode = isDebugMode,
            correlationVector = correlationVector,
            experimentFeatureFlags = NotesLibrary.getInstance().experimentFeatureFlags
        )

        return UserSyncHandler(
            context = context,
            store = store,
            outboundQueue = outboundQueue,
            eventHandler = apiResponseEventHandler,
            outboundRequestHandler = apiRequestOperationHandler,
            notesLogger = notesLogger,
            realtimeEnabled = realtimeEnabled,
            createSdkManager = createSdkManager,
            apiHostInitialized = apiHostInitialized
        )
    }

    private fun mimeTypeToFileExtension(mimeType: String): String =
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ".jpeg"
}
