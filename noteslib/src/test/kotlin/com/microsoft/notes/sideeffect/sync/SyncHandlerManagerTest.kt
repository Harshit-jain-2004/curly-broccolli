package com.microsoft.notes.sideeffect.sync

import android.content.Context
import com.microsoft.notes.models.AccountType
import com.microsoft.notes.noteslib.ExperimentFeatureFlags
import com.microsoft.notes.store.SideEffect
import com.microsoft.notes.store.StateHandler
import com.microsoft.notes.store.Store
import com.microsoft.notes.sync.NotesClientHost
import com.microsoft.notes.sync.RequestPriority
import com.microsoft.notes.utils.utils.RoutingPrefix
import com.microsoft.notes.utils.utils.UserInfo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.mock
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import org.hamcrest.CoreMatchers.`is` as iz

class SyncHandlerManagerTest {
    val sideEffects: CopyOnWriteArrayList<SideEffect> = CopyOnWriteArrayList()
    val stateHandlers: CopyOnWriteArrayList<StateHandler> = CopyOnWriteArrayList()

    @Mock
    val mockContext = mock<Context> {}

    @Mock
    val mockFile = mock<File> {}

    private lateinit var store: Store

    @Before
    fun setup() {
        store = object : Store(
            sideEffects = sideEffects, stateHandlers = stateHandlers,
            isDebugMode = true,
            createdByApp = "Test"
        ) {}
    }

    @Test
    fun createSdk_should_initialize_sdk_with_default_static_host() {
        val userID = "user1"
        val syncHandlerManager = createSyncHandlerManager()
        val sdk = syncHandlerManager.createSdkManager(
            UserInfo(
                userID = userID,
                email = "email1",
                accessToken = "123",
                accountType = AccountType.MSA,
                userInfoSuffix = "",
                routingPrefix = RoutingPrefix.Unprefixed,
                tenantID = ""
            )
        )
        assertThat<NotesClientHost>(sdk.notes.host, iz(NotesClientHost.StaticHost.default))
    }

    private fun createSyncHandlerManager(): SyncHandlerManager {
        val experimentFeatureFlags = ExperimentFeatureFlags(
            gsonParserEnabled = true, multiAccountEnabled = true,
            inkEnabled = true, realTimeEnabled = true
        )
        return SyncHandlerManager(
            context = mockContext,
            rootDirectory = mockFile,
            userAgent = "UserAgent",
            notesLogger = null,
            isDebugMode = true,
            store = store,
            experimentFeatureFlags = experimentFeatureFlags,
            requestPriority = { RequestPriority.background }
        )
    }
}
