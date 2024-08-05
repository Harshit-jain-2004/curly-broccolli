package com.microsoft.notes.sampleapp.utils

import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.utils.utils.IdentityMetaData

fun executePostSignInTask(identityMetaData: IdentityMetaData, onCompletionTask: () -> Unit) {
    with (NotesLibrary.getInstance()) {
        newAuthToken(identityMetaData).then { onCompletionTask() }
        fetchNotes(identityMetaData.userID)
        startPolling(identityMetaData.userID)
    }
}
