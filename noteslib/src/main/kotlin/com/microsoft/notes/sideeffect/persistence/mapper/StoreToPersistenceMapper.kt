package com.microsoft.notes.sideeffect.persistence.mapper

import com.microsoft.notes.notesReference.models.NoteRefSourceId
import com.microsoft.notes.sideeffect.persistence.extensions.toJson
import com.microsoft.notes.sideeffect.persistence.extensions.toNoteReferenceMediaJson
import com.microsoft.notes.models.Color as StoreColor
import com.microsoft.notes.models.MeetingNote as StoreMeetingNote
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.models.NoteReference as StoreNoteReference
import com.microsoft.notes.sideeffect.persistence.MeetingNote as PersistenceMeetingNote
import com.microsoft.notes.sideeffect.persistence.Note as PersistenceNote
import com.microsoft.notes.sideeffect.persistence.NoteReference as PersistenceNoteReference

fun StoreColor.toPersistenceColor(): Int =
    when (this) {
        StoreColor.GREY -> 0
        StoreColor.YELLOW -> 1
        StoreColor.GREEN -> 2
        StoreColor.PINK -> 3
        StoreColor.PURPLE -> 4
        StoreColor.BLUE -> 5
        StoreColor.CHARCOAL -> 6
    }

private fun NoteRefSourceId.toSourceId(): String? =
    when (this) {
        is NoteRefSourceId.FullSourceId -> this.fullSourceId
        else -> null
    }

private fun NoteRefSourceId.toPartialId(): String? =
    when (this) {
        is NoteRefSourceId.PartialSourceId -> this.partialId
        else -> null
    }

private fun NoteRefSourceId.toNotebookUrl(): String? =
    when (this) {
        is NoteRefSourceId.PartialSourceId -> this.nbUrl
        else -> null
    }

fun StoreNote.toPersistenceNote(): PersistenceNote = PersistenceNote(
    id = localId,
    isDeleted = isDeleted,
    color = color.toPersistenceColor(),
    localCreatedAt = localCreatedAt,
    documentModifiedAt = documentModifiedAt,
    remoteData = remoteData?.toJson(),
    document = document.toJson(),
    media = media.toJson(),
    createdByApp = createdByApp,
    title = title,
    isPinned = isPinned,
    pinnedAt = pinnedAt,
    context = metadata.context?.toJson(),
    reminder = metadata.reminder?.toJson()
)

fun StoreNoteReference.toPersistenceNoteReference(): PersistenceNoteReference = PersistenceNoteReference(
    id = localId,
    type = type,
    lastModifiedAt = lastModifiedAt,
    title = title,
    clientUrl = clientUrl,
    color = color.value.toString(),
    createdAt = createdAt,
    previewImageUrl = previewImageUrl,
    previewText = previewText,
    remoteId = remoteId,
    pageSourceId = this.pageSourceId?.toSourceId(),
    pagePartialSourceId = this.pageSourceId?.toPartialId(),
    pageLocalId = pageLocalId,
    sectionSourceId = sectionSourceId?.fullSourceId,
    sectionLocalId = sectionLocalId,
    isLocalOnlyPage = isLocalOnlyPage,
    isDeleted = isDeleted,
    notebookUrl = this.pageSourceId?.toNotebookUrl(),
    webUrl = webUrl,
    weight = weight,
    containerName = containerName,
    rootContainerName = rootContainerName,
    rootContainerSourceId = rootContainerSourceId?.fullSourceId,
    isMediaPresent = isMediaPresent,
    previewRichText = previewRichText,
    isPinned = isPinned,
    pinnedAt = pinnedAt,
    media = media?.toNoteReferenceMediaJson()
)

fun StoreMeetingNote.toPersistenceMeetingNote(): PersistenceMeetingNote = PersistenceMeetingNote(
    localId = localId,
    remoteId = remoteId,
    fileName = fileName,
    createdTime = createdTime,
    lastModifiedTime = lastModifiedTime,
    title = title,
    type = type,
    staticTeaser = staticTeaser,
    accessUrl = accessUrl,
    containerUrl = containerUrl,
    containerTitle = containerTitle,
    docId = docId,
    fileUrl = fileUrl,
    driveId = driveId,
    itemId = itemId,
    modifiedBy = modifiedBy,
    modifiedByDisplayName = modifiedByDisplayName,
    meetingId = meetingId,
    meetingSubject = meetingSubject,
    meetingStartTime = meetingStartTime,
    meetingEndTime = meetingEndTime,
    meetingOrganizer = meetingOrganizer,
    seriesMasterId = seriesMasterId,
    occuranceId = occuranceId
)
