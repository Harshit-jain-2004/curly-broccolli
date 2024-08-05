package com.microsoft.notes.ui.note.options

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.noteslib.extensions.showClearCanvasButton
import com.microsoft.notes.noteslib.extensions.showDeleteButton
import com.microsoft.notes.noteslib.extensions.showFeedbackButton
import com.microsoft.notes.noteslib.extensions.showSearchInNoteButton
import com.microsoft.notes.noteslib.extensions.showShareButton
import com.microsoft.notes.ui.extensions.getHasImagesTelemetryValue
import com.microsoft.notes.ui.extensions.isSamsungNote
import com.microsoft.notes.ui.extensions.sendAccessibilityAnnouncement
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.HostTelemetryKeys
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import kotlinx.android.synthetic.main.sn_note_options_bottom_sheet.*
import kotlinx.android.synthetic.main.sn_note_options_bottom_sheet.view.*

/**
 * NoteOptionsFragment can be used directly, however, if you need to extend it for your use-case, please include
 * the following in the parent layout:
 * <include layout="@layout/sn_note_options_bottom_sheet" />
 */
open class NoteOptionsFragment :
    BottomSheetDialogFragment(),
    FragmentApi,
    NoteColorPicker.NoteColorPickerListener {

    companion object {
        const val FRAGMENT_NAME = "EDIT_NOTE"
    }

    private var currentNoteId: String? = null

    protected fun getCurrentNoteId(): String? = currentNoteId

    fun setCurrentNoteId(currentNoteId: String) {
        this.currentNoteId = currentNoteId
    }

    protected open val presenter: NoteOptionsPresenter by lazy {
        NoteOptionsPresenter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.SNSettingsBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.sn_note_options_bottom_sheet, container, false)
        setUpView(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (getCurrentNote().isSamsungNote()) {
            colorPicker.visibility = View.GONE
        } else {
            colorPicker.visibility = View.VISIBLE
        }
        if (getCurrentNote()?.isInkNote == true && NotesLibrary.getInstance().showClearCanvasButton()) {
            clearCanvas.visibility = View.VISIBLE
        } else {
            clearCanvas.visibility = View.GONE
        }
        if (NotesLibrary.getInstance().showFeedbackButton()) {
            sendFeedback.visibility = View.VISIBLE
        } else {
            sendFeedback.visibility = View.GONE
        }
        if (NotesLibrary.getInstance().showSearchInNoteButton()) {
            searchInNote.visibility = View.VISIBLE
        } else {
            searchInNote.visibility = View.GONE
        }
        if (NotesLibrary.getInstance().showShareButton() && getCurrentNote()?.isInkNote != true) {
            shareNote.visibility = View.VISIBLE
        } else {
            shareNote.visibility = View.GONE
        }
        if (NotesLibrary.getInstance().showDeleteButton() && !getCurrentNote().isSamsungNote()) {
            deleteNote.visibility = View.VISIBLE
        } else {
            deleteNote.visibility = View.GONE
        }
        if (NotesLibrary.getInstance().showDeleteButton() && getCurrentNote().isSamsungNote()) {
            dismissSamsungNote.visibility = View.VISIBLE
        } else {
            dismissSamsungNote.visibility = View.GONE
        }
        val accessibilityManager = context?.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (accessibilityManager.isEnabled) {
            closeOptionsDivider.visibility = View.VISIBLE
            closeOptions.visibility = View.VISIBLE
        } else {
            closeOptionsDivider.visibility = View.GONE
            closeOptions.visibility = View.GONE
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        colorPicker.setListener(this)
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    @Suppress("UnsafeCast")
    override fun onResume() {
        super.onResume()
        presenter.onResume()
        setBehavior()
        colorPicker.setSelectedColor(color = presenter.note?.color ?: Color.getDefault())
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        announceDisplayNoteOptions()
    }

    override fun onDetach() {
        super.onDetach()
        announceDismissNoteOptions()
        NotesLibrary.getInstance().sendNoteOptionsDismissedAction()
    }

    private fun announceDisplayNoteOptions() {
        recordTelemetryEvent(eventMarker = EventMarkers.LaunchBottomSheet)
        context?.sendAccessibilityAnnouncement(resources.getString(R.string.sn_note_options_displayed))
    }

    private fun announceDismissNoteOptions() {
        recordTelemetryEvent(eventMarker = EventMarkers.DismissBottomSheet)
        context?.sendAccessibilityAnnouncement(resources.getString(R.string.sn_note_options_dismissed))
    }

    @Suppress("LongMethod")
    private fun setUpView(view: View) {
        with(view) {
            searchInNote.setOnClickListener {
                searchInNote()
                dismiss()
            }
            shareNote.setOnClickListener {
                shareNote()
                dismiss()
            }
            deleteNote.setOnClickListener { deleteNote() }
            dismissSamsungNote.setOnClickListener { deleteSamsungNote() }
            sendFeedback.setOnClickListener {
                sendFeedback()
                dismiss()
            }
            clearCanvas.setOnClickListener {
                clearCanvas()
                dismiss()
            }

            closeOptions.setOnClickListener {
                dismiss()
            }
            // This is used over the android:drawableStart attribute due to vector incompatibility with pre-Lollipop
            clearCanvas.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(
                    context,
                    R.drawable
                        .sn_ic_clear_canvas
                ),
                null, null, null
            )
            closeOptions.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(
                    context,
                    R.drawable
                        .sn_ic_close_options
                ),
                null, null, null
            )
            searchInNote.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(
                    context,
                    R.drawable
                        .sn_ic_search
                ),
                null, null, null
            )
            shareNote.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(
                    context,
                    R.drawable
                        .sn_ic_export
                ),
                null, null, null
            )
            deleteNote.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(
                    context,
                    R.drawable
                        .sn_ic_delete
                ),
                null, null, null
            )
            dismissSamsungNote.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(
                    context,
                    R.drawable
                        .samsung_ic_remove
                ),
                null, null, null
            )
            sendFeedback.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(
                    context,
                    R.drawable
                        .sn_ic_send_feedback
                ),
                null, null, null
            )
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun setBehavior() {
        val behavior = BottomSheetBehavior.from(view!!.parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onColorSelected(color: Color) {
        presenter.updateNoteColor(color)
    }

    private fun clearCanvas() {
        presenter.clearCanvas()
    }

    private fun searchInNote() {
        NotesLibrary.getInstance().sendNoteOptionsSearchInNoteAction()
    }

    private fun shareNote() {
        presenter.shareNote(activity as Activity)
    }

    private fun deleteNote() {
        fun sendTelemetryForCancellation() {
            recordTelemetryEvent(
                EventMarkers.DeleteNoteCancelled,
                Pair(
                    NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES,
                    getCurrentNote()?.getHasImagesTelemetryValue() ?: "false"
                ),
                Pair(HostTelemetryKeys.TRIGGER_POINT, FRAGMENT_NAME)
            )
        }

        recordTelemetryEvent(
            EventMarkers.DeleteNoteTriggered,
            Pair(
                NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES,
                getCurrentNote()?.getHasImagesTelemetryValue() ?: "false"
            ),
            Pair(HostTelemetryKeys.TRIGGER_POINT, FRAGMENT_NAME)
        )

        context?.let {
            openDeleteNoteDialog(false, it, {
                presenter.deleteNote()
                dismiss()
            }, { sendTelemetryForCancellation() })
        }
    }

    fun deleteSamsungNote() {
        fun sendTelemetryForCancellation() {
            recordTelemetryEvent(
                EventMarkers.DismissSamsungNoteCancelled,
                Pair(
                    NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES,
                    "false"
                ), // TODO 03-Feb-21 gopalsa: Fix when Samsung Notes support HTML media notes
                Pair(HostTelemetryKeys.TRIGGER_POINT, FRAGMENT_NAME)
            )
        }

        recordTelemetryEvent(
            EventMarkers.DismissSamsungNoteTriggered,
            Pair(
                NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES,
                "false"
            ), // TODO 03-Feb-21 gopalsa: Fix when Samsung Notes support HTML media notes
            Pair(HostTelemetryKeys.TRIGGER_POINT, FRAGMENT_NAME)
        )

        context?.let {
            if (dontShowDismissDialog(it)) {
                presenter.deleteSamsungNote()
                dismiss()
            } else {
                openDismissSamsungNoteDialog(
                    context = it,
                    onSuccess = {
                        presenter.deleteSamsungNote()
                        dismiss()
                    },
                    onCancel = { sendTelemetryForCancellation() }
                )
            }
        }
    }

    private fun sendFeedback() {
        NotesLibrary.getInstance().sendNoteOptionsSendFeedbackAction()
    }

    // ---- FragmentApi ----//
    override fun getCurrentNote(): Note? {
        val safeCurrentNoteId = currentNoteId
        return if (safeCurrentNoteId != null) {
            NotesLibrary.getInstance().getNoteById(safeCurrentNoteId)
        } else {
            null
        }
    }

    override fun setCurrentColor(color: Color) {
        // colorPicker might be null depending on lifecycle stage
        colorPicker?.setSelectedColor(color)
    }

    private fun recordTelemetryEvent(eventMarker: EventMarkers, vararg keyValuePairs: Pair<String, String>) {
        presenter.recordTelemetry(eventMarker, *keyValuePairs)
    }
}
