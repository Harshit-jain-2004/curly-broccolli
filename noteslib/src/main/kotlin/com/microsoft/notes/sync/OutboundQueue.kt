package com.microsoft.notes.sync

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.JsonIOException
import com.microsoft.notes.noteslib.ExperimentFeatureFlags
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
import com.microsoft.notes.sync.ApiResponseEvent.OutboundQueueSyncActive
import com.microsoft.notes.sync.ApiResponseEvent.OutboundQueueSyncInactive
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncError.SyncErrorType
import com.microsoft.notes.sync.QueueInstruction.AddOperation
import com.microsoft.notes.sync.QueueInstruction.BroadcastEvent
import com.microsoft.notes.sync.QueueInstruction.BroadcastSyncErrorEvent
import com.microsoft.notes.sync.QueueInstruction.DelayQueue
import com.microsoft.notes.sync.QueueInstruction.LogTelemetry
import com.microsoft.notes.sync.QueueInstruction.MapQueue
import com.microsoft.notes.sync.QueueInstruction.PauseQueue
import com.microsoft.notes.sync.QueueInstruction.RemoveOperation
import com.microsoft.notes.sync.QueueInstruction.ReplaceOperation
import com.microsoft.notes.sync.QueueInstruction.ResetQueue
import com.microsoft.notes.sync.QueueInstruction.SetDelay
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.SyncProperty.SYNC_ACTIVE_STATUS
import com.microsoft.notes.utils.logging.SyncActiveStatus
import com.microsoft.notes.utils.utils.Constants

interface ApiResponseEventHandler {
    fun handleEvent(apiResponseEvent: ApiResponseEvent)
}

class OutboundQueue(
    private val context: Context,
    private val userID: String,
    private val backingQueue: PriorityQueue,
    private val apiRequestOperationHandler: ApiRequestOperationHandler,
    private val apiResponseEventHandler: ApiResponseEventHandler,
    private val notesLogger: NotesLogger? = null,
    private val sleep: (Long) -> ApiPromise<Unit>,
    private val isDebugMode: Boolean,
    val correlationVector: CorrelationVector,
    private val experimentFeatureFlags: ExperimentFeatureFlags
) {
    private var delayTimeInMilliseconds: Long = 0
    private var _isPaused = false
    private var _isWorking = false
    private var _consecutive503Count: Int = 0
    private var isFRESyncing = false
    private val feedFreSyncPreferences: SharedPreferences = context.getSharedPreferences(Constants.FEED_FRE_SYNC_PREFERENCE, Context.MODE_PRIVATE)

    /*
    * Belong strings would be stored in shared pref to keep track of FRE sync
    * Queued strings are used for full sync operation for first time queue
    * Completed strings are used for FRE sync completion
    * */
    private val freSyncCompletedString = "FRESyncCompleted"
    private val freNoteReferencesSyncCompletedString = "FRENoteReferencesSyncCompleted"
    private val freMeetingNotesSyncCompletedString = "FREMeetingNotesSyncCompleted"
    private val freSamsungNotesSyncCompletedString = "FRESamsungNotesSyncCompleted"
    private val freSyncQueuedString = "FRESyncQueued"
    private val freNoteReferencesSyncQueuedString = "FRENoteReferencesSyncQueued"
    private val freMeetingNotesSyncQueuedString = "FREMeetingNotesSyncQueued"
    private val freSamsungNotesSyncQueuedString = "FRESamsungNotesSyncQueued"
    private val freSyncingKey = Constants.FEED_FRE_SYNC_PREFERENCE_KEY + "_" + userID
    private val noteReferencesSyncEnabled
        get() = experimentFeatureFlags.noteReferencesSyncEnabled
    var meetingNotesSyncEnabled = false
    private val samsungNotesSyncEnabled
        get() = experimentFeatureFlags.samsungNotesSyncEnabled
    private val feedFREFastSyncEnabled
        get() = experimentFeatureFlags.feedFREFastSyncEnabled

    fun reset() {
        backingQueue.clear()
    }

    fun clearNoteReferencesOperation() {
        backingQueue.removeIf { apiRequestOperation ->
            when (apiRequestOperation) {
                is NoteReferencesSync -> true
                else -> false
            }
        }
    }

    fun clearSamsungNotesOperationsFromQueue() {
        backingQueue.removeIf { apiRequestOperation ->
            when (apiRequestOperation) {
                is SamsungNotesSync -> true
                else -> false
            }
        }
    }

    fun clearFeedFRESyncSharedPreferences() {
        feedFreSyncPreferences.edit().remove(freSyncingKey).apply()
    }

    var isPaused: Boolean
        get() = synchronized(this) {
            return _isPaused
        }
        set(value) = synchronized(this) {
            notesLogger?.recordTelemetry(
                eventMarker = EventMarkers.SyncActiveStatus,
                keyValuePairs = *arrayOf(
                    Pair(
                        SYNC_ACTIVE_STATUS,
                        if (value) SyncActiveStatus.INACTIVE.name else SyncActiveStatus.ACTIVE.name
                    )
                )
            )

            apiResponseEventHandler.handleEvent(
                if (value) OutboundQueueSyncInactive() else OutboundQueueSyncActive()
            )

            _isPaused = value
        }

    private var isWorking: Boolean
        get() = synchronized(this) {
            return _isWorking
        }
        set(value) = synchronized(this) {
            _isWorking = value
        }

    /*
    * Check if FRE sync is completed for all type of sync operations
    * */
    private fun isFRESyncForAllCompleted(): Boolean {
        val freValues = feedFreSyncPreferences.getString(freSyncingKey, null)?.split(',')
            ?: return false
        if (!freValues.contains(freSyncCompletedString) || (noteReferencesSyncEnabled && !freValues.contains(freNoteReferencesSyncCompletedString)) || (samsungNotesSyncEnabled && !freValues.contains(freSamsungNotesSyncCompletedString)) || (meetingNotesSyncEnabled && !freValues.contains(freMeetingNotesSyncCompletedString))) {
            return false
        }
        return true
    }

    @Suppress("ExpressionBodySyntax")
    private fun isFullSyncOperation(operation: ApiRequestOperation): Boolean {
        return (operation is Sync && operation.deltaToken == null) || (operation is NoteReferencesSync && operation.deltaToken == null) || (operation is SamsungNotesSync && operation.deltaToken == null || (operation is MeetingNotesSync))
    }

    /*
    * Check if the operation is a full sync request for the very first time
    * Add it to shared preference accordingly
    * */
    private fun isFullSyncFirstTimeQueue(operation: ApiRequestOperation): Boolean {
        if (!isFullSyncOperation(operation)) return false
        val freValue = feedFreSyncPreferences.getString(freSyncingKey, null)
        val freValues = freValue?.split(',')
        return !isOperationAlreadyInQueue(operation, freValues)
    }

    private fun updateFREQueuedString(operation: ApiRequestOperation) {
        if (!isFullSyncOperation(operation)) return
        val freValue = feedFreSyncPreferences.getString(freSyncingKey, null)
        var updateString = when (operation) {
            is Sync -> {
                freSyncQueuedString
            }
            is NoteReferencesSync -> {
                freNoteReferencesSyncQueuedString
            }
            is SamsungNotesSync -> {
                freSamsungNotesSyncQueuedString
            }
            is MeetingNotesSync -> {
                freMeetingNotesSyncQueuedString
            }
            else -> ""
        }
        if (freValue != null) {
            updateString = "$freValue,$updateString"
        }
        feedFreSyncPreferences.edit().putString(freSyncingKey, updateString).apply()
    }

    private fun isOperationAlreadyInQueue(operation: ApiRequestOperation, freValues: List<String>?): Boolean =
        when (operation) {
            is Sync -> freValues?.contains(freSyncQueuedString)
            is NoteReferencesSync -> freValues?.contains(freNoteReferencesSyncQueuedString)
            is SamsungNotesSync -> freValues?.contains(freSamsungNotesSyncQueuedString)
            is MeetingNotesSync -> freValues?.contains(freMeetingNotesSyncQueuedString)
            else -> false
        } == true

    fun push(operation: ApiRequestOperation, workImmediately: Boolean = true) {
        val updatedOperation = if (operation is ApiRequestOperation.InvalidApiRequestOperation)
            mapInvalidOperationIfNeeded(operation)
        else
            operation

        /*
        * If it is a full sync, we only push it to the queue if this request is coming for the first time or FRE sync is completed for all note types.
        * Reason behind this:
            * When we process a full sync operation, if it is SN (Sync) or SamsungNote Sync, there could be media (images) attached to it. We process the operation response, figure out if there is a media and then push it to the queue.
            * If a full sync request comes while we figuring out if there is a media attached to the sync response, it might start before the mediaDownload request which means we would be waiting for the full sync operation to complete to download the media for FRE requests.
            * Hence we are blocking the full sync (after the FRE sync) until we complete the processing of the sync operation result.
        * */
        if (!feedFREFastSyncEnabled || !isFullSyncOperation(operation) || isFullSyncFirstTimeQueue(operation) || isFRESyncForAllCompleted()) {
            backingQueue.push(updatedOperation)
            squash(backingQueue, updatedOperation)
            // Update shared pref only when request is pushed to queue to avoid racing condition
            updateFREQueuedString(updatedOperation)
        }
        // Start work if not already started for every push operation
        if (workImmediately) {
            workUntilEmpty()
        }
    }

    private fun workUntilEmpty() {
        work().onComplete {
            if (it.unwrap() == true) {
                workUntilEmpty()
            }
        }
    }

    fun work(): ApiPromise<Boolean> {
        val operation = next() ?: return ApiPromise.of(false)
        val freValues = feedFreSyncPreferences.getString(freSyncingKey, null)?.split(',')
        isFRESyncing = false
        val work = when (operation) {
            is Sync -> {
                isFRESyncing = feedFREFastSyncEnabled && !(freValues?.contains(freSyncCompletedString) ?: false)
                apiRequestOperationHandler.setIsFRESyncing(isFRESyncing)
                handle(operation, apiRequestOperationHandler::handleSync)
            }
            is CreateNote -> handle(operation, apiRequestOperationHandler::handleCreate)
            is UpdateNote -> handle(operation, apiRequestOperationHandler::handleUpdate)
            is GetNoteForMerge -> handle(operation, apiRequestOperationHandler::handleGetForMerge)
            is DeleteNote -> handle(operation, apiRequestOperationHandler::handleDelete)
            is DeleteNoteReference -> handle(operation, apiRequestOperationHandler::handleNoteReferenceDelete)
            is DeleteSamsungNote -> handle(operation, apiRequestOperationHandler::handleSamsungNoteDelete)
            is UploadMedia -> handle(operation, apiRequestOperationHandler::handleUploadMedia)
            is DownloadMedia -> handle(operation, apiRequestOperationHandler::handleDownloadMedia)
            is DeleteMedia -> handle(operation, apiRequestOperationHandler::handleDeleteMedia)
            is UpdateMediaAltText -> handle(operation, apiRequestOperationHandler::handleUpdateMediaAltText)
            is NoteReferencesSync -> {
                isFRESyncing = feedFREFastSyncEnabled && !(freValues?.contains(freNoteReferencesSyncCompletedString) ?: false)
                apiRequestOperationHandler.setIsFRESyncing(isFRESyncing)
                handle(operation, apiRequestOperationHandler::handleNoteReferenceSync)
            }
            is MeetingNotesSync -> {
                isFRESyncing = feedFREFastSyncEnabled && !(freValues?.contains(freMeetingNotesSyncCompletedString) ?: false)
                apiRequestOperationHandler.setIsFRESyncing(isFRESyncing)
                handle(operation, apiRequestOperationHandler::handleMeetingNoteSync)
            }
            is SamsungNotesSync -> {
                isFRESyncing = feedFREFastSyncEnabled && !(freValues?.contains(freSamsungNotesSyncCompletedString) ?: false)
                apiRequestOperationHandler.setIsFRESyncing(isFRESyncing)
                handle(operation, apiRequestOperationHandler::handleSamsungNoteSync)
            }
        }
        return work.map { true }
    }

    val count
        get() = backingQueue.count

    fun toList(): List<ApiRequestOperation> =
        backingQueue.toList()

    @Synchronized
    private fun next(): ApiRequestOperation.ValidApiRequestOperation? {
        if (_isPaused) return null
        if (_isWorking) return null
        val operation = backingQueue.peek() ?: return null
        return if (operation is ApiRequestOperation.ValidApiRequestOperation) {
            _isWorking = true
            operation
        } else {
            null
        }
    }

    private fun <T : ApiRequestOperation.ValidApiRequestOperation, U : ApiResponseEvent> handle(
        operation: T,
        handler: (T) -> ApiPromise<U>
    ): ApiPromise<Unit> {
        operation.isProcessing = true
        operation.requestId = correlationVector.incrementAndGet()
        operation.realTimeSessionId = correlationVector.correlationVectorBase
        backingQueue.replace(operation)
        var item = ""
        when (operation) {
            is Sync -> {
                notifySyncOperationStart()
                item = freSyncCompletedString
            }
            is NoteReferencesSync -> {
                notifyNoteReferenceSyncOperationStart()
                item = freNoteReferencesSyncCompletedString
            }
            is SamsungNotesSync -> {
                notifySamsungNotesSyncOperationStart()
                item = freSamsungNotesSyncCompletedString
            }
            is MeetingNotesSync -> {
                notifyMeetingNoteSyncOperationStart()
                item = freMeetingNotesSyncCompletedString
            }
        }

        try {
            var successful = false
            return handler(operation)
                .mapResult { result ->
                    successful = when (result) {
                        is ApiResult.Success -> true
                        is ApiResult.Failure -> false
                    }
                    getInstructions(operation, result)
                }
                .flatMap { instructions ->
                    handleInstructions(instructions)
                }
                .andThen {
                    finishWorking(operation, successful)
                        /*
                        * If FRE syncing for NoteReferenceSync, there won't be any post processing to check the media,
                        * so add the shared pref value here and push the full syncs.
                        * We are pushing the full sync for all after each full sync FRE completion because we don't know which request would be the last.
                        * If this is not the last FRE full sync, it would be discarded in the queue push logic. Please check the push function logic.
                        * */
                    if (feedFREFastSyncEnabled && isFRESyncing && (!successful || operation is NoteReferencesSync || operation is MeetingNotesSync)) {
                        updateSharedPref(item)
                        if (isFullSyncOperation(operation)) {
                            pushFullSyncOperations()
                        }
                    }
                }
        } catch (e: JsonIOException) {
            // We catch this exception that could be thrown while parsing a string using Gson, so we are able
            // to crash the app, but we avoid data loss and entering in a crash loop when opening the app again
            // since we delete the operation from the queue.
            backingQueue.remove(operation)
            if (feedFREFastSyncEnabled && isFRESyncing) {
                updateSharedPref(item)
            }
            throw e
        }
    }

    private fun updateSharedPref(value: String) {
        if (value.isEmpty()) return
        var item = value
        val freValue = feedFreSyncPreferences.getString(freSyncingKey, null)
        if (freValue != null) {
            item = "$freValue,$value"
        }
        feedFreSyncPreferences.edit().putString(freSyncingKey, item).apply()
    }

    private fun pushFullSyncOperations() {
        push(Sync(deltaToken = null))
        if (noteReferencesSyncEnabled) {
            push(NoteReferencesSync(deltaToken = null))
        }
        if (samsungNotesSyncEnabled) {
            push(SamsungNotesSync(deltaToken = null))
        }
        if (meetingNotesSyncEnabled) {
            push(MeetingNotesSync())
        }
    }

    private fun <T : ApiResponseEvent> getInstructions(
        operation: ApiRequestOperation.ValidApiRequestOperation,
        result: ApiResult<T>
    ): ApiResult.Success<List<QueueInstruction>> {
        return ApiResult.Success(
            ApiResultHandler(notesLogger, isDebugMode).handleResult(result, operation)
        )
    }

    private fun handleInstructions(instructions: List<QueueInstruction>): ApiPromise<Unit> {
        return ApiPromise.task {
            instructions.forEach { handleQueueResult(it).get() }
        }
    }

    private fun finishWorking(operation: ApiRequestOperation, successful: Boolean) {
        operation.telemetryBundle.reset()
        operation.isProcessing = false
        backingQueue.replace(operation)

        /*
        * We don't want to update the state as FRE sync might not be full sync
        * */
        if (!(feedFREFastSyncEnabled && isFRESyncing && successful)) {
            if (operation is Sync) {
                updateFinalSyncOperationState(successful)
            } else if (operation is NoteReferencesSync) {
                updateFinalNoteReferenceSyncOperationState(successful)
            } else if (operation is SamsungNotesSync) {
                updateFinalSamsungNotesSyncOperationState(successful)
            } else if (operation is MeetingNotesSync) {
                updateFinalMeetingNotesSyncOperationState(successful)
            }
        }
        isWorking = false
    }

    private fun notifySyncOperationStart() {
        apiResponseEventHandler.handleEvent(ApiResponseEvent.RemoteNotesSyncStarted())
    }

    private fun notifyNoteReferenceSyncOperationStart() {
        apiResponseEventHandler.handleEvent(ApiResponseEvent.RemoteNoteReferencesSyncStarted())
    }

    private fun notifyMeetingNoteSyncOperationStart() {
        apiResponseEventHandler.handleEvent(ApiResponseEvent.RemoteMeetingNotesSyncStarted())
    }

    private fun notifySamsungNotesSyncOperationStart() {
        apiResponseEventHandler.handleEvent(ApiResponseEvent.RemoteSamsungNotesSyncStarted())
    }

    private fun notifySyncOperationError(errorType: SyncErrorType) {
        val pendingSyncOperations = backingQueue.toList().filter { it is Sync }
        if (pendingSyncOperations.isNotEmpty()) {
            apiResponseEventHandler.handleEvent(
                ApiResponseEvent.RemoteNotesSyncError(errorType)
            )
        }
    }

    private fun updateFinalSyncOperationState(successful: Boolean) {
        apiResponseEventHandler.handleEvent(
            if (successful) ApiResponseEvent.RemoteNotesSyncSucceeded() else ApiResponseEvent.RemoteNotesSyncFailed()
        )
    }

    private fun updateFinalNoteReferenceSyncOperationState(successful: Boolean) {
        apiResponseEventHandler.handleEvent(
            if (successful) ApiResponseEvent.RemoteNoteReferencesSyncSucceeded() else ApiResponseEvent.RemoteNoteReferencesSyncFailed()
        )
    }

    private fun updateFinalSamsungNotesSyncOperationState(successful: Boolean) {
        apiResponseEventHandler.handleEvent(
            if (successful) ApiResponseEvent.RemoteSamsungNotesSyncSucceeded() else ApiResponseEvent.RemoteSamsungNotesSyncFailed()
        )
    }

    private fun updateFinalMeetingNotesSyncOperationState(successful: Boolean) {
        apiResponseEventHandler.handleEvent(
            if (successful) ApiResponseEvent.RemoteMeetingNotesSyncSucceeded() else ApiResponseEvent.RemoteMeetingNotesSyncFailed()
        )
    }

    private fun delayPromise(): ApiPromise<Unit> = sleep(delayTimeInMilliseconds)

    // If an in-progress operation's response is being handled at the same time as another
    // matching invalid operation is being pushed onto the queue, it's possible that the
    // operation being pushed won't have it's remote ID updated with the rest of the items
    // already in the queue. This closes that race condition.
    private fun mapInvalidOperationIfNeeded(operation: ApiRequestOperation.InvalidApiRequestOperation): ApiRequestOperation {
        return when (operation) {
            is InvalidUpdateNote -> {
                val remoteData = apiRequestOperationHandler.remoteDataMap[operation.note.id]
                    ?: return operation
                val note = operation.note.copy(remoteData = remoteData)
                UpdateNote(note, operation.uiBaseRevision, operation.uniqueId)
            }
            is InvalidDeleteNote -> {
                val remoteData = apiRequestOperationHandler.remoteDataMap[operation.localId]
                    ?: return operation
                DeleteNote(operation.localId, remoteData.id, operation.uniqueId)
            }
            is InvalidUploadMedia -> {
                val remoteData = apiRequestOperationHandler.remoteDataMap[operation.note.id]
                    ?: return operation
                UploadMedia(
                    note = operation.note.copy(remoteData = remoteData),
                    mediaLocalId = operation.mediaLocalId,
                    localUrl = operation.localUrl,
                    mimeType = operation.mimeType,
                    uniqueId = operation.uniqueId
                )
            }
            is InvalidDeleteMedia -> operation
            is InvalidUpdateMediaAltText -> operation
        }
    }

    @Suppress("LongMethod")
    private fun handleQueueResult(queueInstruction: QueueInstruction): ApiPromise<Unit> {
        return when (queueInstruction) {
            is MapQueue -> {
                this.backingQueue.map(queueInstruction.mapper)
                ApiPromise.of(Unit)
            }
            is RemoveOperation -> {
                backingQueue.remove(queueInstruction.operation)
                ApiPromise.of(Unit)
            }
            is ReplaceOperation -> {
                backingQueue.remove(queueInstruction.old)
                backingQueue.push(queueInstruction.new)
                ApiPromise.of(Unit)
            }
            is AddOperation -> {
                backingQueue.push(queueInstruction.operation)
                ApiPromise.of(Unit)
            }
            is LogTelemetry -> {
                val telemetryResult = queueInstruction.result
                when (telemetryResult) {
                    is ApiResult.Failure -> {
                        queueInstruction.bundle
                            .setStatus(telemetryResult, shouldModifySyncScore(telemetryResult.error))
                            .recordTelemetry(notesLogger, queueInstruction.eventMarker)
                    }
                    is ApiResult.Success -> {
                        queueInstruction.bundle.setStatus(telemetryResult)
                            .recordTelemetry(notesLogger, queueInstruction.eventMarker)
                    }
                }

                setConsecutive503Count(telemetryResult)

                ApiPromise.of(Unit)
            }
            is BroadcastEvent -> {
                apiResponseEventHandler.handleEvent(queueInstruction.event)
                ApiPromise.of(Unit)
            }
            is BroadcastSyncErrorEvent -> {
                notifySyncOperationError(queueInstruction.syncErrorType)
                ApiPromise.of(Unit)
            }
            is SetDelay -> {
                delayTimeInMilliseconds = when (queueInstruction.delta) {
                    is SetDelay.DelayDelta.ResetTo -> queueInstruction.delta.amount
                    is SetDelay.DelayDelta.Exponential -> {
                        if (delayTimeInMilliseconds == 0L) {
                            1000L
                        } else {
                            minOf(delayTimeInMilliseconds * queueInstruction.delta.factor, queueInstruction.delta.until)
                        }
                    }
                }
                ApiPromise.of(Unit)
            }
            is DelayQueue -> {
                delayPromise()
            }
            is PauseQueue -> {
                isPaused = true
                ApiPromise.of(Unit)
            }
            is ResetQueue -> {
                reset()
                if (apiRequestOperationHandler is OutboundQueueApiRequestOperationHandler) {
                    apiRequestOperationHandler.reset()
                }
                ApiPromise.of(Unit)
            }
        }
    }

    private fun setConsecutive503Count(telemetryResult: ApiResult<Any>) {
        if (telemetryResult is ApiResult.Failure && telemetryResult.error is HttpError503) {
            _consecutive503Count++
        } else if (telemetryResult is ApiResult.Success) {
            _consecutive503Count = 0
        }
    }

    private fun shouldModifySyncScore(telemetryError: ApiError): Boolean {
        return when (telemetryError) {
            is HttpError401 -> false
            is HttpError404.Http404RestApiNotFound -> false
            is HttpError409 -> false
            is HttpError410 -> false
            is HttpError503 -> _consecutive503Count > 0
            else -> true
        }
    }
}
