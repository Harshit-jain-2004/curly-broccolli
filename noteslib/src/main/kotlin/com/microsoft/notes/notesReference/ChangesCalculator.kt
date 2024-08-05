package com.microsoft.notes.notesReference

import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.notesReference.models.NoteRefLocalChanges

// returns a List of notes(with isLocalOnlyPage true) to be deleted based on their presence in a list of valid Notes
//
// Consider a scenario where we create a new page but are not able to send signal because we have
// only partial id available on this client. On another client this page is deleted.
// Now on this client the entry will stay in the SDK db forever.
fun calculateChangesForLocalOnlyNoteRefsCleanUp(
    localNotes: List<NoteReference>,
    validPageLocalIds: List<String>
): NoteRefLocalChanges {
    val validIds = validPageLocalIds.toHashSet()
    val toDelete = localNotes.filter {
        it.isLocalOnlyPage && !validIds.contains(it.pageLocalId)
    }
    return NoteRefLocalChanges(toDelete = toDelete)
}
