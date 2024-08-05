package com.microsoft.notes.sideeffect.persistence.mapper

import com.microsoft.notes.models.extensions.NoteRefColor
import com.microsoft.notes.notesReference.models.NoteRefSourceId
import com.microsoft.notes.richtext.scheme.NoteMetadata
import com.microsoft.notes.sideeffect.persistence.extensions.fromDocumentJson
import com.microsoft.notes.sideeffect.persistence.extensions.fromMediaJson
import com.microsoft.notes.sideeffect.persistence.extensions.fromMetadataContextJson
import com.microsoft.notes.sideeffect.persistence.extensions.fromMetadataReminderWrapperJson
import com.microsoft.notes.sideeffect.persistence.extensions.fromNoteReferenceMediaJson
import com.microsoft.notes.sideeffect.persistence.extensions.fromRemoteDataJson
import com.microsoft.notes.sync.models.Document.SamsungHtmlDocument.Companion.DEFAULT_DATA_VERSION
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.models.Color as StoreColor
import com.microsoft.notes.models.MeetingNote as StoreMeetingNote
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.models.NoteReference as StoreNoteReference
import com.microsoft.notes.sideeffect.persistence.MeetingNote as PersistenceMeetingNote
import com.microsoft.notes.sideeffect.persistence.Note as PersistenceNote
import com.microsoft.notes.sideeffect.persistence.NoteReference as PersistenceNoteReference

fun Int.toStoreColor(): StoreColor =
    when (this) {
        0 -> StoreColor.GREY
        1 -> StoreColor.YELLOW
        2 -> StoreColor.GREEN
        3 -> StoreColor.PINK
        4 -> StoreColor.PURPLE
        5 -> StoreColor.BLUE
        6 -> StoreColor.CHARCOAL
        else -> StoreColor.BLUE
    }

fun PersistenceNote.toStoreNote(notesLogger: NotesLogger? = null): StoreNote? {
    try {
        var document = document.fromDocumentJson()
        if (document == null) {
            notesLogger?.recordTelemetry(
                EventMarkers.ParsingDocumentJSONFailed,
                Pair(NotesSDKTelemetryKeys.NoteProperty.NOTE_TYPE, createdByApp ?: "")
            )
            return null
        }

        // Handle migration: dataVersion field was added later, so it could be empty in existing notes
        if (document.isSamsungNoteDocument && document.dataVersion.isEmpty()) {
            document = document.copy(dataVersion = DEFAULT_DATA_VERSION)
        }

        val remoteData = remoteData?.fromRemoteDataJson()
        val media = media.fromMediaJson()
        val metadata = NoteMetadata(context = context?.fromMetadataContextJson(), reminder = reminder?.fromMetadataReminderWrapperJson())

        return StoreNote(
            localId = id,
            isDeleted = isDeleted,
            localCreatedAt = localCreatedAt,
            documentModifiedAt = documentModifiedAt,
            color = color.toStoreColor(),
            remoteData = remoteData,
            document = document,
            media = media,
            createdByApp = createdByApp,
            title = title,
            isPinned = isPinned,
            pinnedAt = pinnedAt,
            metadata = metadata
        )
    } catch (ex: Exception) {
        notesLogger?.recordTelemetry(
            EventMarkers.PersistenceNotesConversionException,
            Pair(NotesSDKTelemetryKeys.SyncProperty.EXCEPTION_TYPE, ex.javaClass.simpleName)
        )
        return null
    }
}

internal fun List<PersistenceNote>.toStoreNoteList(notesLogger: NotesLogger? = null): List<StoreNote> {
    val storeNotesCollection = arrayListOf<StoreNote>()
    this.forEach { note ->
        note.toStoreNote(notesLogger)?.let { storeNotesCollection.add(it) }
    }
    return storeNotesCollection
}

internal fun List<PersistenceNoteReference>.toStoreNoteReferenceList(): List<StoreNoteReference> =
    map {
        with(it) {
            StoreNoteReference(
                localId = id,
                type = type,
                lastModifiedAt = lastModifiedAt,
                title = title,
                clientUrl = clientUrl,
                color = NoteRefColor.fromInt(color?.toIntOrNull()),
                createdAt = createdAt,
                previewImageUrl = previewImageUrl,
                previewText = previewText,
                remoteId = remoteId,
                pageSourceId = mapToNoteRefSourceId(pageSourceId, pagePartialSourceId, notebookUrl),
                pageLocalId = pageLocalId,
                sectionSourceId = sectionSourceId?. let { id -> NoteRefSourceId.FullSourceId(id) },
                sectionLocalId = sectionLocalId,
                isLocalOnlyPage = isLocalOnlyPage,
                isDeleted = isDeleted,
                webUrl = webUrl,
                weight = weight,
                containerName = containerName,
                rootContainerSourceId = rootContainerSourceId?.let { id -> NoteRefSourceId.FullSourceId(id) },
                rootContainerName = rootContainerName,
                isMediaPresent = isMediaPresent,
                previewRichText = previewRichText,
                isPinned = isPinned,
                pinnedAt = pinnedAt,
                media = media?.fromNoteReferenceMediaJson()
            )
        }
    }

internal fun List<PersistenceMeetingNote>.toStoreMeetingNoteList(): List<StoreMeetingNote> =
    map {
        with(it) {
            StoreMeetingNote(
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
        }
    }

private fun mapToNoteRefSourceId(sourceId: String?, partialId: String?, nbUrl: String?): NoteRefSourceId? {
    if (sourceId != null) {
        return NoteRefSourceId.FullSourceId(sourceId)
    } else if (partialId != null && nbUrl != null) {
        return NoteRefSourceId.PartialSourceId(partialId, nbUrl)
    }
    return null
}
