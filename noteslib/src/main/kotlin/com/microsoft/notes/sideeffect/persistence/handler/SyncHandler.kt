package com.microsoft.notes.sideeffect.persistence.handler

import android.database.sqlite.SQLiteDiskIOException
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.sideeffect.persistence.Preference
import com.microsoft.notes.sideeffect.persistence.PreferenceKeys
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceColor
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceNote
import com.microsoft.notes.store.action.Action
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.action.SyncResponseAction.ApplyChanges
import com.microsoft.notes.store.action.SyncResponseAction.ApplyConflictResolution
import com.microsoft.notes.store.action.SyncResponseAction.InvalidateClientCache
import com.microsoft.notes.store.action.SyncResponseAction.InvalidateNoteReferencesClientCache
import com.microsoft.notes.store.action.SyncResponseAction.InvalidateSamsungNotesClientCache
import com.microsoft.notes.store.action.SyncResponseAction.MediaAltTextUpdated
import com.microsoft.notes.store.action.SyncResponseAction.MediaDeleted
import com.microsoft.notes.store.action.SyncResponseAction.MediaDownloaded
import com.microsoft.notes.store.action.SyncResponseAction.PermanentlyDeleteNote
import com.microsoft.notes.store.action.SyncResponseAction.PermanentlyDeleteNoteReference
import com.microsoft.notes.store.action.SyncResponseAction.PermanentlyDeleteSamsungNote
import com.microsoft.notes.store.action.SyncResponseAction.RemoteDataUpdated
import com.microsoft.notes.ui.extensions.SAMSUNG_NOTES_APP_NAME
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys.NoteProperty.ACTION
import com.microsoft.notes.models.Note as StoreNote

object SyncHandler : ActionHandler<SyncResponseAction> {

    override fun handle(
        action: SyncResponseAction,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?,
        findNote: (String) -> StoreNote?,
        actionDispatcher: (Action) -> Unit
    ) {
        when (action) {
            is ApplyChanges -> handleApplyChanges(action, notesDB, notesLogger)
            is RemoteDataUpdated -> handleRemoteDataUpdated(action, notesDB, notesLogger)
            is MediaDownloaded -> handleMediaDownloaded(findNote, action, notesDB, notesLogger)
            is MediaDeleted -> handleMediaDeleted(findNote, action, notesDB, notesLogger)
            is MediaAltTextUpdated -> handleMediaAltTextUpdated(findNote, action, notesDB, notesLogger)
            is PermanentlyDeleteNote -> handlePermanentlyDeleteNote(action.noteLocalId, notesDB)
            is PermanentlyDeleteNoteReference -> handlePermanentlyDeleteNoteReference(action.noteLocalId, notesDB)
            is PermanentlyDeleteSamsungNote -> handlePermanentlyDeleteNote(action.noteLocalId, notesDB)
            is ApplyConflictResolution -> handleApplyConflictResolution(action, notesDB, findNote, notesLogger)
            is InvalidateClientCache -> handleInvalidateClientCache(notesDB)
            is InvalidateNoteReferencesClientCache -> handleInvalidateClientCacheForNoteReference(notesDB)
            is InvalidateSamsungNotesClientCache -> handleInvalidateClientCacheForAppSpecificNotes(notesDB, SAMSUNG_NOTES_APP_NAME)
        }
    }

    private fun handleApplyChanges(
        action: ApplyChanges,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?
    ) {
        try {
            notesDB.runInTransaction {
                with(action) {
                    changes.toCreate.forEach {
                        insertNoteIntoDB(
                            storeNote = it,
                            notesDB = notesDB,
                            notesLogger = notesLogger
                        )
                    }

                    changes.toReplace.map { it.noteFromServer }.forEach {
                        replaceNote(
                            storeNote = it,
                            notesDB = notesDB,
                            notesLogger = notesLogger
                        )
                    }

                    changes.toDelete.forEach { notesDB.noteDao().deleteNoteById(noteId = it.localId) }

                    notesDB.preferencesDao().insertOrUpdate(
                        Preference(
                            id = PreferenceKeys.deltaToken,
                            value = action.deltaToken
                        )
                    )
                }
            }
        } catch (e: SQLiteDiskIOException) {
            notesLogger?.recordTelemetry(EventMarkers.SQLiteDiskIOException, Pair(ACTION, action.toPIIFreeString()))
        }
    }
    private fun replaceNote(
        storeNote: StoreNote,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?
    ) {
        val note = storeNote.toPersistenceNote()
        notesDB.noteDao().updateColor(note.id, storeNote.color.toPersistenceColor())
        notesDB.noteDao().updateDocumentModifiedAt(note.id, storeNote.documentModifiedAt)
        storeNote.remoteData?.let {
            updateRemoteData(note.id, it, notesDB, notesLogger)
        }
        updateDocument(note.id, storeNote.document, notesDB, notesLogger)
        updateMedia(note.id, storeNote.media, notesDB, notesLogger)
    }

    private fun handleRemoteDataUpdated(
        action: RemoteDataUpdated,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?
    ) =
        with(action) {
            updateRemoteData(noteLocalId, remoteData, notesDB, notesLogger)
        }

    private fun handleMediaDownloaded(
        findNote: (String) -> StoreNote?,
        action: MediaDownloaded,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?
    ) =
        with(action) {
            val note: StoreNote = findNote(noteId) ?: return
            updateMedia(noteId, note.media, notesDB, notesLogger)
        }

    private fun handleMediaDeleted(
        findNote: (String) -> StoreNote?,
        action: MediaDeleted,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?
    ) =
        with(action) {
            val note: StoreNote = findNote(noteId) ?: return
            val deletedMedia = note.media.find { mediaLocalId == it.localId } ?: return
            val updatedMedia = note.media - deletedMedia
            updateMedia(noteId, updatedMedia, notesDB, notesLogger)
        }

    private fun handleMediaAltTextUpdated(
        findNote: (String) -> StoreNote?,
        action: MediaAltTextUpdated,
        notesDB: NotesDatabase,
        notesLogger: NotesLogger?
    ) =
        with(action) {
            val note: StoreNote = findNote(noteId) ?: return

            val mediaWithUpdatedAltText = note.media.find { media.localId == it.localId } ?: return
            val updatedMedia = note.media - mediaWithUpdatedAltText + media.copy(
                localUrl = mediaWithUpdatedAltText.localUrl
            )

            updateMedia(noteId, updatedMedia, notesDB, notesLogger)
            note.remoteData?.let {
                updateRemoteData(noteId, it.copy(changeKey = action.changeKey), notesDB, notesLogger)
            } ?: return
        }

    private fun handlePermanentlyDeleteNote(noteLocalId: String, notesDB: NotesDatabase) =
        notesDB.noteDao().deleteNoteById(noteLocalId)

    private fun handlePermanentlyDeleteNoteReference(noteLocalId: String, notesDB: NotesDatabase) =
        notesDB.noteReferenceDao().deleteById(noteLocalId)

    private fun handleApplyConflictResolution(
        action: SyncResponseAction.ApplyConflictResolution,
        notesDB: NotesDatabase,
        findNote: (String) -> StoreNote?,
        notesLogger: NotesLogger?
    ) {
        val updatedNote = findNote(action.noteLocalId)
        updatedNote?.let {
            replaceNote(storeNote = it, notesDB = notesDB, notesLogger = notesLogger)
        }
    }

    private fun handleInvalidateClientCache(notesDB: NotesDatabase) {
        notesDB.noteDao().deleteAll()
    }

    private fun handleInvalidateClientCacheForNoteReference(notesDB: NotesDatabase) {
        notesDB.noteReferenceDao().deleteAll()
    }

    private fun handleInvalidateClientCacheForAppSpecificNotes(notesDB: NotesDatabase, createdByApp: String) {
        notesDB.noteDao().deleteNoteByAppCreated(createdByApp)
    }
}
