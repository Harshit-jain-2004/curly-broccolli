package com.microsoft.notes.notesReference.models

import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.NoteReferenceUpdate
import com.microsoft.notes.models.extensions.describeNoteReferencesList

data class NoteRefLocalChanges(
    val toCreate: List<NoteReference> = emptyList(),
    val toReplace: List<NoteReferenceUpdate> = emptyList(),
    val toDelete: List<NoteReference> = emptyList(),
    val toMarkAsDeleted: List<NoteReference> = emptyList()
) {

    companion object {
        const val DELIMITER = "\n: "
    }

    fun count(): Int = toCreate.size + toReplace.size + toDelete.size + toMarkAsDeleted.size

    fun isEmpty(): Boolean = toCreate.isEmpty() && toReplace.isEmpty() && toDelete.isEmpty() &&
        toMarkAsDeleted.isEmpty()

    fun appendToCreate(created: NoteReference): NoteRefLocalChanges = copy(toCreate = toCreate + created)

    fun appendToReplace(replaced: NoteReferenceUpdate): NoteRefLocalChanges = copy(toReplace = toReplace + replaced)

    fun appendToDelete(deleted: NoteReference): NoteRefLocalChanges = copy(toDelete = toDelete + deleted)

    fun appendToMarkAsDeleted(markedAsDeleted: NoteReference): NoteRefLocalChanges =
        copy(toMarkAsDeleted = toMarkAsDeleted + markedAsDeleted)

    override fun toString(): String {
        val stringBldr = StringBuilder()
        stringBldr.append(this::class.java.simpleName)
            .append("${DELIMITER}toCreate:  ${toCreate.describeNoteReferencesList()}")
            .append("${DELIMITER}toDelete:  ${toDelete.describeNoteReferencesList()}")
            .append("${DELIMITER}toMarkAsDeleted:  ${toMarkAsDeleted.describeNoteReferencesList()}")
            .append("${DELIMITER}toReplace: ${toReplace.map { it.remoteNote }.describeNoteReferencesList()}")

        return stringBldr.toString()
    }
}
