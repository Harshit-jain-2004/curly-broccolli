package com.microsoft.notes.noteslib

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.NoteReferenceChanges
import com.microsoft.notes.models.NoteReferenceMedia
import com.microsoft.notes.models.Reminder
import com.microsoft.notes.models.extensions.updateNoteReferenceMediaWithLocalUrl
import com.microsoft.notes.models.toTelemetryNoteType
import com.microsoft.notes.notesReference.SerialActionDispatcher
import com.microsoft.notes.notesReference.calculateChangesForLocalOnlyNoteRefsCleanUp
import com.microsoft.notes.notesReference.models.NoteRefLocalChanges
import com.microsoft.notes.notesReference.models.NoteRefSourceId
import com.microsoft.notes.notesReference.models.PageChangeSignalMetaData
import com.microsoft.notes.notesReference.translateAppendPageIfNeededSignal
import com.microsoft.notes.notesReference.translatePageChangedSignal
import com.microsoft.notes.notesReference.translatePageDeletedSignal
import com.microsoft.notes.notesReference.translateSectionChangedSignal
import com.microsoft.notes.notesReference.translateSectionDeletedSignal
import com.microsoft.notes.noteslib.extensions.calculateNoteReferencesListChanges
import com.microsoft.notes.noteslib.extensions.compress
import com.microsoft.notes.noteslib.extensions.recordImageAddedTelemetry
import com.microsoft.notes.noteslib.extensions.sanitizeForUseAsHttpHeader
import com.microsoft.notes.noteslib.extensions.urlToMimeType
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.DocumentType
import com.microsoft.notes.richtext.scheme.Range
import com.microsoft.notes.sideeffect.persistence.extensions.gsonSerializer
import com.microsoft.notes.sideeffect.persistence.mapper.toStoreNote
import com.microsoft.notes.sideeffect.ui.UiBindings
import com.microsoft.notes.sideeffect.ui.UiSideEffect
import com.microsoft.notes.store.AuthState
import com.microsoft.notes.store.NotesList
import com.microsoft.notes.store.OutboundSyncState
import com.microsoft.notes.store.Promise
import com.microsoft.notes.store.PromiseImpl
import com.microsoft.notes.store.SideEffectsList
import com.microsoft.notes.store.State
import com.microsoft.notes.store.SyncErrorState
import com.microsoft.notes.store.action.AuthAction
import com.microsoft.notes.store.action.AuthenticatedSyncRequestAction
import com.microsoft.notes.store.action.CompoundAction
import com.microsoft.notes.store.action.CreationAction.AddNoteAction
import com.microsoft.notes.store.action.DeleteAction.DeleteAllNotesAction
import com.microsoft.notes.store.action.DeleteAction.MarkNoteAsDeletedAction
import com.microsoft.notes.store.action.DeleteAction.MarkNoteReferenceAsDeletedAction
import com.microsoft.notes.store.action.DeleteAction.MarkSamsungNoteAsDeletedAction
import com.microsoft.notes.store.action.DeleteAction.UnmarkNoteAsDeletedAction
import com.microsoft.notes.store.action.NoteReferenceAction
import com.microsoft.notes.store.action.PollingAction
import com.microsoft.notes.store.action.PreferencesAction
import com.microsoft.notes.store.action.ReadAction.FetchAllNotesAction
import com.microsoft.notes.store.action.SyncRequestAction.DeleteMedia
import com.microsoft.notes.store.action.SyncRequestAction.DeleteNote
import com.microsoft.notes.store.action.SyncRequestAction.DeleteNoteReference
import com.microsoft.notes.store.action.SyncRequestAction.DeleteSamsungNote
import com.microsoft.notes.store.action.SyncRequestAction.UpdateMediaAltText
import com.microsoft.notes.store.action.SyncRequestAction.UploadMedia
import com.microsoft.notes.store.action.UIAction
import com.microsoft.notes.store.action.UpdateAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateDocumentRange
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithAddedMediaAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithColorAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithDocumentAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithRemovedMediaAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithUpdateMediaAltTextAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateTimeReminderAction
import com.microsoft.notes.store.combinedNoteReferencesForAllUsers
import com.microsoft.notes.store.combinedNotesForAllUsers
import com.microsoft.notes.store.combinedNotesListForAllUsers
import com.microsoft.notes.store.combinedSamsungNotesForAllUsers
import com.microsoft.notes.store.extensions.map
import com.microsoft.notes.store.getNoteReferencesCollectionForUser
import com.microsoft.notes.store.getNotificationsForAllUsers
import com.microsoft.notes.store.getUserStateForUserID
import com.microsoft.notes.sync.RequestPriority
import com.microsoft.notes.ui.NoteColorSharedPreferences
import com.microsoft.notes.ui.feed.filter.FeedFilters
import com.microsoft.notes.ui.feed.recyclerview.FeedLayoutType
import com.microsoft.notes.ui.feed.sourcefilter.FeedSourceFilterOption
import com.microsoft.notes.ui.noteslist.UserNotifications
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.utils.logging.SeverityLevel
import com.microsoft.notes.utils.threading.ThreadExecutor
import com.microsoft.notes.utils.utils.Constants
import com.microsoft.notes.utils.utils.IdentityMetaData
import com.microsoft.notes.utils.utils.ImageUtils
import com.microsoft.notes.utils.utils.PrefixedIdentityMetaData
import com.microsoft.notes.utils.utils.UserInfoUtils
import com.microsoft.notes.utils.utils.UserInfoUtils.Companion.getSignedInUsers
import com.microsoft.notes.utils.utils.toParagraphs
import com.microsoft.notes.utils.utils.toUserInfo
import java.io.File
import java.lang.ref.WeakReference
import java.net.URI
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import kotlin.concurrent.thread
import com.microsoft.notes.sideeffect.persistence.Note as PersistenceNote

@Suppress("TooManyFunctions", "LargeClass")
class NotesLibrary private constructor(
    private val context: WeakReference<Context>,
    private val isRestricted: Boolean = false,
    private val dbName: String = Constants.DATABASE_NAME,
    internal val uiOptionFlags: UiOptionFlags,
    val experimentFeatureFlags: ExperimentFeatureFlags,
    val clientThread: ThreadExecutor? = null,
    val notesLogger: NotesLogger? = null,
    private val store: NotesLibraryStore,
    private val userAgent: String,
    internal val contentUri: (context: Context, imageUrl: String) -> Uri,
    theme: NotesThemeOverride,
    internal val imageFromCache: (mediaRemoteID: String?, clientURL: String?, noteReferenceLocalID: String?, mediaType: String?) -> Boolean,
    val onMicrophoneButtonClick: ((context: Context) -> Unit)?,
    internal val microphoneEnabled: (currentUserID: String) -> Boolean,
    internal val withGalleryIconInsteadOfCamera: Boolean,
    internal val autoDiscoverHost: String,
    internal val startPollingDisabledOnNewToken: Boolean,
    internal val showEmailInEditNote: Boolean,
    internal val getSSLConfigurationFromIntune: ((currentUserID: String, url: String) -> Pair<SSLSocketFactory, X509TrustManager>?)?,
    var feedAuthProvider: FeedAuthProvider? = null
) {

    companion object {
        private lateinit var notesLibrary: NotesLibrary
        private const val LOG_TAG = "NotesLibrary"
        @JvmStatic
        fun init(configuration: LibraryConfiguration) {
            with(configuration) {
                notesLibrary = NotesLibrary(
                    WeakReference(context),
                    isRestricted = isRestricted,
                    uiOptionFlags = uiOptionFlags,
                    experimentFeatureFlags = experimentFeatureFlags,
                    dbName = dbName,
                    clientThread = clientThread,
                    notesLogger = notesLogger,
                    store = store,
                    userAgent = userAgent,
                    contentUri = contentUri,
                    theme = theme,
                    imageFromCache = imageFromCache,
                    onMicrophoneButtonClick = onMicrophoneButtonClick,
                    autoDiscoverHost = autoDiscoverHost,
                    microphoneEnabled = microphoneEnabled,
                    withGalleryIconInsteadOfCamera = withGalleryIconInsteadOfCamera,
                    startPollingDisabledOnNewToken = startPollingDisabledOnNewToken,
                    showEmailInEditNote = showEmailInEditNote,
                    getSSLConfigurationFromIntune = getSSLConfigurationFromIntune,
                    feedAuthProvider = feedAuthProvider
                )
            }
            notesLibrary.init()
        }

        @JvmStatic
        fun getInstance(): NotesLibrary {
            try {
                return notesLibrary
            } catch (exception: UninitializedPropertyAccessException) {
                throw UninitializedPropertyAccessException("NotesLibrary must be initialised first.")
            }
        }
    }

    val currentState get() = store.state
    val currentUserID get() = store.state.currentUserID
    val currentUserState get() = store.state.getUserStateForUserID(currentUserID)
    val currentNotes: List<Note>
        get() = if (experimentFeatureFlags.combinedListForMultiAccountEnabled) {
            store.state.combinedNotesForAllUsers()
        } else {
            currentUserState.notesList.notesCollection
        }
    val currentSamsungNotes: List<Note>
        get() = if (experimentFeatureFlags.samsungNotesSyncEnabled) {
            if (experimentFeatureFlags.combinedListForMultiAccountEnabled) {
                store.state.combinedSamsungNotesForAllUsers()
            } else {
                currentUserState.samsungNotesList.samsungNotesCollection
            }
        } else {
            emptyList()
        }
    val currentNoteReferences: List<NoteReference>
        get() = if (experimentFeatureFlags.combinedListForMultiAccountEnabled) {
            store.state.combinedNoteReferencesForAllUsers()
        } else {
            currentUserState.noteReferencesList.noteReferencesCollection
        }

    val notesList: NotesList
        get() = if (experimentFeatureFlags.combinedListForMultiAccountEnabled) {
            store.state.combinedNotesListForAllUsers()
        } else {
            currentUserState.notesList
        }
    val authState: AuthState get() = currentUserState.authenticationState.authState
    internal val currentSyncErrorState: SyncErrorState get() = currentUserState.currentSyncErrorState
    internal val allUserNotificationsForCurrentState: Map<String, UserNotifications>
        get() = store.state.getNotificationsForAllUsers()
    fun getNotesSessionID(userID: String) = store.getNotesSessionId(userID)

    fun getNoteReferencesForUser(userID: String) =
        store.state.getNoteReferencesCollectionForUser(userID)

    // Themes
    private val themeChangeBroadcaster: ThemeChangeBroadcaster = ThemeChangeBroadcaster(theme)
    val theme: NotesThemeOverride get() = themeChangeBroadcaster.currentTheme

    fun setTheme(theme: NotesThemeOverride) {
        themeChangeBroadcaster.currentTheme = theme
    }

    // It will return true if app dark mode is enabled else false.
    fun isDarkThemeEnabled() = (theme.noteRefCanvasThemeOverride != null)

    internal fun registerViewForTheming(view: View) {
        themeChangeBroadcaster.registerAndSetTheme(view)
    }

    // Request Priority - gets reset when the library is reinitialised
    internal var requestPriority = RequestPriority.background
    internal var isActionsEnabledOnNoteReferences = false
    internal var disableNoteOptionsFeedbackButton = false

    private val serialDispatcher by lazy { SerialActionDispatcher() }

    private fun init() {
        enableUiBindings()
        if (!isRestricted) {
            if (!experimentFeatureFlags.syncOnSDKInitializationDisabled) {
                enableSync()
            }
            enablePersistence()
            enablePreferencesSideEffect()
            if (experimentFeatureFlags.enableReminderInNotes) {
                enableLocalSideEffect()
            }
        }
    }

    private fun enableLocalSideEffect() {
        context.get()?.let {
            store.enableLocalSideEffect(it, dbName, experimentFeatureFlags.multiAccountEnabled)
        }
    }
    private fun enablePersistence() {
        context.get()?.let {
            store.enablePersistence(it, dbName, experimentFeatureFlags.multiAccountEnabled)
        }
    }

    // TODO : Serialize the using Note object instead of PersistenceNote
    // This is a temporary fix to serialize-deserialize the note object */
    fun getStoreNoteFromSerializedPersistenceNote(noteData: String): Note? {
        val deserializedNote: PersistenceNote = gsonSerializer.fromJson(noteData, PersistenceNote::class.java)
        return deserializedNote.toStoreNote()
    }
    fun enableSync() {
        context.get()?.let {
            store.enableSync(
                it,
                it.filesDir,
                sanitizeForUseAsHttpHeader(userAgent),
                experimentFeatureFlags,
                autoDiscoverHost = autoDiscoverHost
            ) { requestPriority }
        }
    }

    private fun enableUiBindings() {
        store.enableUiSideEffect(experimentFeatureFlags)
    }

    private fun enablePreferencesSideEffect() {
        context.get()?.let {
            store.enablePreferencesSideEffect(it)
        }
    }

    fun startPolling(userID: String) {
        store.dispatch(PollingAction.Start(userID))
    }

    fun stopPolling(userID: String) {
        store.dispatch(PollingAction.Stop(userID))
    }

    private fun SideEffectsList.hasUiSideEffect(): UiSideEffect? = find { it is UiSideEffect } as? UiSideEffect

    fun addUiBindings(uiBindings: UiBindings): Boolean? =
        store.sideEffects.hasUiSideEffect()?.addUiBindings(uiBindings)

    fun removeUiBindings(uiBindings: UiBindings): Boolean? =
        store.sideEffects.hasUiSideEffect()?.removeUiBindings(uiBindings)

    fun getNoteById(noteId: String): Note? =
        currentNotes.find { it.localId == noteId } ?: currentSamsungNotes.find { it.localId == noteId }

    fun getNoteReferenceById(noteRefId: String): NoteReference? =
        currentNoteReferences.find { it.localId == noteRefId }

    fun fetchNotes(userID: String): Promise<State> =
        store.dispatch(FetchAllNotesAction(userID, experimentFeatureFlags.samsungNotesSyncEnabled), clientThread)

    fun fetchNotesForAllUsers() {
        if (experimentFeatureFlags.combinedListForMultiAccountEnabled) {
            getAllUsers().forEach { userID -> store.dispatch(FetchAllNotesAction(userID, experimentFeatureFlags.samsungNotesSyncEnabled), clientThread) }
        } else {
            store.dispatch(FetchAllNotesAction(currentUserID, experimentFeatureFlags.samsungNotesSyncEnabled), clientThread)
        }
    }

    fun startPollingForAllUsers() {
        if (experimentFeatureFlags.combinedListForMultiAccountEnabled) {
            getAllUsers().forEach { userID -> store.dispatch(PollingAction.Start(userID)) }
        } else {
            store.dispatch(PollingAction.Start(currentUserID))
        }
    }

    fun stopPollingForAllUsers() {
        if (experimentFeatureFlags.combinedListForMultiAccountEnabled) {
            getAllUsers().forEach { userID -> store.dispatch(PollingAction.Stop(userID)) }
        } else {
            store.dispatch(PollingAction.Stop(currentUserID))
        }
    }

    /* Intune policies should be handled by client if any before calling this api */
    fun copyAndMoveNoteToDifferentAccount(note: Note, userID: String): Promise<Note> {
        val newNote = addNote(
            Note(
                document = note.document, color = note.color, localCreatedAt = note.localCreatedAt,
                documentModifiedAt = note.documentModifiedAt, title = note.title, createdByApp = note.createdByApp, metadata = note.metadata
            ),
            userID
        )
        newNote.then {
            val mediaList: MutableList<String> = mutableListOf()
            for (media in note.media) {
                if (media.localUrl != null) {
                    mediaList.add(media.localUrl)
                }
            }
            if (mediaList.isNotEmpty()) {
                addMultipleMediaToNote(it, mediaList, false, {
                    markAsDeleteAndDelete(note.localId, note.remoteData?.id)
                })
            } else {
                markAsDeleteAndDelete(note.localId, note.remoteData?.id)
            }
        }
        return newNote
    }

    fun deleteAllNotes(userID: String): Promise<State> =
        store.dispatch(DeleteAllNotesAction(userID), clientThread)

    fun deleteAllNoteReferencesForAllUsers() {
        getAllUsers().forEach { userID -> refreshNoteReferences(emptyList(), userID) }
    }

    fun getAuthState(userID: String): AuthState = store.state.getUserStateForUserID(userID).authenticationState.authState

    internal fun isSyncPaused(userID: String): Boolean = store.state.getUserStateForUserID(userID).outboundSyncState == OutboundSyncState.Inactive

    internal fun isMultipleUsersSignedIn(): Boolean =
        store.state.userIDToUserStateMap.count { it.key.isNotEmpty() } > 1

    internal fun isRestricted(): Boolean = isRestricted

    @Deprecated(
        "UserID is not required, please use the other variation which" +
            " doesn't take userID as param"
    )

    fun markNoteAsDeleted(noteLocalId: String, noteRemoteId: String?, userID: String): Promise<State> =
        store.dispatch(MarkNoteAsDeletedAction(noteLocalId, noteRemoteId, userID), clientThread)

    fun markNoteAsDeleted(noteLocalId: String, noteRemoteId: String?): Promise<State> =
        store.dispatch(
            MarkNoteAsDeletedAction(noteLocalId, noteRemoteId, store.getUserIDForNoteLocalID(noteLocalId)),
            clientThread
        )

    @Deprecated(
        "UserID is not required, please use the other variation which" +
            " doesn't take userID as param"
    )
    fun unmarkNoteAsDeleted(noteLocalId: String, noteRemoteId: String?, userID: String): Promise<State> =
        store.dispatch(UnmarkNoteAsDeletedAction(noteLocalId, noteRemoteId, userID), clientThread)

    fun unmarkNoteAsDeleted(noteLocalId: String, noteRemoteId: String?): Promise<State> =
        store.dispatch(
            UnmarkNoteAsDeletedAction(noteLocalId, noteRemoteId, store.getUserIDForNoteLocalID(noteLocalId)),
            clientThread
        )

    @Deprecated(
        "UserID is not required, please use the other variation which" +
            " doesn't take userID as param"
    )
    fun permanentlyDeleteNote(noteLocalId: String, noteRemoteId: String?, userID: String): Promise<State> =
        store.dispatch(DeleteNote(noteLocalId, noteRemoteId, userID), clientThread)

    fun permanentlyDeleteNote(noteLocalId: String, noteRemoteId: String?): Promise<State> =
        store.dispatch(
            DeleteNote(noteLocalId, noteRemoteId, store.getUserIDForNoteLocalID(noteLocalId)),
            clientThread
        )

    @Deprecated(
        "Please use newAuthToken with the PrefixedIdentityMetaData parameter instead, as it has a greater" +
            " chance of succeeding in the AutoDiscover call"
    )
    fun newAuthToken(identityMetaData: IdentityMetaData): Promise<State> {
        with(identityMetaData) {
            notesLogger?.d(LOG_TAG, "NewAuthToken AccountType: $accountType")

            val localContext = context.get()
                ?: throw IllegalStateException("Context is null, ignoring auth token of AccountType: $accountType")

            val userInfo = identityMetaData.toUserInfo(localContext)

            return store.dispatch(AuthAction.NewAuthTokenAction(userInfo), clientThread)
        }
    }

    fun newAuthToken(identityMetaData: PrefixedIdentityMetaData): Promise<State> {
        with(identityMetaData) {
            notesLogger?.d(LOG_TAG, "NewAuthToken AccountType: $accountType")

            val localContext = context.get()
                ?: throw IllegalStateException("Context is null, ignoring auth token of AccountType: $accountType")

            val userInfo = identityMetaData.toUserInfo(localContext)

            return store.dispatch(AuthAction.NewAuthTokenAction(userInfo), clientThread)
        }
    }

    fun userAuthFailed(userID: String): Promise<State> =
        store.dispatch(AuthAction.ClientAuthFailedAction(userID))

    fun getAllUsers(): Set<String> = context.get()?.let { getSignedInUsers(it) } ?: emptySet()

    fun logout(userID: String): Promise<State> {
        context.get()?.let { UserInfoUtils.updateUserInfoSuffixForSignedOutUser(userID, it) }
        return store.dispatch(AuthAction.LogoutAction(userID), clientThread)
    }

    fun setCurrentUserID(userID: String) =
        store.dispatch(UIAction.UpdateCurrentUserID(userID))

    fun updateTimeReminder(
        noteLocalId: String,
        reminder: Reminder.TimeReminder,
        uiRevision: Long
    ): Promise<State> {
        return store.dispatch(
            UpdateTimeReminderAction(
                noteLocalId, reminder,
                userID = store.getUserIDForNoteLocalID(noteLocalID = noteLocalId),
                uiRevision = uiRevision
            ),
            clientThread
        )
    }
    /**
     * Default telemetry method, with severityLevel set to Info
     */
    fun recordTelemetry(eventMarker: EventMarkers, vararg keyValuePairs: Pair<String, String>) {
        notesLogger?.recordTelemetry(
            eventMarker = eventMarker,
            keyValuePairs = *keyValuePairs,
            severityLevel = SeverityLevel.Info
        )
    }

    @Suppress("unused")
    fun recordTelemetry(
        eventMarker: EventMarkers,
        vararg keyValuePairs: Pair<String, String>,
        severityLevel: SeverityLevel
    ) {
        notesLogger?.recordTelemetry(
            eventMarker = eventMarker,
            keyValuePairs = *keyValuePairs,
            severityLevel = severityLevel
        )
    }

    fun log(message: String) {
        notesLogger?.i(message = message)
    }

    fun showToast(messageId: Int) {
        Handler(Looper.getMainLooper()).post {
            if (context.get() != null) {
                Toast.makeText(context.get(), messageId, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun updateNoteDocument(
        noteLocalId: String,
        updatedDocument: Document,
        uiRevision: Long
    ): Promise<State> =
        store.dispatch(
            UpdateNoteWithDocumentAction(
                noteLocalId = noteLocalId,
                updatedDocument = updatedDocument,
                uiRevision = uiRevision,
                userID = store.getUserIDForNoteLocalID(noteLocalID = noteLocalId)
            ),
            clientThread
        )

    fun updateDocumentRange(noteLocalId: String, newRange: Range): Promise<State> =
        store.dispatch(
            UpdateDocumentRange(
                noteLocalId, newRange,
                userID = store.getUserIDForNoteLocalID(noteLocalID = noteLocalId)
            ),
            clientThread
        )

    fun addMediaToNote(
        uiRevision: Long,
        note: Note,
        localUrl: String,
        deleteOriginal: Boolean,
        compressionCompleted: (successful: Boolean) -> Unit,
        triggerPoint: String? = null
    ) {
        // image compression is done in a new thread, as this method might be called on UI thread
        thread {
            compressImageAndAddToNote(uiRevision, note, localUrl, deleteOriginal, compressionCompleted, triggerPoint)
        }
    }

    fun addMultipleMediaToNote(
        note: Note,
        localUrlList: List<String>,
        deleteOriginal: Boolean,
        operationCompleted: (successful: Boolean) -> Unit,
        triggerPoint: String? = null
    ) {
        // image compression is done in a new thread, as this method might be called on UI thread
        thread {
            var operationSuccess = true
            fun compressionComplete(successful: Boolean) {
                operationSuccess = operationSuccess && successful
            }
            for ((index, localUrl) in localUrlList.withIndex()) {
                compressImageAndAddToNote(note.uiRevision + index + 1, note, localUrl, deleteOriginal, ::compressionComplete, triggerPoint)
            }
            sendImageCompressionCompletedAction(true)
            operationCompleted(operationSuccess)
        }
    }

    fun saveImageBase64StringToCache(mediaId: String, imageBase64String: String, notereferenceId: String) {
        val noteReference = this.currentNoteReferences.find { it.localId == notereferenceId }
        if (noteReference?.media?.size != 0 && noteReference?.media?.get(0)?.mediaID != null && noteReference.media[0].mediaID == mediaId) {
            val localImageUrl = context.get()?.let { ImageUtils.saveBase64UrlToFileAsLocalUrl(imageBase64String, mediaId, it) }
            if (!localImageUrl.isNullOrEmpty()) {
                val userID = store.getUserIDForNoteReferenceLocalID(noteReferenceLocalID = noteReference.localId)
                store.dispatch(NoteReferenceAction.UpdateNoteReferenceMedia(noteReference, userID, noteReference.media?.updateNoteReferenceMediaWithLocalUrl(noteReference.media[0].mediaID, localImageUrl)))
            }
        }
    }

    fun deleteCachedImagesForDeletedNoteReference(noteReferenceMedia: List<NoteReferenceMedia>?) {
        if (!noteReferenceMedia.isNullOrEmpty()) {
            for (media in noteReferenceMedia) {
                val mediaId = media.mediaID
                context.get()?.let { ImageUtils.deleteCachedImage(mediaId, it) }
            }
        }
    }

    fun deleteCachedImage(mediaId: String) = context.get()?.let { ImageUtils.deleteCachedImage(mediaId, it) }

    private fun compressImageAndAddToNote(
        uiRevision: Long,
        note: Note,
        localUrl: String,
        deleteOriginal: Boolean,
        compressionCompleted: (successful: Boolean) -> Unit,
        triggerPoint: String? = null
    ) {
        var mimeType = urlToMimeType(localUrl)

        if (mimeType.contains("audio")) {
            mimeType = "image/gif"
        }

        var newLocalUrl = Uri.fromFile(File(URI.create(localUrl))).toString()

        if (!mimeType.contains("gif")) {
            newLocalUrl = compressImage(
                note, localUrl, urlToMimeType(localUrl), deleteOriginal, compressionCompleted, triggerPoint
            )
                ?: return


            mimeType = urlToMimeType(newLocalUrl)
        }

        val userID = store.getUserIDForNoteLocalID(noteLocalID = note.localId)

        val media = Media(
            localUrl = newLocalUrl,
            mimeType = mimeType,
            altText = null,
            imageDimensions = null
        )
        val updateMedia = UpdateNoteWithAddedMediaAction(
            noteLocalId = note.localId,
            media = media,
            uiRevision = uiRevision,
            userID = userID
        )
        val uploadMedia = UploadMedia(note, media.localId, newLocalUrl, mimeType, userID)
        store.dispatch(CompoundAction(updateMedia, uploadMedia), clientThread)
    }

    private fun compressImage(
        note: Note,
        localUrl: String,
        mimeType: String,
        deleteOriginal: Boolean,
        compressionCompleted: (successful: Boolean) -> Unit,
        triggerPoint: String?
    ): String? {
        val fileUri = URI.create(localUrl)
        val uncompressedImage = File(fileUri)
        val uncompressedImageSize = uncompressedImage.length()
        val safeContext = context.get()
        if (safeContext == null) {
            compressionCompleted(false)
            return null
        }

        val compressedImage = uncompressedImage.compress(safeContext, deleteOriginal)
        if (compressedImage == null) {
            recordTelemetry(EventMarkers.AddImageCompressionError)
            compressionCompleted(false)
            return null
        }

        compressionCompleted(true)
        recordImageAddedTelemetry(
            note, mimeType, uncompressedImageSize, compressedImage.length(), triggerPoint
        )
        return Uri.fromFile(compressedImage).toString()
    }

    fun deleteMediaFromNote(uiRevision: Long, note: Note, media: Media) {
        val deleteMedia = DeleteMedia(
            note.localId, note.remoteData?.id, media.localId, media.remoteId,
            store
                .state.userIDForLocalNoteID(localNoteID = note.localId)
        )
        val updateNote = UpdateNoteWithRemovedMediaAction(
            noteLocalId = note.localId,
            media = media,
            uiRevision = uiRevision,
            userID = store.getUserIDForNoteLocalID(noteLocalID = note.localId)
        )
        store.dispatch(CompoundAction(deleteMedia, updateNote), clientThread)
    }

    fun updateMediaAltText(uiRevision: Long, note: Note, media: Media, altText: String?) {
        val updateMediaAltText = UpdateMediaAltText(
            note,
            note.localId, note.remoteData?.id, media.localId, media.remoteId, altText,
            store.state
                .userIDForLocalNoteID(localNoteID = note.localId)
        )

        val updateNote = UpdateNoteWithUpdateMediaAltTextAction(
            noteLocalId = note.localId,
            mediaLocalId = media.localId,
            altText = altText,
            uiRevision = uiRevision,
            userID = store.getUserIDForNoteLocalID(noteLocalID = note.localId)
        )
        store.dispatch(CompoundAction(updateMediaAltText, updateNote), clientThread)
    }

    fun clearCanvas() {
        store.dispatch(UIAction.ClearCanvasAction(), clientThread)
    }

    fun markAsDeleteAndDelete(noteLocalId: String, noteRemoteId: String?, isUserTriggered: Boolean = false): Promise<State> {
        val userIDForLocalNoteID = store.getUserIDForNoteLocalID(noteLocalID = noteLocalId)
        val markAsDeleted = MarkNoteAsDeletedAction(noteLocalId, noteRemoteId, userIDForLocalNoteID, isUserTriggered)
        val deleteNote = DeleteNote(noteLocalId, noteRemoteId, userIDForLocalNoteID)
        return store.dispatch(CompoundAction(markAsDeleted, deleteNote), clientThread)
    }

    /**
     USE WITH CAUTION: This should only be used for non responsive OneNote pages.
     This function would delete NoteReference from local database and server.
     The function doesn't support the NoteRemoteId as null as there is no use case.
     **/
    fun markNonResponsiveNoteReferenceAsDeleteAndDelete(noteLocalId: String, noteRemoteId: String, isUserTriggered: Boolean = true): Promise<State> {
        val userIDForLocalNoteID = store.getUserIDForNoteReferenceLocalID(noteReferenceLocalID = noteLocalId)
        val markAsDeleted = MarkNoteReferenceAsDeletedAction(noteLocalId, noteRemoteId, userIDForLocalNoteID, isUserTriggered)
        val deleteNote = DeleteNoteReference(noteLocalId, noteRemoteId, userIDForLocalNoteID)
        return store.dispatch(CompoundAction(markAsDeleted, deleteNote), clientThread)
    }

    fun markSamsungNoteAsDeleteAndDelete(noteLocalId: String, noteRemoteId: String): Promise<State> {
        val userIDForLocalNoteID = store.getUserIDForNoteLocalID(noteLocalID = noteLocalId)
        val markAsDeleted = MarkSamsungNoteAsDeletedAction(noteLocalId, noteRemoteId, userIDForLocalNoteID)
        val deleteNote = DeleteSamsungNote(noteLocalId, noteRemoteId, userIDForLocalNoteID)
        return store.dispatch(CompoundAction(markAsDeleted, deleteNote), clientThread)
    }

    fun requestManualSync(userID: String) {
        store.dispatch(AuthenticatedSyncRequestAction.ManualSyncRequestAction(userID))
    }

    fun forceFullSyncForAllUsers() {
        for (userID in getAllUsers()) {
            store.dispatch(AuthenticatedSyncRequestAction.ForceFullSyncRequestAction(userID))
        }
    }

    fun sendAddPhotoAction() {
        store.dispatch(UIAction.AddPhotoAction(), clientThread)
    }

    fun sendCaptureNoteAction() {
        store.dispatch(UIAction.CaptureNoteAction(), clientThread)
    }

    internal fun sendOnMicroPhoneButtonClickedAction() {
        store.dispatch(UIAction.OnMicroPhoneButtonClickedAction(), clientThread)
    }

    fun sendScanButtonClickedAction() {
        store.dispatch(UIAction.OnScanButtonClickedAction(), clientThread)
    }

    @Deprecated("Use the variation which requires userID as param")
    fun requestManualSync() =
        store.dispatch(AuthenticatedSyncRequestAction.ManualSyncRequestAction(currentUserID))

    fun requestManualNoteReferencesSync(userID: String) =
        store.dispatch(AuthenticatedSyncRequestAction.ManualNoteReferencesSyncRequestAction(userID))

    fun requestManualMeetingNotesSync(userID: String) =
        store.dispatch(AuthenticatedSyncRequestAction.ManualMeetingNotesSyncRequestAction(userID))

    fun requestManualSyncWithDeltaToken() =
        store.dispatch(AuthenticatedSyncRequestAction.RemoteChangedDetected(currentUserID))

    fun requestSamsungNoteSync(userID: String) =
        store.dispatch(AuthenticatedSyncRequestAction.ManualSamsungNotesSyncRequestAction(userID))

    fun updateNoteColor(noteLocalId: String, color: Color, revision: Long): Promise<State> {
        context.get()?.let {
            val cache = NoteColorSharedPreferences(it)
            cache.setNoteColorPreference(color)
        }

        return store.dispatch(
            UpdateNoteWithColorAction(
                noteLocalId = noteLocalId, color = color,
                uiRevision = revision,
                userID = store.getUserIDForNoteLocalID(noteLocalID = noteLocalId)
            ),
            clientThread
        )
    }

    fun addNote(note: Note, userID: String): Promise<Note> {
        val promise = store.dispatch(AddNoteAction(note, userID), clientThread).map { note }
        promise.then {
            recordTelemetry(
                EventMarkers.NoteCreated,
                Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_LOCAL_ID, it.localId),
                Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_TYPE, it.toTelemetryNoteType().toString())
            )
        }
        return promise
    }

    @JvmOverloads
    fun addNote(userID: String, text: String? = null, color: Color = getColorForAddedNote()): Promise<Note> {
        val document = if (text != null) {
            Document(toParagraphs(listOf(text)))
        } else {
            Document()
        }

        val note = Note(document = document, createdByApp = createdByApp(), color = color)
        return addNote(note, userID = userID)
    }

    fun addNoteWithMultiLineText(
        textLines: List<String>,
        color: Color = getColorForAddedNote(),
        userID: String
    ): Promise<Note> {
        val document = Document(toParagraphs(textLines))
        val note = Note(document = document, createdByApp = createdByApp(), color = color)
        return addNote(note, userID = userID)
    }

    @JvmOverloads
    fun addInkNote(userID: String, color: Color = getColorForAddedNote()): Promise<Note> {
        val document = Document(type = DocumentType.INK)
        val note = Note(document = document, createdByApp = createdByApp(), color = color)
        return addNote(note, userID = userID)
    }

    fun switchFeedLayoutToGridView() {
        store.dispatch(UIAction.ChangeFeedLayout(FeedLayoutType.GRID_LAYOUT))
    }

    fun switchFeedLayoutToListView() {
        store.dispatch(UIAction.ChangeFeedLayout(FeedLayoutType.LIST_LAYOUT))
    }

    fun showFilterAndSortPanel() {
        store.dispatch(UIAction.DisplayFilterAndSortPanel())
    }

    fun updateSignedInAccountData(userID: String, emailID: String) {
        store.dispatch(PreferencesAction.UpdateStoredEmailForUserIDAction(userID = userID, emailID = emailID))
    }

    fun getUserIDForNoteLocalID(noteLocalId: String) = store.getUserIDForNoteLocalID(noteLocalId)
    fun getUserIDForNoteReferenceLocalID(noteReferenceLocalID: String): String = store.getUserIDForNoteReferenceLocalID(noteReferenceLocalID)

    public fun getColorForAddedNote(): Color {
        val ctx = context.get() ?: return Color.getDefault()
        val cache = NoteColorSharedPreferences(ctx)
        return cache.getNoteColorPreference() ?: Color.getDefault()
    }

    fun createdByApp(): String = store.createdByApp

    fun notifyAccountChanged(identityMetaData: IdentityMetaData) {
        store.dispatch(UIAction.AccountChanged(identityMetaData.userID))

        notesLogger?.d(LOG_TAG, "Account changed to AccountType: ${identityMetaData.accountType}")
        notesLogger?.recordTelemetry(EventMarkers.AccountSwitchTriggered)
    }

    // Request Priority
    /**
     * RequestPriority needs to be set to foreground when user can see Sticky Notes UI, and back to background
     * when Sticky Notes UI is not visible (user switches to a different area of the app, app goes into
     * background, etc.)
     */
    fun setRequestPriority(requestPriority: RequestPriority) {
        if (this.requestPriority == RequestPriority.background && requestPriority == RequestPriority.foreground) {
            store.dispatch(AuthenticatedSyncRequestAction.RemoteChangedDetected(currentUserID))
            if (experimentFeatureFlags.noteReferencesSyncEnabled) {
                store.dispatch(AuthenticatedSyncRequestAction.RemoteNoteReferencesChangedDetected(currentUserID))
            }
            if (experimentFeatureFlags.samsungNotesSyncEnabled) {
                store.dispatch(AuthenticatedSyncRequestAction.SamsungNotesChangedDetected(currentUserID))
            }
            if (experimentFeatureFlags.meetingNotesSyncEnabled) {
                store.dispatch(AuthenticatedSyncRequestAction.MeetingNotesChangedDetected(currentUserID))
            }
        }
        this.requestPriority = requestPriority
    }

    fun refreshNoteReferences(remoteNotes: List<NoteReference>, userID: String) {
        val changes = calculateNoteReferencesListChanges(
            store.state.getNoteReferencesCollectionForUser(userID),
            remoteNotes
        )
        if (!changes.isEmpty()) {
            store.dispatch(NoteReferenceAction.ApplyChanges(changes, userID, null, isLocalChange = true))
        }
    }

    fun pinNoteReferences(noteReferencesList: List<NoteReference>) {
        for (user in getAllUsers()) {
            store.dispatch(NoteReferenceAction.PinNoteReference(noteReferencesList, user))
        }
    }

    fun tryDownloadMediaOnBackPress(pageId: String) {
        val noteReference = this.currentNoteReferences.find { ImageUtils.extractPageId(it.clientUrl) == pageId }
        if (noteReference?.media != null && noteReference.media.isNotEmpty() && noteReference.media[0].localImageUrl.isNullOrEmpty()) {
            downloadMediaFromCache(noteReference)
        }
    }

    fun downloadMediaFromCache(noteReference: NoteReference) {
        imageFromCache(noteReference.media?.get(0)?.mediaID, noteReference.clientUrl, noteReference.localId, noteReference.media?.get(0)?.mediaType?.substringAfter('/'))
    }

    fun unPinNoteReferences(noteReferencesList: List<NoteReference>) {
        for (user in getAllUsers()) {
            store.dispatch(NoteReferenceAction.UnpinNoteReference(noteReferencesList, user))
        }
    }

    fun pinNotes(notesList: List<Note>) {
        for (user in getAllUsers()) {
            store.dispatch(UpdateAction.PinNotes(notesList, user))
        }
    }

    fun unpinNotes(notesList: List<Note>) {
        for (user in getAllUsers()) {
            store.dispatch(UpdateAction.UnpinNotes(notesList, user))
        }
    }

    fun getEmailForUserID(userID: String) = store.state.getUserStateForUserID(userID).email

    fun getEmailForNote(note: Note) = getEmailForUserID(store.state.userIDForLocalNoteID(note.localId))

    fun finishActionMode() {
        store.dispatch(UIAction.FinishActionModeOnFeed())
    }

    // Local NoteReference Change Signals
    fun onPageChangedLocalSignal(
        userId: String,
        pageLocalId: String,
        pageSourceId: NoteRefSourceId?,
        metaData: PageChangeSignalMetaData
    ) {
        serialDispatcher.dispatchActionTask {
            val changes = translatePageChangedSignal(
                localNotes = getNoteReferencesForUser(userId),
                pageLocalId = pageLocalId,
                pageSourceId = pageSourceId,
                metaData = metaData,
                isDebugMode = store.isDebugMode
            )
            return@dispatchActionTask dispatchLocalChangesAction(changes, userId)
        }
    }

    fun onPageDeletedLocalSignal(
        userId: String,
        pageLocalId: String,
        pageSourceId: NoteRefSourceId?
    ) {
        serialDispatcher.dispatchActionTask {
            val changes = translatePageDeletedSignal(
                localNotes = getNoteReferencesForUser(userId),
                pageLocalId = pageLocalId,
                pageSourceId = pageSourceId,
                isDebugMode = store.isDebugMode
            )
            return@dispatchActionTask dispatchLocalChangesAction(changes, userId)
        }
    }

    fun onSectionDeletedLocalSignal(
        userId: String,
        sectionLocalId: String,
        sectionSourceId: NoteRefSourceId?
    ) {
        serialDispatcher.dispatchActionTask {
            val changes = translateSectionDeletedSignal(getNoteReferencesForUser(userId), sectionLocalId, sectionSourceId)
            return@dispatchActionTask dispatchLocalChangesAction(changes, userId)
        }
    }

    fun onSectionChangedLocalSignal(
        userId: String,
        sectionLocalId: String,
        sectionSourceId: NoteRefSourceId?,
        sectionName: String
    ) {
        serialDispatcher.dispatchActionTask {
            val changes = translateSectionChangedSignal(getNoteReferencesForUser(userId), sectionLocalId, sectionSourceId, sectionName)
            return@dispatchActionTask dispatchLocalChangesAction(changes, userId)
        }
    }

    // create new note if page is not found
    // Only update page ids (without accepting the page signal metaData) if conflicting(same) LMTs
    // usage:
    // This is used by OneNote host app to supply SDK with page signal for recent pages (to compensate for 28day feed expiry)
    // The signal may not be complete, in the sense that it ma not contain page preview, correct title...
    // So, we only want to avoid accepting signal.metaData whenever possible.
    // Following cases arise:
    // 1. note corresponding to signal is not found in SDK -> create a new note using all signal details
    // 2. note is found with LMT same as that of signal -> just update the local Ids without updating other fields like title
    // 3. note is found with LMT older than signal's -> accept all signal data
    //    see https://github.com/microsoft-notes/notes-android-sdk/pull/1673 for more details
    fun onAppendPageIfNeededSignal(
        userId: String,
        pageLocalId: String,
        pageSourceId: NoteRefSourceId?,
        metaData: PageChangeSignalMetaData
    ) {
        serialDispatcher.dispatchActionTask {
            val changes = translateAppendPageIfNeededSignal(
                localNotes = getNoteReferencesForUser(userId),
                pageLocalId = pageLocalId,
                pageSourceId = pageSourceId,
                metaData = metaData,
                isDebugMode = store.isDebugMode
            )
            return@dispatchActionTask dispatchLocalChangesAction(changes, userId)
        }
    }

    private fun dispatchLocalChangesAction(changes: NoteRefLocalChanges, userID: String): Promise<State> {
        if (!changes.isEmpty()) {
            val applyChangesAction = NoteReferenceAction.ApplyChanges(
                changes = NoteReferenceChanges(
                    toCreate = changes.toCreate,
                    toReplace = changes.toReplace,
                    toDelete = changes.toDelete
                ),
                userID = userID,
                deltaToken = null,
                isLocalChange = true
            )

            if (changes.toMarkAsDeleted.isEmpty()) {
                return store.dispatch(applyChangesAction)
            } else {
                val markAsDeletedAction = NoteReferenceAction.MarkAsDeleted(changes.toMarkAsDeleted, userID)
                return store.dispatch(CompoundAction(applyChangesAction, markAsDeletedAction))
            }
        }

        val promise = PromiseImpl<State>()
        promise.resolve(store.state)
        return promise
    }

    fun cleanUpStaleLocalOnlyNoteReferences(pageLocalIdsGroupedByUserId: Map<String, List<String>>) {
        pageLocalIdsGroupedByUserId.forEach {
            val changes = calculateChangesForLocalOnlyNoteRefsCleanUp(
                localNotes = getNoteReferencesForUser(it.key),
                validPageLocalIds = it.value
            )
            dispatchLocalChangesAction(changes, it.key)
        }
    }

    fun enableActionsOnNoteReferences(enable: Boolean) {
        isActionsEnabledOnNoteReferences = enable
        store.dispatch(UIAction.InvalidateActionModeOnFeed())
    }

    fun disableNoteOptionsFeedbackButton(disable: Boolean) {
        disableNoteOptionsFeedbackButton = disable
    }

    // dispatching UI bindings actions
    internal fun sendAddNoteAction(note: Note) =
        store.dispatch(UIAction.AddNewNote(note), clientThread)

    fun sendEditNoteAction(note: Note) =
        store.dispatch(UIAction.EditNote(note), clientThread)

    internal fun sendEditSearchNoteAction(note: Note) =
        store.dispatch(UIAction.EditSearchNote(note), clientThread)

    internal fun sendEditFeedSearchNoteAction(note: Note) =
        store.dispatch(UIAction.EditFeedSearchNote(note), clientThread)

    internal fun sendSwipeToRefreshStartedAction() =
        store.dispatch(UIAction.SwipeToRefreshStarted(), clientThread)

    internal fun sendSwipeToRefreshCompletedAction() =
        store.dispatch(UIAction.SwipeToRefreshCompleted(), clientThread)

    internal fun sendFeedSwipeToRefreshStartedAction() =
        store.dispatch(UIAction.FeedSwipeToRefreshStarted(), clientThread)

    internal fun sendNoteOptionsDismissedAction() =
        store.dispatch(UIAction.NoteOptionsDismissed(), clientThread)

    internal fun sendNoteOptionsNoteDeletedAction() =
        store.dispatch(UIAction.NoteOptionsNoteDeleted(), clientThread)

    internal fun sendNoteOptionsSendFeedbackAction() =
        store.dispatch(UIAction.NoteOptionsSendFeedback(), clientThread)

    internal fun sendNoteOptionsColorPickedAction() =
        store.dispatch(UIAction.NoteOptionsColorPicked(), clientThread)

    internal fun sendNoteOptionsSearchInNoteAction() =
        store.dispatch(UIAction.NoteOptionsSearchInNote(), clientThread)

    internal fun sendNoteOptionsNoteSharedAction() =
        store.dispatch(UIAction.NoteOptionsNoteShared(), clientThread)

    internal fun sendImageCompressionCompletedAction(successful: Boolean) =
        store.dispatch(UIAction.ImageCompressionCompleted(successful), clientThread)

    internal fun sendNoteFirstEditedAction() =
        store.dispatch(UIAction.NoteFirstEdited(), clientThread)

    internal fun sendFeedSourceFilterSelectedAction(source: FeedSourceFilterOption) =
        store.dispatch(UIAction.FeedSourceFilterSelected(source), clientThread)

    internal fun sendComprehensiveFeedFilterSelectedAction(feedFilters: FeedFilters, scrollToTop: Boolean) =
        store.dispatch(UIAction.ComprehensiveFeedSourceFilterSelected(feedFilters, scrollToTop), clientThread)

    internal fun sendFeedNoteOrganiseAction(note: Note) =
        store.dispatch(UIAction.FeedNoteOrganiseAction(note), clientThread)

    internal fun sendRequestClientAuthAction(userID: String) {
        store.dispatch(UIAction.RequestClientAuthAction(userID))
    }

    internal fun sendDeletedMultipleNotesAction() =
        store.dispatch(UIAction.DeletedMultipleNotes(), clientThread)

    fun undoRedoInNotesEditText(isRedoAction: Boolean = false) =
        store.dispatch(UIAction.UndoRedoInNotesEditText(isRedoAction), clientThread)
}
