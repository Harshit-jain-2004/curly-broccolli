package com.microsoft.notes.richtext.editor

import android.app.AlertDialog
import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.render.ImageSpanWithMedia
import com.microsoft.notes.richtext.scheme.InlineMedia
import java.util.Locale

interface InlineMediaContextMenuCallbacks {
    fun updateMediaWithAltText(localId: String, altText: String)
    fun isSelectionOnlyImage(): Boolean
    fun getMediaListInCurrentSelection(): List<ImageSpanWithMedia>?
}

class InlineMediaContextMenuManager(
    val inlineMediaContextMenuCallBack: InlineMediaContextMenuCallbacks,
    val context: Context
) {

    val actionModeCallback = object : ActionMode.Callback {

        override fun onDestroyActionMode(mode: ActionMode?) {
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {

            if (inlineMediaContextMenuCallBack.isSelectionOnlyImage()) {
                showContextMenuForImage(menu)
                return true
            }
            showDefaultContextMenu(menu)
            return false
        }

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val inflater = mode?.menuInflater ?: return false
            inflater.inflate(R.menu.sn_inline_media_context_menu, menu)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.sn_menu_inline_media_alt_text -> {
                    showAltTextUI()
                    return true
                }
            }
            return false
        }
    }

    private fun showContextMenuForImage(contextMenu: Menu?) {
        contextMenu?.let {
            for (menuItemIndex in 0 until it.size()) {
                it.getItem(menuItemIndex)?.isVisible = false
            }
            it.findItem(R.id.sn_menu_inline_media_alt_text)?.isVisible = true
            it.findItem(R.id.sn_menu_inline_media_open)?.setVisible(false)
        }
    }

    private fun showDefaultContextMenu(contextMenu: Menu?) {
        contextMenu?.let {
            for (menuItemIndex in 0 until it.size()) {
                it.getItem(menuItemIndex)?.isVisible = true
            }
            it.findItem(R.id.sn_menu_inline_media_alt_text)?.isVisible = false
            it.findItem(R.id.sn_menu_inline_media_open)?.setVisible(false)
        }
    }

    private fun showAltTextUI() {
        val altTextDialogView = View.inflate(context, R.layout.sn_inline_media_alt_text_dialog_layout, null)
        val altTextDialog = AlertDialog.Builder(context, R.style.SNNotesAlertDialogStyle)
            .setView(altTextDialogView)
            .setTitle(R.string.sn_inline_media_notes_alttext)
            .setPositiveButton(
                context.getString(R.string.sn_inline_media_notes_done)
                    .uppercase(Locale.getDefault())
            ) { _, _ ->
                saveAltText(altTextDialogView)
            }
            .setNegativeButton(
                context.getString(R.string.sn_inline_media_notes_cancel)
                    .uppercase(Locale.getDefault())
            ) { _, _ ->
            }.create()

        altTextDialog.setOnShowListener {
            showPreviousAltText(altTextDialogView)
        }
        altTextDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        altTextDialog.show()
    }

    private fun showPreviousAltText(altTextDialogView: View) {
        val currentAltText = getMedia()?.altText
        val altTextEditText = altTextDialogView.findViewById<EditText>(R.id.sn_inline_media_alttext_edittext)
        altTextEditText.setText(currentAltText)
        altTextEditText.setSelection(altTextEditText.text.length)
    }

    private fun saveAltText(altTextDialogView: View) {
        val altTextEditText = altTextDialogView.findViewById<EditText>(R.id.sn_inline_media_alttext_edittext)
        val altText = altTextEditText.text
        setAltText(altText.toString())
    }

    private fun setAltText(altText: String) {
        getMedia()?.let {
            inlineMediaContextMenuCallBack.updateMediaWithAltText(it.localId, altText)
        }
    }

    private fun getMedia(): InlineMedia? {
        val images = inlineMediaContextMenuCallBack.getMediaListInCurrentSelection()
        return images?.first()?.media
    }
}
