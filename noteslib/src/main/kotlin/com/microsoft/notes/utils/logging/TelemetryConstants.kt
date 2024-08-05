package com.microsoft.notes.utils.logging

const val NOTES_SDK: String = "NotesSDK"

enum class SeverityLevel {
    Error,
    Warning,
    Info,
    Verbose,
    Spam,
    Debug
}

enum class SamplingPolicy {
    Measure,
    Critical
}

enum class ExpirationDate {
    Perpetual,
    HostDefined
}

// Per spec: https://github.com/microsoft-notes/sticky-notes/blob/master/engineering/telemetry/privacy.md
enum class DiagnosticLevel {
    ReservedDoNotUse,
    BasicEvent,
    FullEvent,
    NecessaryServiceDataEvent,
    AlwaysOnNecessaryServiceDataEvent
}

/**
 * CostPriority decides whether an event will be transmitted on low cost networks(e.g. wifi), or also on high cost networks(e.g. 3G, 4G).
 **/
enum class CostPriority {
    NotSet,
    Normal,
    High
}

/**
 * PersistencePriority decides on how long the event data will be persisted on the device before being uploaded.
 **/
enum class PersistencePriority {
    NotSet,
    Normal,
    High
}

// Per spec: https://www.owiki.ms/wiki/Telemetry/Instrumentation/DataCategories
enum class DataCategory {
    NotSet,
    SoftwareSetup,
    ProductServiceUsage,
    ProductServicePerformance,
    DeviceConfiguration,
    InkingTypingSpeech
}

enum class EventMarkers(
    val samplingPolicy: SamplingPolicy,
    val isExportable: Boolean = false,
    val category: Categories = Categories.None,
    val costPriority: CostPriority = CostPriority.Normal,
    val persistencePriority: PersistencePriority = PersistencePriority.Normal,
    val diagnosticLevel: DiagnosticLevel = DiagnosticLevel.FullEvent,
    val dataCategory: DataCategory = DataCategory.ProductServiceUsage
) {
    // This will contain the Events that we want to log
    CreateNoteTriggered                     (SamplingPolicy.Critical, isExportable = true),
    AddImageTriggered                       (SamplingPolicy.Measure,  isExportable = true),
    DeleteNoteTriggered                     (SamplingPolicy.Critical, isExportable = true),
    DismissSamsungNoteTriggered             (SamplingPolicy.Critical, isExportable = true),
    DeleteNoteCancelled                     (SamplingPolicy.Critical, isExportable = true),
    DismissSamsungNoteCancelled             (SamplingPolicy.Critical, isExportable = true),
    LaunchBottomSheet                       (SamplingPolicy.Critical, isExportable = true),
    DismissBottomSheet                      (SamplingPolicy.Critical, isExportable = true),
    ShareNoteTriggered                      (SamplingPolicy.Critical, isExportable = true),
    ShareNoteSuccessful                     (SamplingPolicy.Critical),
    LaunchFilterUIBottomSheet               (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    DismissFilterUIBottomSheet              (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    FilterUITypeSet                         (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    ShareNoteFailed                         (SamplingPolicy.Critical),
    ManualSyncAction                        (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.NecessaryServiceDataEvent),
    NoteReferencesSyncStarted               (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    MeetingNotesSyncStarted                 (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    SamsungNotesSyncStarted                 (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    SyncFeatureFlag                         (SamplingPolicy.Critical),
    SyncJsonError                           (SamplingPolicy.Critical, diagnosticLevel = DiagnosticLevel.NecessaryServiceDataEvent, costPriority = CostPriority.High),
    SyncMalformedUrlException               (SamplingPolicy.Critical),
    AddImageCompressionError                (SamplingPolicy.Critical),
    FullScreenImageViewError                (SamplingPolicy.Critical),
    AccountSwitchTriggered                  (SamplingPolicy.Critical),
    FutureNoteEncountered                   (SamplingPolicy.Measure),
    SQLiteDiskIOException                   (SamplingPolicy.Critical),
    RealTimeSyncUnexpectedEvent             (SamplingPolicy.Critical),
    SNOutsideAppNoteTaking                  (SamplingPolicy.Critical),

    /*
     * SDK Events
     * Events which are needed by SDK for health monitoring and are fired within sdk itself
     */
    CommandTriggered                     (SamplingPolicy.Measure, isExportable = true),
    SyncRequestStarted                   (SamplingPolicy.Critical, diagnosticLevel = DiagnosticLevel.NecessaryServiceDataEvent, costPriority = CostPriority.High),
    SyncRequestCompleted                 (SamplingPolicy.Critical, diagnosticLevel = DiagnosticLevel.NecessaryServiceDataEvent, costPriority = CostPriority.High),
    SyncRequestAction                    (SamplingPolicy.Critical, diagnosticLevel = DiagnosticLevel.NecessaryServiceDataEvent, costPriority = CostPriority.High),
    SyncActiveStatus                     (SamplingPolicy.Critical),
    SyncCorruptedOutboundQueueBackup     (SamplingPolicy.Critical),
    SyncRealtimeAction                   (SamplingPolicy.Measure, diagnosticLevel = DiagnosticLevel.NecessaryServiceDataEvent, costPriority = CostPriority.High),
    OutboundQueueMigrationTriggered      (SamplingPolicy.Critical),
    InContentHyperlinkClicked            (SamplingPolicy.Critical, isExportable = true),
    NoteEditSessionComplete              (SamplingPolicy.Critical),
    StoredNotesOnBootException           (SamplingPolicy.Critical),
    PersistenceNotesConversionException  (SamplingPolicy.Measure),
    PersistenceNotesFetchException       (SamplingPolicy.Measure),
    ParsingDocumentJSONFailed            (SamplingPolicy.Measure),
    FilterSelectionsUpdated              (SamplingPolicy.Measure),
    SortSelectionUpdated                 (SamplingPolicy.Measure),
    PageNotOpenedInMultiSelectMode       (SamplingPolicy.Critical, diagnosticLevel = DiagnosticLevel.FullEvent),
    NoteReferenceRichPreviewFailed       (SamplingPolicy.Measure),

    /*
     * Shared telemetry events. Do not modify until a spec says so
     * https://github.com/microsoft-notes/sticky-notes/blob/master/engineering/telemetry/sharedEvents.md
     * - Do not add category
     */
    PersistedNotesOnBoot                    (SamplingPolicy.Measure),
    PersistedNoteDeltaTokensRetrieved       (SamplingPolicy.Measure),
    StoredNotesOnBoot                       (SamplingPolicy.Critical),
    SyncRequestFailed                       (SamplingPolicy.Critical, diagnosticLevel = DiagnosticLevel.NecessaryServiceDataEvent, costPriority = CostPriority.High),
    SyncSessionAction                       (SamplingPolicy.Critical, diagnosticLevel = DiagnosticLevel.NecessaryServiceDataEvent, costPriority = CostPriority.High),
    NoteCreated                             (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.BasicEvent, costPriority = CostPriority.High),
    NoteDeleted                             (SamplingPolicy.Critical, isExportable = true),
    SamsungNoteDismissed                    (SamplingPolicy.Critical, isExportable = true),
    NoteContentUpdated                      (SamplingPolicy.Critical, isExportable = true),
    NoteColorUpdated                        (SamplingPolicy.Critical, isExportable = true),
    NoteContentActionTaken                  (SamplingPolicy.Critical, isExportable = true),
    InkAddedToEmptyNote                     (SamplingPolicy.Critical, isExportable = true),
    DictationTriggered                      (SamplingPolicy.Critical, isExportable = true),
    ImageActionTaken                        (SamplingPolicy.Critical, isExportable = true),
    NoteViewed                              (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.BasicEvent, costPriority = CostPriority.High),
    SearchOpened                            (SamplingPolicy.Critical, isExportable = true),
    SearchInitiated                         (SamplingPolicy.Critical, isExportable = true),
    SearchResultSelected                    (SamplingPolicy.Critical, isExportable = true),
    AutoDiscoverApiHostRequest              (SamplingPolicy.Critical, isExportable = false, diagnosticLevel = DiagnosticLevel.NecessaryServiceDataEvent, costPriority = CostPriority.High),
    TappedOnFeedItem                        (SamplingPolicy.Critical, isExportable = true),
    FeedActionModeStarted                   (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    FeedActionModeFinished                  (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    SharePageLinkFeedItem                   (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    DeleteFeedItemStarted                   (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    DeleteFeedItemFinished                  (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    OrganizeFeedItemStarted                 (SamplingPolicy.Critical, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    DuplicatePageSourceId                   (SamplingPolicy.Measure, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    NoteReferenceSyncLatency                (SamplingPolicy.Measure, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    PinnedFeedItems                         (SamplingPolicy.Measure, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent),
    UnpinnedFeedItems                       (SamplingPolicy.Measure, isExportable = true, diagnosticLevel = DiagnosticLevel.FullEvent)
}

enum class Categories constructor(private val mCategory: String) {
    None(""),
    NotesSDK("NotesSDK"),
    HostApp("HostApp");

    fun mCategory(): String = mCategory
}

enum class NoteType {
    Text,
    Image,
    TextWithImage,
    Ink,
    Future,
    SamsungNote,
    Empty
}

enum class FeedItemType {
    OneNotePage,
    StickyNote,
    SamsungNote
}

enum class Percentile(val value: Int) {
    P25(25),
    P50(50),
    P75(75),
    P100(100),
}

enum class NoteColor {
    Grey,
    Yellow,
    Green,
    Pink,
    Purple,
    Blue,
    Charcoal;
}

enum class FormattingStyleType {
    Bold,
    Italic,
    Underline,
    Strikethrough,
    Bullet
}

enum class Mode {
    Edit,
    View
}

enum class FormattingToggleSource {
    System,
    Custom
}

enum class GenericError {
    UNKNOWN_ERROR
}

enum class SyncActiveStatus {
    ACTIVE,
    INACTIVE
}

enum class TelemetryProperties {
    InteractionType,
    Touch
}

enum class ImageTrigger {
    Canvas,
    Ribbon
}

enum class ManualSyncRequestStatus {
    Success,
    NetworkUnavailable,
    AutoDiscoverGenericFailure,
    EnvironmentNotSupported,
    UserNotFoundInAutoDiscover,
    Unauthenticated,
    SyncPaused,
    SyncFailure
}

enum class JsonParser {
    CustomJsonParserInit,
    GsonJsonParserInit,
    JsonParserException
}

object HostTelemetryKeys {
    const val TRIGGER_POINT = NOTES_SDK + "." + "TriggerPoint"
    const val ERROR_MESSAGE = NOTES_SDK + "." + "ErrorMessage"
    const val RESULT = NOTES_SDK + "." + "Result"
    const val TIME_TAKEN_IN_MS = NOTES_SDK + "." + "TimeTakenInMilliSeconds"
    const val HYPER_LINK_TYPE = NOTES_SDK + "." + "HyperLinkType"
}

object SyncActionType {
    const val START = "Start"
    const val FAIL = "Fail"
    const val STOP = "Stop"
    const val END = "End"
    const val COMPLETE = "Complete"
}

object AutoDiscoverApiHostRequestResult {
    const val SUCCESS = "Success"
    const val FAILURE = "Failure"
}

object NoteContentActionType {
    const val NOTE_INLINE_STYLE_TOGGLED = "NoteInlineStyleToggled"
    const val TEXT_ADDED_TO_EMPTY_NOTE = "TextAddedToEmptyNote"
    const val NOTE_BLOCK_STYLE_TOGGLED = "NoteBlockStyleToggled"
}

object ImageActionType {
    const val IMAGE_VIEWED = "ImageViewed"
    const val IMAGE_ADDED = "ImageAdded"
    const val IMAGE_ADDED_TO_EMPTY_NOTE = "ImageAddedToEmptyNote"
    const val IMAGE_DELETED = "ImageDeleted"
    const val IMAGE_ALT_TEXT_EDITED = "ImageAltTextEdited"
    const val IMAGE_ALT_TEXT_DELETED = "ImageAltTextDeleted"
}

class NotesSDKTelemetryKeys {
    object NoteProperty {
        const val NOTES_COUNT = "Notes"
        const val NOTE_LOCAL_ID = "NoteLocalId"
        const val NOTE_REMOTE_ID = "NoteRemoteId"
        const val NOTE_TYPE = "NoteType"
        const val NOTES_BY_TYPE = "NotesByType"
        const val NOTES_BY_COLOR = "NotesByColor"
        const val PARAGRAPH_LENGTH_PERCENTILES = "ParagraphLengthPercentiles"
        const val IMAGE_COUNT_PERCENTILES = "ImageCountPercentiles"
        const val FORMATTING_TOGGLE_SOURCE = "ToggleSource"
        const val IMAGE_LOCAL_ID = "ImageLocalId"
        const val IMAGE_REMOTE_ID = "ImageRemoteId"
        const val IMAGE_MIME_TYPE = "ImageMimeType"
        const val IMAGE_UNCOMPRESSED_SIZE = "ImageSize"
        const val IMAGE_COMPRESSED_SIZE = "ImageCompressedSize"
        const val IMAGE_ADDED_TO_EMPTY_NOTE = "NoteWasEmpty"
        const val FORMATTING_STYLE_TYPE = "StyleType"
        const val IS_NOTE_EMPTY = "Empty"
        const val COLOR_CHANGED_FROM = "From"
        const val COLOR_CHANGED_TO = "To"
        const val IMAGE_TRIGGER = "ImageTrigger"
        const val NOTE_HAS_IMAGES = "HasImages"
        const val COUNT = "Count"
        const val ACTION = "Action"
        const val FEED_ITEM_TYPE = "FeedItemType"
        const val NOTE_REFERENCE_COUNT = "PageCount"
        const val LARGE_NOTE_DELETION_DONE = "IsLargeNoteDeletionDone"
        const val COUNT_OF_PINNED_ITEMS = "CountOfPinnedItems"
        const val COUNT_OF_UNPINNED_ITEMS = "CountOfUnpinnedItems"
    }

    object FeedProperty {
        const val SYNC_LATENCY = "SyncLatency"
        const val MATCHED_NOTE_REFERENCE_SIZE = "MatchedNoteReferenceSize"
    }

    object FeedUIProperty {
        const val FILTER_UI_TYPE = "FilterUIType"
        const val FEED_SELECTED_ITEM_DEPTH = "FeedItemDepth"
    }

    object RequestProperty {
        const val HTTP_STATUS = "HttpStatus"
        const val DURATION_IN_MS = "DurationInMs"
    }

    object SyncProperty {
        const val OPERATION_TYPE = "Operation"
        const val ERROR_TYPE = "ErrorType"
        const val IS_RETRIED = "Retry"
        const val IS_SYNC_SCORE = "IsSyncScore"
        const val NEW_OPERATION = "NewOperation"
        const val SERVICE_CORRELATION_VECTOR = "ServiceCorrelationVector"
        const val SERVICE_X_CALCULATED_BE_TARGET = "ServiceXCalculatedBETarget"
        const val SERVICE_REQUEST_ID = "ServiceRequestId"
        const val ERROR_VALUE = "ErrorValue"
        const val SYNC_ACTIVE_STATUS = "SyncActive"
        const val EXCEPTION_TYPE = "exceptionType"
        const val IS_EXPORTABLE = "IsExportable"
        const val JSON_PARSER = "JSON_PARSER"
        const val URL = "Url"
        const val IS_REALTIME = "IsRealTime"
    }

    object AutoDiscoverProperty {
        const val EXISTS_IN_CACHE = "ExistsInCache"
        const val API_HOST = "ApiHost"
        const val ERROR_CODE = "ErrorCode"
        const val ERROR_MESSAGE = "ErrorMessage"
    }

    object SortingProperty {
        const val PREVIOUS_SORTING_CRITERION = "PreviousSortingCriterion"
        const val SELECTED_SORTING_CRITERION = "SelectedSortingCriterion"
        const val PREVIOUS_SORTING_STATE = "PreviousSortingState"
        const val SELECTED_SORTING_STATE = "SelectedSortingState"
    }

    object FilterProperty {
        const val ALL_SELECTION_STATUS = "IsAllFilterSelected"
        const val STICKYNOTES_SELECTION_STATUS = "IsStickyNotesFilterSelected"
        const val SAMSUNGNOTES_SELECTION_STATUS = "IsSamsungNotesFilterSelected"
        const val PAGES_SELECTION_STATUS = "IsNoteReferencesFilterSelected"
        const val ACCOUNT_FILTER_SELECTION_STATUS = "IsAccountsFilterSelected"
        const val FILTER_UPDATE_SOURCE = "SourceOfFilterUpdates"
        const val FILTER_SORT_PANEL = "FilterSortPanel"
        const val TOPBAR_FILTER_CHIPS = "TopBarFilterChips"
    }

    object NotesRole {
        const val IMAGE_INSERTED_FROM = "ImageInsertedFrom"
        const val FROM_CAMERA = "FromCamera"
    }
}
