package com.microsoft.notes.noteslib

import android.content.Context
import android.net.Uri
import com.microsoft.notes.ui.note.options.createContentUri
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.threading.ThreadExecutor
import com.microsoft.notes.utils.utils.Constants
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

class TestNotesLibraryConfiguration
private constructor(
    override val context: Context,
    override val isRestricted: Boolean,
    override val store: NotesLibraryStore,
    override val uiOptionFlags: UiOptionFlags,
    override val experimentFeatureFlags: ExperimentFeatureFlags,
    override val dbName: String = Constants.DATABASE_NAME,
    override val clientThread: ThreadExecutor? = null,
    override val notesLogger: NotesLogger?,
    override val userAgent: String,
    override val contentUri: (context: Context, imageUrl: String) -> Uri,
    override val theme: NotesThemeOverride,
    override val onMicrophoneButtonClick: ((context: Context) -> Unit)?,
    override val microphoneEnabled: (currentUserID: String) -> Boolean,
    override val withGalleryIconInsteadOfCamera: Boolean,
    override val autoDiscoverHost: String,
    override val startPollingDisabledOnNewToken: Boolean,
    override val showEmailInEditNote: Boolean,
    override val getSSLConfigurationFromIntune: ((currentUserID: String, url: String) -> Pair<SSLSocketFactory, X509TrustManager>?)?,
    override val feedAuthProvider: FeedAuthProvider? = null,
    override val imageFromCache: (mediaRemoteID: String?, clientURL: String?, noteReferenceLocalID: String?, mediaType: String?) -> Boolean
) : LibraryConfiguration {

    object Builder {
        fun build(
            context: Context,
            storeThread: ThreadExecutor? = null,
            appName: String = "",
            userAgent: String = ""
        ): TestNotesLibraryConfiguration {
            return TestNotesLibraryConfiguration(
                context = context,
                isRestricted = false,
                store = NotesLibraryStore(
                    storeThread = storeThread,
                    appName = appName,
                    isDebugMode = true
                ),
                uiOptionFlags = UiOptionFlags(),
                experimentFeatureFlags = ExperimentFeatureFlags(),
                clientThread = null,
                notesLogger = null,
                userAgent = userAgent,
                contentUri = ::createContentUri,
                theme = NotesThemeOverride.default,
                onMicrophoneButtonClick = null,
                microphoneEnabled = { false },
                withGalleryIconInsteadOfCamera = false,
                autoDiscoverHost = "https://outlook.office365.com",
                startPollingDisabledOnNewToken = false,
                getSSLConfigurationFromIntune = null,
                showEmailInEditNote = false,
                imageFromCache = { _: String?, _: String?, _: String?, _: String? -> false }
            )
        }
    }
}
