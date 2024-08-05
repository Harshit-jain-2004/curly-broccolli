package com.microsoft.notes.noteslib

import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.AuthenticatedSyncRequestAction
import java.util.Timer
import kotlin.concurrent.timer

const val FETCH_PERIOD = 5000L
const val REALTIME_FETCH_PERIOD = 30000L
const val NOTEREFERENCES_FETCH_PERIOD = 5000L
const val SAMSUNG_NOTES_FETCH_PERIOD = 5000L
const val MEETING_NOTES_FETCH_PERIOD = 300000L

class GlobalPoller(
    private val isRealtimeEnabled: Boolean,
    private val isNoteReferencesSyncEnabled: Boolean,
    private val isSamsungNotesSyncEnabled: Boolean = false,
    private val isMeetingNotesSyncEnabled: Boolean = false,
    private val isFeedRealtimeSyncEnabled: Boolean,
    val userID: String,
    val dispatch: (Action) -> Unit
) {
    val timers = mutableListOf<Timer>()

    fun startPolling() {
        timers.add(fetchAllNotesTimer())
        timers.add(flushToDiskTimer())
        timers.add(cleanUpRecordedStatesTimer())
        if (isNoteReferencesSyncEnabled) {
            timers.add(fetchAllNoteReferencesTimer())
        }
        if (isSamsungNotesSyncEnabled) {
            timers.add(fetchAllSamsungNotesTimer())
        }
        if (isMeetingNotesSyncEnabled) {
            timers.add(fetchAllMeetingNotesTimer())
        }
    }

    fun stopPolling() {
        timers.forEach {
            it.cancel()
            it.purge()
        }
        timers.clear()
    }

    fun isPollingRunning(): Boolean =
        timers.isNotEmpty()

    private fun fetchAllNotesTimer(): Timer {
        val period = when (isRealtimeEnabled) {
            true -> REALTIME_FETCH_PERIOD
            else -> FETCH_PERIOD
        }
        return timer(name = "fetchAllNotes", period = period, action = {
            dispatch.invoke(AuthenticatedSyncRequestAction.RemoteChangedDetected(userID))
        })
    }

    private fun fetchAllNoteReferencesTimer(): Timer {
        val period = when (isRealtimeEnabled && isFeedRealtimeSyncEnabled) {
            true -> REALTIME_FETCH_PERIOD
            else -> NOTEREFERENCES_FETCH_PERIOD
        }
        return timer(name = "fetchAllNoteReferences", period = period, action = {
            dispatch.invoke(AuthenticatedSyncRequestAction.RemoteNoteReferencesChangedDetected(userID))
        })
    }

    private fun fetchAllSamsungNotesTimer(): Timer {
        val period = when (isRealtimeEnabled && isFeedRealtimeSyncEnabled) {
            true -> REALTIME_FETCH_PERIOD
            else -> SAMSUNG_NOTES_FETCH_PERIOD
        }
        return timer(name = "fetchAllSamsungNotesTimer", period = period, action = {
            dispatch.invoke(AuthenticatedSyncRequestAction.SamsungNotesChangedDetected(userID))
        })
    }

    private fun fetchAllMeetingNotesTimer(): Timer {
        return timer(name = "fetchAllMeetingNotesTimer", period = MEETING_NOTES_FETCH_PERIOD, action = {
            dispatch.invoke(AuthenticatedSyncRequestAction.MeetingNotesChangedDetected(userID))
        })
    }

    private fun flushToDiskTimer(): Timer =
        timer(name = "flushToDisk", period = 60000, action = {})

    private fun cleanUpRecordedStatesTimer(): Timer =
        timer(name = "cleanUpRecordedStates", period = 120000, action = {})
}
