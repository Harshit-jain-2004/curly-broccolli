package com.microsoft.notes.ui.feed.filter

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.extensions.hide
import com.microsoft.notes.ui.extensions.sendAccessibilityAnnouncement
import com.microsoft.notes.utils.accessibility.announceViewAsHeading
import com.microsoft.notes.utils.accessibility.setClassNameOfViewAsButton
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.utils.utils.UserInfoUtils
import com.microsoft.notes.utils.utils.getFeedSortingChipOptionColorStateList
import com.microsoft.notes.utils.utils.getFeedSortingTextOptionColorStateList
import kotlinx.android.synthetic.main.sn_feed_source_filter_comprehensive_bottom_sheet.*

open class FeedComprehensiveFilterFragment :
    BottomSheetDialogFragment(),
    FragmentApi {

    private val presenter: FeedComprehensiveFilterPresenter by lazy {
        FeedComprehensiveFilterPresenter(this)
    }

    var feedFilters = FeedFilters()
    var feedNoteTypeToFilterChipMapping: HashMap<FeedNotesTypeFilterOption, Chip?> = hashMapOf()
    lateinit var feedSelectedSort: Pair<SortingCriterion, SortingState>
    private var allowBottomSheetDragging = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setOrientationSpecificStyle(this.resources.configuration.orientation)
    }

    private fun setOrientationSpecificStyle(orientation: Int) {
        val isDarkThemeEnabled = NotesLibrary.getInstance().isDarkThemeEnabled()
        val isPortraitModeOn = (orientation == Configuration.ORIENTATION_PORTRAIT)

        val style = when {
            isDarkThemeEnabled && isPortraitModeOn -> R.style.CustomBottomSheetDialogThemeDark
            isDarkThemeEnabled && !isPortraitModeOn -> R.style.CustomBottomSheetDialogThemeLandscapeDark
            !isDarkThemeEnabled && !isPortraitModeOn -> R.style.CustomBottomSheetDialogThemeLandscape
            !isDarkThemeEnabled && isPortraitModeOn -> R.style.CustomBottomSheetDialogTheme
            else -> R.style.CustomBottomSheetDialogTheme
        }

        setStyle(STYLE_NORMAL, style)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.sn_feed_source_filter_comprehensive_bottom_sheet, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setUpView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        presenter.recordTelemetry(eventMarker = EventMarkers.LaunchFilterUIBottomSheet)
        context?.sendAccessibilityAnnouncement(resources.getString(R.string.sn_sort_filter_ui_options_displayed))
    }

    override fun onDetach() {
        super.onDetach()
        presenter.recordTelemetry(eventMarker = EventMarkers.DismissFilterUIBottomSheet)
        context?.sendAccessibilityAnnouncement(resources.getString(R.string.sn_sort_filter_ui_options_closed))
    }

    /**
     * onCreateDialog
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {
            val bottomSheetDialog = it as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let {
                val behaviour = BottomSheetBehavior.from(it)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                behaviour.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_DRAGGING -> {
                                if (!allowBottomSheetDragging) {
                                    behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                                }
                            }
                            BottomSheetBehavior.STATE_HIDDEN -> {
                                dismiss()
                            }
                        }
                    }
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }
                })
            }
        }
        return dialog
    }

    private fun setUpView() {
        // Display all filter options
        setUpNoteTypeFilters()
        setUpAccountFilters()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setUpBottomSheetColorTheme()
        }

        // Setup selection change listeners
        setFilterSelectionChangeListener(notes_type_filter)
        setFilterSelectionChangeListener(account_filter)

        // Setup selection application/clearing listeners
        apply_filter_sort_selections.setOnClickListener { onSelectionsApplied() }
        clear_selections_option.setOnClickListener {
            onSelectionsCleared()
            onSelectionsApplied()
        }

        // Setup sorting views
        setupSortingCriteria()
        setupOnScrollListener()

        setupAccessibility()
    }

    private fun setUpBottomSheetColorTheme() {
        val applyFilterSortSelectionsBg =
            apply_filter_sort_selections.background as? GradientDrawable
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.sort_filter_bottom_sheet_button_stroke)
        applyFilterSortSelectionsBg?.setStroke(
            strokeWidth,
            context?.getColor(NotesLibrary.getInstance().theme.primaryAppColor) ?: 0
        )
        val context = context
        if (context != null) {
            ntf_all.chipBackgroundColor = getFeedSortingChipOptionColorStateList(context)
            ntf_all.setTextColor(getFeedSortingTextOptionColorStateList(context))

            account_all.chipBackgroundColor = getFeedSortingChipOptionColorStateList(context)
            account_all.setTextColor(getFeedSortingTextOptionColorStateList(context))
        }
    }

    private fun setupAccessibility() {
        // Announce views as headings
        announceViewAsHeading(view?.findViewById(R.id.bottom_sheet_title))
        announceViewAsHeading(view?.findViewById(R.id.sort_title))
        announceViewAsHeading(view?.findViewById(R.id.filter_title))

        // Set className of clickable textviews as buttons
        setClassNameOfViewAsButton(view?.findViewById(R.id.clear_selections_option))
        for (i in 0 until (notes_type_filter?.childCount ?: 0)) {
            setClassNameOfViewAsButton(notes_type_filter?.getChildAt(i))
        }
        for (i in 0 until (account_filter?.childCount ?: 0)) {
            setClassNameOfViewAsButton(account_filter?.getChildAt(i))
        }
        setClassNameOfViewAsButton(view?.findViewById(R.id.sort_by_modification_date_option))
        setClassNameOfViewAsButton(view?.findViewById(R.id.sort_by_created_date_option))
        setClassNameOfViewAsButton(view?.findViewById(R.id.sort_by_title_option))
        setClassNameOfViewAsButton(view?.findViewById(R.id.apply_filter_sort_selections))
    }

    private fun setupOnScrollListener() {
        filter_sort_panel.viewTreeObserver.addOnScrollChangedListener {
            val scrollY = filter_sort_panel.scrollY
            allowBottomSheetDragging = scrollY <= 0
        }
    }

    /**
     * setUpNoteTypeFilters
     * This function creates chips for all FeedSourceFilterOption values
     * selectedSourceFilters keeps track of selected options, it is used to set chips as checked/unchecked
     * If none of the note types are selected (sticky notes, samsung notes, note references) then we set ALL selected by default
     */
    private fun setUpNoteTypeFilters() {
        var anyNotesTypesSelected: Boolean = false

        feedFilters.selectedNotesTypeFilters.forEach {
            // The option 'All' is treated differently, as its value depends on other selections. So initializing it from the xml instead of dynamically
            if (it.key == FeedNotesTypeFilterOption.ALL) {
                val defaultChip: Chip? = notes_type_filter?.getChildAt(0) as Chip?
                feedNoteTypeToFilterChipMapping.put(FeedNotesTypeFilterOption.ALL, defaultChip)
                return@forEach
            }

            // The option 'Samsung Notes' is only to be shown if Samsung Notes are actually present in the feed
            if (it.key == FeedNotesTypeFilterOption.SAMSUNG_NOTES && NotesLibrary.getInstance().currentSamsungNotes.isEmpty()) return@forEach

            // Create chips for SN, Samsung Notes and Pages, followed by adding them to the notes_type_filter
            val nonDefaultChip: Chip? = FilterChipView.createFilterChip(context = context, label = it.key.toString(), isChecked = it.value)
            notes_type_filter?.addView(nonDefaultChip)
            feedNoteTypeToFilterChipMapping.put(it.key, nonDefaultChip)
            anyNotesTypesSelected = anyNotesTypesSelected.or(it.value ?: false)
        }

        // If no other option is selected, check All
        if (!anyNotesTypesSelected) {
            ntf_all.isChecked = true
            feedFilters.selectedNotesTypeFilters.set(FeedNotesTypeFilterOption.ALL, true)
        }
    }

    /**
     * setUpNoteTypeFilters
     * This function creates chips for all signed in accounts
     * selectedSignedInEmails keeps track of selected options, it is used to set chips as checked/unchecked
     * If none of the signed in accounts are selected then we set ALL selected by default
     */
    private fun setUpAccountFilters() {
        var areAnyAccountsSelected: Boolean = false
        val signedInUserIDs: Set<String> = if (isCombinedListForMultiAccountEnabled()) {
            context?.let { UserInfoUtils.getSignedInUsers(it) } ?: setOf()
        } else {
            setOf(NotesLibrary.getInstance().currentUserID)
        }

        // From the last filter display to now, if any signed accounts have been removed, then remove it from the selectedSignedInEmails map as well
        feedFilters.selectedUserIDs = feedFilters.selectedUserIDs.filter { signedInUserIDs.contains(it.key) } as HashMap<String, Boolean>

        // Add newly signed in accounts to the selectedSignedInEmails map, and create the filter chips for the same
        signedInUserIDs.forEach {
            if (!feedFilters.selectedUserIDs.keys.contains(it))
                feedFilters.selectedUserIDs.put(it, false)
        }

        if (feedFilters.selectedUserIDs.size < 2) {
            account_filter_title?.hide()
            account_filter?.hide()
            return
        }

        feedFilters.selectedUserIDs.forEach {
            val userID = it.key
            val userEmailID: String = context?.let { UserInfoUtils.getEmailIDFromUserID(userID, it) } ?: String()
            if (userEmailID.isNotEmpty()) {
                account_filter?.addView(FilterChipView.createFilterChip(context = context, label = userEmailID, isChecked = it.value))
                areAnyAccountsSelected = areAnyAccountsSelected.or(it.value)
            }
        }

        // If no other option is selected, check All
        if (!areAnyAccountsSelected)
            account_all.isChecked = true
    }

    /**
     * setupSortingCriteria
     * Sorting options are shown to the users as FeedSortingCriterionView views.
     * FeedSortingCriterionView consist of
     * - Title of the sorting category (e.g. Modification date, Created date and Title)
     * - Indicator of the sorting state (e.g. ascending, descending, or none)
     * - Bottom divider
     *
     * setupSortingCriteria sets up these views with their titles, current sorting state and clicklistener
     */
    private fun setupSortingCriteria() {
        feedSelectedSort = FeedSortCache.fetchPreferredSortSelection(context)
        SortingCriterion.values().forEach { sortingCriterion ->
            val sortingView: FeedSortingCriterionView? = getViewForSortingCriterion(sortingCriterion)
            val sortingState = if (sortingCriterion == feedSelectedSort.first) feedSelectedSort.second else SortingState.DISABLED

            sortingView?.setSortCriterionTitle(getString(sortingCriterion.getLabelResource()))
            sortingView?.sortingCriterion = sortingCriterion
            sortingView?.sortingState = sortingState
            sortingView?.contentDescription = getContentDescriptionForSortingView(sortingCriterion, sortingState)
            sortingView?.setOnClickListener { onSortingCriterionClicked(sortingCriterion) }
        }
    }

    /**
     * setFilterSelectionChangeListener
     * Filter chips allow for multi-selection, which is what we need, but this doesn't make sense for the option of ALL
     * So logic here is (The default option for filters here is ALL):
     * Operation                Result
     * Select ALL               Unselect other options, select ALL
     * Select other option      Unselect ALL, select other option
     * Unselect other option    Unselect other option, and if no other options are selected, then select ALL
     * Unselect ALL             No-op
     *
     * Note : We are using setOnClickListener on individual elements because :
     * - ChipGroup.onCheckedChangeListener only works for single selection ChipGroups, and hence can't be used here
     * - Chip.onCheckedChangeListener is called if the isChecked value is changed for the chip manually or programmatically. So logic
     *      requiring manipulation like below could cause an infinite loop
     */
    private fun setFilterSelectionChangeListener(chipGroup: ChipGroup) {
        if (chipGroup.childCount < 1)
            return

        val defaultChip: Chip = chipGroup.getChildAt(0) as Chip
        val nonDefaultChips: ArrayList<Chip> = arrayListOf()
        for (i in 1 until chipGroup.childCount) {
            val chip: Chip? = chipGroup.getChildAt(i) as Chip?
            chip?.let { nonDefaultChips.add(it) }
        }

        defaultChip.setOnClickListener {
            if (defaultChip.isChecked) nonDefaultChips.forEach { it.isChecked = false }
            else defaultChip.isChecked = true
        }

        nonDefaultChips.forEach {
            it.setOnClickListener { nonDefaultChipView ->
                val nonDefaultChip = nonDefaultChipView as Chip
                if (nonDefaultChip.isChecked) defaultChip.isChecked = false
                else {
                    if (nonDefaultChips.any { it.isChecked }) return@setOnClickListener
                    else defaultChip.isChecked = true
                }
            }
        }
    }

    /**
     * onSelectionsApplied
     * Updates the local maps of filter options and their selection values (selectedSourceFilters, selectedSignedInEmails)
     * Makes call to presenter to update the Feed list based on said filter selections
     * Finally dismisses Filter/Sort BottomSheet as selection making is done!
     */
    fun onSelectionsApplied() {
        for (i in 0 until (notes_type_filter?.childCount ?: 0)) {
            val chip: Chip = notes_type_filter?.getChildAt(i) as Chip
            feedFilters.selectedNotesTypeFilters.put(feedNoteTypeToFilterChipMapping.filter { it.value == chip }.keys.first(), chip.isChecked)
        }

        if ((account_filter?.childCount ?: 0) > 1) {
            for (i in 1 until (account_filter?.childCount ?: 0)) {
                val chip: Chip = account_filter?.getChildAt(i) as Chip

                val userID: String = context?.let { UserInfoUtils.getUserIDFromEmailID(chip.text.toString(), it) } ?: String()

                if (!userID.isNullOrEmpty())
                    feedFilters.selectedUserIDs[userID] = chip.isChecked
            }
        }

        NotesLibrary.getInstance().recordTelemetry(
            EventMarkers.FilterSelectionsUpdated,
            Pair(NotesSDKTelemetryKeys.FilterProperty.ALL_SELECTION_STATUS, feedFilters.selectedNotesTypeFilters[FeedNotesTypeFilterOption.ALL].toString()),
            Pair(NotesSDKTelemetryKeys.FilterProperty.STICKYNOTES_SELECTION_STATUS, feedFilters.selectedNotesTypeFilters[FeedNotesTypeFilterOption.STICKY_NOTES].toString()),
            Pair(NotesSDKTelemetryKeys.FilterProperty.SAMSUNGNOTES_SELECTION_STATUS, feedFilters.selectedNotesTypeFilters[FeedNotesTypeFilterOption.SAMSUNG_NOTES].toString()),
            Pair(NotesSDKTelemetryKeys.FilterProperty.PAGES_SELECTION_STATUS, feedFilters.selectedNotesTypeFilters[FeedNotesTypeFilterOption.ONENOTE_PAGES].toString()),
            Pair(NotesSDKTelemetryKeys.FilterProperty.ACCOUNT_FILTER_SELECTION_STATUS, feedFilters.isAnyAccountFilterSelected().toString()),
            Pair(
                NotesSDKTelemetryKeys.FilterProperty.FILTER_UPDATE_SOURCE,
                NotesSDKTelemetryKeys.FilterProperty.FILTER_SORT_PANEL
            )
        )
        FeedSortCache.updatePreferredSortSelection(context, feedSelectedSort)
        presenter.updateComprehensiveFilter(feedFilters, true)
        dismiss()
    }

    /**
     * onSelectionsCleared
     * Reset all filter options to default (ALL)
     * Does not dismiss Filter/Sort BottomSheet as selections may still be made
     */
    fun onSelectionsCleared() {
        ntf_all.isChecked = true
        for (i in 1 until (notes_type_filter?.childCount ?: 0)) {
            val chip: Chip = notes_type_filter?.getChildAt(i) as Chip
            chip.isChecked = false
        }

        account_all.isChecked = true
        for (i in 1 until (account_filter?.childCount ?: 0)) {
            val chip: Chip = account_filter?.getChildAt(i) as Chip
            chip.isChecked = false
        }
        context?.let {
            FeedSortCache.resetCachedPreferredSortSelection(it)
            feedSelectedSort = FeedSortCache.fetchPreferredSortSelection(it)
        }
    }

    /**
     * onSortingCriterionClicked
     * Logic :
     * SORTING OPTION           INITIAL STATE       OPERATION   FINAL STATE
     * Title                    DISABLED            Tap         ENABLED_ASCENDING
     * Created/Modified Date    DISABLED            Tap         ENABLED_DESCENDING
     * Any                      ENABLED_ASCENDING   Tap         ENABLED_DESCENDING
     * Any                      ENABLED_DESCENDING  Tap         ENABLED_ASCENDING
     * Any                      Any state           Tap         DISABLED
     *
     * onSortingCriterionClicked sets the correct sortingState values in all sorting views,
     * for displaying the updated value to the user.
     */
    private fun onSortingCriterionClicked(selectedSortingCriterion: SortingCriterion) {
        var selectedSortingState: SortingState = SortingState.DISABLED
        SortingCriterion.values().forEach {
            var currentSortingState: SortingState = SortingState.DISABLED
            if (it == selectedSortingCriterion) {
                selectedSortingState =
                    if (feedSelectedSort.first == it) {
                        when (feedSelectedSort.second) {
                            SortingState.ENABLED_ASCENDING -> SortingState.ENABLED_DESCENDING
                            else -> SortingState.ENABLED_ASCENDING
                        }
                    } else {
                        when (it) {
                            SortingCriterion.TITLE -> SortingState.ENABLED_ASCENDING
                            else -> SortingState.ENABLED_DESCENDING
                        }
                    }
                currentSortingState = selectedSortingState
            }

            getViewForSortingCriterion(it)?.let {
                it.sortingState = currentSortingState
                it.contentDescription = getContentDescriptionForSortingView(it.sortingCriterion, it.sortingState)
                if (it.sortingState != SortingState.DISABLED) {
                    it.announceForAccessibility(it.contentDescription)
                }
            }
        }
        feedSelectedSort = Pair(selectedSortingCriterion, selectedSortingState)
    }

    /**
     * getViewForSortingCriterion
     * getViewForSortingCriterion maps Sorting criteria with corresponding views in the FilterSort panel
     */
    private fun getViewForSortingCriterion(sortingCriterion: SortingCriterion): FeedSortingCriterionView? {
        return when (sortingCriterion) {
            SortingCriterion.DATE_MODIFIED -> sort_by_modification_date_option
            SortingCriterion.DATE_CREATED -> sort_by_created_date_option
            SortingCriterion.TITLE -> sort_by_title_option
        }
    }

    private fun getContentDescriptionForSortingView(sortingCriterion: SortingCriterion, sortingState: SortingState): String? =
        getString(
            sortingState.getContentDescriptionResource(),
            getString(sortingCriterion.getLabelResource())
        )

    val isCombinedListForMultiAccountEnabled: () -> Boolean = {
        NotesLibrary.getInstance().experimentFeatureFlags.combinedListForMultiAccountEnabled
    }
}

interface FragmentApi
