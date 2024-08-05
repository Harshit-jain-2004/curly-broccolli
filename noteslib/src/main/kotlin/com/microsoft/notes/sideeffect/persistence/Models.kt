package com.microsoft.notes.sideeffect.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MeetingNote(
    @PrimaryKey var localId: String,
    var remoteId: String,
    var fileName: String,
    var createdTime: Long,
    var lastModifiedTime: Long,
    var title: String,
    var type: String,
    var staticTeaser: String,
    var accessUrl: String,
    var containerUrl: String,
    var containerTitle: String,
    var docId: Long,
    var fileUrl: String,
    val driveId: String,
    val itemId: String,
    var modifiedBy: String,
    var modifiedByDisplayName: String,
    val meetingId: String?,
    val meetingSubject: String?,
    val meetingStartTime: Long?,
    val meetingEndTime: Long?,
    val meetingOrganizer: String?,
    val seriesMasterId: String?,
    val occuranceId: String?
)

@Entity
data class Note(
    @PrimaryKey var id: String,
    var isDeleted: Boolean,
    var color: Int,
    var localCreatedAt: Long,
    var documentModifiedAt: Long,
    var remoteData: String?,
    var document: String,
    var media: String,
    var createdByApp: String? = null,
    var title: String? = null,
    var isPinned: Boolean = false,
    val pinnedAt: Long?,
    val context: String? = null,
    val reminder: String? = null
)

@Entity
data class NoteReference(
    @PrimaryKey var id: String,
    var remoteId: String?,
    var type: String,
    var pageSourceId: String? = null,
    var pagePartialSourceId: String? = null,
    var pageLocalId: String? = null,
    var sectionSourceId: String? = null,
    var sectionLocalId: String? = null,
    var isLocalOnlyPage: Boolean,
    var isDeleted: Boolean,
    var createdAt: Long,
    var lastModifiedAt: Long,
    var weight: Float?,
    var title: String?,
    var previewText: String,
    var previewImageUrl: String?,
    var color: String?, // TODO Migrate this to Int
    var notebookUrl: String? = null,
    var webUrl: String?,
    var clientUrl: String?,
    val containerName: String? = null,
    val rootContainerName: String? = null,
    val rootContainerSourceId: String? = null,
    val isMediaPresent: Int? = null,
    val previewRichText: String? = null,
    val isPinned: Boolean = false,
    val pinnedAt: Long?,
    val media: String? = null
)

@Entity
data class Preference(
    @PrimaryKey var id: String,
    var value: String? = null
)

object PreferenceKeys {
    const val deltaToken = "deltaToken"
    const val samsungNotesDeltaToken = "samsungNotesDeltaToken"
    const val noteReferencesDeltaToken = "noteReferencesDeltaToken"
}
