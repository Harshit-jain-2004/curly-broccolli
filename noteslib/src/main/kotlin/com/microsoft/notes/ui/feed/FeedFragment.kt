package com.microsoft.notes.ui.feed

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.styled.ReadOnlyStyledView
import com.microsoft.notes.store.Promise
import com.microsoft.notes.ui.extensions.hide
import com.microsoft.notes.ui.extensions.isSamsungNote
import com.microsoft.notes.ui.extensions.show
import com.microsoft.notes.ui.feed.filter.FeedComprehensiveFilterFragment
import com.microsoft.notes.ui.feed.filter.FeedFilters
import com.microsoft.notes.ui.feed.filter.FeedNotesTypeFilterOption
import com.microsoft.notes.ui.feed.filter.FeedSortCache
import com.microsoft.notes.ui.feed.filter.FilterChipView
import com.microsoft.notes.ui.feed.recyclerview.FeedItem
import com.microsoft.notes.ui.feed.recyclerview.FeedLayoutType
import com.microsoft.notes.ui.feed.recyclerview.feeditem.NoteReferenceFeedItemComponent
import com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes.SamsungNoteFeedItemComponent
import com.microsoft.notes.ui.feed.sourcefilter.FeedSourceFilterFragment
import com.microsoft.notes.ui.feed.sourcefilter.FeedSourceFilterOption
import com.microsoft.notes.ui.noteslist.NotesListComponent
import com.microsoft.notes.ui.noteslist.UserNotification
import com.microsoft.notes.ui.noteslist.UserNotifications
import com.microsoft.notes.ui.noteslist.getUserNotification
import com.microsoft.notes.ui.noteslist.recyclerview.noteitem.NoteItemComponent
import com.microsoft.notes.ui.noteslist.toUserNotificationResIDsLocal
import com.microsoft.notes.ui.shared.CollapsibleMessageBarView
import com.microsoft.notes.utils.accessibility.setClassNameOfViewAsButton
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.FeedItemType
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.utils.utils.Constants
import com.microsoft.notes.utils.utils.UserInfoUtils
import kotlinx.android.synthetic.main.feed_layout.*
import kotlinx.android.synthetic.main.sn_feed_component_layout.*

@Suppress("TooManyFunctions")
open class FeedFragment :
    Fragment(),
    FragmentApi,
    ReadOnlyStyledView.RecordTelemetryCallback,
    ActionModeClickListener {
    var noteReferenceCallbacks: NoteReferenceFeedItemComponent.Callbacks? = null
    var actionModeChangeListener: ActionModeChangeListener? = null
    private lateinit var actionModeController: ActionModeController
    private var messageBar: CollapsibleMessageBarView? = null
    protected var currentFeedItemLocalId: String = ""
    private var sourceFilter: FeedSourceFilterOption = FeedSourceFilterOption.ALL

    private var cachedFeedFilters: FeedFilters = FeedFilters()

    private val presenterDelegate = lazy { FeedPresenter(this) }
    private val presenter by presenterDelegate

    val isFeedDifferentViewModesEnabled: () -> Boolean = {
        NotesLibrary.getInstance().experimentFeatureFlags.feedDifferentViewModesEnabled
    }

    val isPinnedNotesEnabled: () -> Boolean = {
        NotesLibrary.getInstance().experimentFeatureFlags.pinnedNotesEnabled
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onPause() {
        super.onPause()
        feedComponent.stopRefreshAnimation()
        presenter.onPause()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()

        if (NotesLibrary.getInstance().experimentFeatureFlags.userNotificationEnabled) {
            updateUserNotificationsUi(NotesLibrary.getInstance().allUserNotificationsForCurrentState)
        }
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (presenterDelegate.isInitialized())
            presenter.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.feed_layout, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupClickCallbacks()
        setupScrollCallbacks()
        messageBar = view?.findViewById(R.id.collapsibleMessageBar) as? CollapsibleMessageBarView
        if (isFeedDifferentViewModesEnabled()) {
            sourceFilterButton.hide()
            timeHeaderTopView.hide()
        }
        if (NotesLibrary.getInstance().uiOptionFlags.showActionModeOnFeed) {
            actionModeController = ActionModeController(
                activity as Activity, actionModeClickListener = this,
                callback = object : Callback {
                    override fun onStartActionMode() {
                        recordTelemetryEvent(eventMarker = EventMarkers.FeedActionModeStarted)
                        refreshFilterButton(false)
                        actionModeChangeListener?.onStartActionMode()
                        feedComponent.setSwipeToRefreshEnabled(isEnabled = false)
                    }

                    override fun onFinishActionMode() {
                        recordTelemetryEvent(eventMarker = EventMarkers.FeedActionModeFinished)
                        actionModeChangeListener?.onFinishActionMode()
                        refreshFilterButton(true)
                        feedComponent.selectionTracker.clearSelection()
                        feedComponent.setSwipeToRefreshEnabled(isEnabled = true)
                    }

                    override fun getActionModeTitle(): String =
                        if (NotesLibrary.getInstance().experimentFeatureFlags.multiSelectInActionModeEnabled) getString(R.string.sn_notes_selected, feedComponent.selectionTracker.getSelectedItems().size)
                        else ""

                    override fun onPrepareActionMode(menu: Menu): Boolean {
                        if (feedComponent.selectionTracker.isSelectionEnabled()) {
                            val selectedItems = feedComponent.getSelectedItems()
                            val deleteItem = menu.findItem(R.id.sn_menu_actionmode_delete)
                            deleteItem.actionView = getImageButton(R.drawable.sn_ic_actionmode_delete, getString(R.string.sn_action_delete_note), View.OnClickListener { onActionItemClicked(deleteItem) })
                            val organizeItem = menu.findItem(R.id.sn_menu_actionmode_organise)
                            organizeItem.actionView = getImageButton(R.drawable.sn_ic_actionmode_organise, getString(R.string.sn_notes_organise_label), View.OnClickListener { onActionItemClicked(organizeItem) })
                            val pinNoteItem = menu.findItem(R.id.sn_menu_actionmode_pin)
                            if (!NotesLibrary.getInstance().experimentFeatureFlags.multiSelectInActionModeEnabled) {
                                setActionModeMenuItemsForSingleItem(selectedItems[0], menu)
                            } else {
                                if (selectedItems.size == 1) {
                                    setActionModeMenuItemsForSingleItem(selectedItems[0], menu)
                                } else {
                                    val isNoteReferenceSelected = selectedItems.any { it is FeedItem.NoteReferenceItem }
                                    val isNoteSelected = selectedItems.any { it is FeedItem.NoteItem }
                                    val enableAction = if (isNoteReferenceSelected) NotesLibrary.getInstance().isActionsEnabledOnNoteReferences else true
                                    val unpinnedNoteReferences = selectedItems.filterIsInstance<FeedItem.NoteReferenceItem>().map { it.noteReference }.filter { !it.isPinned }
                                    val unpinnedNotes = selectedItems.filterIsInstance<FeedItem.NoteItem>().map { it.note }.filter { !it.isPinned }
                                    if (unpinnedNoteReferences.isEmpty() && unpinnedNotes.isEmpty()) {
                                        pinNoteItem.actionView = getImageButton(R.drawable.sn_ic_actionmode_unpin, getString(R.string.unpin_label), View.OnClickListener { onActionItemClicked(pinNoteItem) })
                                    } else {
                                        pinNoteItem.actionView = getImageButton(R.drawable.sn_ic_actionmode_pin, getString(R.string.pin_label), View.OnClickListener { onActionItemClicked(pinNoteItem) })
                                    }
                                    /*
                                     * a. Organize option in multi-select is only available when all the selected items are NoteReferences i.e. notebook pages
                                     * b. Delete option is multi-select is available when -
                                     *  1. If all the notes selected are sticky notes / samsung notes
                                     *  2. If a notebook page is selected when we need to wait until the actions are enabled for notereferences
                                     */
                                    setActionModeViewAlpha(organizeItem.actionView, enableAction && !isNoteSelected)
                                    setActionModeMenuItemAlpha(menu, R.id.sn_menu_actionmode_share, false)
                                    setActionModeMenuItemAlpha(menu, R.id.sn_menu_actionmode_pin_shortcut_to_home, isEnabled = false)
                                    setActionModeMenuItemVisibility(menu, R.id.sn_menu_actionmode_pin, enableAction && isPinnedNotesEnabled())
                                    setActionModeMenuItemVisibility(menu, R.id.sn_menu_actionmode_delete, enableAction)
                                    setActionModeMenuItemVisibility(menu, R.id.samsung_menu_actionmode_dismiss, false)
                                }
                            }
                            setActionModeMenuItemVisibility(menu, R.id.sn_menu_actionmode_pin_shortcut_to_home, visibility = NotesLibrary.getInstance().uiOptionFlags.showPinShortcutToHomeOption)
                        }
                        return true
                    }

                    private fun setActionModeMenuItemsForSingleItem(selectedFeedItem: FeedItem, menu: Menu) {
                        when (selectedFeedItem) {
                            is FeedItem.NoteItem -> {
                                val selectedItem = selectedFeedItem.note
                                val disableOrganizeInActionMode = selectedItem.document.isInkDocument ||
                                    selectedItem.document.isRenderedInkDocument
                                val pinNoteItem = menu.findItem(R.id.sn_menu_actionmode_pin)
                                if (selectedFeedItem.note.isPinned) {
                                    pinNoteItem.actionView = getImageButton(R.drawable.sn_ic_actionmode_unpin, getString(R.string.unpin_label), View.OnClickListener { onActionItemClicked(pinNoteItem) })
                                } else {
                                    pinNoteItem.actionView = getImageButton(R.drawable.sn_ic_actionmode_pin, getString(R.string.pin_label), View.OnClickListener { onActionItemClicked(pinNoteItem) })
                                }
                                setActionModeMenuItemAlpha(menu, R.id.sn_menu_actionmode_organise, isEnabled = !disableOrganizeInActionMode)
                                setActionModeMenuItemAlpha(menu, R.id.sn_menu_actionmode_share, isEnabled = false)
                                setActionModeMenuItemAlpha(menu, R.id.sn_menu_actionmode_pin_shortcut_to_home, isEnabled = false)
                                setActionModeMenuItemVisibility(menu, R.id.sn_menu_actionmode_pin, isPinnedNotesEnabled())
                                setActionModeMenuItemVisibility(menu, R.id.sn_menu_actionmode_delete, visibility = !selectedItem.isSamsungNote())
                                setActionModeMenuItemVisibility(menu, R.id.samsung_menu_actionmode_dismiss, visibility = selectedItem.isSamsungNote())
                            }
                            is FeedItem.NoteReferenceItem -> {
                                val pinNoteItem = menu.findItem(R.id.sn_menu_actionmode_pin)
                                if (selectedFeedItem.noteReference.isPinned) {
                                    pinNoteItem.actionView = getImageButton(R.drawable.sn_ic_actionmode_unpin, getString(R.string.unpin_label), View.OnClickListener { onActionItemClicked(pinNoteItem) })
                                } else {
                                    pinNoteItem.actionView = getImageButton(R.drawable.sn_ic_actionmode_pin, getString(R.string.pin_label), View.OnClickListener { onActionItemClicked(pinNoteItem) })
                                }
                                val enableAction = NotesLibrary.getInstance().isActionsEnabledOnNoteReferences
                                setActionModeMenuItemAlpha(menu, R.id.sn_menu_actionmode_pin, enableAction)
                                setActionModeMenuItemAlpha(menu, R.id.sn_menu_actionmode_organise, enableAction)
                                setActionModeMenuItemAlpha(menu, R.id.sn_menu_actionmode_share, enableAction && NotesLibrary.getInstance().experimentFeatureFlags.shareLinkToPageInActionModeEnabled)
                                setActionModeMenuItemAlpha(menu, R.id.sn_menu_actionmode_pin_shortcut_to_home, isEnabled = true)
                                setActionModeMenuItemVisibility(menu, R.id.sn_menu_actionmode_pin, enableAction && isPinnedNotesEnabled())
                                setActionModeMenuItemVisibility(menu, R.id.sn_menu_actionmode_delete, enableAction)
                                setActionModeMenuItemVisibility(menu, R.id.samsung_menu_actionmode_dismiss, false)
                            }
                        }
                    }

                    private fun setActionModeMenuItemAlpha(menu: Menu?, menuID: Int, isEnabled: Boolean) {
                        val menuItem = menu?.findItem(menuID)
                        menuItem?.isEnabled = isEnabled
                        menuItem?.icon?.alpha =
                            if (isEnabled) {
                                255
                            } else {
                                (255 * Constants.OPTIONS_DISABLED_ALPHA).toInt()
                            }
                    }

                    private fun setActionModeViewAlpha(view: View?, isEnabled: Boolean) {
                        if (isEnabled) {
                            view?.alpha = Constants.OPTIONS_ENABLED_ALPHA
                        } else {
                            view?.alpha = Constants.OPTIONS_DISABLED_ALPHA
                        }
                    }

                    private fun setActionModeMenuItemVisibility(menu: Menu?, menuID: Int, visibility: Boolean) {
                        val menuItem = menu?.findItem(menuID)
                        menuItem?.isVisible = visibility
                    }

                    private fun refreshFilterButton(enable: Boolean) {
                        if (isFeedDifferentViewModesEnabled()) {
                            sourceFilterButton.hide()
                        } else {
                            sourceFilterButton.show()
                            sourceFilterButton.isClickable = enable
                            sourceFilterButton.isFocusable = enable
                            sourceFilterButton.alpha = if (enable) 1.0F else 0.5F
                        }
                    }
                }
            )
        }
        FeedSortCache.fetchPreferredSortSelection(context)
    }

    private fun pinOrUnpinNoteItemsInFeed(feedItems: List<FeedItem?>) {
        val noteReferences: List<NoteReference> = feedItems.filterIsInstance<FeedItem.NoteReferenceItem>().map { it.noteReference }
        val notes: List<Note> = feedItems.filterIsInstance<FeedItem.NoteItem>().map { it.note }
        val pinnedNoteReferencesList: MutableList<NoteReference> = mutableListOf()
        val unPinnedNoteReferencesList: MutableList<NoteReference> = mutableListOf()
        val pinnedNotesList: MutableList<Note> = mutableListOf()
        val unPinnedNotesList: MutableList<Note> = mutableListOf()
        noteReferences.forEach {
            if (it.isPinned)
                pinnedNoteReferencesList.add(it)
            else
                unPinnedNoteReferencesList.add(it)
        }
        notes.forEach {
            if (it.isPinned)
                pinnedNotesList.add(it)
            else
                unPinnedNotesList.add(it)
        }
        if (unPinnedNoteReferencesList.isNotEmpty() || unPinnedNotesList.isNotEmpty()) {
            NotesLibrary.getInstance().pinNoteReferences(unPinnedNoteReferencesList)
            NotesLibrary.getInstance().pinNotes(unPinnedNotesList)
            NotesLibrary.getInstance().showToast(R.string.pinned_notes_toast)
            recordTelemetryEvent(
                EventMarkers.PinnedFeedItems,
                Pair(NotesSDKTelemetryKeys.NoteProperty.COUNT_OF_PINNED_ITEMS, (unPinnedNoteReferencesList.size + unPinnedNotesList.size).toString())
            )
        } else {
            NotesLibrary.getInstance().unPinNoteReferences(pinnedNoteReferencesList)
            NotesLibrary.getInstance().unpinNotes(pinnedNotesList)
            NotesLibrary.getInstance().showToast(R.string.unpinned_notes_toast)
            recordTelemetryEvent(
                EventMarkers.UnpinnedFeedItems,
                Pair(NotesSDKTelemetryKeys.NoteProperty.COUNT_OF_UNPINNED_ITEMS, (pinnedNoteReferencesList.size + pinnedNotesList.size).toString())
            )
        }
        finishActionMode()
        presenter.updateFragmentWithSourceAndAccountFilteredFeedItems(true)
    }

    private fun getImageButton(drawable: Int, contentDescription: String, clickListener: View.OnClickListener): ImageButton? {
        val imageButton = ImageButton(context)
        imageButton.setImageResource(drawable)
        val imageButtonPadding = context?.resources?.getDimension(R.dimen.feed_action_mode_item_padding)?.toInt()
        imageButtonPadding?.let { imageButton.setPadding(it, it, it, it) }

        val outValue = TypedValue()
        context?.theme?.resolveAttribute(R.attr.selectableItemBackground, outValue, true)
        imageButton.setBackgroundResource(outValue.resourceId)

        imageButton.contentDescription = contentDescription
        imageButton.setOnClickListener(clickListener)
        setClassNameOfViewAsButton(imageButton)
        return imageButton
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedComponent.setSwipeToRefreshEnabled(isEnabled = true)
        /**
         * Work around for android bug https://issuetracker.google.com/issues/37088814
         */
        turnOffAnimationsForAccessibility()
    }

    private fun turnOffAnimationsForAccessibility() {
        val accessibilityManager = context?.getSystemService(
            Context.ACCESSIBILITY_SERVICE
        ) as? AccessibilityManager
        if (accessibilityManager?.isEnabled ?: false) {
            feedRecyclerView?.itemAnimator = null
        }
    }

    override fun recordTelemetryEvent(eventMarker: EventMarkers, vararg keyValuePairs: Pair<String, String>) {
        presenter.recordTelemetry(eventMarker, *keyValuePairs)
    }

    fun setCurrentFeedItemId(currentFeedItemLocalId: String) {
        this.currentFeedItemLocalId = currentFeedItemLocalId
    }

    private fun setupScrollCallbacks() {
        feedRecyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (shouldAddTimeHeader()) {
                    updateTimeHeader()
                }
            }
        })
    }

    private fun invokeFeedFilterPanel() {
        activity?.let {
            if (isFeedDifferentViewModesEnabled()) {
                val feedComprehensiveFilterFragment = FeedComprehensiveFilterFragment()
                feedComprehensiveFilterFragment.feedFilters = cachedFeedFilters
                feedComprehensiveFilterFragment.show(it.supportFragmentManager, "feed_source_filter")
            } else {
                val feedSourceFilterFragment = FeedSourceFilterFragment()
                feedSourceFilterFragment.sourceFilter = sourceFilter
                feedSourceFilterFragment.show(it.supportFragmentManager, "feed_source_filter")
            }
        }
    }

    private fun setupClickCallbacks() {
        sourceFilterButton.setOnClickListener {
            invokeFeedFilterPanel()
        }

        feedComponent.noteReferenceCallbacks = object : NoteReferenceFeedItemComponent.Callbacks {
            override fun onNoteItemClicked(note: NoteReference) {
                noteReferenceCallbacks?.onNoteItemClicked(note)
                setCurrentFeedItemId(note.localId)
                recordTelemetryEvent(
                    EventMarkers.TappedOnFeedItem,
                    Pair(NotesSDKTelemetryKeys.NoteProperty.FEED_ITEM_TYPE, FeedItemType.OneNotePage.name),
                    Pair(NotesSDKTelemetryKeys.FeedUIProperty.FEED_SELECTED_ITEM_DEPTH, feedComponent?.adapter?.getIndexFromItemLocalId(note.localId).toString())
                )
                // TODO on androidx upgrade should not have '?' here
            }

            override fun onNoteItemLongPress(note: NoteReference, view: View) {}
            override fun canPerformActionOnNote(note: NoteReference): Promise<Boolean>? {
                return noteReferenceCallbacks?.canPerformActionOnNote(note)
            }
        }

        feedComponent.stickyNoteCallbacks = object : NoteItemComponent.Callbacks() {
            override fun onNoteItemClicked(note: Note) {
                // TODO re-evaluate how we pass the click listener and ensure we have telemetry
                NotesListComponent.DefaultNotesListComponentCallbacks.onNoteClicked(note)
                setCurrentFeedItemId(note.localId)
                recordTelemetryEvent(
                    EventMarkers.TappedOnFeedItem,
                    Pair(NotesSDKTelemetryKeys.NoteProperty.FEED_ITEM_TYPE, FeedItemType.StickyNote.name),
                    Pair(NotesSDKTelemetryKeys.FeedUIProperty.FEED_SELECTED_ITEM_DEPTH, feedComponent.adapter.getIndexFromItemLocalId(note.localId).toString())
                )
            }

            override fun onNoteItemLongPress(note: Note, view: View) {}
        }

        feedComponent.samsungNoteCallbacks = object : SamsungNoteFeedItemComponent.Callbacks {
            override fun onNoteItemClicked(note: Note) {
                NotesListComponent.DefaultNotesListComponentCallbacks.onNoteClicked(note)
                setCurrentFeedItemId(note.localId)
                recordTelemetryEvent(
                    EventMarkers.TappedOnFeedItem,
                    Pair(NotesSDKTelemetryKeys.NoteProperty.FEED_ITEM_TYPE, FeedItemType.SamsungNote.name),
                    Pair(NotesSDKTelemetryKeys.FeedUIProperty.FEED_SELECTED_ITEM_DEPTH, feedComponent.adapter.getIndexFromItemLocalId(note.localId).toString())
                )
            }

            override fun onNoteItemLongPress(note: Note, view: View) {}
        }

        feedComponent.feedListCallbacks = object : FeedComponent.FeedListCallbacks() {
            override fun onSwipeToRefreshTriggered() {
                NotesLibrary.getInstance().sendFeedSwipeToRefreshStartedAction()
                feedComponent.dismissSwipeToRefresh()

                if (!isFeedDifferentViewModesEnabled()) {
                    val noteTypeFilterOption: FeedNotesTypeFilterOption = when (sourceFilter) {
                        FeedSourceFilterOption.ALL -> FeedNotesTypeFilterOption.ALL
                        FeedSourceFilterOption.STICKY_NOTES -> FeedNotesTypeFilterOption.STICKY_NOTES
                        FeedSourceFilterOption.ONENOTE_PAGES -> FeedNotesTypeFilterOption.ONENOTE_PAGES
                        FeedSourceFilterOption.SAMSUNG_NOTES -> FeedNotesTypeFilterOption.SAMSUNG_NOTES
                    }
                    refreshItems(hashMapOf(noteTypeFilterOption to true))
                } else {
                    refreshItems(cachedFeedFilters.selectedNotesTypeFilters)
                }
            }

            override fun startActionMode() {
                actionModeController.startActionMode()
            }

            override fun endActionMode() {
                actionModeController.finishActionMode()
            }

            override fun invalidateActionMenu() {
                actionModeController.invalidateActionMenu()
            }

            private fun refreshItems(filters: HashMap<FeedNotesTypeFilterOption, Boolean>) {
                if (filters.isEmpty() || FeedNotesTypeFilterOption.ALL in filters) {
                    presenter.refreshList()
                    presenter.refreshNoteReferencesList()
                    if (NotesLibrary.getInstance().experimentFeatureFlags.samsungNotesSyncEnabled) {
                        presenter.refreshSamsungNotesList()
                    }
                }
                if (FeedNotesTypeFilterOption.ONENOTE_PAGES in filters) {
                    presenter.refreshNoteReferencesList()
                }
                if (FeedNotesTypeFilterOption.SAMSUNG_NOTES in filters) {
                    presenter.refreshSamsungNotesList()
                }
                if (FeedNotesTypeFilterOption.STICKY_NOTES in filters) {
                    presenter.refreshList()
                }
            }
        }
    }

    override fun finishActionMode() {
        activity?.runOnUiThread {
            actionModeController.finishActionMode()
        }
    }

    override fun invalidateActionMode() {
        activity?.runOnUiThread {
            actionModeController.invalidateActionMenu()
        }
    }

    @Suppress("LongMethod")
    override fun onActionItemClicked(menuItem: MenuItem?): Boolean {
        if (NotesLibrary.getInstance().experimentFeatureFlags.multiSelectInActionModeEnabled && feedComponent.selectionTracker.getSelectedItems().size > 1) {
            return multiSelectActionItemClicked(menuItem)
        }

        val selectedFeedItem = feedComponent.getSelectedItems().getOrNull(0)
        if (selectedFeedItem is FeedItem.NoteReferenceItem) {
            val selectedItem = selectedFeedItem.noteReference
            when (menuItem?.itemId) {
                R.id.sn_menu_actionmode_delete -> {
                    recordTelemetryEvent(
                        EventMarkers.DeleteFeedItemStarted,
                        Pair(NotesSDKTelemetryKeys.NoteProperty.FEED_ITEM_TYPE, FeedItemType.OneNotePage.name)
                    )
                    noteReferenceCallbacks?.deleteNoteItem(
                        selectedItem
                    )
                    return true
                }
                R.id.sn_menu_actionmode_share -> {
                    recordTelemetryEvent(
                        EventMarkers.SharePageLinkFeedItem,
                        Pair(NotesSDKTelemetryKeys.NoteProperty.FEED_ITEM_TYPE, FeedItemType.OneNotePage.name)
                    )
                    actionModeController.finishActionMode()
                    noteReferenceCallbacks?.shareNoteItem(
                        selectedItem
                    )
                    return true
                }
                R.id.sn_menu_actionmode_organise -> {
                    recordTelemetryEvent(
                        EventMarkers.OrganizeFeedItemStarted,
                        Pair(NotesSDKTelemetryKeys.NoteProperty.FEED_ITEM_TYPE, FeedItemType.OneNotePage.name)
                    )
                    noteReferenceCallbacks?.organiseNoteItem(
                        selectedItem
                    )
                    return true
                }
                R.id.sn_menu_actionmode_pin_shortcut_to_home -> {
                    noteReferenceCallbacks?.pinNoteShortcutToHomeScreen(selectedItem)
                    return true
                }
                R.id.sn_menu_actionmode_pin -> {
                    pinOrUnpinNoteItemsInFeed(listOf(feedComponent.getSelectedItems().getOrNull(0)))
                    return true
                }
            }
        } else if (selectedFeedItem is FeedItem.NoteItem) {
            val selectedItem = selectedFeedItem.note
            val feedItemNameForTelemetry =
                if (selectedItem.isSamsungNote()) FeedItemType.SamsungNote.name else FeedItemType.StickyNote.name
            val feedItemPairForTelemetry =
                NotesSDKTelemetryKeys.NoteProperty.FEED_ITEM_TYPE to feedItemNameForTelemetry

            when (menuItem?.itemId) {
                R.id.sn_menu_actionmode_delete -> {
                    recordTelemetryEvent(
                        EventMarkers.DeleteFeedItemStarted,
                        feedItemPairForTelemetry
                    )
                    context?.let {
                        presenter.deleteNote(
                            it, selectedItem,
                            onDelete = {
                                recordTelemetryEvent(
                                    EventMarkers.DeleteFeedItemFinished,
                                    feedItemPairForTelemetry
                                )
                                actionModeController.finishActionMode()
                            }
                        )
                    }
                    return true
                }
                R.id.samsung_menu_actionmode_dismiss -> {
                    recordTelemetryEvent(
                        EventMarkers.DeleteFeedItemStarted,
                        feedItemPairForTelemetry
                    )
                    context?.let {
                        presenter.deleteSamsungNote(
                            it, selectedItem,
                            onDelete = {
                                recordTelemetryEvent(
                                    EventMarkers.DeleteFeedItemFinished,
                                    feedItemPairForTelemetry
                                )
                                actionModeController.finishActionMode()
                            }
                        )
                    }
                    return true
                }
                R.id.sn_menu_actionmode_organise -> {
                    recordTelemetryEvent(
                        EventMarkers.OrganizeFeedItemStarted,
                        feedItemPairForTelemetry
                    )
                    NotesListComponent.DefaultNotesListComponentCallbacks.onNoteOrganise(selectedItem)
                    return true
                }
                R.id.sn_menu_actionmode_pin -> {
                    pinOrUnpinNoteItemsInFeed(listOf(feedComponent.getSelectedItems().getOrNull(0)))
                    return true
                }
            }
        }
        return false
    }

    private fun multiSelectActionItemClicked(menuItem: MenuItem?): Boolean {
        val selectedFeedItems = feedComponent.getSelectedItems()
        val isNoteSelected = selectedFeedItems.any { it is FeedItem.NoteItem }

        when (menuItem?.itemId) {
            R.id.sn_menu_actionmode_delete -> {
                context?.let {
                    presenter.deleteNotes(it, selectedFeedItems, onDelete = {
                        actionModeController.finishActionMode()
                    })
                }
                return true
            }
            R.id.sn_menu_actionmode_organise -> {
                context?.let {
                    if (!isNoteSelected) {
                        noteReferenceCallbacks?.organizeNoteItems(
                            selectedFeedItems
                                .filterIsInstance<FeedItem.NoteReferenceItem>().map { it.noteReference }
                        )
                    }
                }
            }
            R.id.sn_menu_actionmode_pin -> {
                context?.let {
                    pinOrUnpinNoteItemsInFeed(selectedFeedItems)
                }
            }
        }
        return false
    }

    fun ensureCurrentFeedItemVisibility() {
        feedComponent.ensureFeedItemVisibility(currentFeedItemLocalId)
    }

    fun scrollToTop() {
        feedComponent.scrollToTop()
    }

    fun updateSelectedFiltersDisplay() {
        // This view is hosted on top of the Feed list and updates on 2 occassions :
        // 1. User changed their selection of filters from the filter fragment
        // 2. User cancelled some filters which were being displayed in this view
        // In any case we get an update to the FeedFragment that an update has been made to the selected filters view.
        // Since any amount of change could have been made to the selected filters, we don't try to find the difference from the previous state.
        // We instead clear the view entirely and add the new chips for all selected filters.
        selected_filters_display?.removeAllViews()

        // Add selected Account filter chips to selected_filters_display
        cachedFeedFilters.selectedUserIDs.forEach {
            if (it.value) {
                val userID: String = it.key
                selected_filters_display?.addView(
                    createSelectedFilterChips(
                        label = context?.let { UserInfoUtils.getEmailIDFromUserID(userID, it) } ?: "",
                        onFilterClose = { cachedFeedFilters.selectedUserIDs[it.key] = false }
                    )
                )
            }
        }

        // Add selected NoteType filter chips to selected_filters_display
        cachedFeedFilters.selectedNotesTypeFilters.forEach {
            if (it.value && it.key != FeedNotesTypeFilterOption.ALL) {
                selected_filters_display?.addView(
                    createSelectedFilterChips(
                        label = it.key.toString(),
                        onFilterClose = { cachedFeedFilters.selectedNotesTypeFilters.put(it.key, false) }
                    )
                )
            }
        }

        if (selected_filters_display?.childCount ?: 0 > 0) {
            feedRecyclerView?.setPadding(0, 0, 0, 0)
            val description = context?.resources?.let {
                if (selected_filters_display.childCount == 1) it.getString(R.string.applied_filter_container_description)
                else it.getString(R.string.applied_filters_container_description, selected_filters_display.childCount)
            }
            selected_filters_display_container?.contentDescription = description
            selected_filters_display_container?.show()
        } else {
            val feedTopPadding = context?.resources?.getDimensionPixelSize(R.dimen.feed_refresh_top_margin) ?: 0
            feedRecyclerView?.setPadding(0, feedTopPadding, 0, 0)

            val hidingChips = (selected_filters_display_container?.visibility == View.VISIBLE)
            if (hidingChips)
                feedRecyclerView?.scrollBy(0, -feedTopPadding)

            selected_filters_display_container?.hide()
        }
    }

    private fun createSelectedFilterChips(label: String, onFilterClose: () -> Unit): View? {
        return FilterChipView.createSelectedFilterView(
            context = context, label = label,
            onCloseListener = {
                onFilterClose()
                NotesLibrary.getInstance().recordTelemetry(
                    EventMarkers.FilterSelectionsUpdated,
                    Pair(NotesSDKTelemetryKeys.FilterProperty.ALL_SELECTION_STATUS, cachedFeedFilters.selectedNotesTypeFilters[FeedNotesTypeFilterOption.ALL].toString()),
                    Pair(NotesSDKTelemetryKeys.FilterProperty.STICKYNOTES_SELECTION_STATUS, cachedFeedFilters.selectedNotesTypeFilters[FeedNotesTypeFilterOption.STICKY_NOTES].toString()),
                    Pair(NotesSDKTelemetryKeys.FilterProperty.SAMSUNGNOTES_SELECTION_STATUS, cachedFeedFilters.selectedNotesTypeFilters[FeedNotesTypeFilterOption.SAMSUNG_NOTES].toString()),
                    Pair(NotesSDKTelemetryKeys.FilterProperty.PAGES_SELECTION_STATUS, cachedFeedFilters.selectedNotesTypeFilters[FeedNotesTypeFilterOption.ONENOTE_PAGES].toString()),
                    Pair(NotesSDKTelemetryKeys.FilterProperty.ACCOUNT_FILTER_SELECTION_STATUS, cachedFeedFilters.isAnyAccountFilterSelected().toString()),
                    Pair(
                        NotesSDKTelemetryKeys.FilterProperty.FILTER_UPDATE_SOURCE,
                        NotesSDKTelemetryKeys.FilterProperty.TOPBAR_FILTER_CHIPS
                    )
                )
                NotesLibrary.getInstance().sendComprehensiveFeedFilterSelectedAction(cachedFeedFilters, false)
            }
        )
    }

    override fun updateFeedItems(feedItems: List<FeedItem>, scrollToTop: Boolean?) {
        feedComponent?.updateFeedItems(feedItems, scrollToTop = scrollToTop)
    }

    override fun shouldAddTimeHeader(): Boolean = !NotesLibrary.getInstance().experimentFeatureFlags.feedDifferentViewModesEnabled

    override fun updateTimeHeader() {
        val feedItems = feedComponent?.adapter?.feedItems ?: return
        val firstVisibleIndex = feedComponent.getTopVisibleItemIndex()

        if (firstVisibleIndex < 0 || firstVisibleIndex >= feedItems.size) return

        val item = feedItems[firstVisibleIndex]

        val bucket = this.context?.let { item.timeBucket?.getTitle(it) }
        if (bucket != null)
            timeHeaderTopView?.text = bucket
    }

    override fun updateSourceFilterButtonLabel(source: FeedSourceFilterOption) {
        // TODO l18n
        this.sourceFilter = source
        context?.let {
            sourceFilterButton?.text = getString(
                when (source) {
                    FeedSourceFilterOption.ALL -> R.string.heading_filter
                    FeedSourceFilterOption.STICKY_NOTES -> R.string.heading_sticky_notes
                    FeedSourceFilterOption.ONENOTE_PAGES -> R.string.heading_all_pages
                    FeedSourceFilterOption.SAMSUNG_NOTES -> R.string.heading_samsung_notes
                }
            )
        }
    }

    override fun updateFilterOptions(feedFilters: FeedFilters) {
        this.cachedFeedFilters = feedFilters
        updateSelectedFiltersDisplay()
    }

    override fun hideSwipeToRefreshSpinner(source: FeedSourceFilterOption) {
        if (source != sourceFilter) {
            feedComponent.stopRefreshAnimation()
        }
    }

    override fun onRefreshCompleted(errorMessageId: Int?) {
        feedComponent.stopRefreshAnimation()
    }

    override fun getConnectivityManager(): ConnectivityManager? =
        activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

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
            messageBar?.unsetError()
            messageBar?.remove()
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
                messageBar?.setError(
                    userNotificationResId.title, userNotificationResId.description,
                    userNotificationResId.iconResId, userNotificationResId.buttonInfo
                )
                messageBar?.show()
            }
        }
    }

    override fun changeFeedLayout(layoutType: FeedLayoutType) {
        activity?.runOnUiThread {
            feedComponent.changeFeedLayout(layoutType)
        }
    }

    override fun displayFilterAndSortPanel() {
        invokeFeedFilterPanel()
    }

    override fun deleteNoteReferences(noteReferences: List<NoteReference>) {
        noteReferenceCallbacks?.deleteNoteItems(noteReferences)
    }

    fun getFeedLayout(): FeedLayoutType {
        if (feedComponent != null)
            return feedComponent.getFeedLayout()
        return FeedLayoutType.LIST_LAYOUT
    }

    interface ActionModeChangeListener {
        fun onStartActionMode() {}
        fun onFinishActionMode() {}
    }
}
