@file:Suppress("TooManyFunctions")
package com.microsoft.notes.sideeffect.sync.mapper

import com.microsoft.notes.models.extensions.NoteRefColor
import com.microsoft.notes.notesReference.models.NoteRefSourceId
import com.microsoft.notes.richtext.scheme.NoteContext
import com.microsoft.notes.richtext.scheme.NoteMetadata
import com.microsoft.notes.sync.ApiResponseEvent.RemoteNotesSyncError.SyncErrorType
import com.microsoft.notes.sync.models.InlineStyle
import com.microsoft.notes.sync.models.ParagraphChunk
import com.microsoft.notes.sync.models.RemoteNoteMetadata
import com.microsoft.notes.utils.utils.Constants.PREVIEW_AGENDA_HEADER
import com.microsoft.notes.utils.utils.Constants.PREVIEW_FOLLOWUP_TASKS_HEADER
import com.microsoft.notes.utils.utils.Constants.PREVIEW_MEETING_NOTE_HEADER
import com.microsoft.notes.utils.utils.SIZE_OF_UUID
import com.microsoft.notes.utils.utils.parseISO8601StringToMillis
import com.microsoft.notes.utils.utils.toBase64UrlEncoding
import com.microsoft.notes.utils.utils.toBigEndianByteArray
import java.util.UUID
import com.microsoft.notes.models.Color as StoreColor
import com.microsoft.notes.models.ImageDimensions as StoreImageDimensions
import com.microsoft.notes.models.Media as StoreMedia
import com.microsoft.notes.models.MeetingNote as StoreMeetingNote
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.models.NoteReference as StoreNoteReference
import com.microsoft.notes.models.NoteReferenceMedia as StoreNoteReferenceMedia
import com.microsoft.notes.models.RemoteData as StoreRemoteData
import com.microsoft.notes.richtext.scheme.Content as StoreContent
import com.microsoft.notes.richtext.scheme.Document as StoreDocument
import com.microsoft.notes.richtext.scheme.DocumentType as StoreDocumentType
import com.microsoft.notes.richtext.scheme.InkPoint as StoreInkPoint
import com.microsoft.notes.richtext.scheme.InlineMedia as StoreInlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph as StoreParagraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle as StoreParagraphStyle
import com.microsoft.notes.richtext.scheme.Span as StoreSpan
import com.microsoft.notes.richtext.scheme.SpanStyle as StoreSpanStyle
import com.microsoft.notes.richtext.scheme.Stroke as StoreStroke
import com.microsoft.notes.store.action.SyncStateAction.RemoteNotesSyncErrorAction.SyncErrorType as StoreErrorType
import com.microsoft.notes.sync.models.Block.InlineMedia as SyncInlineMedia
import com.microsoft.notes.sync.models.Block.Paragraph as SyncParagraph
import com.microsoft.notes.sync.models.Document as SyncDocument
import com.microsoft.notes.sync.models.Document.DocumentType as SyncDocumentType
import com.microsoft.notes.sync.models.Document.FutureDocument as SyncFutureDocument
import com.microsoft.notes.sync.models.Document.InkDocument as SyncInkDocument
import com.microsoft.notes.sync.models.Document.RenderedInkDocument as SyncRenderedInkDocument
import com.microsoft.notes.sync.models.Document.RichTextDocument as SyncRichTextDocument
import com.microsoft.notes.sync.models.Document.SamsungHtmlDocument as SyncSamsungHtmlDocument
import com.microsoft.notes.sync.models.ImageDimensions as SyncImageDimensions
import com.microsoft.notes.sync.models.InkPoint as SyncInkPoint
import com.microsoft.notes.sync.models.Media as SyncMedia
import com.microsoft.notes.sync.models.NoteReferenceMedia as SyncNoteReferenceMedia
import com.microsoft.notes.sync.models.RemoteMeetingNote as SyncRemoteMeetingNote
import com.microsoft.notes.sync.models.RemoteNote as SyncRemoteNote
import com.microsoft.notes.sync.models.RemoteNoteReference as SyncRemoteNoteReference
import com.microsoft.notes.sync.models.Stroke as SyncStroke
import com.microsoft.notes.sync.models.localOnly.Note.Color as SyncColor

internal fun SyncRemoteNote.toLastServerVersion(localNoteId: String): StoreNote =
    toLastServerVersion(StoreNote(localId = localNoteId))

internal fun SyncRemoteNote.toLastServerVersion(localNote: StoreNote, revision: Long = 0): StoreNote =
    toStoreNoteWithoutRemoteData(localNote, revision)

private fun SyncRemoteNote.toStoreNoteWithoutRemoteData(localNote: StoreNote, revision: Long): StoreNote {
    return StoreNote(
        localId = localNote.localId,
        media = media.toStoreMediaList(localNote.media),
        remoteData = null,
        document = document.toStoreDocument(localDocument = localNote.document),
        isDeleted = localNote.isDeleted,
        color = color.toStoreColor(),
        localCreatedAt = parseISO8601StringToMillis(createdAt),
        documentModifiedAt = parseDocumentModifiedAt(),
        uiRevision = revision,
        uiShadow = null,
        createdByApp = createdByApp,
        isPinned = localNote.isPinned,
        pinnedAt = localNote.pinnedAt,
        metadata = metadata.toNoteMetadata(localNote.metadata)
    )
}

private fun SyncRemoteNote.parseDocumentModifiedAt(): Long {
    val documentModifiedAt = documentModifiedAt
    return if (documentModifiedAt != null) {
        parseISO8601StringToMillis(documentModifiedAt)
    } else {
        parseISO8601StringToMillis(lastModifiedAt)
    }
}

internal fun SyncRemoteNote.toStoreNote(localNoteId: String): StoreNote =
    toStoreNote(StoreNote(localId = localNoteId))

internal fun SyncRemoteNote.toStoreNote(localNote: StoreNote, revision: Long = 0): StoreNote {
    val remoteData = StoreRemoteData(
        id = id,
        changeKey = changeKey,
        lastServerVersion = toLastServerVersion(localNote, revision),
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt
    )

    return StoreNote(
        localId = localNote.localId,
        media = media.toStoreMediaList(localNote.media),
        remoteData = remoteData,
        document = document.toStoreDocument(localDocument = localNote.document),
        isDeleted = localNote.isDeleted,
        color = color.toStoreColor(),
        localCreatedAt = remoteData.createdAt,
        documentModifiedAt = parseDocumentModifiedAt(),
        uiRevision = revision,
        uiShadow = localNote.uiShadow,
        createdByApp = createdByApp,
        title = title,
        isPinned = localNote.isPinned,
        pinnedAt = localNote.pinnedAt,
        metadata = metadata.toNoteMetadata(localNote.metadata)
    )
}

// TODO: Remove the parameter isMediaPresent when the sync payload contains this field
internal fun SyncRemoteNoteReference.toStoreNoteReference(localNoteReferenceId: String, pageLocalId: String?, sectionLocalId: String?, isDeleted: Boolean, isMediaPresent: Int? = null, isPinned: Boolean, pinnedAt: Long?, localNoteReferenceMedia: List<StoreNoteReferenceMedia>?): StoreNoteReference {
    return StoreNoteReference(
        localId = localNoteReferenceId,
        remoteId = id,
        type = metaData.type,
        pageSourceId = metaData.id,
        pageLocalId = pageLocalId,
        sectionSourceId = NoteRefSourceId.FullSourceId(visualizationData.containers.first().id),
        sectionLocalId = sectionLocalId,
        isLocalOnlyPage = false,
        isDeleted = isDeleted,
        createdAt = metaData.createdAt,
        lastModifiedAt = metaData.lastModified,
        weight = weight,
        title = visualizationData.title,
        previewText = visualizationData.previewText,
        color = NoteRefColor.fromInt(visualizationData.color),
        previewImageUrl = "",
        webUrl = metaData.webUrl,
        clientUrl = metaData.clientUrl,
        containerName = visualizationData.containers.first().name,
        rootContainerName = visualizationData.containers.last().name,
        rootContainerSourceId = NoteRefSourceId.FullSourceId(visualizationData.containers.last().id),
        isMediaPresent = isMediaPresent ?: 0,
        previewRichText = visualizationData.previewRichText,
        isPinned = isPinned,
        pinnedAt = pinnedAt,
        media = visualizationData.media?.toStoreNoteReferenceMediaList(localNoteReferenceMedia ?: emptyList())
    )
}

internal fun SyncRemoteMeetingNote.toStoreMeetingNote(localMeetingNoteId: String): StoreMeetingNote {
    return StoreMeetingNote(
        localId = localMeetingNoteId,
        remoteId = id,
        fileName = fileName,
        createdTime = createdTime,
        lastModifiedTime = lastModifiedTime,
        title = visualizationData.title,
        type = visualizationData.type,
        staticTeaser = getSanitizedStaticTeaser(visualizationData.staticTeaser),
        accessUrl = visualizationData.accessUrl,
        containerUrl = visualizationData.containerUrl,
        containerTitle = visualizationData.containerTitle,
        docId = sharepointItem.docId,
        fileUrl = sharepointItem.fileUrl,
        driveId = getDriveIdV2(
            sharepointItem.siteId,
            sharepointItem.webId,
            sharepointItem.listId
        ),
        itemId = ((id).split('_').getOrElse(2) { "" }),
        modifiedBy = sharepointItem.modifiedBy,
        modifiedByDisplayName = sharepointItem.modifiedByDisplayName,
        meetingId = null,
        meetingSubject = null,
        meetingStartTime = null,
        meetingEndTime = null,
        meetingOrganizer = null,
        seriesMasterId = null,
        occuranceId = null
    )
}

private fun getDriveIdV2(siteId: String, webId: String, listId: String): String {
    return encodeDriveIdV2(
        UUID.fromString(siteId),
        UUID.fromString(webId),
        UUID.fromString(listId)
    )
}

private fun encodeDriveIdV2(siteId: UUID, webId: UUID, listId: UUID): String {
    // Generate the composite id by converting all UUIDs to their unsigned big-endian byte representation, constructing a
    // concatenated array, and then Base64 encoding it.
    val compositeIdBytes = ByteArray(SIZE_OF_UUID * 3)

    siteId.toBigEndianByteArray().copyInto(compositeIdBytes, 0, 0, SIZE_OF_UUID)
    webId.toBigEndianByteArray().copyInto(compositeIdBytes, SIZE_OF_UUID, 0, SIZE_OF_UUID)
    listId.toBigEndianByteArray().copyInto(compositeIdBytes, SIZE_OF_UUID * 2, 0, SIZE_OF_UUID)

    val driveCompositeIdPrefixV2 = "b!"

    return driveCompositeIdPrefixV2 +
        compositeIdBytes.toBase64UrlEncoding()
}

fun getSanitizedStaticTeaser(staticTeaser: String): String {
    /* Show agenda, meeting notes and followup tasks if exists in that order
        If the headers itself have been edited/removed then fallback to showing the first line having more than 4 words
        Preview text logic may be updated in future based on user feedback
     */
    val agendaHeadingStart = staticTeaser.indexOf(PREVIEW_AGENDA_HEADER)
    val meetingNoteHeadingStart = staticTeaser.indexOf(PREVIEW_MEETING_NOTE_HEADER)

    if (agendaHeadingStart != -1 && meetingNoteHeadingStart > agendaHeadingStart) {
        val agenda = staticTeaser.substring(agendaHeadingStart + PREVIEW_AGENDA_HEADER.length, meetingNoteHeadingStart).trim()
        if (agenda.isNotEmpty()) return agenda
    }

    val followupNotesHeadingStart = staticTeaser.indexOf(PREVIEW_FOLLOWUP_TASKS_HEADER)

    if (meetingNoteHeadingStart != -1 && followupNotesHeadingStart > meetingNoteHeadingStart) {
        val meetingNotes = staticTeaser.substring(meetingNoteHeadingStart + PREVIEW_MEETING_NOTE_HEADER.length, followupNotesHeadingStart).trim()
        if (meetingNotes.isNotEmpty()) return meetingNotes
    }

    if (followupNotesHeadingStart != -1) {
        val followupNotes = staticTeaser.substring(followupNotesHeadingStart + PREVIEW_FOLLOWUP_TASKS_HEADER.length).trim()
        if (followupNotes.isNotEmpty()) return followupNotes
    }

    // Eliminating initial lines having less than 5 words, as these can be part of Tasks field in meeting note and not required
    return (
        staticTeaser.lines()
            .dropWhile { (it.split("[,.!?\\s]+".toRegex()).size < 5) }
        ).joinToString("\n")
}

fun List<SyncNoteReferenceMedia>.toStoreNoteReferenceMediaList(localNoteReferenceMediaList: List<StoreNoteReferenceMedia>): List<StoreNoteReferenceMedia> {
    return this.map {
        val localMedia = localNoteReferenceMediaList.find { localMedia ->
            localMedia.mediaID == it.mediaID
        }
        it.toStoreNoteReferenceMedia(localMedia)
    }
}

fun List<SyncMedia>.toStoreMediaList(localMediaList: List<StoreMedia>): List<StoreMedia> {
    return this.map {
        val localMedia = localMediaList.find { localMedia ->
            localMedia.remoteId == it.id || localMedia.localId == it.createdWithLocalId
        }
        if (localMedia != null) {
            it.toStoreMedia(localMedia)
        } else {
            it.toStoreMedia()
        }
    }
}

fun SyncDocument.toStoreDocument(localDocument: StoreDocument): StoreDocument {
    return when (this) {
        is SyncRichTextDocument -> StoreDocument(
            blocks = blocks.map {
                when (it) {
                    is SyncParagraph -> it.toStoreParagraph()
                    is SyncInlineMedia -> {
                        val media = localDocument.blocks.find { b -> b.localId == it.id }
                        it.toStoreInlineMedia(media as? StoreInlineMedia)
                    }
                }
            },
            type = toStoreDocumentType()
        )
        is SyncSamsungHtmlDocument -> StoreDocument(
            body = body,
            bodyPreview = bodyPreview,
            blocks = blocks.map {
                when (it) {
                    is SyncParagraph -> it.toStoreParagraph()
                    // TODO 29-Jan-21 gopalsa: Currently samsung notes don't support inline media.
                    // But we are unsure of how blocks change when media notes are supported.
                    is SyncInlineMedia -> {
                        val media = localDocument.blocks.find { b -> b.localId == it.id }
                        it.toStoreInlineMedia(media as? StoreInlineMedia)
                    }
                }
            },
            type = toStoreDocumentType(),
            dataVersion = dataVersion
        )
        is SyncRenderedInkDocument -> {
            StoreDocument(
                blocks = listOf(toStoreInlineMedia()),
                type = toStoreDocumentType()
            )
        }
        is SyncInkDocument -> {
            StoreDocument(
                strokes = strokes.map { it.toStoreStroke() },
                type = toStoreDocumentType()
            )
        }
        is SyncFutureDocument -> {
            StoreDocument(
                blocks = emptyList(),
                type = toStoreDocumentType()
            )
        }
    }
}

private fun SyncStroke.toStoreStroke(): StoreStroke =
    StoreStroke(id = id, points = points.map { it.toStoreInkPoint() })

private fun SyncInkPoint.toStoreInkPoint(): StoreInkPoint = StoreInkPoint(x = x, y = y, p = p)

private fun SyncRenderedInkDocument.toStoreInlineMedia(): StoreInlineMedia =
    StoreInlineMedia(localUrl = image, altText = text, mimeType = mimeType)

internal fun SyncParagraph.toStoreParagraph(): StoreParagraph {
    var text = ""
    val spans = mutableListOf<StoreSpan>()
    var currentIndex = 0
    content.forEach {
        val chunkText = when (it) {
            is ParagraphChunk.PlainText -> {
                it.string
            }
            is ParagraphChunk.RichText -> {
                val spanStyle = StoreSpanStyle(
                    bold = it.inlineStyles.contains(InlineStyle.Bold),
                    italic = it.inlineStyles.contains(InlineStyle.Italic),
                    underline = it.inlineStyles.contains(InlineStyle.Underlined),
                    strikethrough = it.inlineStyles.contains(InlineStyle.Strikethrough)
                )
                val span = StoreSpan(
                    style = spanStyle,
                    start = currentIndex,
                    end = currentIndex + it.string.length,
                    flag = 0
                )
                spans.add(span)
                it.string
            }
        }
        text += chunkText
        currentIndex += chunkText.length
    }
    return StoreParagraph(
        localId = id, content = StoreContent(text = text, spans = spans),
        style = StoreParagraphStyle(this.blockStyles.unorderedList, this.blockStyles.rightToLeft)
    )
}

private fun SyncInlineMedia.toStoreInlineMedia(localMedia: StoreInlineMedia?): StoreInlineMedia {
    return StoreInlineMedia(
        localId = id,
        localUrl = localMedia?.localUrl,
        remoteUrl = source,
        mimeType = mimeType,
        altText = altText
    )
}

internal fun SyncInlineMedia.toStoreInlineMedia(localUrl: String?): StoreInlineMedia {
    return StoreInlineMedia(
        localId = id,
        localUrl = localUrl,
        remoteUrl = source,
        mimeType = mimeType,
        altText = altText
    )
}

internal fun SyncImageDimensions.toStoreImageDimensions(): StoreImageDimensions =
    StoreImageDimensions(height = height.toLong(), width = width.toLong())

internal fun SyncMedia.toStoreMedia(localMedia: StoreMedia): StoreMedia {
    return StoreMedia(
        localId = localMedia.localId,
        remoteId = id,
        localUrl = localMedia.localUrl,
        mimeType = mimeType,
        altText = altText,
        imageDimensions = imageDimensions?.toStoreImageDimensions(),
        lastModified = parseISO8601StringToMillis(lastModified)
    )
}

internal fun SyncMedia.toStoreMedia(): StoreMedia {
    return StoreMedia(
        localId = createdWithLocalId,
        remoteId = id,
        localUrl = null,
        mimeType = mimeType,
        altText = altText,
        imageDimensions = imageDimensions?.toStoreImageDimensions(),
        lastModified = parseISO8601StringToMillis(lastModified)
    )
}

internal fun SyncNoteReferenceMedia.toStoreNoteReferenceMedia(localMedia: StoreNoteReferenceMedia?): StoreNoteReferenceMedia {
    val mediaInfo = localMedia ?: StoreNoteReferenceMedia(mediaID, mediaType, "")
    return StoreNoteReferenceMedia(
        mediaID = mediaInfo.mediaID,
        mediaType = mediaInfo.mediaType,
        localImageUrl = mediaInfo.localImageUrl
    )
}

internal fun Int.toStoreColor(): StoreColor {
    val color = SyncColor.fromInt(this)
    return when (color) {
        SyncColor.BLUE -> StoreColor.BLUE
        SyncColor.GREY -> StoreColor.GREY
        SyncColor.YELLOW -> StoreColor.YELLOW
        SyncColor.GREEN -> StoreColor.GREEN
        SyncColor.PINK -> StoreColor.PINK
        SyncColor.PURPLE -> StoreColor.PURPLE
        SyncColor.CHARCOAL -> StoreColor.CHARCOAL
        else -> StoreColor.getDefault()
    }
}

internal fun SyncDocument.toStoreDocumentType(): StoreDocumentType {
    return when (type) {
        SyncDocumentType.RICH_TEXT -> StoreDocumentType.RICH_TEXT
        SyncDocumentType.HTML -> StoreDocumentType.SAMSUNG_NOTE
        SyncDocumentType.RENDERED_INK -> StoreDocumentType.RENDERED_INK
        SyncDocumentType.INK -> StoreDocumentType.INK
        SyncDocumentType.FUTURE -> StoreDocumentType.FUTURE
    }
}

internal fun SyncErrorType.toStoreSyncErrorType(): StoreErrorType {
    return when (this) {
        SyncErrorType.NetworkUnavailable -> StoreErrorType.NetworkUnavailable
        SyncErrorType.Unauthenticated -> StoreErrorType.Unauthenticated
        SyncErrorType.SyncPaused -> StoreErrorType.SyncPaused
        SyncErrorType.SyncFailure -> StoreErrorType.SyncFailure
    }
}

// need ot think about how to balance local and remote metadata, for now just taking remote
internal fun RemoteNoteMetadata.toNoteMetadata(localMetadata: NoteMetadata): NoteMetadata =
    NoteMetadata(context = context?.let { NoteContext(host = it.host, hostIcon = it.hostIcon, displayName = it.displayName, url = it.url) }, reminder = localMetadata.reminder)
