package com.microsoft.notes.ui.note.options

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.utils.utils.Constants.SAMSUNG_DISMISS_DIALOG_STATUS
import java.util.Locale

/**
 * Opens a dialog for deleting a note. Does not have delete logic.
 * Should be used if you need the dialog without having to use the
 * bottom sheet.
 */
fun openDeleteNoteDialog(
    isFeed: Boolean,
    context: Context,
    onSuccess: (() -> Unit),
    onCancel: (() -> Unit)? = null
) {
    context?.let {
        val builder = AlertDialog.Builder(it)
        if (isFeed) {
            builder.setTitle(it.getString(R.string.sn_delete_sticky_note_dialog))
            builder.setMessage(it.getString(R.string.sn_delete_sticky_note_description))
        } else {
            builder.setTitle(it.getString(R.string.sn_delete_note_dialog))
            builder.setMessage(it.getString(R.string.sn_delete_note_description))
        }
        builder.setPositiveButton(it.getString(R.string.sn_action_delete_note).toUpperCase()) { _, _ ->
            onSuccess()
        }
        builder.setNegativeButton(it.getString(R.string.sn_dialog_cancel).toUpperCase()) { _, _ ->
            onCancel?.invoke()
        }
        builder.setOnCancelListener { onCancel?.invoke() }
        builder.show()
    }
}

fun openDeleteNotesDialog(
    context: Context,
    onSuccess: (() -> Unit),
    onCancel: (() -> Unit)? = null
) {
    context.let {
        val builder = AlertDialog.Builder(it)
        builder.setTitle(it.getString(R.string.sn_delete_notes_dialog))
        builder.setMessage(it.getString(R.string.sn_delete_notes_description))
        builder.setPositiveButton(it.getString(R.string.sn_action_delete_note).toUpperCase()) { _, _ ->
            onSuccess()
        }
        builder.setNegativeButton(it.getString(R.string.sn_dialog_cancel).toUpperCase()) { _, _ ->
            onCancel?.invoke()
        }
        builder.setOnCancelListener { onCancel?.invoke() }
        builder.show()
    }
}

fun openDismissSamsungNoteDialog(
    context: Context,
    onSuccess: (() -> Unit),
    onCancel: (() -> Unit)? = null
): Unit = context?.let {
    val mView: View = LayoutInflater.from(it).inflate(
        R.layout.samsung_dismiss_alert_checkbox,
        null
    )
    val builder = AlertDialog.Builder(it)
    builder.setTitle(it.getString(R.string.samsung_dismiss_note_title))
    builder.setMessage(it.getString(R.string.samsung_dismiss_note_description))
    builder.setView(mView)
    builder.setPositiveButton(
        it.getString(R.string.samsung_action_dismiss_note).toUpperCase()
    ) { _, _ ->
        // We store checkbox preference only if positive button is clicked.
        // If user clicks cancel/outside, as his intention is not clear,
        // we discard his preference and harmlessly show the alert dialog again.
        val checkBox: CheckBox = mView.findViewById(R.id.dont_show_checkBox)
        if (checkBox.isChecked) {
            storeDismissDialogStatus(true, context)
        }
        onSuccess()
    }
    builder.setNegativeButton(
        it.getString(R.string.sn_dialog_cancel).uppercase(Locale.getDefault())
    ) { _, _ -> }
    builder.setOnCancelListener { onCancel?.invoke() }
    builder.show()
}

private const val DONT_SHOW_DISMISS_DIALOG = "dont_show_dismiss_dialog"

private fun storeDismissDialogStatus(isChecked: Boolean, context: Context) {
    val mSharedPreferences: SharedPreferences = context.getSharedPreferences(
        SAMSUNG_DISMISS_DIALOG_STATUS,
        MODE_PRIVATE
    )
    val mEditor = mSharedPreferences.edit()
    mEditor.putBoolean(DONT_SHOW_DISMISS_DIALOG, isChecked)
    mEditor.apply()
}

fun dontShowDismissDialog(context: Context): Boolean =
    context.getSharedPreferences(SAMSUNG_DISMISS_DIALOG_STATUS, MODE_PRIVATE).getBoolean(DONT_SHOW_DISMISS_DIALOG, false)
