package com.microsoft.notes.sampleapp.com.microsoft.notes.sampleapp.utils

import com.microsoft.notes.models.NoteReference

fun getDummyNoteReferencesList(): List<NoteReference> {
    return listOf(
            NoteReference(
                    title = "November Meetings",
                    type = "Sample App",
                    previewText = "\"This is text preview for note references. This preview comes below the tile and besides the preview image. This text might be too long intentionally.\"",
                    rootContainerName = "Anna @ Contoso",
                    containerName = "Meeting Notes"
            ),
            NoteReference(
                    title = "December Meetings",
                    type = "Sample App",
                    previewText = "\"This is text preview for note references. This preview comes below the tile and besides the preview image. This text might be too long intentionally.\"",
                    rootContainerName = "Anna @ Contoso",
                    containerName = "Meeting Notes",
                    lastModifiedAt = System.currentTimeMillis() - 60000
            ),
            NoteReference(
                    title = "Random Title",
                    type = "Sample App",
                    previewText = "",
                    containerName = "Quick Notes",
                    lastModifiedAt = System.currentTimeMillis() - 120000
            )
    )
}

fun List<NoteReference>.addNoteReferences(notes: List<NoteReference>): List<NoteReference> {
    val list = this.toMutableList()
    list.addAll(notes)
    return list
}

fun List<NoteReference>.updateNoteReference(note: NoteReference): List<NoteReference> {
    val list = this.toMutableList()
    list.replaceAll { when (it.localId == note.localId) {
        true -> note
        false -> it
    } }
    return list
}

fun List<NoteReference>.deleteNoteReference(localId: String): List<NoteReference> =
        this.filter { it.localId != localId }