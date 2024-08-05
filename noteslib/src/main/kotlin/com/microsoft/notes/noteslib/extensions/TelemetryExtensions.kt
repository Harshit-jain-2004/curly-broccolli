package com.microsoft.notes.noteslib.extensions

import android.webkit.MimeTypeMap
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.toTelemetryNoteType
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.ui.extensions.getHasImagesTelemetryValue
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.HostTelemetryKeys
import com.microsoft.notes.utils.logging.ImageActionType
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.NoteProperty

internal fun NotesLibrary.recordImageAddedTelemetry(
    note: Note,
    mimeType: String,
    uncompressedImageSizeInBytes: Long,
    compressedImageSizeInBytes: Long,
    triggerPoint: String?
) {
    recordNoteContentUpdated(note)
    val isEmptyNote = note.isEmpty
    if (isEmptyNote) {
        recordTelemetry(
            EventMarkers.ImageActionTaken,
            *getNoteTelemetryProperties(note).toTypedArray(),
            Pair(NoteProperty.ACTION, ImageActionType.IMAGE_ADDED_TO_EMPTY_NOTE)
        )
    }

    val imageAddedTelemetryProperties = mutableListOf(
        Pair(NoteProperty.IMAGE_MIME_TYPE, mimeType),
        Pair(NoteProperty.IMAGE_UNCOMPRESSED_SIZE, uncompressedImageSizeInBytes.toString()),
        Pair(NoteProperty.IMAGE_COMPRESSED_SIZE, compressedImageSizeInBytes.toString()),
        Pair(NoteProperty.IMAGE_ADDED_TO_EMPTY_NOTE, isEmptyNote.toString())
    )
    imageAddedTelemetryProperties.addAll(getNoteTelemetryProperties(note))
    triggerPoint?.let { imageAddedTelemetryProperties.add(Pair(HostTelemetryKeys.TRIGGER_POINT, it)) }

    recordTelemetry(
        EventMarkers.ImageActionTaken,
        *imageAddedTelemetryProperties.toTypedArray(),
        Pair(NoteProperty.ACTION, ImageActionType.IMAGE_ADDED)
    )
}

internal fun NotesLibrary.recordNoteContentUpdated(note: Note) {
    recordTelemetry(
        EventMarkers.NoteContentUpdated,
        Pair(NoteProperty.NOTE_HAS_IMAGES, note.getHasImagesTelemetryValue()),
        Pair(NoteProperty.NOTE_TYPE, note.toTelemetryNoteType().toString()),
        *getNoteTelemetryProperties(note).toTypedArray()
    )
}

internal fun getNoteTelemetryProperties(note: Note): List<Pair<String, String>> {
    return listOf(
        Pair(NoteProperty.NOTE_LOCAL_ID, note.localId),
        Pair(NoteProperty.NOTE_REMOTE_ID, note.remoteData?.id ?: "")
    )
}

internal fun getMediaTelemetryProperties(media: Media): List<Pair<String, String>> {
    return listOf(
        Pair(NoteProperty.IMAGE_LOCAL_ID, media.localId),
        Pair(NoteProperty.IMAGE_REMOTE_ID, media.remoteId ?: "")
    )
}

internal fun urlToMimeType(url: String): String {
    val extension = MimeTypeMap.getFileExtensionFromUrl(url) ?: ""
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: ""
}
