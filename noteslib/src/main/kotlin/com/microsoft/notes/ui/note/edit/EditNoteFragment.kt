package com.microsoft.notes.ui.note.edit

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.noteslib.extensions.getMediaTelemetryProperties
import com.microsoft.notes.noteslib.extensions.recordNoteContentUpdated
import com.microsoft.notes.richtext.editor.NotesEditTextCallback
import com.microsoft.notes.richtext.editor.styled.NoteStyledView
import com.microsoft.notes.richtext.editor.styled.ReadOnlyStyledView
import com.microsoft.notes.richtext.editor.styled.ReadOnlyStyledView.ImageCallbacks
import com.microsoft.notes.richtext.editor.styled.ReadOnlyStyledView.RecordTelemetryCallback
import com.microsoft.notes.richtext.editor.styled.SamsungNoteStyledView
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Range
import com.microsoft.notes.ui.extensions.isSamsungNote
import com.microsoft.notes.ui.extensions.sendAccessibilityAnnouncement
import com.microsoft.notes.ui.note.ink.EditInkView
import com.microsoft.notes.ui.note.ink.InkState
import com.microsoft.notes.ui.note.ink.NotesEditInkCallback
import com.microsoft.notes.ui.shared.StickyNotesFragment
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.HostTelemetryKeys
import com.microsoft.notes.utils.logging.ImageActionType
import com.microsoft.notes.utils.logging.ImageTrigger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.NoteProperty
import com.microsoft.notes.utils.utils.Constants
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

/**
 * EditNoteFragment can be used directly, however, if you need to extend it for your use-case, please include
 * the following in the parent layout:
 * <include layout="@layout/sn_edit_note_layout" />
 */
@Suppress("TooManyFunctions")
open class EditNoteFragment :
    StickyNotesFragment(),
    NotesEditTextCallback,
    NotesEditInkCallback,
    RecordTelemetryCallback,
    ImageCallbacks,
    NoteStyledView.MicroPhoneCallbacks,
    ReadOnlyStyledView.RibbonCallbacks,
    FragmentApi {

    private var isCurrentNoteSamsungNote: Boolean = false
    private var softInputModeBeforeResume: Int = -1
    private var lastNoteEdited: Note? = null
    private var noteStyledView: NoteStyledView? = null
    private var samsungNoteStyledView: SamsungNoteStyledView? = null
    private var noteGalleryItemInkView: EditInkView? = null
    var onUndoStackChanged: ((count: Int) -> Unit)? = null
    var onTextUndoChanged: ((isEnabled: Boolean) -> Unit)? = null
    var onTextRedoChanged: ((isEnabled: Boolean) -> Unit)? = null
    var viewListener: IOnViewCreatedListener? = null
    //private var recorder: MediaRecorder = MediaRecorder()
    private lateinit var recorder: MediaRecorder
    private var outputFile: String? = null
    private var isAudioSourceSet = false

    fun interface IOnViewCreatedListener {
        fun isViewCreatedCallback()
    }

    companion object {
        private const val LOG_TAG = "EditNoteFragment"
        private const val CURRENT_NOTE_ID = "CURRENT_NOTE_ID"
        private const val EDIT_MODE = "EDIT_MODE"
        const val FRAGMENT_NAME = "EDIT_NOTE"
        const val NO_ACTIVITY_RESOLVE_UNKNOWN_ERROR = "NoActivityResolve_UnknownError"
        const val INVALID_MIME_TYPE = "InvalidMimeType"
    }

    private fun getReadOnlyStyledView(): ReadOnlyStyledView? {
        return if (isCurrentNoteSamsungNote) {
            samsungNoteStyledView
        } else {
            noteStyledView
        }
    }

    protected open val presenter: EditNotePresenter by lazy {
        EditNotePresenter(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(LOG_TAG, "OnCreateView being called")
        return if (isCurrentNoteSamsungNote) {
            inflater.inflate(R.layout.samsung_note_layout, container, false)
        } else {
            inflater.inflate(R.layout.sn_edit_note_layout, container, false)
        }
    }

    override fun onEditModeChanged(isEditMode: Boolean) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteStyledView = view.findViewById(R.id.noteStyledView)
        samsungNoteStyledView = view.findViewById(R.id.samsungNoteStyledView)
        noteGalleryItemInkView = view.findViewById(R.id.noteGalleryItemInkView)

        savedInstanceState?.let {
            if (it.containsKey(CURRENT_NOTE_ID)) {
                it.getString(CURRENT_NOTE_ID)?.let { currentNoteId ->
                    setCurrentNoteId(currentNoteId)
                }
            }
            if (it.containsKey(EDIT_MODE)) {
                noteStyledView?.setEditMode(it.getBoolean(EDIT_MODE))
            }
        }
        noteStyledView?.getNotesEditText()?.onUndoChanged = onTextUndoChanged
        noteStyledView?.getNotesEditText()?.onRedoChanged = onTextRedoChanged
        viewListener?.isViewCreatedCallback()


        val notesFilesDir = File(requireContext().filesDir, Constants.NOTES_FOLDER_NAME)
        if (!notesFilesDir.exists()) {
            notesFilesDir.mkdirs()
        }
        val notesImagesDir = File(notesFilesDir, Constants.NOTES_IMAGES_FOLDER_NAME)
        if (!notesImagesDir.exists()) {
            notesImagesDir.mkdirs()
        }
        outputFile = File(notesImagesDir, "recording_" + System.currentTimeMillis() + ".wav").absolutePath
        checkPermission()
        initMediaRecorder()
        initSpeechToText()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CURRENT_NOTE_ID, getCurrentNoteId())
        outState.putBoolean(EDIT_MODE, noteStyledView?.isInEditMode ?: false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        getReadOnlyStyledView()?.let { setupStyledViewCallbacksAndTransitions(it) }
        getCurrentNote()?.let { setUpNote(it) }
    }

    @Keep
    protected fun setupStyledViewCallbacksAndTransitions(styledView: ReadOnlyStyledView) {
        setupTransitionNames(styledView)

        if (styledView is NoteStyledView) {
            styledView.setupNoteBodyCallbacks(this)
            styledView.setupNoteInkCallback(this)
            styledView.microPhoneCallbacks = this
        }

        styledView.telemetryCallback = this
        styledView.imageCallbacks = this
        styledView.ribbonCallbacks = this
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onResume() {
        super.onResume()
        getReadOnlyStyledView()?.onReEntry()
        showSoftInputIfRequired()
        presenter.onResume()
    }

    fun setInkState(newInkState: InkState, shouldToggle: Boolean) {
        /* When toggling an item (Ink/eraser), if current state is already ON it should turn off and vice versa
        Also clicking one item should turn off the other. Both Off= READ state*/
        if (shouldToggle && getInkState() == newInkState) {
            noteGalleryItemInkView?.inkState = InkState.READ
        } else {
            noteGalleryItemInkView?.inkState = newInkState
        }
        activity?.invalidateOptionsMenu()
    }

    fun getInkState(): InkState {
        val galleryInkView = noteGalleryItemInkView
        return galleryInkView?.inkState ?: InkState.INK
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        getReadOnlyStyledView()?.onConfigurationChanged()
    }

    override fun onPause() {
        super.onPause()
        restoreToOldSoftInputState()
        getReadOnlyStyledView()?.onNavigatingAway()
        presenter.onPause()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
        NoteStyledView.isEdit=false

    }

    private fun setUpNote(note: Note) {
        if (!note.isSamsungNote()) {
            noteStyledView?.enableMicrophoneButton(
                !note.isInkNote &&
                    NotesLibrary.getInstance()
                        .microphoneEnabled(NotesLibrary.getInstance().currentUserID)
            )
        }
        getReadOnlyStyledView()?.setNoteContent(note)
    }

    private fun showSoftInputIfRequired() {
        if (!isCurrentNoteSamsungNote && noteStyledView?.isInEditMode == true) {
            activity?.window?.let {
                softInputModeBeforeResume = it.attributes.softInputMode
                it.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                        or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                )
            }
        }
    }

    private fun restoreToOldSoftInputState() {
        if (softInputModeBeforeResume > 0) {
            activity?.window?.setSoftInputMode(softInputModeBeforeResume)
        }
    }

    open fun onNavigateToTransitionCompleted(showSoftInput: Boolean = true) {
        if (!isAdded) {
            Log.i(LOG_TAG, "EditNoteFragment has not been attached")
            return
        }

        if (!isCurrentNoteSamsungNote) {
            if (getCurrentNote()?.isEmpty == true) {
                if (getCurrentNote()?.isRichTextNote == true) noteStyledView?.enableEditingMode(showSoftInput)
                else if (getCurrentNote()?.isInkNote == true) setInkState(InkState.INK, false)
            } else {
                resetEditingMode(showSoftInput)
            }
        }
    }

    fun resetEditingMode(showSoftInput: Boolean = true) {
        if (!isCurrentNoteSamsungNote) {
            noteStyledView?.resetEditingMode(showSoftInput)
        }
        if (getCurrentNote()?.isInkNote == true) {
            noteGalleryItemInkView?.resetUndoStack()
        }
    }

    private fun setupTransitionNames(styledView: ReadOnlyStyledView) {
        styledView.let {
            it.getEditNoteLayout()?.let { editNoteLayout ->
                ViewCompat.setTransitionName(editNoteLayout, "card")
            }
            it.getNoteContainerLayout()?.let { noteContainerLayout ->
                ViewCompat.setTransitionName(noteContainerLayout, "linearLayout")
            }
            it.getNotesEditText()?.let {
                    notesEditText ->
                ViewCompat.setTransitionName(notesEditText, "body")
            }
        }
    }

    fun isEmpty(): Boolean = (getCurrentNote()?.isDocumentEmpty ?: true) && (noteStyledView?.isEmpty() ?: true)

    fun prepareSharedElements(markSharedElement: (View, String) -> Unit) {
        getReadOnlyStyledView()?.getEditNoteLayout()?.let { markSharedElement(it, "card") }
        getReadOnlyStyledView()?.getNoteContainerLayout()?.let { markSharedElement(it, "linearLayout") }
        getReadOnlyStyledView()?.getNotesEditText()?.let { markSharedElement(it, "body") }
    }

    override fun updateDocument(document: Document, uiRevision: Long) {
        sendNoteFirstEditedActionIfFirstEdit()
        presenter.updateNoteWithDocument(updatedDocument = document, uiRevision = uiRevision)
    }

    override fun updateInkDocument(document: Document, uiRevision: Long) {
        sendNoteFirstEditedActionIfFirstEdit()
        presenter.updateNoteWithDocument(updatedDocument = document, uiRevision = uiRevision)
    }

    override fun updateRange(range: Range) {
        presenter.updateRange(range)
    }

    override fun openMediaInFullScreen(mediaLocalUrl: String, mediaMimeType: String) {
        recordTelemetryEvent(
            EventMarkers.ImageActionTaken,
            Pair(NoteProperty.ACTION, ImageActionType.IMAGE_VIEWED)
        )

        val safeContext = context
        val safeActivity = activity

        if (safeContext != null && safeActivity != null) {
            val mediaUri = NotesLibrary.getInstance().contentUri(safeContext, mediaLocalUrl)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setDataAndType(mediaUri, mediaMimeType)

            try {
                safeActivity.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                val errorMessage = if (mediaMimeType.isEmpty()) INVALID_MIME_TYPE
                else e.message ?: NO_ACTIVITY_RESOLVE_UNKNOWN_ERROR
                recordTelemetryEvent(
                    EventMarkers.FullScreenImageViewError,
                    Pair(HostTelemetryKeys.ERROR_MESSAGE, errorMessage)
                )
            }
        }
    }

    override fun addPhoto(imageTrigger: ImageTrigger) {
        presenter.recordTelemetry(
            EventMarkers.AddImageTriggered,
            Pair(NoteProperty.IMAGE_TRIGGER, imageTrigger.name),
            Pair(HostTelemetryKeys.TRIGGER_POINT, FRAGMENT_NAME)
        )
        NotesLibrary.getInstance().sendAddPhotoAction()
    }

    override fun onMicroPhoneButtonClicked() {
        NotesLibrary.getInstance().sendOnMicroPhoneButtonClickedAction()
    }

    override fun onAudioRecordButtonClicked(isRecording: Boolean) {
        if (isRecording) {
            //speechRecognizer.stopListening()
            stopRecording()

        } else {
            //speechRecognizer.startListening(speechRecognizerIntent)
            startRecording()
        }
    }



    lateinit var speechRecognizer: SpeechRecognizer
    lateinit var speechRecognizerIntent:Intent
    private fun initSpeechToText() {

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle) {
                Log.d("STT","onReadyForSpeech")
                //startRecording()
            }

            override fun onBeginningOfSpeech() {
                Log.d("STT","onBeginningOfSpeech")

            }

            override fun onRmsChanged(v: Float) {
                Log.d("STT","onRmsChanged")

            }

            override fun onBufferReceived(bytes: ByteArray) {
                Log.d("STT","onBufferReceived")

            }

            override fun onEndOfSpeech() {
                Log.d("STT","onEndOfSpeech")

            }

            override fun onError(error: Int) {
                val errorMessage = getErrorText(error)
                Log.d("SpeechRecognition", "Error: $errorMessage")
                Log.d("STT","onError ${errorMessage}")
                if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    // Retry listening
                    speechRecognizer.stopListening()
                    speechRecognizer.startListening(speechRecognizerIntent)
                }

            }

            override fun onResults(bundle: Bundle) {
                Log.d("STT","onResults")
                val matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)//getting all the matches
                //displaying the first match
                if (matches != null){
                    //editText.setText(matches[0])
                    noteStyledView?.getNotesEditText()?.setText(matches[0])
                    //stopRecording()
                    Toast.makeText(requireContext(),matches[0].toString(),Toast.LENGTH_SHORT).show()
                }

            }

            override fun onPartialResults(bundle: Bundle) {
                Log.d("STT","onPartialResults")
            }

            override fun onEvent(i: Int, bundle: Bundle) {
                Log.d("STT","onEvent")
            }
        })
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + requireContext().packageName))
            startActivity(intent)
            Toast.makeText(requireContext(), "Enable Microphone Permission..!!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initMediaRecorder() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile)
        }
    }

    private fun startRecording() {
        try {
//            // Initialize MediaRecorder
//            if (isAudioSourceSet == false) {
//                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
//                isAudioSourceSet = true
//            }
//            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//            recorder.setOutputFile(outputFile)
//            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
//            recorder.prepare()
//            recorder.start()
            recorder.prepare()
            recorder.start()
            Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        recorder.stop()
        recorder.release()
        // Toast.makeText(context, "Recording stopped. File saved at $outputFile", Toast.LENGTH_LONG).show()
        Log.d("ttt", "$outputFile")
        addPhotosToNote(listOf("file:$outputFile"), false)
    }

    private fun convertBase64ToMp3() {
        val decodedBytes = Base64.decode(Constants.mp3Base64, Base64.DEFAULT)

        try {
            FileOutputStream(outputFile).use { outputStream ->
                outputStream.write(decodedBytes)
            }
            Toast.makeText(context, "MP3 file saved at: $outputFile", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun readBytesFromFile(file: File): ByteArray? {
        var fileInputStream: FileInputStream? = null
        try {
            fileInputStream = FileInputStream(file)
            val byteArray = ByteArray(file.length().toInt())
            fileInputStream.read(byteArray)
            return byteArray
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            fileInputStream?.close()
        }
        return null
    }
    private fun convertMp3ToBase64() {
        val mp3File = File(outputFile)
        val byteArray = readBytesFromFile(mp3File)

        if (byteArray != null) {
            // Encode ByteArray to Base64
            val base64EncodedString = Base64.encodeToString(byteArray, Base64.DEFAULT)
            Log.d("ttt", "Base64 encoded MP3: $base64EncodedString")
        } else {
            Log.e("ttt", "Failed to read MP3 file")
        }
    }

    /**
     * Adds an image to the current note.
     * Pass in deleteOriginal parameter for SDK to clean up the file used.
     */
    fun addPhotosToNote(uriList: List<String>, deleteOriginal: Boolean = false) {
        noteStyledView?.prepareForNewImage()
        presenter.addMedia(uriList, deleteOriginal, compressionCompleted = { successful ->
            activity?.let {
                if (successful) {
                    sendNoteFirstEditedActionIfFirstEdit()
                    context?.sendAccessibilityAnnouncement(getString(R.string.sn_image_added))
                } else {
                    Toast.makeText(it, R.string.sn_adding_image_failed, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun sendNoteFirstEditedActionIfFirstEdit() {
        val currentNote = getCurrentNote()
        if (currentNote != null) {
            if (lastNoteEdited?.localId != currentNote.localId) {
                lastNoteEdited = currentNote
                NotesLibrary.getInstance().sendNoteFirstEditedAction()
            }
        }
    }

    override fun recordTelemetryEvent(eventMarker: EventMarkers, vararg keyValuePairs: Pair<String, String>) {
        presenter.recordTelemetry(eventMarker, *keyValuePairs)
    }

    override fun recordNoteContentUpdated() {
        getCurrentNote()?.let {
            NotesLibrary.getInstance().recordNoteContentUpdated(it)
        }
    }

    override fun getCustomInputConnectionForNotesEditText(editorInfo: EditorInfo): InputConnection? = null

    fun searchInNote(query: String) {
        noteStyledView?.getNotesEditText()?.searchInNote(query)
    }

    fun navigateSearchedOccurrence(previous: Boolean) {
        noteStyledView?.getNotesEditText()?.navigateSearchedOccurrence(previous)
    }

    override fun onUndoStackChanged(count: Int) {
        onUndoStackChanged?.invoke(count)
    }

    @JvmOverloads
    fun resumeEditNoteState(showSoftInput: Boolean = true) {
        if (!isCurrentNoteSamsungNote) {
            activity?.runOnUiThread {
                Handler().postDelayed({ noteStyledView?.resumeEditingMode(showSoftInput) }, 100)
            }
        }
    }

    fun findNoteStyledView(): NoteStyledView? = noteStyledView

    /**
     * Override onContextMenuClosed() method in your Activity and forward the call to this method.
     * This is used for updating UI after alt text/deletion context menu is dismissed.
     */
    fun onContextMenuClosed() {
        getReadOnlyStyledView()?.onContextMenuClosed()
    }

    override fun editAltText(media: Media) {
        val dialog = AltTextDialog.createDialog(media.altText)
        dialog.listener = object : AltTextDialog.Listener {
            override fun onAltTextChanged(altText: String) {
                sendNoteFirstEditedActionIfFirstEdit()
                presenter.updateAltText(media, altText)
                recordNoteContentUpdated()
                recordTelemetryEvent(
                    EventMarkers.ImageActionTaken,
                    *getMediaTelemetryProperties(media).toTypedArray(),
                    Pair(
                        NoteProperty.ACTION,
                        if (altText.isEmpty()) ImageActionType.IMAGE_ALT_TEXT_DELETED else ImageActionType.IMAGE_ALT_TEXT_EDITED
                    )
                )
            }
        }
        dialog.show(childFragmentManager, "ALT_TEXT")
    }

    override fun deleteMedia(media: Media) {
        context?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(it.getString(R.string.sn_image_dialog_delete_description))
            builder.setPositiveButton(it.getString(R.string.sn_image_dialog_delete_action).toUpperCase()) { _, _ ->
                sendNoteFirstEditedActionIfFirstEdit()
                presenter.deleteMedia(media)
                recordNoteContentUpdated()
                recordTelemetryEvent(
                    EventMarkers.ImageActionTaken,
                    *getMediaTelemetryProperties(media).toTypedArray(),
                    Pair(NoteProperty.ACTION, ImageActionType.IMAGE_DELETED)
                )
            }
            builder.setNegativeButton(it.getString(R.string.sn_dialog_cancel).toUpperCase()) { _, _ -> }
            builder.show()
        }
    }

    override fun deleteNote(media: Media) {
        sendNoteFirstEditedActionIfFirstEdit()
        presenter.deleteMedia(media)
        recordNoteContentUpdated()
        recordTelemetryEvent(
            EventMarkers.ImageActionTaken,
            *getMediaTelemetryProperties(media).toTypedArray(),
            Pair(NoteProperty.ACTION, ImageActionType.IMAGE_DELETED)
        )
    }

    override fun undo() {
        if (NotesLibrary.getInstance().experimentFeatureFlags.enableUndoRedoActionInNotes) {
            noteStyledView?.getNotesEditText()?.undo()
        }
    }

    override fun redo() {
        if (NotesLibrary.getInstance().experimentFeatureFlags.enableUndoRedoActionInNotes) {
            noteStyledView?.getNotesEditText()?.redo()
        }
    }

    override fun setCurrentNoteId(currentNoteId: String) {
        super.setCurrentNoteId(currentNoteId)
        lastNoteEdited = null
        getCurrentNote()?.let {
            isCurrentNoteSamsungNote = it.isSamsungNote()
            if (it.isInkNote) noteGalleryItemInkView?.getScaleFactor = null
            setUpNote(it)
        }
    }

    fun insertTextToCurrentNote(text: String) {
        if (text.isNotEmpty()) {
            noteStyledView?.insertTextToCurrentNote(text)
        }
    }

    fun isUndoStackEmpty() = noteGalleryItemInkView?.isUndoStackEmpty() ?: true

    fun canPerformUndo() = noteStyledView?.getNotesEditText()?.canPerformUndo() ?: true

    fun canPerformRedo() = noteStyledView?.getNotesEditText()?.canPerformUndo() ?: true

    fun undoLastInkStroke() {
        noteGalleryItemInkView?.undoLastStroke()
    }

    override fun clearCanvas() {
        noteGalleryItemInkView?.clearCanvas()
    }

    // ---- FragmentApi ----//
    override fun onSetNoteDetails(note: Note?) {
        note?.let { setUpNote(note) }
    }

    override fun getCurrentEditNote(): Note? = getCurrentNote()

    // Default value is true if the fragment is alive, override in your app to change the default value
    // If you are overriding you should manually set the note content when visibility changes
    override fun isFragmentVisible(): Boolean = true
}
