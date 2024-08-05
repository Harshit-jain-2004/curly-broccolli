package com.microsoft.notes.sync

import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncError.SyncErrorType
import com.microsoft.notes.utils.logging.EventMarkers

sealed class QueueInstruction {
    data class AddOperation(val operation: ApiRequestOperation) : QueueInstruction()
    data class RemoveOperation(val operation: ApiRequestOperation) : QueueInstruction()
    data class ReplaceOperation(val old: ApiRequestOperation, val new: ApiRequestOperation) : QueueInstruction()

    data class SetDelay(val delta: DelayDelta) : QueueInstruction() {
        sealed class DelayDelta {
            data class ResetTo(val amount: Long) : DelayDelta()
            data class Exponential(val factor: Long, val until: Long) : DelayDelta()
        }
    }

    class DelayQueue : QueueInstruction()
    class PauseQueue : QueueInstruction()
    class ResetQueue : QueueInstruction()
    data class BroadcastEvent(val event: ApiResponseEvent) : QueueInstruction()
    data class BroadcastSyncErrorEvent(val syncErrorType: SyncErrorType) : QueueInstruction()
    data class LogTelemetry(
        val bundle: SyncRequestTelemetry,
        val result: ApiResult<Any>,
        val eventMarker: EventMarkers
    ) : QueueInstruction()

    data class MapQueue(val mapper: (operation: ApiRequestOperation) -> ApiRequestOperation) : QueueInstruction()
}
