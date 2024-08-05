package com.microsoft.notes.ui.note.edit

import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.extensions.getNoteTelemetryProperties
import com.microsoft.notes.richtext.editor.extensions.increaseRevision
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Range
import com.microsoft.notes.sideeffect.ui.InkNoteChanges
import com.microsoft.notes.sideeffect.ui.NoteChanges
import com.microsoft.notes.sideeffect.ui.SamsungNoteChanges
import com.microsoft.notes.sideeffect.ui.UndoRedoPerformedInNotesEditText
import com.microsoft.notes.ui.extensions.isSamsungNote
import com.microsoft.notes.ui.shared.StickyNotesPresenter
import com.microsoft.notes.utils.logging.EventMarkers

open class EditNotePresenter(private val fragmentApi: FragmentApi) :
    StickyNotesPresenter(),
    NoteChanges,
    SamsungNoteChanges,
    InkNoteChanges,
    UndoRedoPerformedInNotesEditText {

    protected open val note: Note?
        get() = fragmentApi.getCurrentEditNote()

    // presenter functions
    open fun updateNoteWithDocument(updatedDocument: Document, uiRevision: Long) {
        note?.let {
            NotesLibrary.getInstance().updateNoteDocument(
                noteLocalId = it.localId,
                updatedDocument = updatedDocument,
                uiRevision = uiRevision
            )
        }
    }

    open fun addMedia(urlList: List<String>, deleteOriginal: Boolean, compressionCompleted: (successful: Boolean) -> Unit) {
        val note = note ?: return
        NotesLibrary.getInstance().addMultipleMediaToNote(
            note, urlList, deleteOriginal,
            operationCompleted = { successful ->
                if (!successful) {
                    NotesLibrary.getInstance().sendImageCompressionCompletedAction(successful = false)
                    runIfActivityIsRunning {
                        runOnClientThread {
                            compressionCompleted(false)
                        }
                    }
                    return@addMultipleMediaToNote
                }

                NotesLibrary.getInstance().sendImageCompressionCompletedAction(successful = true)

                runIfActivityIsRunning {
                    runOnClientThread {
                        compressionCompleted(true)
                    }
                }
            },
            triggerPoint = EditNoteFragment.FRAGMENT_NAME
        )
    }

    open fun updateRange(newRange: Range) {
        note?.let {
            NotesLibrary.getInstance().updateDocumentRange(
                noteLocalId = it.localId,
                newRange = newRange
            )
        }
    }

    open fun updateAltText(media: Media, altText: String) {
        note?.let {
            NotesLibrary.getInstance().updateMediaAltText(it.uiRevision.increaseRevision(), it, media, altText)
        }
    }

    open fun deleteMedia(media: Media) {
        note?.let {
            NotesLibrary.getInstance().deleteMediaFromNote(it.uiRevision.increaseRevision(), it, media)
        }
    }

    // UI bindings
    override fun addUiBindings() {
        try {
            NotesLibrary.getInstance().addUiBindings(this)
        } catch (exception: UninitializedPropertyAccessException) {
            NotesLibrary.getInstance().log(message = "UninitializedPropertyAccessException when adding ui binding")
        }
    }

    override fun removeUiBindings() {
        try {
            NotesLibrary.getInstance().removeUiBindings(this)
        } catch (exception: UninitializedPropertyAccessException) {
            NotesLibrary.getInstance().log(
                message = "UninitializedPropertyAccessException when removing ui binding"
            )
        }
    }

    // telemetry
    override fun recordTelemetry(eventMarker: EventMarkers, vararg keyValuePairs: Pair<String, String>) {
        note?.let {
            NotesLibrary.getInstance().recordTelemetry(
                eventMarker,
                *keyValuePairs,
                *getNoteTelemetryProperties(it).toTypedArray()
            )
        }
    }

    // --- UI Bindings function listeners ------//

    // NoteChanges
    override fun notesUpdated(stickyNotesCollectionsByUser: HashMap<String, List<Note>>, notesLoaded: Boolean) {
        runIfActivityIsRunning {
            runOnClientThread {
                if (fragmentApi.isFragmentVisible()) {
                    note?.let {
                        if (!it.isSamsungNote()) {
                            fragmentApi.onSetNoteDetails(it)
                        }
                    }
                }
            }
        }
    }

    override fun samsungNotesUpdated(samsungNotesCollectionsByUser: HashMap<String, List<Note>>) {
        runIfActivityIsRunning {
            runOnClientThread {
                if (fragmentApi.isFragmentVisible()) {
                    note?.let {
                        if (it.isSamsungNote()) {
                            fragmentApi.onSetNoteDetails(it)
                        }
                    }
                }
            }
        }
    }

    override fun noteDeleted() {
        // no action, handled elsewhere
    }

    override fun clearCanvas() {
        runIfActivityIsRunning {
            runOnClientThread {
                if (fragmentApi.isFragmentVisible()) {
                    note?.let {
                        if (it.isInkNote) {
                            fragmentApi.clearCanvas()
                        }
                    }
                }
            }
        }
    }

    override fun undo() {
        runIfActivityIsRunning {
            runOnClientThread {
                if (fragmentApi.isFragmentVisible()) {
                    note?.let {
                        if (it.isRichTextNote) {
                            fragmentApi.undo()
                        }
                    }
                }
            }
        }
    }

    override fun redo() {
        runIfActivityIsRunning {
            runOnClientThread {
                if (fragmentApi.isFragmentVisible()) {
                    note?.let {
                        if (it.isRichTextNote) {
                            fragmentApi.redo()
                        }
                    }
                }
            }
        }
    }
}

interface FragmentApi {
    fun onSetNoteDetails(note: Note?)
    fun getCurrentEditNote(): Note?
    fun isFragmentVisible(): Boolean
    fun clearCanvas()
    fun undo()
    fun redo()
}
