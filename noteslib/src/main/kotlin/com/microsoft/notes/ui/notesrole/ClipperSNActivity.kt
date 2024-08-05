package com.microsoft.notes.ui.notesrole

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.sideeffect.ui.EditNote
import com.microsoft.notes.ui.note.edit.EditNoteFragment
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.NotesRole

abstract class ClipperSNActivity : AppCompatActivity() {
    private val minLineCount = 5
    private val maxExpandedCanvasLineCount = 18

    companion object {
        private const val CLIPPER_SHARED_TEXT = ".clipper_shared_text"
        private const val MEDIA_FILE_PATH = ".media_file_path"
    }

    var mEditNoteFragment: EditNoteFragment? = null

    abstract val editNoteBindings: EditNote

    private fun setExpandableCanvas(isImageInsertedInCanvas: Boolean) {
        if (isImageInsertedInCanvas) return
        val notesEditText = mEditNoteFragment?.findNoteStyledView()?.getNotesEditText()
        notesEditText?.gravity = Gravity.TOP
        var previousLineCount = 0

        notesEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                charSequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(editable: Editable?) {
                val currentLineCount = notesEditText.lineCount ?: 0
                val lineCountDifference = currentLineCount - previousLineCount
                if (lineCountDifference != 0) {
                    val layoutParams =
                        findViewById<FrameLayout>(R.id.edit_note_fragment).layoutParams as ViewGroup.LayoutParams

                    when {
                        currentLineCount > previousLineCount && currentLineCount >= minLineCount -> {
                            layoutParams.height = Math.min(
                                layoutParams.height + (
                                    notesEditText.textSize
                                        ?: 0f
                                    ) * lineCountDifference,
                                applicationContext.resources.getDimension(R.dimen.clipper_sticky_note_max_height)
                            ).toInt()
                        }

                        currentLineCount < previousLineCount && previousLineCount >= minLineCount && previousLineCount < maxExpandedCanvasLineCount -> {
                            layoutParams.height = Math.max(
                                layoutParams.height + (
                                    notesEditText.textSize
                                        ?: 0f
                                    ) * lineCountDifference,
                                applicationContext.resources.getDimension(R.dimen.clipper_sticky_note_height)
                            ).toInt()
                        }
                    }
                    findViewById<FrameLayout>(R.id.edit_note_fragment).layoutParams = layoutParams
                    previousLineCount = currentLineCount
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null || intent == null) {
            return
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.clipper_sn_canvas)
    }

    fun initSNCanvas(packageName: String, primaryNotesId: String) {
        val clipperSharedText = packageName + CLIPPER_SHARED_TEXT
        val mediaFilePath = packageName + MEDIA_FILE_PATH
        val clipperNote = if (intent.getStringExtra(clipperSharedText) != null)
            NotesLibrary.getInstance()
                .addNote(primaryNotesId, intent.getStringExtra(clipperSharedText))
        else NotesLibrary.getInstance().addNote(primaryNotesId)

        clipperNote.then { note ->
            mEditNoteFragment = EditNoteFragment()
            mEditNoteFragment?.setCurrentNoteId(note.localId)
            mEditNoteFragment?.let { fragment ->
                supportFragmentManager.beginTransaction()
                    .add(R.id.edit_note_fragment, fragment)
                    .commit()
            }
            val isImageInsertedInCanvas = resizeCanvasAndAddMedia(
                intent.getStringArrayListExtra(mediaFilePath)?.toList(),
                false
            )

            mEditNoteFragment?.onNavigateToTransitionCompleted(true)
            mEditNoteFragment?.viewListener = EditNoteFragment.IOnViewCreatedListener {
                setExpandableCanvas(isImageInsertedInCanvas)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        NotesLibrary.getInstance().addUiBindings(editNoteBindings)
    }

    override fun onStop() {
        super.onStop()
        NotesLibrary.getInstance().removeUiBindings(editNoteBindings)
    }

    abstract fun switchToFloatModeAndSaveNote()

    fun resizeCanvasAndAddMedia(
        mediaList: List<String>?,
        isImageInsertedFromCamera: Boolean,
        triggerPoint: String ? = null
    ): Boolean {
        if (!mediaList.isNullOrEmpty()) {
            mEditNoteFragment?.addPhotosToNote(mediaList, false)
            val layoutParams =
                findViewById<FrameLayout>(R.id.edit_note_fragment).layoutParams as ViewGroup.LayoutParams
            layoutParams.height =
                applicationContext.resources.getDimension(R.dimen.clipper_sticky_note_max_height)
                    .toInt()
            if (triggerPoint != null) {
                val stringPair: Pair<String, String> = Pair(NotesRole.IMAGE_INSERTED_FROM, if (isImageInsertedFromCamera) NotesRole.FROM_CAMERA else triggerPoint)
                NotesLibrary.getInstance().recordTelemetry(EventMarkers.SNOutsideAppNoteTaking, stringPair)
            }
            return true
        }
        return false
    }
}
