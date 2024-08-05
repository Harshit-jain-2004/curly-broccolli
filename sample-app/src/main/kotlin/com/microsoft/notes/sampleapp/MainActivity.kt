package com.microsoft.notes.sampleapp

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.text.InputType
import android.text.SpannableString
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager
import com.microsoft.notes.models.AccountType
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.extensions.markAsDeleteAndDeleteIfEmpty
import com.microsoft.notes.sampleapp.auth.AuthManager
import com.microsoft.notes.sampleapp.auth.IdentityProvider
import com.microsoft.notes.sampleapp.auth.handleTokenOnActivityResult
import com.microsoft.notes.sampleapp.com.microsoft.notes.sampleapp.locale.LocaleManager
import com.microsoft.notes.sampleapp.com.microsoft.notes.sampleapp.noteslist.FixedNotesListFragment
import com.microsoft.notes.sampleapp.com.microsoft.notes.sampleapp.utils.addNoteReferences
import com.microsoft.notes.sampleapp.com.microsoft.notes.sampleapp.utils.getDummyNoteReferencesList
import com.microsoft.notes.sampleapp.images.REQUEST_IMAGE_CAPTURE
import com.microsoft.notes.sampleapp.images.dispatchTakePhotoIntent
import com.microsoft.notes.sampleapp.noteslist.HorizontalNotesListFragment
import com.microsoft.notes.sampleapp.settings.SettingsFragment
import com.microsoft.notes.sampleapp.utils.Constants.Companion.STORAGE_PERMISSIONS_REQUEST_CODE
import com.microsoft.notes.sampleapp.utils.checkStoragePermissions
import com.microsoft.notes.sampleapp.utils.requestAudioPermissions
import com.microsoft.notes.sampleapp.utils.requestStoragePermissions
import com.microsoft.notes.sideeffect.ui.*
import com.microsoft.notes.store.AuthState
import com.microsoft.notes.store.SyncErrorState
import com.microsoft.notes.ui.extensions.filterDeletedAndFutureNotes
import com.microsoft.notes.ui.feed.FeedFragment
import com.microsoft.notes.ui.feed.HorizontalFeedFragment
import com.microsoft.notes.ui.feed.recyclerview.feeditem.NoteReferenceFeedItemComponent
import com.microsoft.notes.ui.note.edit.EditNoteFragment
import com.microsoft.notes.ui.note.options.NoteOptionsFragment
import com.microsoft.notes.ui.notereference.OneNotePageFragment
import com.microsoft.notes.ui.noteslist.NotesListFragment
import com.microsoft.notes.ui.search.FeedSearchFragment
import com.microsoft.notes.ui.search.SearchFragment
import com.microsoft.notes.utils.logging.SNMarker
import com.microsoft.notes.utils.utils.IdentityMetaData
import kotlinx.android.synthetic.main.main_layout.*
import com.microsoft.notes.ui.note.ink.InkState
import com.microsoft.notes.utils.utils.Constants.INTENT_DATA_ID_PARAM
import com.microsoft.notes.utils.utils.Constants.ACTION_OPEN_SN_FROM_DEEPLINK
import com.microsoft.notes.ui.note.reminder.ReminderFragment
import com.microsoft.notes.utils.utils.Constants
import java.io.File
import com.microsoft.notes.sampleapp.utils.Constants as SampleAppConstants

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback, NoteReferenceFeedItemComponent.Callbacks, FeedFragment.ActionModeChangeListener {

    private val notesListFragment: NotesListFragment by lazy { NotesListFragment() }
    private val editNoteFragment: EditNoteFragment by lazy {
        val fragment = EditNoteFragment()
        fragment.onUndoStackChanged = { count -> onUndoStackChanged(count) }
        fragment.onTextUndoChanged = { isEnabled -> onTextUndoChanged(isEnabled) }
        fragment.onTextRedoChanged = { isEnabled -> onTextRedoChanged(isEnabled) }
        fragment
    }
    private val noteOptionsFragment: NoteOptionsFragment by lazy { NoteOptionsFragment() }
    private val reminderFragment : ReminderFragment by lazy { ReminderFragment() }
    private val searchFragment: SearchFragment by lazy { SearchFragment() }
    private val feedSearchFragment: FeedSearchFragment by lazy { FeedSearchFragment() }
    private val settingsFragment: SettingsFragment by lazy { SettingsFragment() }
    private var currentSelectedAccount: String = ""
    private val feedFragment: FeedFragment by lazy {
        val fragment = FeedFragment()
        fragment.noteReferenceCallbacks = this
        fragment.actionModeChangeListener = this
        fragment
    }
    private val onenotePageFragment: OneNotePageFragment by lazy { OneNotePageFragment() }
    private val horizontalFeedFragment: HorizontalFeedFragment by lazy {
        val fragment = HorizontalFeedFragment()
        fragment.noteReferenceCallbacks = this
        fragment
    }

    private val horizontalNotesListFragment: HorizontalNotesListFragment by lazy { HorizontalNotesListFragment() }
    private val fixedNotesListFragment: FixedNotesListFragment by lazy { FixedNotesListFragment() }
    private var currentNoteId: String? = null
    private val preferenceManager: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val transitionCoordinatorImpl: TransitionCoordinator by lazy {
        if (preferenceManager.getBoolean(getString(R.string.transitions_key), true))
            compatibleTransitionCoordinator
        else
            TransitionCoordinator(FragmentTransaction.TRANSIT_NONE)
    }
    private var textUndoMenuItem: MenuItem? = null
    private var textRedoMenuItem: MenuItem? = null
    private var photoUri: Uri? = null
    private var goBackToSearch = false
    private var goBackToFeedSearch = false
    private var goBackToHorizontalList = false
    private var goBackToFixedList = false
    private var goBackToFeed = false
    private var goBackToHorizontalFeed = false


    private val signedInAccountsDropDownAdapter: ArrayAdapter<String> by lazy {
        ArrayAdapter(
                instance,
                android.R.layout.simple_spinner_dropdown_item,
                IdentityProvider.getInstance().getSignedInEmails(this))
    }
    private var instance: MainActivity = this

    private val authChanges: AuthChanges by lazy {
        object : AuthChanges {
            override fun authChanged(auth: AuthState, userID: String) {
                if (NotesLibrary.getInstance().experimentFeatureFlags.multiAccountEnabled) {
                    when (auth) {
                        AuthState.AUTHENTICATED -> {
                            runOnUiThread {
                                signedInAccountsDropDownAdapter.clear()
                                signedInAccountsDropDownAdapter.addAll(
                                        IdentityProvider.getInstance().getSignedInEmails(instance))
                            }
                        }
                        AuthState.UNAUTHENTICATED -> {
                            runOnUiThread {
                                signedInAccountsDropDownAdapter.remove(userID)
                            }
                        }
                    }
                }
            }

            override fun accountInfoForIntuneProtection(databaseName: String, userID: String,
                                                        accountType: AccountType) {
            }

            override fun onRequestClientAuth(userID: String) {
                //invoke password prompt for userID here
            }
        }
    }

    private val notesListBindings: NotesList by lazy {
        object : NotesList {
            override fun noteFromListTapped(note: Note) {
                runOnUiThread {
                    if (horizontalNotesListFragment.isAdded) {
                        goBackToHorizontalList = true
                    }
                    if (fixedNotesListFragment.isAdded) {
                        goBackToFixedList = true
                    }
                    if (feedFragment.isAdded) {
                        goBackToFeed = true
                    }
                    if(horizontalFeedFragment.isAdded) {
                        goBackToHorizontalFeed = true
                    }
                    toEditNoteFragment(note)
                }
            }

            override fun addNewNoteTapped(note: Note) {
                runOnUiThread {
                    toEditNoteFragment(note)
                }
            }

            override fun manualSyncStarted() {
                //no action
            }

            override fun manualSyncCompleted() {
                //no action
            }
        }
    }

    private val editNoteBindings: EditNote by lazy {
        object : EditNote {
            override fun addPhotoTapped() {
                //TODO for now we just use the camera to take the photo, it would be nice to have the gallery too
                photoUri = dispatchTakePhotoIntent()
            }

            override fun captureNoteTapped() {
                // no action
            }

            override fun microPhoneButtonTapped() {
                // no action
            }

            override fun imageCompressionCompleted(successful: Boolean) {
                //no action
            }

            override fun noteFirstEdited() {
                //no action
            }

            override fun scanButtonTapped() {
                //no action
            }
        }
    }

    private val noteOptionsBindings: NoteOptions by lazy {
        object : NoteOptions {
            override fun noteOptionsDismissed() {
                //no action
            }

            override fun noteOptionsSendFeedbackTapped() {
                //no action
            }

            override fun noteOptionsNoteDeleted() {
                //The callbacks are called from the sdk in a different thread than the UI, hence this thread change
                runOnUiThread {
                    when {
                        goBackToSearch -> backToSearchFragment()
                        goBackToFeedSearch -> backToFeedSearchFragment()
                        else -> backToNotesListFragment()
                    }
                }
            }

            override fun noteOptionsColorPicked() {
                // no action
            }

            override fun noteOptionsNoteShared() {
                // no action
            }

            override fun noteOptionsSearchInNote() {
                runOnUiThread {
                    setupSearchInNoteView()
                }
            }
        }
    }

    private val actionmodeBindings: NoteActionMode by lazy {
        object : NoteActionMode {
            override fun organiseNote(note: Note) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Action Mode organise " + note.localId,
                            Toast.LENGTH_LONG).show()
                }
            }

        }
    }

    private val searchBindings: Search by lazy {
        object : Search {
            override fun noteFromSearchTapped(note: Note) {
                runOnUiThread {
                    goBackToSearch = true
                    toEditNoteFragment(note)
                }
            }
        }
    }

    private val feedSearchBindings: FeedSearch by lazy {
        object : FeedSearch {
            override fun noteFromFeedSearchTapped(note: Note) {
                runOnUiThread {
                    goBackToFeedSearch = true
                    toEditNoteFragment(note)
                }
            }
        }
    }

    private val notificationBindings: Notifications by lazy {
        object : Notifications {
            override fun upgradeRequired() {
                // no action
            }

            override fun syncErrorOccurred(error: Notifications.SyncError, userID: String) {
                settingsFragment.setNoteSessionIdForSyncErrors(NotesLibrary.getInstance().getNotesSessionID(userID))
            }
        }
    }

    private val syncStateUpdatesBindings: SyncStateUpdates by lazy {
        object : SyncStateUpdates {
            override fun remoteNotesSyncStarted(userID: String) {
                // no action
            }

            override fun remoteNotesSyncErrorOccurred(errorType: SyncStateUpdates.SyncErrorType, userID: String) {
                settingsFragment.setNoteSessionIdForSyncErrors(NotesLibrary.getInstance().getNotesSessionID(userID))
            }

            override fun remoteNotesSyncFinished(successful: Boolean, userID: String) {
                if (!successful)
                    settingsFragment.setNoteSessionIdForSyncErrors(
                            NotesLibrary.getInstance().getNotesSessionID(userID))
            }

            override fun accountSwitched(syncErrorState: SyncErrorState, userID: String) {
                // no action
            }
        }
    }

    private val noteReferencesSyncRequestBinding: NoteReferencesSyncRequest by lazy {
        object : NoteReferencesSyncRequest {
            override fun syncNoteReferences(userID: String) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Syncing Note Reference", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val samsungNotesSyncRequestBinding: SamsungNotesSyncRequest by lazy {
        object : SamsungNotesSyncRequest {
            override fun syncSamsungNotes(userID: String) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Syncing Samsung Notes", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val feedSwipeToRefreshSyncState: FeedSwipeToRefreshSync by lazy {
        object : FeedSwipeToRefreshSync {
            override fun feedSyncStarted() {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Sync started in feed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun feedSyncCompleted() {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocaleManager.setLocaleFromPreferences(this, preferenceManager)

        setContentView(R.layout.main_layout)
        setSupportActionBar(mainToolbar)
        setupSearchView()
        setPlaceholders()

        supportFragmentManager.beginTransaction().add(R.id.fragment, feedFragment).commit()

        requestAuthToken()

        val experimentFeatureFlags = NotesLibrary.getInstance().experimentFeatureFlags
        if (experimentFeatureFlags.multiAccountEnabled) {
            setupSignedInAccountsDropDown()
        }

        requestStoragePermissions(this)

        requestAudioPermissions(this)

        //fixme This first call to get all the notes it could be not easier to guess, also, the need to update
        //our list fragment collection ui afterwards
        NotesLibrary.getInstance().setCurrentUserID(currentSelectedAccount)
        NotesLibrary.getInstance().fetchNotes(currentSelectedAccount)
        NotesLibrary.getInstance().startPolling(currentSelectedAccount)

        SNMarker.addListener(SampleAppSNMarkerListener())
    }

    override fun onResume() {
        handleOpenFromDeepLinkIfApplicable()
        super.onResume()
    }

    private fun handleOpenFromDeepLinkIfApplicable(){
        if (NotesLibrary.getInstance().experimentFeatureFlags.enableReminderInNotes &&
            intent != null && intent.action == ACTION_OPEN_SN_FROM_DEEPLINK) {
            val data = intent.data
            if (data != null) {
                val noteId = data.getQueryParameter(INTENT_DATA_ID_PARAM)

                if (noteId != null) {
                    val note = NotesLibrary.getInstance().getNoteById(noteId)
                    if (note != null) {
                        NotesLibrary.getInstance().sendEditNoteAction(note)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSIONS_REQUEST_CODE) {
            checkStoragePermissions(this, grantResults)
        }
    }

    private fun setPlaceholders() {
        with(application as StickyNotesApplication) {
            searchFragment.setPlaceholder(
                    image = R.drawable.sn_empty_state_search,
                    imageContentDescription = getString(R.string.empty_search_image_description),
                    title = SpannableString(getString(R.string.empty_search_title)),
                    titleContentDescription = getString(R.string.empty_search_title_description),
                    titleStyle = if (isDarkThemeEnabled) android.R.style.TextAppearance_Holo_Medium_Inverse else
                        android.R.style.TextAppearance_Holo_Medium,
                    subtitle = SpannableString(getString(R.string.empty_search_subtitle)),
                    subtitleContentDescription = getString(R.string.empty_search_subtitle_description),
                    subtitleStyle = if (isDarkThemeEnabled) android.R.style.TextAppearance_Small_Inverse else
                        android.R.style.TextAppearance_Small)

            notesListFragment.setPlaceholder(
                    image = R.drawable.sn_empty_state_fishbowl,
                    imageContentDescription = getString(R.string.empty_note_list_image_description),
                    title = SpannableString(getString(R.string.empty_note_list_title)),
                    titleContentDescription = getString(R.string.empty_note_list_titledescription),
                    titleStyle = if (isDarkThemeEnabled) android.R.style.TextAppearance_Medium_Inverse else
                        android.R.style.TextAppearance_Medium,
                    subtitle = SpannableString(getString(R.string.empty_note_list_subtitle)),
                    subtitleContentDescription = getString(R.string.empty_note_list_subtitle_description),
                    subtitleStyle = if (isDarkThemeEnabled) android.R.style.TextAppearance_Small_Inverse else
                        android.R.style.TextAppearance_Small)
        }
    }

    private fun setupSignedInAccountsDropDown() {
        signedInAccounts.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val emailId = signedInAccounts.adapter.getItem(position) as String
                currentSelectedAccount = emailId
                IdentityProvider.getInstance().getAuthData(emailId)?.let {
                    NotesLibrary.getInstance().notifyAccountChanged(
                            IdentityMetaData(userID = it.email, email = it.email, accessToken = it.accessToken,
                                    accountType = it.accountType))
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        signedInAccounts.visibility = View.VISIBLE
        signedInAccounts.adapter = signedInAccountsDropDownAdapter
    }

    private fun requestAuthToken() {
        val authProvider = AuthManager.getAuthProvider(this)
        val signedInEmails: List<String> = authProvider.refreshAccounts(activity = this)

        if (signedInEmails.isEmpty()) {
            authProvider.login(activity = this,
                    loginHint = null,
                    onSuccess = { authenticationResult ->
                        currentSelectedAccount = authenticationResult?.userInfo?.userId ?: ""
                    })
        } else {
            // This is just a placeholder for now and will be used for multi account support.
            currentSelectedAccount = signedInEmails.get(0)
        }
        Toast.makeText(this, currentSelectedAccount, Toast.LENGTH_SHORT).show()
    }

    private fun setupSearchView() {
        searchView.setIconifiedByDefault(false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if(searchFragment.isAdded) {
                    searchFragment.setSearchText(newText)
                } else {
                    feedSearchFragment.setSearchText(newText)
                }
                return true
            }
        })
    }

    private fun setupSearchInNoteView() {
        searchInNote.visibility =  View.VISIBLE
        searchInNoteSearchView.setIconifiedByDefault(true)
        searchInNoteSearchView.setQuery("", false)
        searchInNoteSearchView.requestFocus()
        searchInNoteSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if(editNoteFragment.isAdded) {
                    editNoteFragment.searchInNote(newText)
                }
                return true
            }
        })

        searchActionNext.setOnClickListener {
            if(editNoteFragment.isAdded) {
                editNoteFragment.navigateSearchedOccurrence(previous = false)
            }
        }

        searchActionPrevious.setOnClickListener {
            if(editNoteFragment.isAdded) {
                editNoteFragment.navigateSearchedOccurrence(previous = true)
            }
        }

        // Set up the onCloseListener
        searchInNoteSearchView.setOnCloseListener {
            searchInNote.visibility = View.GONE
            true
        }
    }

    override fun onStart() {
        super.onStart()
        NotesLibrary.getInstance().addUiBindings(notesListBindings)
        NotesLibrary.getInstance().addUiBindings(editNoteBindings)
        NotesLibrary.getInstance().addUiBindings(noteOptionsBindings)
        NotesLibrary.getInstance().addUiBindings(searchBindings)
        NotesLibrary.getInstance().addUiBindings(feedSearchBindings)
        NotesLibrary.getInstance().addUiBindings(notificationBindings)
        NotesLibrary.getInstance().addUiBindings(syncStateUpdatesBindings)
        NotesLibrary.getInstance().addUiBindings(authChanges)
        NotesLibrary.getInstance().addUiBindings(actionmodeBindings)
        NotesLibrary.getInstance().addUiBindings(noteReferencesSyncRequestBinding)
        NotesLibrary.getInstance().addUiBindings(samsungNotesSyncRequestBinding)
        NotesLibrary.getInstance().addUiBindings(feedSwipeToRefreshSyncState)

        signedInAccountsDropDownAdapter.clear()
        signedInAccountsDropDownAdapter.addAll(IdentityProvider.getInstance().getSignedInEmails(instance))

        with(application as StickyNotesApplication) {
            if (!settingsFragment.isAdded) {
                incrementStickyNotesVisibilityCounter()
            }

            if (isDarkThemeEnabled) {
                darkTheme.backgroundColor.let {
                    val color = ContextCompat.getColor(this.applicationContext, it)
                    fragment.setBackgroundColor(color)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        NotesLibrary.getInstance().removeUiBindings(notesListBindings)
        NotesLibrary.getInstance().removeUiBindings(editNoteBindings)
        NotesLibrary.getInstance().removeUiBindings(noteOptionsBindings)
        NotesLibrary.getInstance().removeUiBindings(searchBindings)
        NotesLibrary.getInstance().removeUiBindings(feedSearchBindings)
        NotesLibrary.getInstance().removeUiBindings(notificationBindings)
        NotesLibrary.getInstance().removeUiBindings(syncStateUpdatesBindings)
        NotesLibrary.getInstance().removeUiBindings(authChanges)
        NotesLibrary.getInstance().removeUiBindings(actionmodeBindings)
        NotesLibrary.getInstance().removeUiBindings(noteReferencesSyncRequestBinding)
        NotesLibrary.getInstance().removeUiBindings(samsungNotesSyncRequestBinding)
        NotesLibrary.getInstance().removeUiBindings(feedSwipeToRefreshSyncState)

        if (!settingsFragment.isAdded) {
            (application as StickyNotesApplication).decrementStickyNotesVisibilityCounter()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NotesLibrary.getInstance().stopPolling(currentSelectedAccount)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> handleRequestImageCapture(resultCode)
            else -> {
                handleTokenResult(requestCode, resultCode, data)
            }
        }
    }

    private fun handleTokenResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleTokenOnActivityResult(requestCode, resultCode, data)
    }

    private fun handleRequestImageCapture(resultCode: Int) {
        if (resultCode == RESULT_OK) {
            val uri = photoUri
            val uriString = uri.toString()
            editNoteFragment.addPhotosToNote(listOf(uriString), deleteOriginal = true)
        }
        photoUri = null
    }

    //call needed for edit view images gallery image context menu actions
    override fun onContextMenuClosed(menu: Menu) {
        super.onContextMenuClosed(menu)
        editNoteFragment.onContextMenuClosed()
    }

    //NAVIGATION
    private fun updateToolbarForFragment() {
        mainToolbar.invalidate()
        invalidateOptionsMenu()

        //enable going back for all fragments apart from notes list
        supportActionBar?.setDisplayHomeAsUpEnabled(when {
            notesListFragment.isAdded -> {
                if (NotesLibrary.getInstance().experimentFeatureFlags.multiAccountEnabled)
                    signedInAccounts.visibility = View.VISIBLE

                false
            }
            else -> {
                if (NotesLibrary.getInstance().experimentFeatureFlags.multiAccountEnabled)
                    signedInAccounts.visibility = View.GONE

                mainToolbar.setNavigationOnClickListener {
                    onBackPressed()
                }
                true
            }
        })

        //adjust toolbar title
        mainToolbar.title = when {
            notesListFragment.isAdded -> getString(R.string.app_name)
            settingsFragment.isAdded -> getString(R.string.settings_menu_option)
            else -> ""
        }

        //settings do not have overflow actions
        if (settingsFragment.isAdded) {
            mainToolbar.hideOverflowMenu()
        }

        //adjust search view visibility
        when {
            searchFragment.isAdded || feedSearchFragment.isAdded -> {
                searchView.onActionViewExpanded()
                searchView.visibility = View.VISIBLE
            }
            else -> {
                searchView.onActionViewCollapsed()
                searchView.visibility = View.GONE
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        when {
            editNoteFragment.isAdded -> {
                menuInflater.inflate(R.menu.edit_note_menu, menu)
                var inkUndoMenuItem: MenuItem? = menu?.findItem(R.id.undo_ink_stroke)
                var inkMenuItem: MenuItem? = menu?.findItem(R.id.ink_stroke)
                var inkEraseMenuItem: MenuItem? = menu?.findItem(R.id.ink_erase_mode)
                var reminderMenuItem: MenuItem? = menu?.findItem(R.id.add_reminder_options)
                textUndoMenuItem = menu?.findItem(R.id.action_undo)
                textRedoMenuItem = menu?.findItem(R.id.action_redo)

                currentNoteId?.let {
                    val currentNote = NotesLibrary.getInstance().getNoteById(it)

                    if (currentNote?.isInkNote == true) {
                        inkEraseMenuItem?.isVisible = true
                        inkMenuItem?.isVisible = true
                        inkUndoMenuItem?.isVisible = !editNoteFragment.isUndoStackEmpty()
                        var inkingState = editNoteFragment.getInkState()
                        if (inkingState != null) {
                            updateIconTint(inkEraseMenuItem, inkingState == InkState.ERASE)
                            updateIconTint(inkMenuItem, inkingState == InkState.INK)
                        }
                    }

                    if (NotesLibrary.getInstance().experimentFeatureFlags.enableUndoRedoActionInNotes && currentNote?.isRichTextNote == true) {
                        textUndoMenuItem?.isVisible = true
                        textRedoMenuItem?.isVisible = true
                    }

                    if(NotesLibrary.getInstance().experimentFeatureFlags.enableReminderInNotes){
                        reminderMenuItem?.isVisible = true
                    }
                }
            }
            notesListFragment.isAdded -> {
                menu?.clear()
                menuInflater.inflate(R.menu.notes_list_menu, menu)

                if (NotesLibrary.getInstance().notesList.notesCollection.filterDeletedAndFutureNotes().isEmpty()) {
                    menu?.findItem(R.id.delete_all_notes)?.setVisible(false)
                    invalidateOptionsMenu()
                }
            }
            feedFragment.isAdded -> {
                menu?.clear()
                menuInflater.inflate(R.menu.feed_menu, menu)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.edit_note_options -> handleEditNoteOptions()
            R.id.add_reminder_options -> handleAddReminderOptions()
            R.id.ink_stroke -> setInkState(InkState.INK)
            R.id.undo_ink_stroke -> handleUndoInkStroke()
            R.id.ink_erase_mode -> setInkState(InkState.ERASE)
            R.id.search_menu_option -> handleSearchMenuOption()
            R.id.feed_search_menu_option -> handleFeedSearchMenuOption()
            R.id.settings_menu_option -> handleSettingsMenuOption()
            R.id.delete_all_notes -> handleDeleteAllNotesOption(item)
            R.id.feed_option -> handleFeedOption()
            R.id.horizontal_feed_option -> handleHorizontalFeedOption()
            R.id.horizontal_list_option -> handleHorizontalListOption()
            R.id.fixed_list_option -> handleFixedListOption()
            R.id.swap_theme_option -> handleSwapThemeOption()
            R.id.pick_locale_option -> handlePickLocaleOption()
            R.id.add_note_reference_menu_option -> handleAddNoteReference()
            R.id.add_sample_note_references_menu_option -> handleAddSampleNoteReferences()
            R.id.clear_note_references_menu_option -> handleClearNoteReferences()
            R.id.grid_view_menu_option -> switchLayoutToGridView()
            R.id.list_view_menu_option -> switchLayoutToListView()
            R.id.filter_sort_panel_menu_option -> showFilterAndSortPanel()
            R.id.action_undo -> handleUndoRedo(isRedoAction = false)
            R.id.action_redo -> handleUndoRedo(isRedoAction = true)
        }
        return true
    }

    private fun handleEditNoteOptions() {
        currentNoteId?.let { noteOptionsFragment.setCurrentNoteId(it) }
        noteOptionsFragment.show(supportFragmentManager, "note_options")
    }

    private fun handleAddReminderOptions() {
        currentNoteId?.let { reminderFragment.setCurrentNoteId(it) }
        reminderFragment.show(supportFragmentManager, "note_reminders")
    }

    private fun handleUndoInkStroke() {
        editNoteFragment.undoLastInkStroke()
    }

    private fun handleUndoRedo(isRedoAction: Boolean) {
        NotesLibrary.getInstance().undoRedoInNotesEditText(isRedoAction)
    }

    private fun setInkState(newInkState: InkState) {
        editNoteFragment.setInkState(newInkState, true)
    }

    private fun updateIconTint(menuItem: MenuItem?, isEnabled: Boolean?) {
        val drawable = menuItem?.icon
        if (isEnabled == null) return
        if (isEnabled) {
            drawable?.setColorFilter(
                    ContextCompat.getColor(applicationContext, R.color.sn_grey_dark),
                    PorterDuff.Mode.SRC_ATOP
            )
        } else {
            drawable?.setColorFilter(
                    ContextCompat.getColor(applicationContext, R.color.sn_grey_light),
                    PorterDuff.Mode.SRC_ATOP
            )
        }
    }

    private fun handleSearchMenuOption() {
        val transaction = beginFragmentTransaction()
        transitionCoordinatorImpl.notesListToSearch(
                this, notesListFragment,searchFragment, transaction)
        transaction.replace(R.id.fragment, searchFragment).commitNow()
    }

    private fun handleFeedSearchMenuOption() {
        val transaction = beginFragmentTransaction()
        transitionCoordinatorImpl.notesListToSearch(
                this, notesListFragment,feedSearchFragment, transaction)
        transaction.replace(R.id.fragment, feedSearchFragment).commitNow()
    }

    private fun handleSettingsMenuOption() {
        Handler().postDelayed({
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment, settingsFragment)
                    .commitNow()
            updateToolbarForFragment()
        }, 80)
        (application as StickyNotesApplication).decrementStickyNotesVisibilityCounter()
    }

    private fun handleDeleteAllNotesOption(item: MenuItem) {
        val notesList = NotesLibrary.getInstance().notesList.notesCollection

        notesList.forEach {
            NotesLibrary.getInstance().markAsDeleteAndDelete(
                    it.localId,
                    it.remoteData?.id)
        }

        item.setVisible(false)
        invalidateOptionsMenu()
    }

    private fun handleFeedOption() {
        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, feedFragment).commitNow()
        NotesLibrary.getInstance().enableActionsOnNoteReferences(enable = true)
    }

    private fun handleHorizontalFeedOption() {
        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, horizontalFeedFragment).commitNow()
    }

    private fun handleHorizontalListOption() {
        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, horizontalNotesListFragment).commitNow()
    }

    private fun handleFixedListOption() {
        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, fixedNotesListFragment).commitNow()
    }

    private fun handleSwapThemeOption() {
        with(application as StickyNotesApplication) {
            val currentTheme = NotesLibrary.getInstance().theme
            val nextTheme = if (currentTheme != darkTheme) darkTheme
            else lightTheme
            NotesLibrary.getInstance().setTheme(nextTheme)

            // Sample-app specific
            val nextColor = ContextCompat.getColor(applicationContext, nextTheme.backgroundColor)
            fragment.setBackgroundColor(nextColor)

            getSharedPreferences(SampleAppConstants.APP_PREFERENCES, Context.MODE_PRIVATE).edit()
                    .putBoolean(SampleAppConstants.DARK_MODE_ENABLED, nextTheme == darkTheme).apply()
        }
    }

    private fun handlePickLocaleOption() {
        LocaleManager.showLocalePicker(this, preferenceManager)
    }

    private fun handleAddNoteReference() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Note Reference")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Ok") { _: DialogInterface, _: Int ->
            val text = input.text.toString()
            val note = NoteReference(
                    type = "Sample App",
                    title = text,
                    previewText = "This is te text preview for note references. This preview comes below the tile and besides the preview image. This text might be too long intentionally."
            )
            val notesLibrary = NotesLibrary.getInstance()
            notesLibrary.refreshNoteReferences(notesLibrary.currentNoteReferences.addNoteReferences(
                    listOf(note)), currentSelectedAccount)
        }

        builder.show()
    }

    private fun handleAddSampleNoteReferences() {
        val notesLibrary = NotesLibrary.getInstance()
        notesLibrary.refreshNoteReferences(notesLibrary.currentNoteReferences.addNoteReferences(
                getDummyNoteReferencesList()), currentSelectedAccount)
    }

    private fun handleClearNoteReferences() {
        NotesLibrary.getInstance().refreshNoteReferences(emptyList(), currentSelectedAccount)
    }

    private fun switchLayoutToGridView() {
        NotesLibrary.getInstance().switchFeedLayoutToGridView()
    }

    private fun switchLayoutToListView() {
        NotesLibrary.getInstance().switchFeedLayoutToListView()
    }

    private fun showFilterAndSortPanel() {
        NotesLibrary.getInstance().showFilterAndSortPanel()
    }

    override fun onBackPressed() {
        searchInNote.visibility = View.GONE

        if (settingsFragment.isAdded) {
            (application as StickyNotesApplication).incrementStickyNotesVisibilityCounter()
        }

        if (editNoteFragment.isAdded) {
            currentNoteId?.let {
                val currentNote = NotesLibrary.getInstance().notesList.getNote(it)
                currentNote?.let { note ->
                    currentNoteId = null
                    NotesLibrary.getInstance().markAsDeleteAndDeleteIfEmpty(note)
                }
            }
        }

        when {
            editNoteFragment.isAdded -> if (goBackToSearch) {
                backToSearchFragment()
            } else if(goBackToFeedSearch) {
                backToFeedSearchFragment()
            } else if (goBackToHorizontalList) {
                backToHorizontalListFragment()
            } else if (goBackToFixedList) {
                backToFixedListFragment()
            } else if (goBackToFeed) {
                backToFeedFragment()
            } else if(goBackToHorizontalFeed) {
                backToHorizontalFeedFragment()
            } else {
                backToNotesListFragment()
            }
            searchFragment.isAdded || feedSearchFragment.isAdded -> backToNotesListFragmentFromSearch()
            settingsFragment.isAdded -> backToNotesListFragmentFromSettings()
            feedFragment.isAdded -> backToNotesListFragmentFromFeed()
            horizontalFeedFragment.isAdded -> backToNotesListFragmentFromFeed()
            horizontalNotesListFragment.isAdded -> backToNotesListFragmentFromHorizontalList()
            fixedNotesListFragment.isAdded -> backToNotesListFragmentFromFixedList()
            onenotePageFragment.isAdded -> backToFeedFragment()
            else -> super.onBackPressed()
        }
    }

    private fun toEditNoteFragment(note: Note) {
        try {
            //fixme the need of setting the currentNoteId before opening the editor
            // it is not really easy to find
            currentNoteId = note.localId
            editNoteFragment.setCurrentNoteId(note.localId)

            val transaction = beginFragmentTransaction()
            when {
                notesListFragment.isAdded -> {
                    transitionCoordinatorImpl.notesListToEditNote(
                        this, notesListFragment, editNoteFragment, transaction, note)
                }
                searchFragment.isAdded -> transitionCoordinatorImpl.searchToEditNote(
                    this, searchFragment, editNoteFragment, transaction, note)
            }
            transaction.replace(R.id.fragment, editNoteFragment).commitNow()
        }catch (e:Exception){
            e.printStackTrace()
        }

    }

    private fun toOneNotePageFragment(page: NoteReference) {
        onenotePageFragment.setCurrentNoteRefId(page.localId)
        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, onenotePageFragment).commitNow()
    }

    private fun beginFragmentTransaction(): FragmentTransaction {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.runOnCommit { updateToolbarForFragment() }
        return transaction
    }

    private fun backToNotesListFragment() {
        //currentNoteId might be null if note was empty and has been deleted
        currentNoteId?.let { notesListFragment.setCurrentNoteId(it) }

        closeNoteOptionsMenu()
        val transaction = beginFragmentTransaction()
        transitionCoordinatorImpl.editNoteToNotesList(
                this, editNoteFragment, notesListFragment, transaction)
        transaction.replace(R.id.fragment, notesListFragment).commitNow()
    }

    private fun backToNotesListFragmentFromSearch() {
        val transaction = beginFragmentTransaction()
        if(searchFragment.isAdded) {
            transitionCoordinatorImpl.searchToNotesList(
                    this, searchFragment, notesListFragment, transaction)
        } else {
            transitionCoordinatorImpl.searchToNotesList(
                    this, feedSearchFragment, notesListFragment, transaction)
        }
        transaction.replace(R.id.fragment, notesListFragment).commitNow()
    }

    private fun backToNotesListFragmentFromSettings() {
        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, notesListFragment).commitNow()
    }

    private fun backToNotesListFragmentFromFeed() {
        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, notesListFragment).commitNow()
    }

    private fun backToNotesListFragmentFromHorizontalList() {
        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, notesListFragment).commitNow()
    }

    private fun backToNotesListFragmentFromFixedList() {
        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, notesListFragment).commitNow()
    }

    private fun backToSearchFragment() {
        goBackToSearch = false

        //currentNoteId might be null if note was empty and has been deleted
        currentNoteId?.let { notesListFragment.setCurrentNoteId(it) }

        closeNoteOptionsMenu()
        val transaction = beginFragmentTransaction()
        transitionCoordinatorImpl.editNoteToSearch(
                this, editNoteFragment, searchFragment, transaction)
        transaction.replace(R.id.fragment, searchFragment).commitNow()
    }

    private fun backToFeedSearchFragment() {
        goBackToFeedSearch = false

        //currentNoteId might be null if note was empty and has been deleted
        currentNoteId?.let { notesListFragment.setCurrentNoteId(it) }

        closeNoteOptionsMenu()
        val transaction = beginFragmentTransaction()
        transitionCoordinatorImpl.editNoteToFeedSearch(
                this, editNoteFragment, feedSearchFragment, transaction)
        transaction.replace(R.id.fragment, feedSearchFragment).commitNow()
    }

    private fun backToHorizontalListFragment() {
        goBackToHorizontalList = false

        closeNoteOptionsMenu()
        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, horizontalNotesListFragment).commitNow()
    }

    private fun backToFixedListFragment() {
        goBackToFixedList = false

        closeNoteOptionsMenu()
        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, fixedNotesListFragment).commitNow()
    }

    private fun backToFeedFragment() {
        goBackToFeed = false

        closeNoteOptionsMenu()
        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, feedFragment).commitNow()
    }

    private fun backToHorizontalFeedFragment() {
        goBackToHorizontalFeed = false

        val transaction = beginFragmentTransaction()
        transaction.replace(R.id.fragment, horizontalFeedFragment).commitNow()
    }

    //It works when transitions are enabled
    private fun closeNoteOptionsMenu() {
        if (noteOptionsFragment.isVisible) {
            noteOptionsFragment.dismiss()
        }
    }

    private fun onUndoStackChanged(count: Int) {
        runOnUiThread {
            if (count <= 1) {
                invalidateOptionsMenu()
            }
        }
    }

    private fun onTextUndoChanged(isEnabled: Boolean) {
        if (!isEnabled) {
            textUndoMenuItem?.setIcon(R.drawable.sn_undo_button_disable)
        }
        else {
            textUndoMenuItem?.setIcon(R.drawable.sn_undo_button_enable)
        }
    }

    private fun onTextRedoChanged(isEnabled: Boolean) {

        if (!isEnabled) {
            textRedoMenuItem?.setIcon(R.drawable.sn_redo_button_disable)
        }
        else {
            textRedoMenuItem?.setIcon(R.drawable.sn_redo_button_enable)
        }
    }

    // NoteReferenceFeedItemComponent.Callbacks
    override fun onNoteItemClicked(note: NoteReference) {
        toOneNotePageFragment(note)
    }

    override fun shareNoteItem(note: NoteReference) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, "Note reference Shared", Toast.LENGTH_LONG).show()
            NotesLibrary.getInstance().finishActionMode()
        }
    }

    override fun deleteNoteItem(note: NoteReference) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, "Note reference deleted", Toast.LENGTH_LONG).show()
        }
    }

    override fun organiseNoteItem(note: NoteReference) {}
}
