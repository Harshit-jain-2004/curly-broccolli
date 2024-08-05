package com.microsoft.notes.ui.noteslist

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.extensions.filterDeletedAndFutureNotes
import com.microsoft.notes.ui.extensions.getHasImagesTelemetryValue
import com.microsoft.notes.ui.noteslist.placeholder.NotesListPlaceholderHelper
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteViewHolder
import com.microsoft.notes.ui.shared.StickyNotesFragment
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.HostTelemetryKeys
import com.microsoft.notes.utils.logging.NoteType
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.utils.logging.TelemetryProperties
import kotlinx.android.synthetic.main.sn_collapsible_message_bar.*
import kotlinx.android.synthetic.main.sn_notes_list_layout.*

/**
 * NotesListFragment can be used directly, however, if you need to extend it for your use-case, please include
 * the following in the parent layout:
 * <include layout="@layout/sn_notes_list_layout" />
 */
@Suppress("TooManyFunctions")
open class NotesListFragment : StickyNotesFragment(), FragmentApi {
    var notesList: VerticalNotesListComponent? = null

    companion object {
        private const val FRAGMENT_NAME = "NOTES_LIST"
        private const val LOG_TAG = "NotesListFragment"
    }

    private val presenter: NotesListPresenter by lazy {
        NotesListPresenter(this)
    }

    private val placeholderHelper by lazy { NotesListPlaceholderHelper() }

    /*
     * Client should set increaseListBottomPaddingForFab in its constructor to determine behavior for notes list bottom
     * padding. Notes list shows empty space of 120dp if client sets this property.
     */
    protected var increaseListBottomPaddingForFab = false

    private fun getBottomPaddingDimension(hasFab: Boolean): Int {
        return if (hasFab) {
            R.dimen.sn_noteslist_padding_bottom_with_fab_button
        } else {
            R.dimen.sn_noteslist_padding_bottom_without_fab_button
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = inflater.inflate(R.layout.sn_notes_list_layout, container, false)

        if (NotesLibrary.getInstance().uiOptionFlags.showNotesListAddNoteButton) {
            val buttonsList = layout.findViewById<LinearLayout>(R.id.notesButtons)
            inflater.inflate(R.layout.sn_new_note_button, buttonsList)
        }

        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notesList = view.findViewById(R.id.notesList)
        val newNoteFab = view.findViewById<FloatingActionButton>(R.id.newNoteFab)

        if (NotesLibrary.getInstance().uiOptionFlags.showNotesListAddNoteButton) {
            newNoteFab?.show()
            increaseListBottomPaddingForFab = true

            val newInkNoteFab = view.findViewById<FloatingActionButton>(R.id.newInkNoteFab)
            if (NotesLibrary.getInstance().uiOptionFlags.showNotesListAddInkNoteButton &&
                NotesLibrary.getInstance().experimentFeatureFlags.inkEnabled
            ) {
                newInkNoteFab?.show()
            } else {
                newInkNoteFab?.hide()
            }
        } else {
            newNoteFab?.hide()
        }

        if (increaseListBottomPaddingForFab) {
            notesList?.apply {
                val notesListBottomPadding = resources.getDimension(getBottomPaddingDimension(increaseListBottomPaddingForFab)).toInt()
                setBottomPadding(notesListBottomPadding)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupNotesList()
        setupAddNote()
    }

    private fun setupNotesList() {
        notesList?.callbacks = object : NotesListComponent.Callbacks() {

            override val noteForReturnTransition: Note?
                get() = getCurrentNote()

            override fun onNoteClicked(note: Note) {
                presenter.recordTelemetry(
                    EventMarkers.NoteViewed,
                    Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES, note.getHasImagesTelemetryValue()),
                    Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_LOCAL_ID, note.localId)
                )
                NotesLibrary.getInstance().sendEditNoteAction(note)
            }

            override fun onSwipeToRefresh() {
                NotesLibrary.getInstance().sendSwipeToRefreshStartedAction()
                presenter.refreshList()
            }
        }

        notesList?.swipeToRefreshEnabled = true
        notesList?.let { placeholderHelper.setNotesList(it) }
    }

    private fun recordNotesEvent(eventName: EventMarkers, vararg keyValuePairs: Pair<String, String>) {
        presenter.recordTelemetry(
            eventName, *keyValuePairs,
            Pair(
                HostTelemetryKeys.TRIGGER_POINT, FRAGMENT_NAME
            )
        )
    }

    private fun setupAddNote() {
        val newNoteButton = view?.rootView?.findViewById<FloatingActionButton>(R.id.newNoteFab)

        newNoteButton?.let {
            it.setOnClickListener {
                addNoteTriggered()
            }
            setupAddNoteAccessibility(it)
        }

        val newInkNoteButton = view?.rootView?.findViewById<FloatingActionButton>(R.id.newInkNoteFab)
        newInkNoteButton?.setOnClickListener { addInkNoteTriggered() }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun addNoteTriggered() {
        editNewNote()
        recordNotesEvent(
            EventMarkers.CreateNoteTriggered,
            Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_TYPE, NoteType.Text.name),
            Pair(TelemetryProperties.InteractionType.name, TelemetryProperties.Touch.name)
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun addInkNoteTriggered() {
        editNewInkNote()
        recordNotesEvent(
            EventMarkers.CreateNoteTriggered,
            Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_TYPE, NoteType.Ink.name),
            Pair(TelemetryProperties.InteractionType.name, TelemetryProperties.Touch.name)
        )
    }

    private fun setupAddNoteAccessibility(newNoteFab: FloatingActionButton) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            notesList?.recyclerViewID.let {
                if (it != null) {
                    newNoteFab.accessibilityTraversalBefore = it
                }
            }
        }
    }

    fun editNewNote(paragraphText: String? = null) {
        presenter.addNote(
            paragraphText, NotesLibrary.getInstance().currentUserID,
            addedNote = { note -> NotesLibrary.getInstance().sendAddNoteAction(note) }
        )
    }

    fun editNewInkNote() {
        presenter.addInkNote(
            NotesLibrary.getInstance().currentUserID,
            addedNote = { note -> NotesLibrary.getInstance().sendAddNoteAction(note) }
        )
    }

    /**
     * See NotesListComponent::setPlaceholder for documentation
     */
    fun setPlaceholder(
        image: Int? = null,
        imageContentDescription: String? = null,
        title: SpannableString? = null,
        titleContentDescription: String? = null,
        titleStyle: Int? = null,
        subtitle: SpannableString? = null,
        subtitleContentDescription: String? = null,
        subtitleStyle: Int? = null
    ) {
        placeholderHelper.setPlaceholder(
            image, imageContentDescription,
            title, titleContentDescription, titleStyle,
            subtitle, subtitleContentDescription, subtitleStyle
        )
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()

        val filteredNotesCollection = NotesLibrary.getInstance().currentNotes.filterDeletedAndFutureNotes()
        notesList?.updateNotesCollection(filteredNotesCollection, ScrollTo.NoScroll)

        if (NotesLibrary.getInstance().experimentFeatureFlags.userNotificationEnabled) {
            updateUserNotificationsUi(NotesLibrary.getInstance().allUserNotificationsForCurrentState)
        } else {
            val currentSyncError = NotesLibrary.getInstance().currentSyncErrorState
            val errorMessageIds = toSyncErrorStringIds(currentSyncError)
            errorMessageIds?.let { setSyncStatusMessage(it, userID = NotesLibrary.getInstance().currentUserID) }
        }
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    // ---- Transitions ----//
    fun findCurrentNoteView(): NoteViewHolder? = getCurrentNote()?.let { notesList?.findNoteView(it) }

    fun findNotesList(): NotesListComponent? = notesList

    fun ensureCurrentEditNoteVisibility() {
        notesList?.ensureCurrentEditNoteVisibility(getCurrentNote())
    }

    // ---- FragmentApi ----//
    override fun onRefreshCompleted(errorMessageId: Int?) {
        notesList?.stopRefreshAnimation()
        NotesLibrary.getInstance().sendSwipeToRefreshCompletedAction()
        errorMessageId?.let { Toast.makeText(activity, it, Toast.LENGTH_SHORT).show() }
    }

    override fun updateNotesCollection(notesCollection: List<Note>, scrollTo: ScrollTo, notesLoaded: Boolean) {
        notesList?.updateNotesCollection(notesCollection, scrollTo)
    }

    override fun getConnectivityManager(): ConnectivityManager? =
        activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    @Deprecated("Please use updateUserNotificationsUi for SyncErrors")
    override fun setSyncStatusMessage(errorMessage: SyncErrorResIds, userID: String) {
        if (!this.isVisible) {
            return
        }

        if (NotesLibrary.getInstance().experimentFeatureFlags.userNotificationEnabled) {
            return
        }

        val currentUserID = NotesLibrary.getInstance().currentUserID

        if (currentUserID != userID) {
            return
        }

        val title = getString(errorMessage.titleId)
        val optionalDescription = if (errorMessage.descriptionId != null)
            getString(errorMessage.descriptionId) else null
        if (NotesLibrary.getInstance().uiOptionFlags.showListErrorUi) {
            collapsibleMessageBar?.setError(title, optionalDescription, errorMessage.errorIconOverrideResId)
            collapsibleMessageBar?.show()
        }
    }

    @Deprecated("Please use updateUserNotificationsUi for SyncErrors")
    override fun unsetSyncStatusMessage(userID: String) {
        if (!this.isVisible) {
            return
        }

        if (NotesLibrary.getInstance().experimentFeatureFlags.userNotificationEnabled ||
            !NotesLibrary.getInstance().uiOptionFlags.showListErrorUi
        ) {
            return
        }
        val currentUserID = NotesLibrary.getInstance().currentUserID

        if (currentUserID != userID) {
            return
        }

        collapsibleMessageBar?.unsetError()
        collapsibleMessageBar?.remove()
    }

    override fun updateUserNotificationsUi(userIdToNotificationsMap: Map<String, UserNotifications>) {
        if (!this.isVisible) {
            return
        }

        if (!NotesLibrary.getInstance().uiOptionFlags.showListErrorUi) {
            return
        }

        val isCombinedListForMultiAccountEnabled =
            NotesLibrary.getInstance().experimentFeatureFlags.combinedListForMultiAccountEnabled

        val userNotificationMessage: UserNotification?
        val userID: String?
        if (isCombinedListForMultiAccountEnabled) {
            val userIDNotificationPair = getUserNotification(userIdToNotificationsMap)
            userID = userIDNotificationPair?.first
            userNotificationMessage = userIDNotificationPair?.second
        } else {
            userID = NotesLibrary.getInstance().currentUserID
            userNotificationMessage = userIdToNotificationsMap[userID]?.get()
        }

        if (userNotificationMessage == null || userID == null) {
            collapsibleMessageBar?.unsetError()
            collapsibleMessageBar?.remove()
            return
        }

        context?.let {
            val userNotificationResId = userNotificationMessage.toUserNotificationResIDsLocal(
                context = it,
                userID = userID,
                showEmailInfo = isCombinedListForMultiAccountEnabled &&
                    NotesLibrary.getInstance().isMultipleUsersSignedIn()
            )
            userNotificationResId?.let {
                collapsibleMessageBar?.setError(
                    userNotificationResId.title, userNotificationResId.description,
                    userNotificationResId.iconResId, userNotificationResId.buttonInfo
                )
                collapsibleMessageBar?.show()
            }
        }
    }
}
