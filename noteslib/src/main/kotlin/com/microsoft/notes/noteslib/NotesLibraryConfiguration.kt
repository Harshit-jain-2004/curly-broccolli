package com.microsoft.notes.noteslib

import android.content.Context
import android.net.Uri
import com.microsoft.notes.sync.NetworkedAutoDiscover
import com.microsoft.notes.ui.note.options.createContentUri
import com.microsoft.notes.utils.logging.Logger
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.TelemetryLogger
import com.microsoft.notes.utils.threading.ThreadExecutor
import com.microsoft.notes.utils.utils.Constants
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

interface LibraryConfiguration {
    val context: Context
    val isRestricted: Boolean
    val store: NotesLibraryStore
    val dbName: String
    val uiOptionFlags: UiOptionFlags
    val experimentFeatureFlags: ExperimentFeatureFlags
    val clientThread: ThreadExecutor?
    val notesLogger: NotesLogger?
    val userAgent: String
    val contentUri: (context: Context, imageUrl: String) -> Uri
    val theme: NotesThemeOverride
    val imageFromCache: (mediaRemoteID: String?, clientURL: String?, noteReferenceLocalID: String?, mediaType: String?) -> Boolean
    val onMicrophoneButtonClick: ((context: Context) -> Unit)?
    val microphoneEnabled: (currentUserID: String) -> Boolean
    val withGalleryIconInsteadOfCamera: Boolean
    val autoDiscoverHost: String
    val startPollingDisabledOnNewToken: Boolean
    val showEmailInEditNote: Boolean
    val getSSLConfigurationFromIntune: ((currentUserID: String, url: String) -> Pair<SSLSocketFactory, X509TrustManager>?)?
    val feedAuthProvider: FeedAuthProvider?
}

data class ExperimentFeatureFlags(
    /**
     * Enables realtime shoulder tap sync
     */
    val realTimeEnabled: Boolean = false,
    /**
     * Enables Gson parsing instead of or original Json manual parsing.
     */
    val gsonParserEnabled: Boolean = false,
    /**
     * Enables multi accounts support
     */
    val multiAccountEnabled: Boolean = false,
    /**
     * Enables previewing and editing ink notes
     */
    val inkEnabled: Boolean = false,
    /**
     * Enables hybrid feed
     */
    val isHybridFeedEnabled: Boolean = false,
    /**
     * Enables Samsung Notes Sync
     */
    val samsungNotesSyncEnabled: Boolean = false,
    /**
     * Enables NoteReferences Sync
     */
    val noteReferencesSyncEnabled: Boolean = false,
    /**
     * Enables Meeting Notes Sync
     */
    val meetingNotesSyncEnabled: Boolean = false,

    /**
     * Enables UserNotifications like FutureNote, SyncError etc.
     */
    val userNotificationEnabled: Boolean = false,

    /**
     * Enables showing combined list of notes for all signed in users
     */
    val combinedListForMultiAccountEnabled: Boolean = false,

    /**
     * Limit the max allowed count of concurrent HTTP requests
     */
    val httpMaxRequestCount: Int? = null,

    val feedRealTimeEnabled: Boolean = false,

    val hidePreviewTextFromNoteRefEnabled: Boolean = false,

    val samsungNoteHtmlRenderingEnabled: Boolean = false,

    val feedDifferentViewModesEnabled: Boolean = false,

    val feedEnrichedPagePreviewsEnabled: Boolean = false,

    /**
     * Enables the card improvements for onenote pages in home feed
     */
    val feedCardImprovementsEnabled: Boolean = false,

    /**
     * Enables the card improvements for sticky notes in home feed
     */
    val stickyNotesCardImprovementsEnabled: Boolean = false,

    val pinnedNotesEnabled: Boolean = false,

    val pageImagePreviewsEnabled: Boolean = false,

    /**
     * Shows multi select in action mode in feed fragment
     */
    val multiSelectInActionModeEnabled: Boolean = false,

    val shareLinkToPageInActionModeEnabled: Boolean = false,

    val feedRichTextPagePreviewsEnabled: Boolean = false,

    val attachAppInstallLinkInShareEnabled: Boolean = false,

    val appendAndroidAppInstallLinkInShareFlowEnabled: Boolean = false,

    val feedFREFastSyncEnabled: Boolean = false,

    val hintTextInSNCanvasEnabled: Boolean = false,

    val syncOnSDKInitializationDisabled: Boolean = false,

    val enableContextInNotes: Boolean = false,

    val enableReminderInNotes: Boolean = false,

    val enableScanButtonInImage: Boolean = false,

    var enableUndoRedoActionInNotes: Boolean = false
)

class ExperimentFeatureFlagsBuilder {
    @Deprecated("This feature flag will be removed in upcoming releases")
    var realTimeEnabled: Boolean = false

    /**
     * Enables Gson parsing instead of or original Json manual parsing.
     */
    @Deprecated("This feature flag will be removed in upcoming releases")
    var gsonParserEnabled: Boolean = false

    /**
     * Enables multi accounts support
     */
    var multiAccountEnabled: Boolean = false

    /**
     * Enables previewing and editing ink notes
     */
    var inkEnabled: Boolean = false

    /**
     * Enables hybrid feed
     */
    var isHybridFeedEnabled: Boolean = false

    /**
     * Enables SamsungNotes sync
     */
    var samsungNotesSyncEnabled: Boolean = false

    /**
     * Enables NoteReferences Sync
     */
    var noteReferencesSyncEnabled: Boolean = false

    /**
     * Enables MeetingNotes Sync
     */
    var meetingNotesSyncEnabled: Boolean = false

    var userNotificationEnabled = false
    var combinedListForMultiAccountEnabled = false

    var httpMaxRequestCount: Int? = null

    var feedRealTimeEnabled: Boolean = false

    var hidePreviewTextFromNoteRefEnabled: Boolean = false

    var samsungNoteHtmlRenderingEnabled: Boolean = false

    var feedDifferentViewModesEnabled: Boolean = false

    var feedCardImprovementsEnabled: Boolean = false

    var stickyNotesCardImprovementsEnabled: Boolean = false

    var pinnedNotesEnabled: Boolean = false

    var pageImagePreviewsEnabled: Boolean = false

    var feedEnrichedPagePreviewsEnabled: Boolean = false

    var multiSelectInActionModeEnabled: Boolean = false

    var shareLinkToPageInActionModeEnabled: Boolean = false

    var feedRichTextPagePreviewsEnabled: Boolean = false

    var attachAppInstallLinkInShareEnabled: Boolean = false

    var appendAndroidAppInstallLinkInShareFlowEnabled: Boolean = false

    var feedFREFastSyncEnabled: Boolean = false

    var hintTextInSNCanvasEnabled: Boolean = false

    var syncOnSDKInitializationDisabled: Boolean = false

    var enableContextInNotes: Boolean = false

    var enableScanButtonInImage: Boolean = false

    var enableReminderInNotes: Boolean = false

    var enableUndoRedoActionInNotes: Boolean = false

    fun build(): ExperimentFeatureFlags =
        ExperimentFeatureFlags(
            realTimeEnabled = realTimeEnabled,
            gsonParserEnabled = gsonParserEnabled,
            multiAccountEnabled = multiAccountEnabled,
            inkEnabled = inkEnabled,
            isHybridFeedEnabled = isHybridFeedEnabled,
            samsungNotesSyncEnabled = samsungNotesSyncEnabled,
            noteReferencesSyncEnabled = noteReferencesSyncEnabled,
            meetingNotesSyncEnabled = meetingNotesSyncEnabled,
            combinedListForMultiAccountEnabled = combinedListForMultiAccountEnabled,
            httpMaxRequestCount = httpMaxRequestCount,
            userNotificationEnabled = userNotificationEnabled,
            feedRealTimeEnabled = feedRealTimeEnabled,
            hidePreviewTextFromNoteRefEnabled = hidePreviewTextFromNoteRefEnabled,
            samsungNoteHtmlRenderingEnabled = samsungNoteHtmlRenderingEnabled,
            feedDifferentViewModesEnabled = feedDifferentViewModesEnabled,
            feedEnrichedPagePreviewsEnabled = feedEnrichedPagePreviewsEnabled,
            feedCardImprovementsEnabled = feedCardImprovementsEnabled,
            stickyNotesCardImprovementsEnabled = stickyNotesCardImprovementsEnabled,
            pinnedNotesEnabled = pinnedNotesEnabled,
            pageImagePreviewsEnabled = pageImagePreviewsEnabled,
            multiSelectInActionModeEnabled = multiSelectInActionModeEnabled,
            shareLinkToPageInActionModeEnabled = shareLinkToPageInActionModeEnabled,
            feedRichTextPagePreviewsEnabled = feedRichTextPagePreviewsEnabled,
            attachAppInstallLinkInShareEnabled = attachAppInstallLinkInShareEnabled,
            appendAndroidAppInstallLinkInShareFlowEnabled = appendAndroidAppInstallLinkInShareFlowEnabled,
            feedFREFastSyncEnabled = feedFREFastSyncEnabled,
            hintTextInSNCanvasEnabled = hintTextInSNCanvasEnabled,
            syncOnSDKInitializationDisabled = syncOnSDKInitializationDisabled,
            enableContextInNotes = enableContextInNotes,
            enableReminderInNotes = enableReminderInNotes,
            enableScanButtonInImage = enableScanButtonInImage,
            enableUndoRedoActionInNotes = enableUndoRedoActionInNotes
        )
}

data class UiOptionFlags(
    /**
     * Displays SDK "Add Note" button in NoteListFragment.
     */
    val showNotesListAddNoteButton: Boolean = false,

    /**
     * Displays "Add Ink Note" button in NoteListFragment
     */
    val showNotesListAddInkNoteButton: Boolean = false,

    /**
     * Hides "Feedback" button in NoteOptionsFragment.
     */
    val hideNoteOptionsFeedbackButton: Boolean = false,

    /**
     * Hides "Share" button in NoteOptionsFragment.
     */
    val hideNoteOptionsShareButton: Boolean = false,

    /**
     * Hides "Clear Canvas" button in NoteOptionsFragment.
     */
    val showClearCanvasButtonForInkNotes: Boolean = false,

    /**
     * Hides Rendered ink notes in the notes list fragment
     */
    val hideRenderedInkNotesWhenInkEnabled: Boolean = false,

    /**
     * Hides "Delete" button in NoteOptionsFragment. If marked true, the host app is expected to implement this
     * in their own UI.
     */
    val hideNoteOptionsDeleteButton: Boolean = false,

    /**
     * Shows error UI in the NoteListFragment and FeedFragment
     */
    val showListErrorUi: Boolean = false,

    /**
     * Shows action mode on long press in feed fragment
     */
    val showActionModeOnFeed: Boolean = false,

    /**
     * Shows "Add to Home Screen" option in action mode for feed home
     */
    val showPinShortcutToHomeOption: Boolean = false,

    /**
     * Shows "Find in Page" option in bottom sheet
     */
    val showSearchInNoteOption: Boolean = true
)

@Suppress("TooManyFunctions")
class NotesLibraryConfiguration
private constructor(
    override val context: Context,
    override val isRestricted: Boolean,
    override val store: NotesLibraryStore,
    override val dbName: String = Constants.DATABASE_NAME,
    override val uiOptionFlags: UiOptionFlags,
    override val experimentFeatureFlags: ExperimentFeatureFlags,
    override val clientThread: ThreadExecutor? = null,
    override val notesLogger: NotesLogger? = null,
    override val userAgent: String,
    override val contentUri: (context: Context, imageUrl: String) -> Uri,
    override val theme: NotesThemeOverride,
    override val imageFromCache: (mediaRemoteID: String?, clientURL: String?, noteReferenceLocalID: String?, mediaType: String?) -> Boolean,
    override val onMicrophoneButtonClick: ((context: Context) -> Unit)?,
    override val microphoneEnabled: (currentUserID: String) -> Boolean,
    override val withGalleryIconInsteadOfCamera: Boolean,
    override val autoDiscoverHost: String,
    override val startPollingDisabledOnNewToken: Boolean = false,
    override val showEmailInEditNote: Boolean,
    override val getSSLConfigurationFromIntune: ((currentUserID: String, url: String) -> Pair<SSLSocketFactory, X509TrustManager>?)?,
    override val feedAuthProvider: FeedAuthProvider? = null
) : LibraryConfiguration {

    object Builder {
        private var isRestricted = false
        private var uiOptionFlags = UiOptionFlags()
        private var experimentFeaturesFlags = ExperimentFeatureFlagsBuilder().build()
        private var userAgent = ""
        private var clientThread: ThreadExecutor? = null
        private var logger: Logger? = null
        private var telemetryLogger: TelemetryLogger? = null
        private var theme: NotesThemeOverride? = null
        private var onMicrophoneButtonClick: ((context: Context) -> Unit)? = null
        private var enableMicrophone: ((currentUserID: String) -> Boolean)? = null
        private var withGalleryIconInsteadOfCamera: Boolean = false
        private var dbName: String = Constants.DATABASE_NAME
        private var contentUri: (context: Context, imageUrl: String) -> Uri = ::createContentUri
        private var imageFromCache: ((mediaRemoteID: String?, clientURL: String?, noteReferenceLocalID: String?, mediaType: String?) -> Boolean)? = null
        private var autoDiscoverHost: String = NetworkedAutoDiscover.DEFAULT_AUTODISCOVER_HOST
        private var startPollingDisabledOnNewToken: Boolean = false
        private var showEmailInEditNote: Boolean = false
        private var getSSLConfigurationFromIntune: ((currentUserID: String, url: String) -> Pair<SSLSocketFactory, X509TrustManager>?)? = null
        private var feedAuthProvider: FeedAuthProvider? = null

        @JvmStatic
        fun withDBName(dbName: String): Builder {
            this.dbName = dbName
            return this
        }

        @JvmStatic
        fun withRestriction(isRestricted: Boolean): Builder {
            this.isRestricted = isRestricted
            return this
        }

        @JvmStatic
        fun withUserAgent(userAgent: String): Builder {
            this.userAgent = userAgent
            return this
        }

        @JvmStatic
        fun withUiOptions(uiOptionFlags: UiOptionFlags = UiOptionFlags()): Builder {
            this.uiOptionFlags = uiOptionFlags
            return this
        }

        @JvmStatic
        fun withExperimentalOptions(
            experimentFeaturesFlags: ExperimentFeatureFlags = ExperimentFeatureFlagsBuilder().build()
        ): Builder {
            this.experimentFeaturesFlags = experimentFeaturesFlags
            return this
        }

        @JvmStatic
        fun withClientThread(thread: ThreadExecutor?): Builder {
            clientThread = thread
            return this
        }

        @JvmStatic
        fun withLogger(logger: Logger?): Builder {
            this.logger = logger
            return this
        }

        @JvmStatic
        fun withTelemetryLogger(telemetryLogger: TelemetryLogger?): Builder {
            this.telemetryLogger = telemetryLogger
            return this
        }

        @JvmStatic
        fun withTheme(theme: NotesThemeOverride?): Builder {
            this.theme = theme
            return this
        }

        @JvmStatic
        fun withMicrophoneButtonInEditOptions(onClick: (context: Context) -> Unit): Builder {
            this.onMicrophoneButtonClick = onClick
            return this
        }

        @JvmStatic
        fun withShouldEnableMicrophoneButton(enableMicrophone: (currentUserID: String) -> Boolean): Builder {
            this.enableMicrophone = enableMicrophone
            return this
        }

        @JvmStatic
        fun withGalleryIconInsteadOfCamera(enable: Boolean): Builder {
            this.withGalleryIconInsteadOfCamera = enable
            return this
        }

        /**
         * If you use a non-default file provider, pass in a lambda which will allow the NoteLibrary to generate
         * content uris
         */
        @JvmStatic
        fun withContentUri(createContentUri: (context: Context, imageUrl: String) -> Uri): Builder {
            this.contentUri = createContentUri
            return this
        }

        @JvmStatic
        fun withGetImageFromCache(imageFromCache: (mediaRemoteID: String?, clientURL: String?, noteReferenceLocalID: String?, mediaType: String?) -> Boolean): Builder {
            this.imageFromCache = imageFromCache
            return this
        }

        @JvmStatic
        fun withAutoDiscoverHost(host: String): Builder {
            this.autoDiscoverHost = host
            return this
        }

        /**
         * Only disable this if you are planning to either:
         * a) Ensure you are calling startPolling yourself, or
         * b) Manual syncing with your own polling logic
         * Failure to do either of the above will result in the user not seeing their latest notes
         * when they expect to!
         */
        @JvmStatic
        fun withStartPollingDisabledOnNewToken(disabled: Boolean): Builder {
            this.startPollingDisabledOnNewToken = disabled
            return this
        }

        @JvmStatic
        fun withShowEmailInEditNote(show: Boolean): Builder {
            this.showEmailInEditNote = show
            return this
        }

        @JvmStatic
        fun withGetSSLConfigurationFromIntune(getSSLConfigurationFromIntune: (currentUserID: String, url: String) -> Pair<SSLSocketFactory, X509TrustManager>?): Builder {
            this.getSSLConfigurationFromIntune = getSSLConfigurationFromIntune
            return this
        }

        @JvmStatic
        fun withFeedAuthProvider(feedAuthProvider: FeedAuthProvider?): Builder {
            this.feedAuthProvider = feedAuthProvider
            return this
        }

        @JvmStatic
        fun build(
            context: Context,
            appName: String,
            isDebugMode: Boolean
        ): NotesLibraryConfiguration {

            val notesLogger = NotesLogger(logger, telemetryLogger)
            val store = NotesLibraryStore(
                notesLogger = notesLogger,
                appName = appName,
                isDebugMode = isDebugMode
            )

            return NotesLibraryConfiguration(
                context = context,
                isRestricted = isRestricted,
                store = store,
                dbName = dbName,
                uiOptionFlags = uiOptionFlags,
                experimentFeatureFlags = experimentFeaturesFlags,
                clientThread = clientThread,
                notesLogger = notesLogger,
                userAgent = userAgent,
                contentUri = contentUri,
                imageFromCache = imageFromCache ?: { _: String?, _: String?, _: String?, _: String? -> false },
                theme = theme ?: NotesThemeOverride.default,
                onMicrophoneButtonClick = onMicrophoneButtonClick,
                autoDiscoverHost = autoDiscoverHost,
                microphoneEnabled = enableMicrophone ?: { false },
                withGalleryIconInsteadOfCamera = withGalleryIconInsteadOfCamera,
                startPollingDisabledOnNewToken = startPollingDisabledOnNewToken,
                showEmailInEditNote = showEmailInEditNote,
                getSSLConfigurationFromIntune = getSSLConfigurationFromIntune,
                feedAuthProvider = feedAuthProvider
            )
        }
    }
}
