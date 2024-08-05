package com.microsoft.notes.ui.note.reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.utils.epochToDate
import com.microsoft.notes.utils.epochToTime

class TimeReminderSelectedFragment : Fragment() {
    companion object {
        const val SELECTED_DATE_TIME_LONG = "SELECTED_DATE_TIME_LONG"
        const val CURRENT_NOTE_ID = "CURRENT_NOTE_ID"
    }

    private lateinit var reminderFragmentCallback: ReminderFragmentCallback

    val presenter: ReminderPresenter by lazy {
        ReminderPresenter()
    }

    fun setReminderFragmentCallback(callback: ReminderFragmentCallback) {
        this.reminderFragmentCallback = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.time_reminder_selected_fragment, container, false)

        val addReminderBtn: View = view.findViewById(R.id.setReminder)
        val selectedDate: TextView = view.findViewById(R.id.selectedDate)
        val selectedTime: TextView = view.findViewById(R.id.selectedTime)

        val dateTime: Long = arguments?.getLong(SELECTED_DATE_TIME_LONG) ?: return view
        presenter.currentNoteId = arguments?.getString(CURRENT_NOTE_ID) ?: return view
        dateTime.let {
            selectedDate.text = epochToDate(it)
            selectedTime.text = epochToTime(it)
        }
        addReminderBtn.setOnClickListener {
            dateTime.let { presenter.addTimeReminder(it) }
            dateTime.let {
                Toast.makeText(
                    context,
                    "${context?.getString(R.string.reminderSetForToastMsg)} : ${epochToDate(it)} ${
                    epochToTime(
                        it
                    )
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
            reminderFragmentCallback.onRequestClose()
        }
        return view
    }
}
