package com.microsoft.notes.store.reducer

import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteUpdate
import com.microsoft.notes.models.extensions.updateMediaWithLocalUrl
import com.microsoft.notes.models.extensions.updateMediaWithRemoteId
import com.microsoft.notes.store.AuthState
import com.microsoft.notes.store.AuthenticationState
import com.microsoft.notes.store.NoteReferencesList
import com.microsoft.notes.store.NotesList
import com.microsoft.notes.store.SamsungNotesList
import com.microsoft.notes.store.State
import com.microsoft.notes.store.action.SyncResponseAction
import com.microsoft.notes.store.action.SyncResponseAction.ApplyChanges
import com.microsoft.notes.store.action.SyncResponseAction.ApplyConflictResolution
import com.microsoft.notes.store.action.SyncResponseAction.ForbiddenSyncError
import com.microsoft.notes.store.action.SyncResponseAction.InvalidateClientCache
import com.microsoft.notes.store.action.SyncResponseAction.InvalidateNoteReferencesClientCache
import com.microsoft.notes.store.action.SyncResponseAction.InvalidateSamsungNotesClientCache
import com.microsoft.notes.store.action.SyncResponseAction.MediaDownloaded
import com.microsoft.notes.store.action.SyncResponseAction.MediaUploaded
import com.microsoft.notes.store.action.SyncResponseAction.NotAuthorized
import com.microsoft.notes.store.action.SyncResponseAction.PermanentlyDeleteNote
import com.microsoft.notes.store.action.SyncResponseAction.PermanentlyDeleteNoteReference
import com.microsoft.notes.store.action.SyncResponseAction.PermanentlyDeleteSamsungNote
import com.microsoft.notes.store.action.SyncResponseAction.RemoteDataUpdated
import com.microsoft.notes.store.action.SyncResponseAction.ServiceUpgradeRequired
import com.microsoft.notes.store.addNotes
import com.microsoft.notes.store.deleteNoteByLocalId
import com.microsoft.notes.store.deleteNoteReferenceByLocalId
import com.microsoft.notes.store.deleteNotes
import com.microsoft.notes.store.deleteSamsungNoteByLocalId
import com.microsoft.notes.store.getNoteForNoteLocalId
import com.microsoft.notes.store.replaceNote
import com.microsoft.notes.store.replaceNotes
import com.microsoft.notes.store.state.toSyncResponseError
import com.microsoft.notes.store.withAuthenticationStateForUser
import com.microsoft.notes.store.withNoteReferencesListForUser
import com.microsoft.notes.store.withNotesListForUser
import com.microsoft.notes.store.withSamsungNotesListForUser
import com.microsoft.notes.store.withSyncErrorStateForUser
import com.microsoft.notes.threeWayMerge.canThreeWayMerge
import com.microsoft.notes.threeWayMerge.merge.SelectionFrom
import com.microsoft.notes.threeWayMerge.noteWithNewType
import com.microsoft.notes.threeWayMerge.threeWayMerge
import com.microsoft.notes.utils.logging.NotesLogger

object SyncResponseReducer : Reducer<SyncResponseAction> {
    var notesLogger: NotesLogger? = null
    var isDebugMode: Boolean = false

    @Suppress("LongMethod")
    override fun reduce(
        action: SyncResponseAction,
        currentState: State,
        notesLogger: NotesLogger?,
        isDebugMode: Boolean
    ): State {
        this.notesLogger = notesLogger
        this.isDebugMode = isDebugMode

        notesLogger?.i(message = "syncResponseReducer: ${action.toLoggingIdentifier()}")
        when (action) {
            is ApplyChanges -> {
                with(action) {
                    notesLogger?.i(
                        message = "applyChanges: toCreate: ${changes.toCreate.size}, " +
                            "toDelete: ${changes.toDelete.size}, toReplace: ${changes.toReplace.size}"
                    )

                    val mergedNotes = changes.toReplace.map {
                        val replacementNote = currentState.getNoteForNoteLocalId(it.noteFromServer.localId)
                        if (replacementNote != null) updateNote(replacementNote, it) else return currentState
                    }
                    return currentState.addNotes(changes.toCreate, action.userID)
                        .replaceNotes(mergedNotes, action.userID)
                        .deleteNotes(changes.toDelete, action.userID)
                }
            }
            is RemoteDataUpdated -> {
                with(currentState) {
                    val note = getNoteForNoteLocalId(action.noteLocalId) ?: return currentState
                    return currentState.replaceNote(note.copy(remoteData = action.remoteData))
                }
            }
            is MediaUploaded -> {
                with(action) {
                    return updateStateWithMediaInfo(
                        currentState = currentState,
                        noteId = noteId,
                        updateNoteWithMediaInfo = { note ->
                            note.media.updateMediaWithRemoteId(
                                mediaLocalId = mediaLocalId, mediaRemoteId = mediaRemoteId
                            )
                        }
                    )
                }
            }
            is MediaDownloaded -> {
                with(action) {
                    val updatedState = updateStateWithMediaInfo(
                        currentState = currentState,
                        noteId = noteId,
                        updateNoteWithMediaInfo = { note ->
                            note.media.updateMediaWithLocalUrl(
                                mediaRemoteId = mediaRemoteId, localUrl = localUrl, mimeType = mimeType
                            )
                        }
                    )
                    return updatedState
                }
            }
            is PermanentlyDeleteNote -> {
                notesLogger?.i(message = "permanentlyDeleteNote. noteLocalId")
                return currentState.deleteNoteByLocalId(action.noteLocalId, action.userID)
            }
            is PermanentlyDeleteNoteReference -> {
                notesLogger?.i(message = "permanentlyDeleteNoteReference. noteLocalId")
                return currentState.deleteNoteReferenceByLocalId(action.noteLocalId, action.userID)
            }
            is PermanentlyDeleteSamsungNote -> {
                notesLogger?.i(message = "permanentlyDeleteNote. noteLocalId")
                return currentState.deleteSamsungNoteByLocalId(action.noteLocalId, action.userID)
            }
            is ApplyConflictResolution -> {
                with(action) {
                    val localNote = currentState.getNoteForNoteLocalId(noteLocalId)
                    return if (localNote != null) {
                        val shadowNote = localNote.remoteData?.lastServerVersion ?: localNote
                        val mergedNote = if (canThreeWayMerge(
                                shadowNote,
                                localNote, remoteNote
                            )
                        ) {
                            val merged = threeWayMerge(shadowNote, localNote, remoteNote)
                            merged.copy(
                                uiShadow = localNote.uiShadow,
                                remoteData = remoteNote.remoteData,
                                uiRevision = localNote.uiRevision,
                                documentModifiedAt = localNote.documentModifiedAt
                            )
                        } else {
                            noteWithNewType(shadowNote, localNote, remoteNote)
                        }

                        currentState.replaceNote(mergedNote)
                    } else {
                        currentState
                    }
                }
            }
            is NotAuthorized -> {
                val newAuthenticationState = AuthenticationState(AuthState.NOT_AUTHORIZED)
                return currentState.withAuthenticationStateForUser(
                    authState = newAuthenticationState,
                    userID = action.userID
                )
            }
            is InvalidateClientCache -> {
                return currentState.withNotesListForUser(notesList = NotesList.emptyNotesList(), userID = action.userID)
            }
            is InvalidateNoteReferencesClientCache -> {
                return currentState.withNoteReferencesListForUser(noteReferencesList = NoteReferencesList.emptyNotesList(), userID = action.userID)
            }
            is InvalidateSamsungNotesClientCache ->
                return currentState
                    .withSamsungNotesListForUser(samsungNotesList = SamsungNotesList.emptySamsungNotesList(), userID = action.userID)
            is ForbiddenSyncError -> {
                return toSyncResponseError(action)?.let {
                    currentState.withSyncErrorStateForUser(
                        userID = action
                            .userID,
                        syncErrorState = it
                    )
                }
                    ?: currentState
            }
            is ServiceUpgradeRequired -> {
                return toSyncResponseError(action)?.let {
                    currentState.withSyncErrorStateForUser(
                        userID = action
                            .userID,
                        syncErrorState = it
                    )
                }
                    ?: currentState
            }
            else -> return currentState
        }
    }

    private fun updateStateWithMediaInfo(
        currentState: State,
        noteId: String,
        updateNoteWithMediaInfo: (note: Note) -> List<Media>
    ): State {
        val note = currentState.getNoteForNoteLocalId(noteId) ?: return currentState
        val mewMedia = updateNoteWithMediaInfo(note)
        val updatedNote = note.copy(media = mewMedia)
        return currentState.replaceNote(updatedNote)
    }

    private fun updateNote(currentNote: Note, noteUpdate: NoteUpdate): Note {
        val (noteFromServer, uiRevision) = noteUpdate
        val favorLocal = currentNote.uiRevision != uiRevision

        return if (currentNote.remoteData != null) {
            val serverShadow = currentNote.remoteData.lastServerVersion

            if (!canThreeWayMerge(serverShadow, noteFromServer, currentNote)) {
                return noteWithNewType(
                    serverShadow, noteFromServer,
                    currentNote
                )
            }

            val updatedNote = when (favorLocal) {
                true -> threeWayMerge(
                    base = serverShadow,
                    primary = currentNote,
                    secondary = noteFromServer,
                    selectionFrom = SelectionFrom.PRIMARY
                )
                false -> threeWayMerge(
                    base = serverShadow,
                    primary = noteFromServer,
                    secondary = currentNote,
                    selectionFrom = SelectionFrom.SECONDARY
                )
            }
            updatedNote.copy(
                uiShadow = currentNote.uiShadow,
                uiRevision = currentNote.uiRevision,
                remoteData = noteFromServer.remoteData,
                documentModifiedAt = if (favorLocal) {
                    currentNote.documentModifiedAt
                } else {
                    noteFromServer.documentModifiedAt
                }
            )
        } else {
            // This shouldn't happen. In debug we will crash, and in release, we'll keep us safe here and use
            // currentNote or noteFromServer depending of the favorLocal value.

            if (isDebugMode) {
                throw IllegalStateException(
                    "currentNote.remoteData is null! this should not happen!" +
                        "\nbase: ${currentNote.remoteData} \ncurrentNote: $currentNote \nnoteFromServer: " +
                        "$noteFromServer"
                )
            } else {
                if (!canThreeWayMerge(currentNote, noteFromServer, currentNote)) {
                    return noteWithNewType(
                        currentNote, noteFromServer,
                        currentNote
                    )
                }
                when (favorLocal) {
                    true -> currentNote
                    false -> noteFromServer
                }
            }
        }
    }
}
