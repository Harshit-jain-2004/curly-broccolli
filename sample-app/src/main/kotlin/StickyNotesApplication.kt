package com.microsoft.notes.sampleapp

import android.content.Context
import android.content.SharedPreferences
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import com.jakewharton.threetenabp.AndroidThreeTen
import com.microsoft.notes.noteslib.*
import com.microsoft.notes.sampleapp.auth.AuthManager
import com.microsoft.notes.sampleapp.logging.StickyNotesLogger
import com.microsoft.notes.sampleapp.logging.StickyNotesTelemetryLogger
import com.microsoft.notes.sampleapp.utils.Constants
import com.microsoft.notes.sync.RequestPriority
import com.microsoft.notes.utils.logging.Logger
import com.microsoft.notes.utils.logging.TelemetryLogger

class StickyNotesApplication : MultiDexApplication() {

    private val logger: Logger by lazy { StickyNotesLogger() }
    private val telemetryLogger: TelemetryLogger by lazy { StickyNotesTelemetryLogger() }
    private val preferenceManager: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    val lightTheme: NotesThemeOverride by lazy { NotesThemeOverride.default }
    val darkTheme: NotesThemeOverride by lazy {
        NotesThemeOverride(
                backgroundColor = R.color.sn_note_color_charcoal_medium,
                optionToolbarBackgroundColor = R.color.sn_note_body_color_charcoal_dark,
                optionIconColor = R.color.secondary_text_color_dark,
                optionTextColor = R.color.primary_text_color_dark,
                optionSecondaryTextColor = R.color.secondary_text_color_dark,
                optionIconBackgroundDrawable = R.drawable.sn_button_bg_dark,
                optionBottomSheetIconColor = R.color.sn_white,
                primaryAppColor = R.color.sn_primary_color_dark,
                feedBackgroundColor = R.color.feed_background_dark,
                dividerColor = R.color.divider_dark,
                searchHighlightForeground = R.color.sn_search_highlight_foreground_dark,
                searchHighlightBackground = R.color.sn_search_highlight_background_dark,
                feedTimestampColor = R.color.note_reference_timestamp_color_dark,
                stickyNoteCanvasThemeOverride = NotesThemeOverride.StickyNoteCanvasThemeOverride(
                        bodyColor = R.color.sn_note_body_color_charcoal_dark,
                        textAndInkColor = R.color.primary_text_color_dark ,
                        textHintColor = R.color.secondary_text_color_dark,
                        noteBorderColor = R.color.sn_note_border_color_dark,
                        metadataColor = R.color.sn_secondary_text_color_dark
                ),
                noteRefCanvasThemeOverride = NotesThemeOverride.NoteRefCanvasThemeOverride(
                        bodyColor = R.color.sn_note_body_color_charcoal_dark,
                        textAndInkColor = R.color.primary_text_color_dark,
                        secondaryTextColor = R.color.secondary_text_color_dark,
                        noteBorderColor = R.color.note_border_color_dark
                ),
                samsungNoteCanvasThemeOverride = NotesThemeOverride.SamsungNoteCanvasThemeOverride(
                        cardBg = R.color.samsung_note_card_bg_for_dark,
                        cardTitleColor = R.color.samsung_note_card_title_for_dark,
                        cardDetailsColor = R.color.samsung_note_card_details_for_dark,
                        contentBg = R.color.samsung_note_bg_for_dark,
                        cardBorderColor = R.color.samsung_note_border_for_dark,
                        contentColor = R.color.samsung_note_content_color_for_dark,
                        timeStampDividerColor = R.color.samsung_note_timestamp_divider_color_for_dark,
                        timeStampTextColor = R.color.samsung_note_timestamp_text_color_for_dark,
                        forceDarkModeInContentHTML = true
                ),
                sortFilterBottomSheetThemeOverride = NotesThemeOverride.SortFilterBottomSheetThemeOverride(
                        selectedOptionChipColor = R.color.bottom_sheet_selected_chip_color,
                        sortArrowUpDrawable = R.drawable.icon_arrowup,
                        sortArrowDownDrawable = R.drawable.icon_arrowdown
                )
        )
    }
    val isDarkThemeEnabled: Boolean by lazy {
        getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(Constants.DARK_MODE_ENABLED, true)
    }

    override fun onCreate() {
        super.onCreate()
        setupNotesLibrary()

        if(getExperimentalOptions().enableReminderInNotes){
            // Init Android Three Ten, used for date/time pickers
            AndroidThreeTen.init(this)
        }
    }

    private fun setupNotesLibrary() {
        //fixme we don't want to force clients to setupNotesLibrary a thread. @See{MainThread}
        val uiThread = MainThread(this)

        val configurationBuilder = NotesLibraryConfiguration.Builder
                .withUserAgent("StickyNotes/SampleAndroidApp/")
                .withClientThread(uiThread)
                .withLogger(logger)
                .withTelemetryLogger(telemetryLogger)
                .withMicrophoneButtonInEditOptions { /* do nothing on press */ }
                .withShouldEnableMicrophoneButton { true }
                .withShowEmailInEditNote(true)
                .withGetSSLConfigurationFromIntune { _, _ -> null }

        if (isDarkThemeEnabled) {
            configurationBuilder.withTheme(darkTheme)
        }

        val feedAuthProvider = object : FeedAuthProvider {
            override fun getAuthTokenForResource(userID: String, resource: String, onSuccess: (token: String) -> Unit, onFailure: () -> Unit) {
                AuthManager.getAuthProvider(this@StickyNotesApplication).acquireTokenForResource(resource, userID, onSuccess, onFailure)
            }
        }

        val experimentalOptions = getExperimentalOptions()
        configurationBuilder.withExperimentalOptions(experimentalOptions)
        configurationBuilder.withUiOptions(getUiOptions())
        configurationBuilder.withFeedAuthProvider(feedAuthProvider)

        val configuration = configurationBuilder.build(
                context = this,
                appName = getString(R.string.app_name) + " Android",
                isDebugMode = BuildConfig.DEBUG)

        NotesLibrary.init(configuration)
    }

    private fun getExperimentalOptions(): ExperimentFeatureFlags {
        val realtimeEnabled = preferenceManager.getBoolean(getString(R.string.realtime_key), true)
        val gsonParserEnabled = preferenceManager.getBoolean(getString(R.string.gson_parser_key), true)
        val multiAccountsSupportEnabled = preferenceManager.getBoolean(getString(R.string
                .multi_accounts_support_key), true)
        val inkEnabled = preferenceManager.getBoolean(getString(R.string.ink_key), true)
        val userNotificationsEnabled = preferenceManager.getBoolean(getString(R.string
                .show_user_notifications_key), true)
        val combinedNotesListForMultiAcount = preferenceManager.getBoolean(getString(R.string.combined_list_for_multiacount_key), true)
        val experimentFeatureFlagsBuilder = ExperimentFeatureFlagsBuilder()
        experimentFeatureFlagsBuilder.realTimeEnabled = realtimeEnabled
        experimentFeatureFlagsBuilder.gsonParserEnabled = gsonParserEnabled
        experimentFeatureFlagsBuilder.multiAccountEnabled = multiAccountsSupportEnabled
        experimentFeatureFlagsBuilder.inkEnabled = inkEnabled
        experimentFeatureFlagsBuilder.userNotificationEnabled = userNotificationsEnabled
        experimentFeatureFlagsBuilder.combinedListForMultiAccountEnabled = combinedNotesListForMultiAcount
        experimentFeatureFlagsBuilder.samsungNotesSyncEnabled = true
        experimentFeatureFlagsBuilder.meetingNotesSyncEnabled = true
        experimentFeatureFlagsBuilder.noteReferencesSyncEnabled = true
        experimentFeatureFlagsBuilder.samsungNotesSyncEnabled = true
        experimentFeatureFlagsBuilder.feedRealTimeEnabled = true
        experimentFeatureFlagsBuilder.hidePreviewTextFromNoteRefEnabled = false
        experimentFeatureFlagsBuilder.samsungNoteHtmlRenderingEnabled = true
        experimentFeatureFlagsBuilder.feedDifferentViewModesEnabled = true
        experimentFeatureFlagsBuilder.feedEnrichedPagePreviewsEnabled = true
        experimentFeatureFlagsBuilder.shareLinkToPageInActionModeEnabled = true
        experimentFeatureFlagsBuilder.feedRichTextPagePreviewsEnabled = true
        experimentFeatureFlagsBuilder.attachAppInstallLinkInShareEnabled = true
        experimentFeatureFlagsBuilder.appendAndroidAppInstallLinkInShareFlowEnabled = true
        experimentFeatureFlagsBuilder.feedFREFastSyncEnabled = true
        experimentFeatureFlagsBuilder.pinnedNotesEnabled = true
        experimentFeatureFlagsBuilder.hintTextInSNCanvasEnabled = true
        experimentFeatureFlagsBuilder.enableContextInNotes = true
        experimentFeatureFlagsBuilder.enableReminderInNotes = true
        experimentFeatureFlagsBuilder.enableScanButtonInImage = true
        experimentFeatureFlagsBuilder.enableUndoRedoActionInNotes = true
        return experimentFeatureFlagsBuilder.build()
    }

    private fun getUiOptions(): UiOptionFlags {
        val showAddNewNoteButton = preferenceManager.getBoolean(getString(R.string.show_add_note_button_key), true)
        val showAddNewInkNoteButton = preferenceManager.getBoolean(getString(R.string.show_add_ink_note_button_key), true)
        val showFeedbackButton = preferenceManager.getBoolean(getString(R.string.show_feedback_key), true)
        val showShareButton = preferenceManager.getBoolean(getString(R.string.show_share_key), true)
        val showClearCanvasButton = preferenceManager.getBoolean(getString(R.string.show_clear_canvas_key), true)
        val showNotesListErrorUi = preferenceManager.getBoolean(getString(R.string.show_notes_list_error_ui_key), true)
        val showActionMode = preferenceManager.getBoolean(getString(R.string.show_action_mode_on_long_press),true)
        val showSearchInNoteOption = preferenceManager.getBoolean(getString(R.string.show_search_in_notes_option),true)

        return UiOptionFlags(showNotesListAddInkNoteButton = showAddNewInkNoteButton,
                showNotesListAddNoteButton = showAddNewNoteButton,
                showListErrorUi = showNotesListErrorUi,
                hideNoteOptionsShareButton = !showShareButton,
                showClearCanvasButtonForInkNotes = showClearCanvasButton,
                hideNoteOptionsFeedbackButton = !showFeedbackButton,
                showActionModeOnFeed = showActionMode,
                showSearchInNoteOption = showSearchInNoteOption)
    }

    //Request priority handling
    private var stickyNotesVisibilityCounter = 0

    fun incrementStickyNotesVisibilityCounter() {
        stickyNotesVisibilityCounter++
        if (stickyNotesVisibilityCounter > 0) {
            NotesLibrary.getInstance().setRequestPriority(RequestPriority.foreground)
        }
    }

    fun decrementStickyNotesVisibilityCounter() {
        if (stickyNotesVisibilityCounter > 0) {
            stickyNotesVisibilityCounter--

            if (stickyNotesVisibilityCounter == 0) {
                NotesLibrary.getInstance().setRequestPriority(RequestPriority.background)
            }
        }
    }
}
