package com.microsoft.notes.ui.feed.sourcefilter

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.extensions.hide
import com.microsoft.notes.ui.extensions.sendAccessibilityAnnouncement
import com.microsoft.notes.ui.feed.sourcefilter.FeedSourceFilterOption.ALL
import com.microsoft.notes.ui.feed.sourcefilter.FeedSourceFilterOption.ONENOTE_PAGES
import com.microsoft.notes.ui.feed.sourcefilter.FeedSourceFilterOption.SAMSUNG_NOTES
import com.microsoft.notes.ui.feed.sourcefilter.FeedSourceFilterOption.STICKY_NOTES
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import kotlinx.android.synthetic.main.sn_feed_source_filter_bottom_sheet.*

open class FeedSourceFilterFragment :
    BottomSheetDialogFragment(),
    FragmentApi {

    private val presenter: FeedSourceFilterPresenter by lazy {
        FeedSourceFilterPresenter(this)
    }

    private val optionToViewMapping: MutableMap<FeedSourceFilterOption, FeedSourceFilterOptionCheckbox> =
        mutableMapOf()

    var sourceFilter: FeedSourceFilterOption = ALL
        set(value) {
            field = value
            refreshCheckedState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setOrientationSpecificStyle(this.resources.configuration.orientation)
    }

    private fun setOrientationSpecificStyle(orientation: Int) {
        val style = if (orientation == Configuration.ORIENTATION_PORTRAIT)
            R.style.CustomBottomSheetDialogTheme
        else
            R.style.CustomBottomSheetDialogThemeLandscape
        setStyle(STYLE_NORMAL, style)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.sn_feed_source_filter_bottom_sheet, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setUpView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        presenter.recordTelemetry(eventMarker = EventMarkers.LaunchFilterUIBottomSheet)
        context?.sendAccessibilityAnnouncement(resources.getString(R.string.sn_filter_ui_options_displayed))
    }

    override fun onDetach() {
        super.onDetach()
        presenter.recordTelemetry(eventMarker = EventMarkers.DismissFilterUIBottomSheet)
        context?.sendAccessibilityAnnouncement(resources.getString(R.string.sn_filter_ui_options_dismissed))
    }

    private fun setUpView() {
        optionToViewMapping[ALL] = allFilterOption
        optionToViewMapping[STICKY_NOTES] = stickyNotesFilterOption
        optionToViewMapping[ONENOTE_PAGES] = pagesFilterOption

        if (NotesLibrary.getInstance().currentSamsungNotes.isEmpty()) {
            dividerSamsungNotesFilterOption.hide()
            samsungNotesFilterOption.hide()
        } else {
            optionToViewMapping[SAMSUNG_NOTES] = samsungNotesFilterOption
        }

        forEachOption { option, view ->
            view.setOnClickListener {
                filterOptionOnClickListener(option)
            }
            view.setFilterOption(option)
        }

        refreshCheckedState()
    }

    private fun forEachOption(func: (option: FeedSourceFilterOption, view: FeedSourceFilterOptionCheckbox) -> Unit) {
        for (mapping in optionToViewMapping) {
            func(mapping.key, mapping.value)
        }
    }

    private fun refreshCheckedState() {
        forEachOption { option, view ->
            view.isChecked = when (sourceFilter) {
                option -> true
                else -> false
            }
        }
    }

    private val filterOptionOnClickListener: (FeedSourceFilterOption) -> Unit = {
        presenter.updateSourceFilter(it)
        presenter.recordTelemetry(EventMarkers.FilterUITypeSet, Pair(NotesSDKTelemetryKeys.FeedUIProperty.FILTER_UI_TYPE, getFilterOptionLabel(it)))
        dismiss()
    }

    /**
     * To be used only for telemetry, Use FeedSourceFilterOptionCheckbox::getLabel(label) function
     */
    private fun getFilterOptionLabel(filterOption: FeedSourceFilterOption): String =
        when (filterOption) {
            ALL -> "All Notes"
            STICKY_NOTES -> "Sticky Notes"
            ONENOTE_PAGES -> "OneNote Pages"
            SAMSUNG_NOTES -> "Samsung Notes"
        }
}

interface FragmentApi
