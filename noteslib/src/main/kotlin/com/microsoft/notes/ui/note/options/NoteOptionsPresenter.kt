package com.microsoft.notes.ui.note.options

import android.app.Activity
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.toTelemetryNoteType
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.extensions.getNoteTelemetryProperties
import com.microsoft.notes.richtext.editor.extensions.increaseRevision
import com.microsoft.notes.richtext.editor.extensions.toTelemetryColorAsString
import com.microsoft.notes.sideeffect.ui.NoteChanges
import com.microsoft.notes.ui.extensions.getHasImagesTelemetryValue
import com.microsoft.notes.ui.shared.StickyNotesPresenter
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.HostTelemetryKeys
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import java.lang.ref.WeakReference

open class NoteOptionsPresenter(private val fragmentApi: FragmentApi) :
    StickyNotesPresenter(), NoteChanges {

    val note: Note?
        get() = fragmentApi.getCurrentNote()

    // presenter functions
    open fun updateNoteColor(color: Color) {
        note?.let {
            val noteType = it.toTelemetryNoteType().toString()
            val colorChangedFrom = it.color.toTelemetryColorAsString()
            val colorChangedTo = color.toTelemetryColorAsString()

            recordTelemetry(
                EventMarkers.NoteColorUpdated,
                Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_TYPE, noteType),
                Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES, it.getHasImagesTelemetryValue()),
                Pair(NotesSDKTelemetryKeys.NoteProperty.COLOR_CHANGED_FROM, colorChangedFrom),
                Pair(NotesSDKTelemetryKeys.NoteProperty.COLOR_CHANGED_TO, colorChangedTo)
            )

            NotesLibrary.getInstance().updateNoteColor(
                it.localId,
                color = color,
                revision = it.uiRevision.increaseRevision()
            )

            NotesLibrary.getInstance().sendNoteOptionsColorPickedAction()
        }
    }

    fun clearCanvas() {
        note?.let {
            NotesLibrary.getInstance().clearCanvas()
        }
    }

    open fun deleteNote() {
        note?.let {
            NotesLibrary.getInstance().markAsDeleteAndDelete(
                it.localId,
                it.remoteData?.id,
                true
            )

            recordTelemetry(
                EventMarkers.NoteDeleted,
                Pair(HostTelemetryKeys.TRIGGER_POINT, NoteOptionsFragment.FRAGMENT_NAME),
                Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES, it.getHasImagesTelemetryValue()),
                Pair(NotesSDKTelemetryKeys.NoteProperty.IS_NOTE_EMPTY, it.isEmpty.toString())
            )

            NotesLibrary.getInstance().sendNoteOptionsNoteDeletedAction()
        }
    }

    fun deleteSamsungNote() {
        note?.let {
            recordTelemetry(
                EventMarkers.SamsungNoteDismissed,
                Pair(HostTelemetryKeys.TRIGGER_POINT, NoteOptionsFragment.FRAGMENT_NAME),
                Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES, "false"), // TODO 03-Feb-21 gopalsa: Fix when Samsung Notes support HTML media notes
                Pair(NotesSDKTelemetryKeys.NoteProperty.IS_NOTE_EMPTY, it.isEmpty.toString())
            )

            NotesLibrary.getInstance().markSamsungNoteAsDeleteAndDelete(
                it.localId, it.remoteData?.id ?: ""
            )

            NotesLibrary.getInstance().sendNoteOptionsNoteDeletedAction()
        }
    }

    fun shareNote(activity: Activity) {
        note?.let {
            recordTelemetry(
                EventMarkers.ShareNoteTriggered,
                Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_HAS_IMAGES, it.getHasImagesTelemetryValue())
            )
            shareNote(it, WeakReference(activity))

            NotesLibrary.getInstance().sendNoteOptionsNoteSharedAction()
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
                note?.let {
                    fragmentApi.setCurrentColor(it.color)
                }
            }
        }
    }

    override fun noteDeleted() {
        // no action, handled elsewhere
    }
}

interface FragmentApi {
    fun getCurrentNote(): Note?
    fun setCurrentColor(color: Color)
}
