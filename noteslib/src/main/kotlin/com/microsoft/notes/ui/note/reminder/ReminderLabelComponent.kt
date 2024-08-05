package com.microsoft.notes.ui.note.reminder

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.microsoft.notes.models.ReminderWrapper
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.theme.ThemedLinearLayout
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderLabelComponent(context: Context, attrs: AttributeSet) : ThemedLinearLayout(context, attrs) {

    fun setReminderLayout(reminder: ReminderWrapper?) {
        val reminderLabel: View? = findViewById(R.id.reminderLabel)
        val reminderText: TextView? = findViewById(R.id.reminderText)
        if (NotesLibrary.getInstance().experimentFeatureFlags.enableReminderInNotes && shouldShowReminderIcon(reminder)) {
            if (shouldShowReminderIcon(reminder)) {
                reminderLabel?.visibility = VISIBLE
            }
            reminderText?.setText(getReminderLabelText(reminder))
        } else {
            reminderLabel?.visibility = GONE
        }
    }

    private fun getReminderLabelText(reminder: ReminderWrapper?): String {
        reminder ?: return ""
        reminder.timeReminder?.let { timeReminder ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val instant = Instant.ofEpochMilli(timeReminder.reminderDateTime)
                val reminderZonedDateTime = instant.atZone(ZoneId.systemDefault())

                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)
                val reminderDate = reminderZonedDateTime.toLocalDate()
                val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
                return when {
                    reminderDate.equals(today) -> "${context.resources.getString(R.string.reminderChipLabelToday)} ${timeFormatter.format(reminderZonedDateTime)}"
                    reminderDate.equals(tomorrow) -> "${context.resources.getString(R.string.reminderChipLabelTomorrow)} ${timeFormatter.format(reminderZonedDateTime)}"
                    else -> "${dateFormatter.format(reminderZonedDateTime)} ${timeFormatter.format(reminderZonedDateTime)}"
                }
            }
        }
        return ""
    }

    private fun shouldShowReminderIcon(reminder: ReminderWrapper?): Boolean {
        return when {
            reminder == null -> false
            reminder.timeReminder != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val timeReminder = reminder.timeReminder
                if (timeReminder != null) {
                    val reminderTimeInstant = Instant.ofEpochMilli(timeReminder.reminderDateTime)
                    val currentTimeInstant = Instant.now()
                    return reminderTimeInstant == currentTimeInstant || reminderTimeInstant.isAfter(
                        currentTimeInstant
                    )
                } else return false
            }
            else -> // Handle other reminder types
                false
        }
    }
}
