package com.microsoft.notes.noteslib

import android.content.Context
import com.microsoft.notes.sideeffect.local.LocalSideEffect
import com.microsoft.notes.sideeffect.local.LocalSideEffectExecutorService
import com.microsoft.notes.sideeffect.local.TimeReminderManager
import com.microsoft.notes.sideeffect.persistence.NotesDatabaseManager
import com.microsoft.notes.sideeffect.persistence.PersistenceSideEffect
import com.microsoft.notes.sideeffect.persistence.PersistenceThreadService
import com.microsoft.notes.sideeffect.preferences.PreferencesSideEffect
import com.microsoft.notes.sideeffect.preferences.PreferencesThreadService
import com.microsoft.notes.sideeffect.sync.SyncHandlerManager
import com.microsoft.notes.sideeffect.sync.SyncSideEffect
import com.microsoft.notes.sideeffect.sync.SyncSideEffectThreadService
import com.microsoft.notes.sideeffect.ui.UiSideEffect
import com.microsoft.notes.sideeffect.ui.UiThreadService
import com.microsoft.notes.store.Store
import com.microsoft.notes.store.StoreThreadService
import com.microsoft.notes.sync.AutoDiscoverCallManager
import com.microsoft.notes.sync.AutoDiscoverSharedPreferences
import com.microsoft.notes.sync.NetworkedAutoDiscover
import com.microsoft.notes.sync.RequestPriority
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.threading.ThreadExecutor
import com.microsoft.notes.utils.utils.Constants.EMPTY_SESSION_ID
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

open class NotesLibraryStore(
    appName: String,
    notesLogger: NotesLogger? = null,
    storeThread: ThreadExecutor? = StoreThreadService(),
    isDebugMode: Boolean
) : Store(
    sideEffects = CopyOnWriteArrayList(),
    stateHandlers = CopyOnWriteArrayList(),
    storeThread = storeThread,
    notesLogger = notesLogger,
    createdByApp = appName,
    isDebugMode = isDebugMode
) {

    fun enablePersistence(context: Context, dbName: String, multiAccountEnabled: Boolean) {
        sideEffects.add(
            PersistenceSideEffect(
                store = this,
                persistenceThread = PersistenceThreadService(),
                notesDatabaseManager = NotesDatabaseManager(context, dbName, multiAccountEnabled, notesLogger),
                notesLogger = notesLogger
            )
        )
    }

    fun enableLocalSideEffect(context: Context, dbName: String, multiAccountEnabled: Boolean) {
        sideEffects.add(
            LocalSideEffect(
                systemThread = LocalSideEffectExecutorService(),
                reminderManager = TimeReminderManager.getInstance(context.applicationContext),
                notesLogger = notesLogger
            )
        )
    }

    @Suppress("LongParameterList")
    fun enableSync(
        context: Context,
        rootDirectory: File,
        userAgent: String,
        experimentFeatureFlags: ExperimentFeatureFlags,
        autoDiscoverHost: String,
        requestPriority: () -> RequestPriority
    ) {
        // Skip if sync is already enabled
        if (sideEffects.any { it is SyncSideEffect }) {
            return
        }

        val autoDiscoverCache = AutoDiscoverSharedPreferences(context)
        val syncHandlerManager = SyncHandlerManager(
            context,
            rootDirectory,
            userAgent,
            notesLogger,
            isDebugMode,
            this,
            experimentFeatureFlags,
            requestPriority
        )

        sideEffects.add(
            SyncSideEffect(
                context = context,
                store = this,
                syncThread = SyncSideEffectThreadService(),
                notesLogger = notesLogger,
                experimentFeatureFlags = experimentFeatureFlags,
                syncHandlerManager = syncHandlerManager,
                autoDiscoverCallManager = AutoDiscoverCallManager(
                    context,
                    NetworkedAutoDiscover(autoDiscoverHost, userAgent, notesLogger),
                    autoDiscoverCache,
                    notesLogger
                ),
                autoDiscoverCache = autoDiscoverCache
            )
        )
    }

    fun enableUiSideEffect(experimentFeatureFlags: ExperimentFeatureFlags) {
        sideEffects.add(
            UiSideEffect(
                store = this,
                uiThread = UiThreadService(),
                notesLogger = notesLogger,
                combinedListForMultiAccountEnabled = experimentFeatureFlags.combinedListForMultiAccountEnabled,
                samsungNotesSyncEnabled = experimentFeatureFlags.samsungNotesSyncEnabled
            )
        )
    }

    fun enablePreferencesSideEffect(context: Context) {
        sideEffects.add(
            PreferencesSideEffect(
                context = context,
                store = this,
                preferencesThread = PreferencesThreadService(),
                notesLogger = notesLogger
            )
        )
    }

    fun getNotesSessionId(userID: String): String {
        val syncSideEffect = sideEffects.firstOrNull { it is SyncSideEffect } as? SyncSideEffect

        return syncSideEffect?.syncHandlerManager
            ?.getSyncHandlerForUser(userID)
            ?.outboundQueue
            ?.correlationVector?.correlationVectorBase
            ?: EMPTY_SESSION_ID
    }

    fun getUserIDForNoteLocalID(noteLocalID: String): String = state.userIDForLocalNoteID(noteLocalID)
    fun getUserIDForNoteReferenceLocalID(noteReferenceLocalID: String): String = state.userIDForLocalNoteReferenceID(noteReferenceLocalID)
}
