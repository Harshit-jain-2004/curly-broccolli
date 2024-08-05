package com.microsoft.notes.sideeffect.sync

import android.content.Context
import com.microsoft.notes.noteslib.ExperimentFeatureFlags
import com.microsoft.notes.store.Store
import com.microsoft.notes.sync.MeetingNotesClientHost
import com.microsoft.notes.sync.NetworkedSdk
import com.microsoft.notes.sync.NotesClientHost
import com.microsoft.notes.sync.RequestPriority
import com.microsoft.notes.sync.SdkManager
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.utils.Constants
import com.microsoft.notes.utils.utils.UserInfo
import java.io.File

class SyncHandlerManager(
    private val context: Context,
    private val rootDirectory: File,
    private val userAgent: String,
    private val notesLogger: NotesLogger?,
    val isDebugMode: Boolean,
    val store: Store,
    private val experimentFeatureFlags: ExperimentFeatureFlags,
    private val requestPriority: () -> RequestPriority
) {

    private val userSyncHandlerMap = mutableMapOf<String, UserSyncHandler>()

    fun getSyncHandlerForUser(userID: String): UserSyncHandler? =
        if (experimentFeatureFlags.multiAccountEnabled) {
            userSyncHandlerMap[userID]
        } else {
            userSyncHandlerMap[Constants.EMPTY_USER_ID]
        }

    val createSdkManager: (UserInfo) -> SdkManager = { userInfo ->
        createSdkManager(userInfo)
    }

    fun createSdkManager(userInfo: UserInfo): SdkManager {
        val sdkManager = SdkManager()
        sdkManager.notes = NetworkedSdk.NotesSdk(
            host = NotesClientHost.StaticHost.default,
            token = userInfo.accessToken,
            userInfo = userInfo,
            userAgent = userAgent,
            notesLogger = notesLogger,
            gsonParserEnabled = experimentFeatureFlags.gsonParserEnabled,
            inkEnabled = experimentFeatureFlags.inkEnabled,
            requestPriority = requestPriority,
            feedRealTimeSyncEnabled = experimentFeatureFlags.feedRealTimeEnabled
        )

        if (experimentFeatureFlags.meetingNotesSyncEnabled) {
            sdkManager.meetingNotes = NetworkedSdk.MeetingNotesSdk(
                workingSetAPIHost = MeetingNotesClientHost.StaticHost.workingSetAPIHost,
                eventsAPIHost = MeetingNotesClientHost.StaticHost.eventsAPIHost,
                collabAPIHost = MeetingNotesClientHost.StaticHost.collabAPIHost,
                token = userInfo.accessToken,
                userInfo = userInfo,
                userAgent = userAgent,
                notesLogger = notesLogger,
                gsonParserEnabled = experimentFeatureFlags.gsonParserEnabled,
                requestPriority = requestPriority
            )
        }
        return sdkManager
    }

    private fun createNewUserSyncHandler(userInfo: UserInfo): UserSyncHandler =
        UserSyncHandlerBuilder(
            context,
            rootDirectory,
            store,
            notesLogger,
            isDebugMode,
            userInfo,
            realtimeEnabled = experimentFeatureFlags.realTimeEnabled,
            createSdkManager = createSdkManager,
            apiHostInitialized = false
        )
            .build()

    fun handleNewUser(userInfo: UserInfo) {
        if (!experimentFeatureFlags.multiAccountEnabled) {
            handleNewUserWithMultiAccountDisabled(userInfo)
        } else {
            handleNewUserWithMultiAccountEnabled(userInfo)
        }
    }

    fun handleLogout(userID: String) {
        getSyncHandlerForUser(userID)?.logout()
    }

    private fun handleNewUserWithMultiAccountDisabled(userInfo: UserInfo) {
        if (!userSyncHandlerMap.containsKey(Constants.EMPTY_USER_ID)) {
            userSyncHandlerMap[Constants.EMPTY_USER_ID] = createNewUserSyncHandler(userInfo)
        }
    }

    private fun handleNewUserWithMultiAccountEnabled(userInfo: UserInfo) {
        if (userSyncHandlerMap.containsKey(userInfo.userID)) {
            return
        }

        val emptyUserIdUserSyncHandler: UserSyncHandler? = getSyncHandlerForUser(Constants.EMPTY_USER_ID)
        if (emptyUserIdUserSyncHandler != null) {
            userSyncHandlerMap.remove(Constants.EMPTY_USER_ID)
        }

        userSyncHandlerMap[userInfo.userID] = createNewUserSyncHandler(userInfo)
    }
}
