package com.microsoft.notes.richtext.editor.styled

import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.editor.NotesEditText
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.ImageTrigger

interface ReadOnlyStyledView {
    var telemetryCallback: RecordTelemetryCallback?
    var imageCallbacks: ImageCallbacks?
    var ribbonCallbacks: RibbonCallbacks?

    fun getNoteContainerLayout(): RelativeLayout?
    fun onReEntry() {}
    fun onConfigurationChanged() {}
    fun onNavigatingAway() {}
    fun setNoteContent(note: Note)
    fun getEditNoteLayout(): FrameLayout?
    fun getNotesEditText(): NotesEditText? = null
    fun onContextMenuClosed() {}

    interface RecordTelemetryCallback {
        fun recordTelemetryEvent(eventMarker: EventMarkers, vararg keyValuePairs: Pair<String, String>)
    }

    interface ImageCallbacks {
        fun addPhoto(imageTrigger: ImageTrigger)
        fun openMediaInFullScreen(mediaLocalUrl: String, mediaMimeType: String)
        fun editAltText(media: Media)
        fun deleteMedia(media: Media)
        fun deleteNote(media: Media)
    }

    interface RibbonCallbacks {
        fun onEditModeChanged(isEditMode: Boolean)
    }
}
