package com.microsoft.notes.ui.note.reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.microsoft.fluentui.datetimepicker.DateTimePickerDialog
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.platform.extensions.px
import com.microsoft.notes.ui.note.reminder.TimeReminderSelectedFragment.Companion.CURRENT_NOTE_ID
import com.microsoft.notes.ui.note.reminder.TimeReminderSelectedFragment.Companion.SELECTED_DATE_TIME_LONG
import com.microsoft.notes.utils.dateTimeToTimeString
import com.microsoft.notes.utils.isToday
import com.microsoft.notes.utils.isTomorrow
import org.threeten.bp.Duration
import org.threeten.bp.Duration.ZERO
import org.threeten.bp.ZonedDateTime

open class ReminderFragment :
    BottomSheetDialogFragment(),
    DateTimePickerDialog.OnDateTimePickedListener,
    ReminderFragmentCallback {

    private lateinit var reminderPickerLayoutMain: View
    private lateinit var backButton: View

    private lateinit var dateTimePickerFragment: DateTimePickerFragment
    private lateinit var selectDateTimeButton: Button

    companion object {
        const val DATE_TIME_PICKER_FRAGMENT_TAG = "date_time_picker_fragment_tag"
    }

    private var currentNoteId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.reminder_fragment_bottomsheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reminderPickerLayoutMain = view.findViewById(R.id.reminderPickerLayoutMain)

        initReminderSuggestionChips(view)
        initSelectDateTimeOptionButton(view)

        initCloseButton(view)
        initBackButton(view)

        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                reminderPickerLayoutMain.visibility = View.VISIBLE
                backButton.visibility = View.GONE
            }
        }
    }

    private fun initSelectDateTimeOptionButton(view: View) {
        selectDateTimeButton = view.findViewById(R.id.setTimeReminder)
        val existingFragment = activity?.supportFragmentManager?.findFragmentByTag(DATE_TIME_PICKER_FRAGMENT_TAG)
        if (existingFragment == null) {
            dateTimePickerFragment = DateTimePickerFragment()
            dateTimePickerFragment.setListener(this)
        }
        selectDateTimeButton.setOnClickListener {
            showDateTimePickerFragment()
        }
    }

    private fun showDateTimePickerFragment() {
        if (!dateTimePickerFragment.isAdded) {
            activity?.supportFragmentManager?.let {
                dateTimePickerFragment.show(
                    it,
                    DATE_TIME_PICKER_FRAGMENT_TAG
                )
            }
        }
    }

    private fun initReminderSuggestionChips(view: View) {
        val chipsLinearLayout = view.findViewById<LinearLayout>(R.id.reminderSuggestionChipLayout)
        val timeSuggestions = getTimeReminderSuggestions(ZonedDateTime.now())
        val chipTexts = timeSuggestions.map {
            if (isToday(it)) {
                "${context?.getString(R.string.reminderChipLabelToday)} ${dateTimeToTimeString(it)}"
            } else if (isTomorrow(it)) "${context?.getString(R.string.reminderChipLabelTomorrow)} ${
            dateTimeToTimeString(
                it
            )
            }"
            else "${context?.getString(R.string.reminderChipLabelNextWeek)} ${
            dateTimeToTimeString(
                it
            )
            }"
        }

        for ((index, text) in chipTexts.withIndex()) {
            val individualFilterView: View = LayoutInflater.from(context)
                .inflate(R.layout.reminder_suggestion_filter_chip, null, false)
            val chip: Chip = individualFilterView.findViewById(R.id.reminder_filter_chip)

            chip.text = text
            chip.isClickable = true
            chip.isCheckable = false

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(4.px, 8.px, 4.px, 0.px)
            }
            chip.layoutParams = layoutParams

            chip.setOnClickListener {
                onDateTimePicked(timeSuggestions[index], ZERO)
            }
            chipsLinearLayout.addView(chip)
        }
    }

    private fun initCloseButton(view: View) {
        val closeButton: View = view.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun initBackButton(view: View) {
        backButton = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            childFragmentManager.popBackStack()
            reminderPickerLayoutMain.visibility = View.VISIBLE
            backButton.visibility = View.GONE
        }
    }

    fun setCurrentNoteId(id: String) {
        this.currentNoteId = id
    }

    override fun onDateTimePicked(dateTime: ZonedDateTime, duration: Duration) {
        if (dateTime.isBefore(ZonedDateTime.now())) {
            Toast.makeText(
                context,
                "${context?.getString(R.string.reminderTimeInPastMsg)}",
                Toast.LENGTH_LONG
            ).show()
        } else {
            TimeReminderSelectedFragment().apply {
                arguments = Bundle().apply {
                    putLong(SELECTED_DATE_TIME_LONG, dateTime.toInstant().toEpochMilli())
                    putString(CURRENT_NOTE_ID, currentNoteId)
                }
                setReminderFragmentCallback(this@ReminderFragment)
            }.also {
                childFragmentManager.beginTransaction()
                    .add(R.id.child_fragment_container, it)
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            }
            reminderPickerLayoutMain.visibility = View.GONE
            backButton.visibility = View.VISIBLE
        }
    }

    override fun onRequestClose() {
        popChildFragmentsIfPresent()
        if (isAdded)
            fragmentManager?.beginTransaction()?.remove(this)?.commit()
    }

    private fun popChildFragmentsIfPresent() {
        if (isAdded && childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
        }
    }
}

interface ReminderFragmentCallback {
    fun onRequestClose()
}
