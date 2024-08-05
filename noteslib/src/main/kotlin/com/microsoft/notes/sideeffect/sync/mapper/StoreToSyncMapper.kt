package com.microsoft.notes.sideeffect.sync.mapper

import com.microsoft.notes.richtext.scheme.DocumentType
import com.microsoft.notes.richtext.scheme.NoteMetadata
import com.microsoft.notes.sync.models.Document.SamsungHtmlDocument
import com.microsoft.notes.sync.models.InlineStyle
import com.microsoft.notes.sync.models.ParagraphChunk
import com.microsoft.notes.sync.models.RemoteMetadataContext
import com.microsoft.notes.sync.models.RemoteNote
import com.microsoft.notes.sync.models.RemoteNoteMetadata
import com.microsoft.notes.utils.utils.parseMillisToISO8601String
import com.microsoft.notes.models.Color as StoreColor
import com.microsoft.notes.models.ImageDimensions as StoreImageDimensions
import com.microsoft.notes.models.Media as StoreMedia
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.models.RemoteData as StoreRemoteData
import com.microsoft.notes.richtext.scheme.Content as StoreContent
import com.microsoft.notes.richtext.scheme.Document as StoreDocument
import com.microsoft.notes.richtext.scheme.InlineMedia as StoreInlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph as StoreParagraph
import com.microsoft.notes.sync.models.Block.InlineMedia as SyncInlineMedia
import com.microsoft.notes.sync.models.Block.Paragraph as SyncParagraph
import com.microsoft.notes.sync.models.BlockStyle as SyncBlockStyle
import com.microsoft.notes.sync.models.Document as SyncDocument
import com.microsoft.notes.sync.models.Document.FutureDocument as SyncFutureDocument
import com.microsoft.notes.sync.models.Document.InkDocument as SyncInkDocument
import com.microsoft.notes.sync.models.Document.RenderedInkDocument as SyncRenderedInkDocument
import com.microsoft.notes.sync.models.Document.RichTextDocument as SyncRichTextDocument
import com.microsoft.notes.sync.models.ImageDimensions as SyncImageDimensions
import com.microsoft.notes.sync.models.InkPoint as SyncInkPoint
import com.microsoft.notes.sync.models.Media as SyncMedia
import com.microsoft.notes.sync.models.Stroke as SyncStroke
import com.microsoft.notes.sync.models.localOnly.Note as SyncNote
import com.microsoft.notes.sync.models.localOnly.Note.Color as SyncColor
import com.microsoft.notes.sync.models.localOnly.RemoteData as SyncRemoteData

internal fun List<StoreMedia>.toSyncMedia(): List<SyncMedia> {
    return this.asSequence().map { it.toSyncMedia() }
        .filterNotNull()
        .toList()
}

internal fun StoreNote.toSyncNote(): SyncNote =
    SyncNote(
        id = localId,
        document = document.toSyncDocument(),
        color = color.toSyncColor(),
        createdByApp = createdByApp,
        remoteData = remoteData?.toSyncRemoteData(),
        documentModifiedAt = parseMillisToISO8601String(documentModifiedAt),
        media = media.toSyncMedia(),
        metadata = metadata.toSyncNoteMetadata()
    )

internal fun StoreRemoteData.toSyncRemoteData(): SyncRemoteData {
    val remoteNote = lastServerVersion.toRemoteNote()
    return SyncRemoteData(
        id = id,
        changeKey = changeKey,
        lastServerVersion = remoteNote,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt
    )
}

internal fun StoreNote.toRemoteNote(): RemoteNote? {
    return if (remoteData != null) {
        RemoteNote(
            id = remoteData.id,
            changeKey = remoteData.changeKey,
            document = document.toSyncDocument(),
            color = color.toSyncColor().value,
            createdWithLocalId = localId,
            createdAt = parseMillisToISO8601String(remoteData.createdAt),
            lastModifiedAt = parseMillisToISO8601String(
                remoteData.lastModifiedAt
            ),
            createdByApp = createdByApp,
            documentModifiedAt = parseMillisToISO8601String(documentModifiedAt),
            media = media.toSyncMedia(),
            metadata = metadata.toSyncNoteMetadata()
        )
    } else {
        null
    }
}

internal fun StoreDocument.toSyncDocument(): SyncDocument {
    return when (this.type) {
        DocumentType.RICH_TEXT -> SyncRichTextDocument(
            blocks = blocks.map {
                when (it) {
                    is StoreParagraph -> it.toSyncParagraph()
                    is StoreInlineMedia -> it.toSyncInlineMedia()
                }
            }
        )
        DocumentType.SAMSUNG_NOTE -> SamsungHtmlDocument(
            body, bodyPreview,
            blocks.map {
                when (it) {
                    is StoreParagraph -> it.toSyncParagraph()
                    // TODO 29-Jan-21 gopalsa: Currently samsung notes don't support inline media.
                    // But we are unsure of how blocks change when media notes are supported.
                    is StoreInlineMedia -> it.toSyncInlineMedia()
                }
            },
            dataVersion
        )
        DocumentType.RENDERED_INK -> {
            val (text, image) = blocks.firstOrNull()?.let {
                when (it) {
                    is StoreInlineMedia -> Pair(it.altText ?: "", it.localUrl ?: "")
                    else -> null
                }
            } ?: Pair("", "")
            SyncRenderedInkDocument(text = text, image = image)
        }
        DocumentType.INK -> SyncInkDocument(
            text = "",
            strokes = strokes.map { storeStroke ->
                SyncStroke(
                    id = storeStroke.id,
                    points = storeStroke.points.map { storeInkPoint ->
                        SyncInkPoint(x = storeInkPoint.x, y = storeInkPoint.y, p = storeInkPoint.p)
                    }
                )
            }
        )
        DocumentType.FUTURE -> SyncFutureDocument()
    }
}

internal fun StoreParagraph.toSyncParagraph(): SyncParagraph {
    val content = content.toSyncParagraphContent()
    val blockStyles = SyncBlockStyle(unorderedList = style.unorderedList, rightToLeft = style.rightToLeft)
    return SyncParagraph(id = localId, content = content, blockStyles = blockStyles)
}

internal fun StoreContent.toSyncParagraphContent(): List<ParagraphChunk> {
    val chunks: MutableList<ParagraphChunk> = mutableListOf()
    var currentIndex = 0
    spans.forEach {
        if (it.start > currentIndex) {
            chunks.add(ParagraphChunk.PlainText(text.substring(currentIndex, it.start)))
            currentIndex = it.start
        }

        if (it.start == currentIndex) {
            val chunkText = text.substring(it.start, it.end)
            val inlineStyles = mutableListOf<InlineStyle>()
            if (it.style.bold) {
                inlineStyles.add(InlineStyle.Bold)
            }
            if (it.style.italic) {
                inlineStyles.add(InlineStyle.Italic)
            }
            if (it.style.underline) {
                inlineStyles.add(InlineStyle.Underlined)
            }
            if (it.style.strikethrough) {
                inlineStyles.add(InlineStyle.Strikethrough)
            }

            chunks.add(ParagraphChunk.RichText(chunkText, inlineStyles = inlineStyles))
            currentIndex = it.end
        }
    }

    if (text.length > currentIndex) {
        chunks.add(ParagraphChunk.PlainText(text.substring(currentIndex, text.length)))
    }

    return chunks
}

internal fun StoreImageDimensions.toSyncImageDimensions(): SyncImageDimensions =
    SyncImageDimensions(height.toString(), width.toString())

internal fun StoreMedia.toSyncMedia(): SyncMedia? {
    return SyncMedia(
        id = remoteId ?: return null,
        createdWithLocalId = localId,
        mimeType = mimeType,
        altText = altText,
        imageDimensions = imageDimensions?.toSyncImageDimensions(),
        lastModified = parseMillisToISO8601String(lastModified)
    )
}

internal fun StoreInlineMedia.toSyncInlineMedia(): SyncInlineMedia =
    SyncInlineMedia(id = localId, source = remoteUrl, mimeType = mimeType, altText = altText)

internal fun StoreColor.toSyncColor(): SyncColor =
    when (this) {
        StoreColor.GREY -> SyncColor.GREY
        StoreColor.YELLOW -> SyncColor.YELLOW
        StoreColor.GREEN -> SyncColor.GREEN
        StoreColor.PINK -> SyncColor.PINK
        StoreColor.PURPLE -> SyncColor.PURPLE
        StoreColor.BLUE -> SyncColor.BLUE
        StoreColor.CHARCOAL -> SyncColor.CHARCOAL
    }

internal fun NoteMetadata.toSyncNoteMetadata(): RemoteNoteMetadata =
    RemoteNoteMetadata(context = context?.let { RemoteMetadataContext(host = it.host, hostIcon = it.hostIcon, displayName = it.displayName, url = it.url) })
