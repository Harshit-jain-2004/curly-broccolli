package com.microsoft.notes.sync.outboundqueue

import android.content.Context
import com.microsoft.notes.noteslib.ExperimentFeatureFlags
import com.microsoft.notes.sync.ApiError
import com.microsoft.notes.sync.ApiPromise
import com.microsoft.notes.sync.ApiRequestOperation
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.CreateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteNoteReference
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DownloadMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.GetNoteForMerge
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.Sync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateMediaAltText
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UploadMedia
import com.microsoft.notes.sync.ApiRequestOperationHandler
import com.microsoft.notes.sync.ApiResponseEvent
import com.microsoft.notes.sync.ApiResponseEvent.FullSync
import com.microsoft.notes.sync.ApiResponseEvent.Gone
import com.microsoft.notes.sync.ApiResponseEvent.MediaAltTextUpdated
import com.microsoft.notes.sync.ApiResponseEvent.MediaDeleted
import com.microsoft.notes.sync.ApiResponseEvent.MediaDownloaded
import com.microsoft.notes.sync.ApiResponseEvent.MediaUploaded
import com.microsoft.notes.sync.ApiResponseEvent.NoteCreated
import com.microsoft.notes.sync.ApiResponseEvent.NoteDeleted
import com.microsoft.notes.sync.ApiResponseEvent.NoteFetchedForMerge
import com.microsoft.notes.sync.ApiResponseEvent.NoteReferenceDeleted
import com.microsoft.notes.sync.ApiResponseEvent.NoteUpdated
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncError
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncFailed
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncStarted
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncSucceeded
import com.microsoft.notes.sync.ApiResponseEvent.SamsungNoteDeleted
import com.microsoft.notes.sync.ApiResponseEventHandler
import com.microsoft.notes.sync.ApiResult
import com.microsoft.notes.sync.CorrelationVector
import com.microsoft.notes.sync.IPersist
import com.microsoft.notes.sync.OutboundQueue
import com.microsoft.notes.sync.PriorityQueue
import com.microsoft.notes.sync.models.MediaAltTextUpdate
import com.microsoft.notes.sync.models.RemoteNote
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.sync.models.localOnly.RemoteData
import com.microsoft.notes.utils.logging.TestConstants
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.mockito.kotlin.mock
import org.hamcrest.CoreMatchers.`is` as iz

class MockOperationHandler(
    val sync: ((ApiRequestOperation) -> ApiResult<ApiResponseEvent>)? = null,
    val create: ((ApiRequestOperation) -> ApiResult<NoteCreated>)? = null,
    val update: ((ApiRequestOperation) -> ApiResult<NoteUpdated>)? = null,
    val getNoteForMerge: ((ApiRequestOperation) -> ApiResult<NoteFetchedForMerge>)? = null,
    val delete: ((ApiRequestOperation) -> ApiResult<NoteDeleted>)? = null,
    val deleteNoteReference: ((ApiRequestOperation) -> ApiResult<NoteReferenceDeleted>)? = null,
    val deleteSamsungNote: ((ApiRequestOperation) -> ApiResult<SamsungNoteDeleted>)? = null,
    val uploadMedia: ((ApiRequestOperation) -> ApiResult<MediaUploaded>)? = null,
    val downloadMedia: ((ApiRequestOperation) -> ApiResult<MediaDownloaded>)? = null,
    val deleteMedia: ((ApiRequestOperation) -> ApiResult<MediaDeleted>)? = null,
    val updateMediaAltText: ((ApiRequestOperation) -> ApiResult<MediaAltTextUpdated>)? = null,
    val noteReferenceSync: ((ApiRequestOperation) -> ApiResult<ApiResponseEvent>)? = null,
    val meetingNoteSync: ((ApiRequestOperation) -> ApiResult<ApiResponseEvent>)? = null,
    val samsungNoteSync: ((ApiRequestOperation) -> ApiResult<ApiResponseEvent>)? = null,
    override val remoteDataMap: Map<String, RemoteData> = emptyMap()
) :
    ApiRequestOperationHandler {

    override fun setIsFRESyncing(value: Boolean) {
        return
    }

    override fun handleSync(operation: Sync): ApiPromise<ApiResponseEvent> {
        return call(sync, operation)
    }

    override fun handleNoteReferenceSync(operation: ApiRequestOperation.ValidApiRequestOperation.NoteReferencesSync): ApiPromise<ApiResponseEvent> {
        return call(noteReferenceSync, operation)
    }

    override fun handleMeetingNoteSync(operation: ApiRequestOperation.ValidApiRequestOperation.MeetingNotesSync): ApiPromise<ApiResponseEvent> {
        return call(meetingNoteSync, operation)
    }

    override fun handleSamsungNoteSync(operation: ApiRequestOperation.ValidApiRequestOperation.SamsungNotesSync): ApiPromise<ApiResponseEvent> {
        return call(samsungNoteSync, operation)
    }

    override fun handleCreate(operation: CreateNote): ApiPromise<NoteCreated> {
        return call(create, operation)
    }

    override fun handleUpdate(operation: UpdateNote): ApiPromise<NoteUpdated> {
        return call(update, operation)
    }

    override fun handleGetForMerge(operation: GetNoteForMerge): ApiPromise<NoteFetchedForMerge> {
        return call(getNoteForMerge, operation)
    }

    override fun handleDelete(operation: DeleteNote): ApiPromise<NoteDeleted> {
        return call(delete, operation)
    }

    override fun handleNoteReferenceDelete(operation: DeleteNoteReference): ApiPromise<NoteReferenceDeleted> {
        return call(deleteNoteReference, operation)
    }

    override fun handleSamsungNoteDelete(operation: ApiRequestOperation.ValidApiRequestOperation.DeleteSamsungNote): ApiPromise<SamsungNoteDeleted> {
        return call(deleteSamsungNote, operation)
    }

    override fun handleUploadMedia(operation: UploadMedia): ApiPromise<MediaUploaded> {
        return call(uploadMedia, operation)
    }

    override fun handleDownloadMedia(operation: DownloadMedia): ApiPromise<MediaDownloaded> {
        return call(downloadMedia, operation)
    }

    override fun handleDeleteMedia(operation: DeleteMedia): ApiPromise<MediaDeleted> {
        return call(deleteMedia, operation)
    }

    override fun handleUpdateMediaAltText(operation: UpdateMediaAltText): ApiPromise<MediaAltTextUpdated> {
        return call(updateMediaAltText, operation)
    }

    private fun <U, T : ApiResponseEvent> call(handler: ((U) -> ApiResult<T>)?, operation: U): ApiPromise<T> {
        return handler?.invoke(operation)?.let { ApiPromise.of(it) } ?: fail("Handler called that was not mocked")
    }
}

fun createQueue(
    operationHandler: ApiRequestOperationHandler,
    eventHandler: (ApiResponseEvent) -> Unit,
    isDebugMode: Boolean,
    context: Context = mock<Context> {}
): OutboundQueue {
    val backingQueue = PriorityQueue(
        queue = emptyList(),
        persistenceOnDiskEnabled = false,
        createBackupFile = { _ ->
            object : IPersist<String> {
                override fun load(): String? {
                    return null
                }

                override fun persist(objectToPersist: String) {}
            }
        }
    )
    return OutboundQueue(
        backingQueue = backingQueue,
        apiRequestOperationHandler = operationHandler,
        apiResponseEventHandler = object : ApiResponseEventHandler {
            override fun handleEvent(apiResponseEvent: ApiResponseEvent) {
                eventHandler(apiResponseEvent)
            }
        },
        sleep = { ApiPromise.of(Unit) },
        isDebugMode = isDebugMode,
        correlationVector = CorrelationVector { _ -> "1234567890123456789012==" }, context = context, userID = TestConstants.TEST_USER_ID, experimentFeatureFlags = ExperimentFeatureFlags()
    )
}

fun createSyncEvent(
    apiRequestOperation: Sync
): (ApiRequestOperation) -> ApiResult<ApiResponseEvent> {
    return { actualOutboundOperation ->
        assertThat<ApiRequestOperation>(apiRequestOperation, iz(actualOutboundOperation))
        ApiResult.Success(FullSync(Token.Delta("someDeltaToken"), emptyList()))
    }
}

fun createCreateEvent(
    apiRequestOperation: CreateNote,
    remoteNote: RemoteNote
): (ApiRequestOperation) -> ApiResult<NoteCreated> {
    return { actualOutboundOperation ->
        assertThat<ApiRequestOperation>(apiRequestOperation, iz(actualOutboundOperation))
        ApiResult.Success(NoteCreated(apiRequestOperation.note.id, remoteNote))
    }
}

fun createUpdateEvent(
    apiRequestOperation: UpdateNote,
    remoteNote: RemoteNote
): (ApiRequestOperation) -> ApiResult<NoteUpdated> {
    return { actualOutboundOperation ->
        assertThat<ApiRequestOperation>(apiRequestOperation, iz(actualOutboundOperation))
        ApiResult.Success(
            NoteUpdated(
                apiRequestOperation.note.id, remoteNote,
                apiRequestOperation.uiBaseRevision
            )
        )
    }
}

fun createMergeEvent(
    apiRequestOperation: GetNoteForMerge,
    remoteNote: RemoteNote
): (ApiRequestOperation) -> ApiResult<NoteFetchedForMerge> {
    return { actualOutboundOperation ->
        assertThat<ApiRequestOperation>(apiRequestOperation, iz(actualOutboundOperation))
        ApiResult.Success(
            NoteFetchedForMerge(
                apiRequestOperation.note.id, remoteNote,
                apiRequestOperation.uiBaseRevision
            )
        )
    }
}

fun createDeleteEvent(
    apiRequestOperation: DeleteNote
): (ApiRequestOperation) -> ApiResult<NoteDeleted> {
    return { actualOutboundOperation ->
        assertThat<ApiRequestOperation>(apiRequestOperation, iz(actualOutboundOperation))
        ApiResult.Success(NoteDeleted(apiRequestOperation.localId, apiRequestOperation.remoteId))
    }
}

fun createDeleteMediaEvent(
    apiRequestOperation: DeleteMedia
): (ApiRequestOperation) -> ApiResult<MediaDeleted> {
    return { actualOutboundOperation ->
        assertThat<ApiRequestOperation>(apiRequestOperation, iz(actualOutboundOperation))
        ApiResult.Success(
            MediaDeleted(
                apiRequestOperation.localNoteId, apiRequestOperation.localMediaId,
                apiRequestOperation.remoteMediaId
            )
        )
    }
}

fun createUpdateMediaAltTextEvent(
    apiRequestOperation: UpdateMediaAltText
): (ApiRequestOperation) -> ApiResult<MediaAltTextUpdated> {
    return { actualOutboundOperation ->
        assertThat<ApiRequestOperation>(apiRequestOperation, iz(actualOutboundOperation))
        ApiResult.Success(
            MediaAltTextUpdated(
                apiRequestOperation.note.id,
                MediaAltTextUpdate(
                    changeKey = "",
                    id = apiRequestOperation.remoteMediaId,
                    createdWithLocalId = apiRequestOperation.localMediaId,
                    mimeType = "",
                    lastModified = "",
                    altText = apiRequestOperation.altText,
                    imageDimensions = null
                )
            )
        )
    }
}

fun <T> createFailedEvent(
    apiRequestOperation: ApiRequestOperation,
    error: ApiError
): (ApiRequestOperation) -> ApiResult<T> {
    return { actualOutboundOperation ->
        assertThat<ApiRequestOperation>(apiRequestOperation, iz(actualOutboundOperation))
        ApiResult.Failure(error)
    }
}

fun testSuccess(
    apiRequestOperation: ApiRequestOperation,
    operationHandler: MockOperationHandler,
    event: ApiResponseEvent,
    context: Context = mock<Context> {}
) {
    testEventSequenceSuccess(apiRequestOperation, operationHandler, listOf(event), context)
}

fun testEventSequenceSuccess(
    apiRequestOperation: ApiRequestOperation,
    operationHandler: MockOperationHandler,
    events: List<ApiResponseEvent>,
    context: Context = mock<Context> {}
) {
    var eventsToBeCalled = events.size
    val eventIterator = events.iterator()
    val outboundQueue = createQueue(operationHandler, eventHandler = { emittedEvent ->
        if (eventIterator.hasNext()) {
            val current = eventIterator.next()
            assertThat(emittedEvent, instanceOf(current::class.java))
            eventsToBeCalled--
        }
    }, isDebugMode = true, context = context)

    outboundQueue.push(apiRequestOperation, workImmediately = false)
    outboundQueue.work().get()

    Assert.assertThat(eventsToBeCalled, iz(0))
    assertThat(outboundQueue.count, iz(0))
}

fun testEventSequenceFailure(
    apiRequestOperation: ApiRequestOperation,
    operationHandler: MockOperationHandler,
    events: List<ApiResponseEvent>,
    context: Context = mock<Context> {}
) {
    testEventSequenceFailure(listOf(apiRequestOperation), operationHandler, events, context = context)
}

fun testEventSequenceFailure(
    apiRequestOperations: List<ApiRequestOperation>,
    operationHandler: MockOperationHandler,
    events: List<ApiResponseEvent>,
    queueShouldBeCleared: Boolean = false,
    context: Context = mock<Context> {}
) {
    var eventsToBeCalled = events.size
    val eventIterator = events.iterator()
    val outboundQueue = createQueue(
        operationHandler,
        eventHandler = { emittedEvent ->
            if (eventIterator.hasNext()) {
                val current = eventIterator.next()
                Assert.assertThat(emittedEvent, instanceOf(current::class.java))
                eventsToBeCalled--

                if (current is RemoteNotesSyncError) {
                    Assert.assertThat((emittedEvent as RemoteNotesSyncError).error, iz(current.error))
                }
            }
        }, isDebugMode = true, context = context
    )

    apiRequestOperations.forEach {
        outboundQueue.push(it, workImmediately = false)
    }
    outboundQueue.work().get()

    Assert.assertThat(eventsToBeCalled, iz(0))
    if (queueShouldBeCleared) {
        Assert.assertThat(outboundQueue.count, iz(0))
    } else {
        Assert.assertThat(outboundQueue.count, iz(apiRequestOperations.size))
        Assert.assertThat(outboundQueue.toList(), iz(apiRequestOperations))
    }
}

fun testFailure(apiRequestOperation: ApiRequestOperation, operationHandler: MockOperationHandler, context: Context = mock<Context> {}) {
    var called = false
    val outboundQueue = createQueue(operationHandler, eventHandler = {
        called = when (it) {
            is RemoteNotesSyncStarted,
            is RemoteNotesSyncFailed -> called
            is RemoteNotesSyncSucceeded -> {
                fail("RemoteNotesSyncSucceeded called for testFailure")
            }
            else -> true
        }
    }, isDebugMode = false, context = context)

    outboundQueue.push(apiRequestOperation, workImmediately = false)
    outboundQueue.work().get()

    assertFalse(called)
    assertThat(outboundQueue.count, iz(1))
    assertThat(outboundQueue.toList(), iz(listOf(apiRequestOperation)))
}

fun testIgnored(
    apiRequestOperation: ApiRequestOperation,
    operationHandler: MockOperationHandler,
    isDebugMode: Boolean = true,
    context: Context = mock<Context> {}
) {
    var called = false
    val outboundQueue = createQueue(operationHandler = operationHandler, eventHandler = {
        called = when (it) {
            is RemoteNotesSyncStarted,
            is RemoteNotesSyncFailed -> called
            is RemoteNotesSyncSucceeded -> {
                fail("RemoteNotesSyncSucceeded called for testIgnored")
            }
            is Gone -> called
            else -> true
        }
    }, isDebugMode = isDebugMode, context = context)

    outboundQueue.push(apiRequestOperation, workImmediately = false)
    outboundQueue.work().get()

    assertFalse(called)
    assertThat(outboundQueue.count, iz(0))
}

fun fail(msg: String): Nothing {
    Assert.fail(msg)
    fail("Unreachable")
}
