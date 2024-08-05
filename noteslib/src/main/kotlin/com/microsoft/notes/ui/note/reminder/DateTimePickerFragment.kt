package com.microsoft.notes.ui.note.reminder

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.microsoft.fluentui.datetimepicker.DateTimePickerDialog

class DateTimePickerFragment : DialogFragment() {

    private var listener: DateTimePickerDialog.OnDateTimePickedListener? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return DateTimePickerDialog(
            requireContext(),
            DateTimePickerDialog.Mode.DATE_TIME,
            DateTimePickerDialog.DateRangeMode.NONE,
            org.threeten.bp.ZonedDateTime.now(),
            org.threeten.bp.Duration.ZERO
        ).apply {
            onDateTimePickedListener = listener
        }
    }

    fun setListener(listener: DateTimePickerDialog.OnDateTimePickedListener) {
        this.listener = listener
    }
}
