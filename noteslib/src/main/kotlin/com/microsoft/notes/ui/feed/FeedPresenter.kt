package com.microsoft.notes.ui.feed

import android.content.Context
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.sideeffect.ui.ActionModeUpdateForFeed
import com.microsoft.notes.sideeffect.ui.ComprehensiveFeedFilterOptions
import com.microsoft.notes.sideeffect.ui.DisplayFilterAndSortPanel
import com.microsoft.notes.sideeffect.ui.FeedLayoutChanges
import com.microsoft.notes.sideeffect.ui.FeedSourceFilterOptions
import com.microsoft.notes.sideeffect.ui.NoteChanges
import com.microsoft.notes.sideeffect.ui.NoteReferenceChanges
import com.microsoft.notes.sideeffect.ui.SamsungNoteChanges
import com.microsoft.notes.sideeffect.ui.UserNotificationsUpdates
import com.microsoft.notes.store.getNoteReferencesCollectionForUser
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.store.getSamsungNotesCollectionForUser
import com.microsoft.notes.ui.extensions.filterDeletedAndFutureNotes
import com.microsoft.notes.ui.extensions.filterDeletedNoteReferences
import com.microsoft.notes.ui.extensions.isSamsungNote
import com.microsoft.notes.ui.feed.filter.FeedFilters
import com.microsoft.notes.ui.feed.filter.FeedNotesTypeFilterOption
import com.microsoft.notes.ui.feed.recyclerview.FeedItem
import com.microsoft.notes.ui.feed.recyclerview.FeedLayoutType
import com.microsoft.notes.ui.feed.recyclerview.addTimeHeaders
import com.microsoft.notes.ui.feed.recyclerview.collateNotes
import com.microsoft.notes.ui.feed.sourcefilter.FeedSourceFilterOption
import com.microsoft.notes.ui.note.options.dontShowDismissDialog
import com.microsoft.notes.ui.note.options.openDeleteNoteDialog
import com.microsoft.notes.ui.note.options.openDeleteNotesDialog
import com.microsoft.notes.ui.note.options.openDismissSamsungNoteDialog
import com.microsoft.notes.ui.noteslist.UserNotifications
import com.microsoft.notes.ui.shared.StickyNotesPresenterWithSyncSpinner
import com.microsoft.notes.ui.shared.SyncStateUpdatingApi
import java.util.ConcurrentModificationException
import java.util.concurrent.ConcurrentHashMap

@Suppress("TooManyFunctions")
class FeedPresenter(private val fragmentApi: FragmentApi) :
    StickyNotesPresenterWithSyncSpinner(fragmentApi),
    NoteReferenceChanges,
    NoteChanges,
    SamsungNoteChanges,
    FeedSourceFilterOptions,
    ActionModeUpdateForFeed,
    UserNotificationsUpdates,
    FeedLayoutChanges,
    ComprehensiveFeedFilterOptions,
    DisplayFilterAndSortPanel {

    private var cachedFeedFilters: FeedFilters = FeedFilters()
    private var cachedStickyNotesListsByUsers: HashMap<String, List<Note>> = hashMapOf()
    private var cachedSamsungNotesListsByUsers: ConcurrentHashMap<String, List<Note>> = ConcurrentHashMap()
    private var cachedNotesReferenceListsByUsers: HashMap<String, List<NoteReference>> = hashMapOf()

    init {
        refreshCachedNotesLists()
    }

    fun refreshCachedNotesLists() {
        val signedInUserIDs: Set<String> = if (NotesLibrary.getInstance().experimentFeatureFlags.combinedListForMultiAccountEnabled) {
            NotesLibrary.getInstance().getAllUsers()
        } else {
            setOf(NotesLibrary.getInstance().currentUserID)
        }

        cachedStickyNotesListsByUsers.clear()
        cachedSamsungNotesListsByUsers.clear()
        cachedNotesReferenceListsByUsers.clear()

        signedInUserIDs.forEach {
            addUserIDToFeedFilterCache(userID = it)

            cachedStickyNotesListsByUsers.put(it, NotesLibrary.getInstance().currentState.getNotesCollectionForUser(it).filterDeletedAndFutureNotes())
            cachedSamsungNotesListsByUsers.put(it, NotesLibrary.getInstance().currentState.getSamsungNotesCollectionForUser(it))
            cachedNotesReferenceListsByUsers.put(it, NotesLibrary.getInstance().currentState.getNoteReferencesCollectionForUser(it).filterDeletedNoteReferences())
        }
    }

    private var stickyNotes: List<Note> = NotesLibrary.getInstance().currentNotes.filterDeletedAndFutureNotes()
    private var samsungNotes: List<Note> = NotesLibrary.getInstance().currentSamsungNotes
    private var noteReferences: List<NoteReference> = NotesLibrary.getInstance().currentNoteReferences.filterDeletedNoteReferences()
    private var sourceFilter: FeedSourceFilterOption = FeedSourceFilterOption.ALL
        set(value) {
            field = value
            fragmentApi.hideSwipeToRefreshSpinner(sourceFilter)
            updateFragmentWithSourceFilteredFeedItems()
        }
    private fun isFeedDifferentViewModesEnabled(): Boolean = NotesLibrary.getInstance().experimentFeatureFlags.feedDifferentViewModesEnabled

    override fun onResume() {
        if (isFeedDifferentViewModesEnabled()) {
            refreshCachedNotesLists()
            updateFragmentWithSourceAndAccountFilteredFeedItems()
        } else {
            stickyNotes = NotesLibrary.getInstance().currentNotes.filterDeletedAndFutureNotes()
            samsungNotes = NotesLibrary.getInstance().currentSamsungNotes
            noteReferences = NotesLibrary.getInstance().currentNoteReferences.filterDeletedNoteReferences()
        }
    }

    fun deleteNotes(context: Context, feedItems: List<FeedItem>, onDelete: (() -> Unit)) {
        openDeleteNotesDialog(context = context, onSuccess = {
            val noteReferences: MutableList<NoteReference> = mutableListOf()
            for (feedItem in feedItems) {
                if (feedItem is FeedItem.NoteItem) {
                    val note = feedItem.note
                    if (note.isSamsungNote()) NotesLibrary.getInstance().markSamsungNoteAsDeleteAndDelete(note.localId, note.remoteData?.id ?: "")
                    else NotesLibrary.getInstance().markAsDeleteAndDelete(note.localId, note.remoteData?.id)
                } else if (feedItem is FeedItem.NoteReferenceItem) {
                    noteReferences.add(feedItem.noteReference)
                }
            }
            fragmentApi.deleteNoteReferences(noteReferences)
            NotesLibrary.getInstance().sendDeletedMultipleNotesAction()
            onDelete()
        }, onCancel = {})
    }

    fun deleteNote(context: Context, note: Note, onDelete: (() -> Unit)) {
        openDeleteNoteDialog(isFeed = true, context = context, onSuccess = {
            NotesLibrary.getInstance().markAsDeleteAndDelete(
                note.localId,
                note.remoteData?.id,
                true
            )
            onDelete()
        }, onCancel = {})
    }

    fun deleteSamsungNote(context: Context, note: Note, onDelete: (() -> Unit)) {
        val markSamsungNoteAsDeletedAndDelete = {
            NotesLibrary.getInstance()
                .markSamsungNoteAsDeleteAndDelete(
                    note.localId,
                    note.remoteData?.id ?: ""
                )
            onDelete()
        }
        if (dontShowDismissDialog(context)) {
            markSamsungNoteAsDeletedAndDelete()
        } else {
            openDismissSamsungNoteDialog(
                context = context,
                onSuccess = markSamsungNoteAsDeletedAndDelete
            )
        }
    }

    private fun addUserIDToFeedFilterCache(userID: String) {
        if (!cachedFeedFilters.selectedUserIDs.contains(userID)) {
            cachedFeedFilters.selectedUserIDs[userID] = false
        }
    }

    override fun noteReferencesUpdated(noteReferencesCollectionsByUser: HashMap<String, List<NoteReference>>) {
        if (isFeedDifferentViewModesEnabled()) {
            cachedNotesReferenceListsByUsers.clear()
            noteReferencesCollectionsByUser.forEach {
                cachedNotesReferenceListsByUsers.put(it.key, it.value.filterDeletedNoteReferences())
                addUserIDToFeedFilterCache(userID = it.key)
            }
            updateFragmentWithSourceAndAccountFilteredFeedItems()
        } else {
            noteReferences = listOf()
            noteReferencesCollectionsByUser.forEach {
                noteReferences = noteReferences + it.value.filterDeletedNoteReferences()
            }
            updateFragmentWithSourceFilteredFeedItems()
        }
    }

    override fun samsungNotesUpdated(samsungNotesCollectionsByUser: HashMap<String, List<Note>>) {
        try {
            if (isFeedDifferentViewModesEnabled()) {
                cachedSamsungNotesListsByUsers = ConcurrentHashMap(samsungNotesCollectionsByUser)
                samsungNotesCollectionsByUser.forEach {
                    addUserIDToFeedFilterCache(userID = it.key)
                }
                updateFragmentWithSourceAndAccountFilteredFeedItems()
            } else {
                samsungNotes = listOf()
                samsungNotesCollectionsByUser.forEach {
                    samsungNotes = samsungNotes + it.value
                }
                updateFragmentWithSourceFilteredFeedItems()
            }
        } catch (e: ConcurrentModificationException) {
            NotesLibrary.getInstance().log(message = "ConcurrentModification exception while calling samsungNotesUpdated")
        }
    }

    override fun notesUpdated(stickyNotesCollectionsByUsers: HashMap<String, List<Note>>, notesLoaded: Boolean) {
        if (isFeedDifferentViewModesEnabled()) {
            cachedStickyNotesListsByUsers.clear()
            stickyNotesCollectionsByUsers.forEach {
                cachedStickyNotesListsByUsers.put(it.key, it.value.filterDeletedAndFutureNotes())
                addUserIDToFeedFilterCache(userID = it.key)
            }
            updateFragmentWithSourceAndAccountFilteredFeedItems()
        } else {
            stickyNotes = listOf()
            stickyNotesCollectionsByUsers.forEach {
                stickyNotes = stickyNotes + it.value.filterDeletedAndFutureNotes()
            }
            updateFragmentWithSourceFilteredFeedItems()
        }
    }

    override fun noteDeleted() {
        // do nothing
    }

    override fun sourceFilterSelected(source: FeedSourceFilterOption) {
        sourceFilter = source
    }

    override fun filtersSelected(
        stickyNotesListsByUsers: HashMap<String, List<Note>>,
        samsungNotesListsByUsers: HashMap<String, List<Note>>,
        notesReferenceListsByUsers: HashMap<String, List<NoteReference>>,
        feedFilters: FeedFilters,
        scrollToTop: Boolean
    ) {
        cachedFeedFilters = feedFilters

        val stickyNotesListsByUsersFiltered: HashMap<String, List<Note>> = HashMap()
        stickyNotesListsByUsers.forEach {
            stickyNotesListsByUsersFiltered[it.key] = it.value.filterDeletedAndFutureNotes()
        }
        cachedStickyNotesListsByUsers = stickyNotesListsByUsersFiltered

        val samsungNotesListsByUsersFiltered: HashMap<String, List<Note>> = HashMap()
        samsungNotesListsByUsers.forEach {
            samsungNotesListsByUsersFiltered[it.key] = it.value.filterDeletedAndFutureNotes()
        }
        cachedSamsungNotesListsByUsers = ConcurrentHashMap(samsungNotesListsByUsersFiltered)

        val notesReferenceListsByUsersFiltered: HashMap<String, List<NoteReference>> = HashMap()
        notesReferenceListsByUsers.forEach {
            notesReferenceListsByUsersFiltered[it.key] = it.value.filterDeletedNoteReferences()
        }
        cachedNotesReferenceListsByUsers = notesReferenceListsByUsersFiltered

        updateFragmentWithSourceAndAccountFilteredFeedItems(scrollToTop)
    }

    fun updateFragmentWithSourceAndAccountFilteredFeedItems(scrollToTop: Boolean? = null) {
        // Create local copies of the StickyNotes list, SamsungNotes list and NoteReferences list
        var locallyFilteredStickyNotes: MutableList<Note> = mutableListOf()
        var locallyFilteredSamsungNotes: MutableList<Note> = mutableListOf()
        var locallyFilteredNoteReferences: MutableList<NoteReference> = mutableListOf()

        val isAnyAccountFilterSelected = cachedFeedFilters.isAnyAccountFilterSelected()
        cachedFeedFilters.selectedUserIDs.forEach {
            if (!isAnyAccountFilterSelected || it.value) {
                locallyFilteredStickyNotes.addAll(cachedStickyNotesListsByUsers.get(it.key) ?: listOf())
                locallyFilteredSamsungNotes.addAll(cachedSamsungNotesListsByUsers.get(it.key) ?: listOf())
                locallyFilteredNoteReferences.addAll(cachedNotesReferenceListsByUsers.get(it.key) ?: listOf())
            }
        }

        // Filter notes lists based on notes types, and collate the same into the list to be displayed
        if (cachedFeedFilters.isAnyNotesTypeFilterSelected()) {
            cachedFeedFilters.selectedNotesTypeFilters.forEach {
                when (it.key) {
                    FeedNotesTypeFilterOption.STICKY_NOTES -> if (!it.value)
                        locallyFilteredStickyNotes = mutableListOf()
                    FeedNotesTypeFilterOption.SAMSUNG_NOTES -> if (!it.value)
                        locallyFilteredSamsungNotes = mutableListOf()
                    FeedNotesTypeFilterOption.ONENOTE_PAGES -> if (!it.value)
                        locallyFilteredNoteReferences = mutableListOf()
                }
            }
        }

        var feedItemsFilteredOnNotesType = collateNotes(stickyNotes = locallyFilteredStickyNotes, samsungNotes = locallyFilteredSamsungNotes, noteReferences = locallyFilteredNoteReferences)

        // Add data to the list to be displayed
        if (fragmentApi.shouldAddTimeHeader()) {
            feedItemsFilteredOnNotesType = addTimeHeaders(feedItemsFilteredOnNotesType)
        }
        updateFragmentWithFeedItems(feedItemsFilteredOnNotesType, scrollToTop)
    }

    private fun updateFragmentWithSourceFilteredFeedItems() {
        var feedItems = when (sourceFilter) {
            FeedSourceFilterOption.ALL -> collateNotes(stickyNotes, samsungNotes, noteReferences)
            FeedSourceFilterOption.STICKY_NOTES -> collateNotes(stickyNotes = stickyNotes)
            // TODO here we are using all note references, but we will need to filter this down more
            // once we decide what 'type' is needed
            FeedSourceFilterOption.ONENOTE_PAGES -> collateNotes(noteReferences = noteReferences)
            FeedSourceFilterOption.SAMSUNG_NOTES -> collateNotes(samsungNotes = samsungNotes)
        }
        if (fragmentApi.shouldAddTimeHeader()) {
            feedItems = addTimeHeaders(feedItems)
        }
        updateFragmentWithFeedItems(feedItems)
    }

    private fun updateFragmentWithFeedItems(feedItemsList: List<FeedItem>, scrollToTop: Boolean? = null) {
        runIfActivityIsRunning {
            runOnClientThread {
                fragmentApi.updateFeedItems(feedItemsList, scrollToTop)
                if (isFeedDifferentViewModesEnabled()) {
                    fragmentApi.updateFilterOptions(cachedFeedFilters)
                } else {
                    fragmentApi.updateSourceFilterButtonLabel(sourceFilter)
                }

                if (fragmentApi.shouldAddTimeHeader()) {
                    fragmentApi.updateTimeHeader()
                }
            }
        }
    }

    // UI bindings
    override fun addUiBindings() {
        try {
            NotesLibrary.getInstance().addUiBindings(this)
        } catch (exception: UninitializedPropertyAccessException) {
            NotesLibrary.getInstance().log(message = "UninitializedPropertyAccessException when adding ui binding")
        }
    }

    override fun removeUiBindings() {
        try {
            NotesLibrary.getInstance().removeUiBindings(this)
        } catch (exception: UninitializedPropertyAccessException) {
            NotesLibrary.getInstance().log(
                message = "UninitializedPropertyAccessException when removing ui binding"
            )
        }
    }

    override fun finishActionMode() {
        fragmentApi.finishActionMode()
    }

    override fun invalidateActionMode() {
        fragmentApi.invalidateActionMode()
    }

    override fun updateUserNotifications(userIdToNotificationsMap: Map<String, UserNotifications>) {
        runIfActivityIsRunning {
            runOnClientThread {
                fragmentApi.updateUserNotificationsUi(userIdToNotificationsMap)
            }
        }
    }

    override fun changeFeedLayout(layoutType: FeedLayoutType) {
        fragmentApi.changeFeedLayout(layoutType)
    }

    override fun displayFilterAndSortPanel() {
        fragmentApi.displayFilterAndSortPanel()
    }
}

interface FragmentApi : SyncStateUpdatingApi {
    fun updateFeedItems(feedItems: List<FeedItem>, scrollToTop: Boolean? = null)
    fun shouldAddTimeHeader(): Boolean
    fun updateSourceFilterButtonLabel(source: FeedSourceFilterOption)
    fun updateFilterOptions(feedFilters: FeedFilters)
    fun updateTimeHeader()
    fun finishActionMode()
    fun invalidateActionMode()
    fun changeFeedLayout(layoutType: FeedLayoutType)
    fun hideSwipeToRefreshSpinner(source: FeedSourceFilterOption)
    fun updateUserNotificationsUi(userIdToNotificationsMap: Map<String, UserNotifications>)
    fun displayFilterAndSortPanel()
    fun deleteNoteReferences(noteReferences: List<NoteReference>)
}
