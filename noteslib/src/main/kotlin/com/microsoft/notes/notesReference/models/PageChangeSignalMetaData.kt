package com.microsoft.notes.notesReference.models

import com.microsoft.notes.models.NoteReferenceMedia

data class PageChangeSignalMetaData(
    val sectionLocalId: String,
    val sectionSourceId: NoteRefSourceId.FullSourceId?,
    val color: String?,
    val webUrl: String?,
    val clientUrl: String,
    val createdDateTime: String,
    val lastModifiedDateTime: String,
    val title: String,
    val sectionName: String,
    val notebookName: String,
    val previewImageUrl: String?,
    val previewText: String,
    val isMediaPresent: Int?,
    val previewRichText: String?,
    val media: List<NoteReferenceMedia>?
)
