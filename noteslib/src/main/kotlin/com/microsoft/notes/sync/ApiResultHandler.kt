package com.microsoft.notes.sync

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
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.NoteReferencesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.SamsungNotesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.Sync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateMediaAltText
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UploadMedia
import com.microsoft.notes.sync.ApiResponseEvent.ForbiddenError
import com.microsoft.notes.sync.ApiResponseEvent.Gone
import com.microsoft.notes.sync.ApiResponseEvent.InvalidateClientCache
import com.microsoft.notes.sync.ApiResponseEvent.InvalidateNoteReferencesClientCache
import com.microsoft.notes.sync.ApiResponseEvent.InvalidateSamsungNotesClientCache
import com.microsoft.notes.sync.ApiResponseEvent.MediaAltTextUpdated
import com.microsoft.notes.sync.ApiResponseEvent.NotAuthorized
import com.microsoft.notes.sync.ApiResponseEvent.NoteCreated
import com.microsoft.notes.sync.ApiResponseEvent.NoteUpdated
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncError.SyncErrorType
import com.microsoft.notes.sync.ApiResponseEvent.UpgradeRequired
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
import com.microsoft.notes.sync.SyncRequestTelemetry.SyncRequestType
import com.microsoft.notes.sync.models.RemoteNote
import com.microsoft.notes.sync.models.localOnly.Note
import com.microsoft.notes.sync.models.localOnly.RemoteData
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import java.net.MalformedURLException
import java.net.URL

private const val MAX_DELAY: Long = 30_000 // ms

class ApiResultHandler(val notesLogger: NotesLogger?, val isDebugMode: Boolean) {
    fun <T : ApiResponseEvent> handleResult(
        result: ApiResult<T>,
        operation: ApiRequestOperation.ValidApiRequestOperation
    ): List<QueueInstruction> =
        when (result) {
            is ApiResult.Success -> handleSuccess(result.value, operation)
            is ApiResult.Failure -> handleFailure(result.error, operation)
        }

    private fun <T : ApiResponseEvent> handleSuccess(
        event: T,
        operation: ApiRequestOperation.ValidApiRequestOperation
    ): List<QueueInstruction> {
        notesLogger?.i(
            message = "OutboundQueue ApiRequestOperation successful " +
                "operation: ${operation.javaClass.canonicalName}, requestId: ${operation.requestId}"
        )
        val successResults = when {
            operation is CreateNote && event is NoteCreated -> {
                listOf(onSuccessfulOperation(operation.note.id, event.remoteNote))
            }
            operation is UpdateNote && event is NoteUpdated -> {
                listOf(onSuccessfulOperation(operation.note.id, event.remoteNote))
            }
            operation is UpdateMediaAltText && event is MediaAltTextUpdated -> {
                operation.note.remoteData?.let {
                    listOf(
                        onSuccessfulUpdateMediaAltText(
                            operation.note.id,
                            it.copy(changeKey = event.mediaAltTextUpdate.changeKey)
                        )
                    )
                } ?: emptyList()
            }
            else -> emptyList()
        }

        return successResults + listOf(
            BroadcastEvent(event),
            RemoveOperation(operation),
            SetDelay(SetDelay.DelayDelta.ResetTo(0)),
            LogTelemetry(
                operation.telemetryBundle, ApiResult.Success(event),
                EventMarkers.SyncRequestCompleted
            )
        )
    }

    private fun handleFailure(
        error: ApiError,
        operation: ApiRequestOperation.ValidApiRequestOperation
    ): List<QueueInstruction> {

        val exception = when (error) {
            is ApiError.Exception -> error.exception
            is ApiError.NetworkError -> error.error
            else -> null
        }

        exception?.let {
            notesLogger?.e(message = "An exception has occurred in sync" + "name: " + it.javaClass.canonicalName)
        }

        notesLogger?.e(
            message = "OutboundQueue ApiRequestOperation failed " +
                "operation: ${operation.javaClass.canonicalName}, requestId: ${operation.requestId}" +
                ", result: ${error.javaClass.canonicalName}"
        )

        notesLogger?.d(message = "Failure in outbound queue: $error")

        val telemetryResult = listOf<QueueInstruction>(
            LogTelemetry(operation.telemetryBundle, ApiResult.Failure(error), EventMarkers.SyncRequestFailed)
        )

        val results = when (error) {
            is HttpError -> handleHttpError(error, operation)
            is ApiError.FatalError -> handleFatalError(error, operation)
            else -> handleGenericError(error, operation)
        }
        return results + telemetryResult
    }

    private fun recoverWith(
        operation: ApiRequestOperation.ValidApiRequestOperation
    ): ((ApiError) -> ApiRequestOperation?)? {
        return when (operation) {
            is UpdateNote -> { error ->
                getNoteForMerge(error, operation.note, operation.uiBaseRevision)
            }
            is UpdateMediaAltText -> { error ->
                getNoteForMerge(error, operation.note, operation.uiBaseRevision)
            }
            else -> null
        }
    }

    private fun ignoreIf(operation: ApiRequestOperation.ValidApiRequestOperation): ((ApiError) -> Boolean)? {
        return when (operation) {
            is UpdateNote -> ::ignore404s
            is GetNoteForMerge -> ::ignore404s
            is DeleteNote -> ::ignore404s
            is DeleteNoteReference -> ::ignore404s
            is DeleteSamsungNote -> ::ignore404s
            is UploadMedia -> ::ignore404s
            is DownloadMedia -> ::ignore404s
            is DeleteMedia -> ::ignore404s
            is UpdateMediaAltText -> ::ignore404s
            else -> null
        }
    }

    private fun handleHttpError(
        error: HttpError,
        operation: ApiRequestOperation.ValidApiRequestOperation
    ): List<QueueInstruction> {
        notesLogger?.d(message = "handleHttpError: $error")
        return when (error) {
            is HttpError400 -> handle400(error, operation)
            is HttpError401 -> handle401(operation)
            is HttpError403 -> handle403(error, operation)
            is HttpError409 -> handle409(error, operation)
            is HttpError410 -> handle410(error, operation)
            is HttpError426 -> handle426(operation)
            is HttpError429 -> handle429(
                getRetryTime(error.headers), operation
            )
            is HttpError503 -> handle503(
                getRetryTime(error.headers), operation
            )
            else -> handleGenericError(error, operation)
        }
    }

    private fun handleFatalError(
        error: ApiError.FatalError,
        operation: ApiRequestOperation.ValidApiRequestOperation
    ): List<QueueInstruction> {
        notesLogger?.d(message = "handleFatalError: ${error.message}")
        operation.telemetryBundle.wasRetryOperation(retry = false)
        return listOf(
            RemoveOperation(operation),
            DelayQueue()
        )
    }

    private fun handleGenericError(
        error: ApiError,
        operation: ApiRequestOperation.ValidApiRequestOperation
    ): List<QueueInstruction> {
        notesLogger?.d(message = "handleGenericError: ${error::class.java.canonicalName}")

        var goneEvent: BroadcastEvent? = null

        // ignored 404s will result in queue operation cleanup, but we also need to cleanup our local state
        when (error) {
            is HttpError404 -> {
                goneEvent = BroadcastEvent(Gone(operation))
            }
        }

        val shouldIgnore = ignoreIf(operation)?.invoke(error) ?: false
        if (shouldIgnore) {
            operation.telemetryBundle.wasRetryOperation(retry = false)
            return listOfNotNull(RemoveOperation(operation = operation), goneEvent)
        }

        val newOperation = recoverWith(operation)?.invoke(error)
        if (newOperation != null) {
            operation.telemetryBundle.wasRetryOperation(retry = false)
            operation.telemetryBundle.newRequestOnFailure(SyncRequestType.requestType(newOperation))
            return listOfNotNull(ReplaceOperation(old = operation, new = newOperation), goneEvent)
        }

        operation.telemetryBundle.wasRetryOperation(retry = true)
        return listOfNotNull(
            SetDelay(SetDelay.DelayDelta.Exponential(2, until = MAX_DELAY)),
            DelayQueue(),
            BroadcastSyncErrorEvent(
                if (error is ApiError.NetworkError) SyncErrorType.NetworkUnavailable else SyncErrorType
                    .SyncFailure
            ),
            goneEvent
        )
    }

    // Invalid
    private fun handle400(
        error: HttpError400,
        operation: ApiRequestOperation
    ): List<QueueInstruction> {
        assertTrue(isDebugMode, "400 error found in development. $error")
        operation.telemetryBundle.wasRetryOperation(retry = false)
        return listOf(RemoveOperation(operation))
    }

    // Unauthorized error
    private fun handle401(operation: ApiRequestOperation): List<QueueInstruction> {
        operation.telemetryBundle.wasRetryOperation(retry = true)

        // The UserID is overriden in ApiResponseEventHandler.
        return listOf(
            PauseQueue(),
            BroadcastEvent(NotAuthorized("")),
            BroadcastSyncErrorEvent(SyncErrorType.Unauthenticated)
        )
    }

    // Forbidden error
    private fun handle403(
        error: HttpError403,
        operation: ApiRequestOperation.ValidApiRequestOperation
    ): List<QueueInstruction> {
        return when (error) {
            is HttpError403.NoMailbox -> {
                handleKnown403Error(ForbiddenError.ErrorType.NoMailbox)
            }
            is HttpError403.QuotaExceeded -> {
                handleKnown403Error(ForbiddenError.ErrorType.QuotaExceeded)
            }
            is HttpError403.GenericError -> {
                val urlString = error.errorDetails?.error?.innerError?.get("url")
                val url = if (urlString is String && urlString.isNotEmpty()) {
                    try {
                        URL(urlString)
                    } catch (e: MalformedURLException) {
                        notesLogger?.recordTelemetry(
                            EventMarkers.SyncMalformedUrlException,
                            Pair(NotesSDKTelemetryKeys.SyncProperty.URL, urlString)
                        )
                        null
                    }
                } else {
                    null
                }
                handleKnown403Error(ForbiddenError.ErrorType.GenericSyncError(url))
            }
            else -> {
                handleGenericError(error, operation)
            }
        }
    }

    private fun handleKnown403Error(errorType: ForbiddenError.ErrorType): List<QueueInstruction> {
        return listOf(
            PauseQueue(),
            BroadcastEvent(ForbiddenError(errorType)),
            BroadcastSyncErrorEvent(SyncErrorType.SyncPaused)
        )
    }

    // Conflict error
    private fun handle409(
        error: HttpError409,
        operation: ApiRequestOperation.ValidApiRequestOperation
    ): List<QueueInstruction> {
        val newOperation = recoverWith(operation)?.invoke(error)
        return if (newOperation != null) {
            operation.telemetryBundle.wasRetryOperation(retry = false)
            operation.telemetryBundle.newRequestOnFailure(SyncRequestType.requestType(newOperation))
            val replaceOperation = ReplaceOperation(old = operation, new = newOperation)
            when (operation) {
                // TODO Replace this workaround with cleaner solution
                // https://github.com/microsoft-notes/notes-android-sdk/issues/866
                is UpdateMediaAltText -> {
                    // Since we use the PATCH note API after conflict resolution, which does not handle media,
                    // we need to persist the media operation in the form of its invalid variant so it does not
                    // get discarded. This will transform back into a valid variant on the incoming note sync. We
                    // opt out of the approach to diff the old and new note after conflict resolution as if there
                    // are multiple operations in the queue, it would easily generate duplicates.
                    listOf(
                        replaceOperation,
                        QueueInstruction.AddOperation(
                            InvalidUpdateMediaAltText(
                                operation.note,
                                operation.localMediaId, operation.altText, operation.uiBaseRevision
                            )
                        )
                    )
                }
                else -> {
                    listOf(replaceOperation)
                }
            }
        } else {
            handleGenericError(error, operation)
        }
    }

    private fun handle410(
        error: HttpError410,
        operation: ApiRequestOperation.ValidApiRequestOperation
    ): List<QueueInstruction> {
        val instructions = when (error) {
            is HttpError410.InvalidSyncToken -> {
                val newOperation = when (operation) {
                    is NoteReferencesSync -> NoteReferencesSync(deltaToken = null)
                    is SamsungNotesSync -> SamsungNotesSync(deltaToken = null)
                    else -> Sync(deltaToken = null)
                }
                operation.telemetryBundle.wasRetryOperation(retry = true)
                    .newRequestOnFailure(SyncRequestType.requestType(newOperation))
                listOf(ReplaceOperation(operation, newOperation))
            }
            is HttpError410.InvalidateClientCache -> {
                val broadcastEvent = when (operation) {
                    is NoteReferencesSync -> InvalidateNoteReferencesClientCache()
                    is SamsungNotesSync -> InvalidateSamsungNotesClientCache()
                    else -> InvalidateClientCache()
                }
                operation.telemetryBundle.wasRetryOperation(retry = false)
                listOf(
                    PauseQueue(),
                    BroadcastSyncErrorEvent(SyncErrorType.SyncPaused),
                    ResetQueue(),
                    BroadcastEvent(broadcastEvent)
                )
            }
            is HttpError410.UnknownHttpError410 -> {
                if (operation is Sync || operation is NoteReferencesSync || operation is SamsungNotesSync) {
                    val newOperation = when (operation) {
                        is Sync -> Sync(deltaToken = null)
                        is NoteReferencesSync -> NoteReferencesSync(deltaToken = null)
                        else -> SamsungNotesSync(deltaToken = null)
                    }
                    operation.telemetryBundle.wasRetryOperation(retry = true)
                        .newRequestOnFailure(SyncRequestType.requestType(newOperation))
                    listOf(ReplaceOperation(operation, newOperation))
                } else {
                    listOf()
                }
            }
        }

        return instructions + listOf(
            SetDelay(SetDelay.DelayDelta.Exponential(2, until = MAX_DELAY)),
            DelayQueue()
        )
    }

    private fun handle426(operation: ApiRequestOperation): List<QueueInstruction> {
        operation.telemetryBundle.wasRetryOperation(retry = true)
        return listOf(
            PauseQueue(),
            BroadcastEvent(UpgradeRequired()),
            BroadcastSyncErrorEvent(SyncErrorType.SyncPaused)
        )
    }

    private fun handle429(retryTime: Long, operation: ApiRequestOperation): List<QueueInstruction> {
        operation.telemetryBundle.wasRetryOperation(retry = true)
        return listOf(
            SetDelay(SetDelay.DelayDelta.ResetTo(retryTime)),
            DelayQueue()
        )
    }

    private fun handle503(retryTime: Long, operation: ApiRequestOperation): List<QueueInstruction> {
        operation.telemetryBundle.wasRetryOperation(retry = true)
        return listOf(
            SetDelay(SetDelay.DelayDelta.ResetTo(retryTime)),
            DelayQueue()
        )
    }

    private fun onSuccessfulOperation(localId: String, remoteNote: RemoteNote): MapQueue {
        return with(remoteNote) {
            val newRemoteData = RemoteData(
                id = id,
                changeKey = changeKey,
                lastServerVersion = this,
                createdAt = createdAt,
                lastModifiedAt = lastModifiedAt
            )
            updateOutboundOperations(localId, newRemoteData, remoteNote)
        }
    }

    // This has to be handled separately to updateOutboundOperations as PATCH /media is the only changeKey-returning
    // endpoint that does not provide a RemoteNote response
    private fun onSuccessfulUpdateMediaAltText(localId: String, remoteData: RemoteData): MapQueue {
        val mapper = { operation: ApiRequestOperation ->
            if (operation is ApiRequestOperation.ValidApiRequestOperation) {
                when {
                    operation is UpdateNote && operation.note.id == localId -> {
                        operation.copy(note = operation.note.mergeRemoteData(remoteData))
                    }
                    operation is GetNoteForMerge && operation.note.id == localId -> {
                        operation.copy(note = operation.note.mergeRemoteData(remoteData))
                    }
                    operation is UploadMedia && operation.note.id == localId -> {
                        operation.copy(note = operation.note.mergeRemoteData(remoteData))
                    }
                    operation is UpdateMediaAltText && operation.note.id == localId -> {
                        operation.copy(note = operation.note.mergeRemoteData(remoteData))
                    }
                }
            }
            operation
        }
        return MapQueue(mapper)
    }

    @Suppress("LongMethod")
    private fun updateOutboundOperations(
        localId: String,
        remoteData: RemoteData,
        remoteNote: RemoteNote
    ): MapQueue {
        val mapper = { operation: ApiRequestOperation ->
            when {
                operation is UpdateNote && operation.note.id == localId -> {
                    operation.copy(note = operation.note.mergeRemoteData(remoteData))
                }
                operation is DeleteNote && operation.localId == localId -> {
                    operation.copy(remoteId = operation.remoteId)
                }
                operation is DeleteNoteReference && operation.localId == localId -> {
                    operation.copy(remoteId = operation.remoteId)
                }
                operation is DeleteSamsungNote && operation.localId == localId -> {
                    operation.copy(remoteId = operation.remoteId)
                }
                operation is GetNoteForMerge && operation.note.id == localId -> {
                    operation.copy(note = operation.note.mergeRemoteData(remoteData))
                }
                operation is UploadMedia && operation.note.id == localId -> {
                    operation.copy(note = operation.note.mergeRemoteData(remoteData))
                }
                operation is DeleteMedia && operation.localNoteId == localId -> {
                    operation.copy(remoteNoteId = remoteData.id)
                }
                operation is UpdateMediaAltText && operation.note.id == localId -> {
                    operation.copy(note = operation.note.mergeRemoteData(remoteData))
                }
                operation is InvalidUpdateNote && operation.note.id == localId -> {
                    UpdateNote(
                        note = operation.note.mergeRemoteData(remoteData),
                        uiBaseRevision = operation.uiBaseRevision,
                        uniqueId = operation.uniqueId
                    )
                }
                operation is InvalidDeleteNote && operation.localId == localId -> {
                    DeleteNote(
                        localId = localId,
                        remoteId = remoteData.id,
                        uniqueId = operation.uniqueId
                    )
                }
                operation is InvalidUploadMedia && operation.note.id == localId -> {
                    UploadMedia(
                        note = operation.note.mergeRemoteData(remoteData),
                        mediaLocalId = operation.mediaLocalId,
                        localUrl = operation.localUrl,
                        mimeType = operation.mimeType,
                        uniqueId = operation.uniqueId
                    )
                }
                operation is InvalidDeleteMedia && operation.noteLocalId == localId -> {
                    val mediaToDelete = remoteNote.media.find { media ->
                        operation.mediaLocalId == media.createdWithLocalId
                    }
                    mediaToDelete?.let { media ->
                        DeleteMedia(
                            localNoteId = operation.noteLocalId,
                            remoteNoteId = remoteData.id,
                            localMediaId = operation.mediaLocalId,
                            remoteMediaId = media.id,
                            uniqueId = operation.uniqueId
                        )
                    } ?: operation
                }
                operation is InvalidUpdateMediaAltText && operation.note.id == localId -> {
                    val mediaToUpdate = remoteNote.media.find { media ->
                        operation.mediaLocalId == media.createdWithLocalId
                    }
                    mediaToUpdate?.let { media ->
                        UpdateMediaAltText(
                            note = operation.note.mergeRemoteData(remoteData),
                            localMediaId = operation.mediaLocalId,
                            remoteMediaId = media.id,
                            altText = operation.altText,
                            uiBaseRevision = operation.uiBaseRevision,
                            uniqueId = operation.uniqueId
                        )
                    } ?: operation
                }
                else -> operation
            }
        }
        return MapQueue(mapper)
    }
}

private fun ignore404s(apiError: ApiError): Boolean =
    when (apiError) {
        is HttpError404 -> true
        else -> false
    }

private fun getNoteForMerge(error: ApiError, note: Note, uiBaseRevision: Long): ApiRequestOperation? =
    when (error) {
        is HttpError409 -> GetNoteForMerge(note = note, uiBaseRevision = uiBaseRevision)
        else -> null
    }

private fun Note.mergeRemoteData(new: RemoteData): Note {
    return if (remoteData != null) {
        copy(
            remoteData = remoteData.copy(
                changeKey = new.changeKey,
                lastModifiedAt = new.lastModifiedAt
            )
        )
    } else {
        copy(remoteData = new)
    }
}

private fun getRetryTime(headers: Map<String, String>): Long {
    val retryAfter = headers[HeaderKeys.RETRY_AFTER_HEADER]?.toIntOrNull()
    return retryAfter as? Long? ?: MAX_DELAY
}

private fun assertTrue(value: Boolean, message: String) = assert(!value) { message }
