package com.microsoft.notes.sync.outboundqueuesquasher

import com.microsoft.notes.sync.ApiRequestOperation
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.NoteReferencesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.SamsungNotesSync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.Sync
import com.microsoft.notes.sync.ApiRequestOperationType
import com.microsoft.notes.sync.FeedFRESetup
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.sync.outboundqueue.MockOperationHandler
import com.microsoft.notes.sync.outboundqueue.createQueue
import org.junit.Assert
import org.junit.Test
import java.lang.IllegalArgumentException
import org.hamcrest.CoreMatchers.`is` as iz

class SquasherReadRequestTest : FeedFRESetup() {

    private val supportedSyncOperationTypes = listOf<ApiRequestOperationType>(
        ApiRequestOperationType.Sync,
        ApiRequestOperationType.NoteReferencesSync,
        ApiRequestOperationType.SamsungNotesSync
    )

    @Test
    fun `should add FullSyncRequest`() {
        fun shouldAddFullSyncRequestInternal(type: ApiRequestOperationType) {
            var called = false
            val outboundQueue = createQueue(
                MockOperationHandler(),
                eventHandler = { called = true },
                isDebugMode = true, context = mockContext
            )

            val fullSyncRequest1 = genFullSyncRequest(type = type, uniqueId = 1)
            outboundQueue.push(fullSyncRequest1, workImmediately = false)

            Assert.assertFalse(called)
            Assert.assertThat(outboundQueue.count, iz(1))
            Assert.assertThat(outboundQueue.toList()[0], iz(fullSyncRequest1))
        }

        supportedSyncOperationTypes.forEach { shouldAddFullSyncRequestInternal(it) }
    }

    @Test
    fun `should keep latest FullSyncRequest`() {
        fun shouldKeepLatestFullSyncRequestInternal(type: ApiRequestOperationType) {
            var called = false
            val outboundQueue = createQueue(
                MockOperationHandler(),
                eventHandler = { called = true },
                isDebugMode = true, context = mockContext
            )

            val fullSyncRequest1 = genFullSyncRequest(type = type, uniqueId = 1)
            val fullSyncRequest2 = genFullSyncRequest(type = type, uniqueId = 2)

            outboundQueue.push(fullSyncRequest1, workImmediately = false)
            outboundQueue.push(fullSyncRequest2, workImmediately = false)

            Assert.assertFalse(called)
            Assert.assertThat(outboundQueue.count, iz(1))
            Assert.assertThat(outboundQueue.toList()[0], iz(fullSyncRequest2))
        }

        supportedSyncOperationTypes.forEach { shouldKeepLatestFullSyncRequestInternal(it) }
    }

    @Test
    fun `should squash DeltaSyncRequest on FullSyncRequest`() {

        fun shouldSquashDeltaSyncRequestOnFullSyncRequestInternal(type: ApiRequestOperationType) {
            var called = false
            val outboundQueue = createQueue(
                MockOperationHandler(),
                eventHandler = { called = true },
                isDebugMode = true, context = mockContext
            )

            val deltaSyncRequest1 = genDeltaSyncRequest(type = type, uniqueId = 1)
            val fullSyncRequest1 = genFullSyncRequest(type = type, uniqueId = 1)

            outboundQueue.push(deltaSyncRequest1, workImmediately = false)
            outboundQueue.push(fullSyncRequest1, workImmediately = false)

            Assert.assertFalse(called)
            Assert.assertThat(outboundQueue.count, iz(1))
            Assert.assertThat(outboundQueue.toList()[0], iz(fullSyncRequest1))
        }

        supportedSyncOperationTypes.forEach { shouldSquashDeltaSyncRequestOnFullSyncRequestInternal(it) }
    }

    @Test
    fun `should keep FullSyncRequest on DeltaSyncRequest`() {
        fun shouldKeepFullSyncRequestOnDeltaSyncRequestInternal(type: ApiRequestOperationType) {
            var called = false
            val outboundQueue = createQueue(
                MockOperationHandler(),
                eventHandler = { called = true },
                isDebugMode = true, context = mockContext
            )

            val deltaSyncRequest1 = genDeltaSyncRequest(type = type, uniqueId = 1)
            val fullSyncRequest1 = genFullSyncRequest(type = type, uniqueId = 1)

            outboundQueue.push(fullSyncRequest1, workImmediately = false)
            outboundQueue.push(deltaSyncRequest1, workImmediately = false)

            Assert.assertFalse(called)
            Assert.assertThat(outboundQueue.count, iz(1))
            Assert.assertThat(outboundQueue.toList()[0], iz(fullSyncRequest1))
        }

        supportedSyncOperationTypes.forEach { shouldKeepFullSyncRequestOnDeltaSyncRequestInternal(it) }
    }

    @Test
    fun `should add DeltaSyncRequest`() {

        fun shouldAddDeltaSyncRequestInternal(type: ApiRequestOperationType) {
            var called = false
            val outboundQueue = createQueue(
                MockOperationHandler(),
                eventHandler = { called = true },
                isDebugMode = true, context = mockContext
            )

            val deltaSyncRequest1 = genDeltaSyncRequest(type = type, uniqueId = 1)
            outboundQueue.push(deltaSyncRequest1, workImmediately = false)

            Assert.assertFalse(called)
            Assert.assertThat(outboundQueue.count, iz(1))
            Assert.assertThat(outboundQueue.toList()[0], iz(deltaSyncRequest1))
        }

        supportedSyncOperationTypes.forEach { shouldAddDeltaSyncRequestInternal(it) }
    }

    @Test
    fun `should keep latest DeltaSyncRequest`() {

        fun shouldKeepLatestDeltaSyncRequestInternal(type: ApiRequestOperationType) {
            var called = false
            val outboundQueue = createQueue(
                MockOperationHandler(),
                eventHandler = { called = true },
                isDebugMode = true, context = mockContext
            )

            val deltaSyncRequest1 = genDeltaSyncRequest(type = type, uniqueId = 1)
            val deltaSyncRequest2 = genDeltaSyncRequest(type = type, uniqueId = 2)

            outboundQueue.push(deltaSyncRequest1, workImmediately = false)
            outboundQueue.push(deltaSyncRequest2, workImmediately = false)

            Assert.assertFalse(called)
            Assert.assertThat(outboundQueue.count, iz(1))
            Assert.assertThat(outboundQueue.toList()[0], iz(deltaSyncRequest2))
        }

        supportedSyncOperationTypes.forEach { shouldKeepLatestDeltaSyncRequestInternal(it) }
    }

    private fun genFullSyncRequest(type: ApiRequestOperationType, uniqueId: Int): ApiRequestOperation =
        genSyncRequest(type, uniqueId, true)

    private fun genDeltaSyncRequest(type: ApiRequestOperationType, uniqueId: Int): ApiRequestOperation =
        genSyncRequest(type, uniqueId, false)

    private fun genSyncRequest(type: ApiRequestOperationType, uniqueId: Int, isFullSync: Boolean): ApiRequestOperation {
        val deltaToken = if (isFullSync) null else Token.Delta(uniqueId.toString())
        return when (type) {
            ApiRequestOperationType.Sync -> Sync(deltaToken, uniqueId.toString())
            ApiRequestOperationType.NoteReferencesSync -> NoteReferencesSync(deltaToken, uniqueId.toString())
            ApiRequestOperationType.SamsungNotesSync -> SamsungNotesSync(deltaToken, uniqueId.toString())
            else -> throw IllegalArgumentException("Unsupported Operation $type")
        }
    }

    @Test
    fun `should not squash operation of diff type`() {
        var called = false
        val outboundQueue = createQueue(
            MockOperationHandler(),
            eventHandler = { called = true },
            isDebugMode = true, context = mockContext
        )

        val fullSyncRequest1 = genFullSyncRequest(type = ApiRequestOperationType.Sync, uniqueId = 1)
        val fullSyncRequest2 = genFullSyncRequest(type = ApiRequestOperationType.NoteReferencesSync, uniqueId = 2)
        val fullSyncRequest3 = genFullSyncRequest(type = ApiRequestOperationType.SamsungNotesSync, uniqueId = 3)

        outboundQueue.push(fullSyncRequest1, workImmediately = false)
        outboundQueue.push(fullSyncRequest2, workImmediately = false)
        outboundQueue.push(fullSyncRequest3, workImmediately = false)

        Assert.assertFalse(called)
        Assert.assertThat(outboundQueue.count, iz(3))
    }
}
