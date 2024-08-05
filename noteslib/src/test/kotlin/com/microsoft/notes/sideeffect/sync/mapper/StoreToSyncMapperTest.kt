package com.microsoft.notes.sideeffect.sync.mapper

import com.microsoft.notes.richtext.scheme.DocumentType
import com.microsoft.notes.richtext.scheme.size
import com.microsoft.notes.sync.models.Document
import com.microsoft.notes.sync.models.InlineStyle
import com.microsoft.notes.sync.models.ParagraphChunk
import com.microsoft.notes.utils.utils.parseMillisToISO8601String
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import com.microsoft.notes.models.Color as StoreColor
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.models.RemoteData as StoreRemoteData
import com.microsoft.notes.richtext.scheme.Content as StoreContent
import com.microsoft.notes.richtext.scheme.Document as StoreDocument
import com.microsoft.notes.richtext.scheme.InlineMedia as StoreInlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph as StoreParagraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle as StoreParagraphStyle
import com.microsoft.notes.richtext.scheme.Span as StoreSpan
import com.microsoft.notes.richtext.scheme.SpanStyle as StoreSpanStyle
import com.microsoft.notes.sync.models.Block.InlineMedia as SyncMedia
import com.microsoft.notes.sync.models.Block.Paragraph as SyncParagraph
import org.hamcrest.CoreMatchers.`is` as iz

class StoreToSyncMapperTest {

    @Test
    fun should_parse_Store_Color_to_Sync_Color() {
        for (index in StoreColor.values().indices) {
            val storeColor = StoreColor.values()[index]
            val syncColor = storeColor.toSyncColor()
            assertThat(syncColor.value, iz(storeColor.value))
        }
    }

    @Test
    fun should_parse_Store_Inline_Media_to_Sync_Inline_Media() {
        val storeMedia = StoreInlineMedia(
            localId = "localId", localUrl = "localUrl",
            remoteUrl = "remoteUrl", mimeType = "mimeType"
        )
        val syncMedia = storeMedia.toSyncInlineMedia()
        with(syncMedia) {
            assertThat(id, iz(storeMedia.localId))
            assertThat(source, iz(storeMedia.remoteUrl))
            assertThat(mimeType, iz(storeMedia.mimeType))
        }
    }

    @Test
    fun should_parse_Store_Content_to_Sync_Content() {
        val storeSpanStyle = StoreSpanStyle(bold = true)
        val storeSpan = StoreSpan(style = storeSpanStyle, start = 0, end = 1, flag = 0)
        val storeContent = StoreContent(text = "text", spans = listOf(storeSpan))

        val syncParagraphChunk = storeContent.toSyncParagraphContent()
        assertThat(syncParagraphChunk.size, iz(2))
        with(syncParagraphChunk.component1()) {
            assertThat(this is ParagraphChunk.RichText, iz(true))
            val richText = this as ParagraphChunk.RichText
            assertThat(richText, iz(not(nullValue())))
            assertThat(richText.string, iz(storeContent.text.substring(storeSpan.start, storeSpan.end)))
            assertThat(richText.inlineStyles.size, iz(1))
            assertThat(richText.inlineStyles.component1(), iz(InlineStyle.Bold))
        }
        with(syncParagraphChunk.component2()) {
            assertThat(this is ParagraphChunk.PlainText, iz(true))
            val plainText = this as ParagraphChunk.PlainText
            assertThat(plainText, iz(not(nullValue())))
            assertThat(plainText.string, iz(storeContent.text.substring(storeSpan.end, storeContent.text.length)))
        }
    }

    @Test
    fun should_parse_Store_Paragraph_to_Sync_Paragraph() {
        val storeSpanStyle = StoreSpanStyle(bold = true)
        val storeSpan = StoreSpan(style = storeSpanStyle, start = 0, end = 1, flag = 0)
        val storeContent = StoreContent(text = "text", spans = listOf(storeSpan))
        val storeParagraphStyle = StoreParagraphStyle(unorderedList = true, rightToLeft = false)
        val storeParagraph = StoreParagraph(
            localId = "localId", content = storeContent,
            style = storeParagraphStyle
        )

        val syncParagraph = storeParagraph.toSyncParagraph()
        with(syncParagraph) {
            assertThat(id, iz(storeParagraph.localId))
            assertThat(blockStyles.unorderedList, iz(true))
            assertThat(blockStyles.rightToLeft, iz(false))
            assertThat(syncParagraph.content.size, iz(2))
            with(syncParagraph.content.component1()) {
                assertThat(this is ParagraphChunk.RichText, iz(true))
                val richText = this as ParagraphChunk.RichText
                assertThat(richText, iz(not(nullValue())))
                assertThat(richText.string, iz(storeContent.text.substring(storeSpan.start, storeSpan.end)))
                assertThat(richText.inlineStyles.size, iz(1))
                assertThat(richText.inlineStyles.component1(), iz(InlineStyle.Bold))
            }
            with(syncParagraph.content.component2()) {
                assertThat(this is ParagraphChunk.PlainText, iz(true))
                val plainText = this as ParagraphChunk.PlainText
                assertThat(plainText, iz(not(nullValue())))
                assertThat(
                    plainText.string,
                    iz(storeContent.text.substring(storeSpan.end, storeContent.text.length))
                )
            }
        }
    }

    @Test
    fun should_parse_Store_Document_to_Sync_RichText_Document() {
        val storeSpanStyle = StoreSpanStyle(bold = true)
        val storeSpan = StoreSpan(style = storeSpanStyle, start = 0, end = 1, flag = 0)
        val storeContent = StoreContent(text = "text", spans = listOf(storeSpan))
        val storeParagraphStyle = StoreParagraphStyle(unorderedList = true, rightToLeft = true)
        val storeParagraph = StoreParagraph(
            localId = "localId", content = storeContent,
            style = storeParagraphStyle
        )
        val storeMedia = StoreInlineMedia(
            localId = "localId", localUrl = "localUrl",
            remoteUrl = "remoteUrl", mimeType = "mimeType"
        )

        val storeDocument = StoreDocument(
            blocks = listOf(storeParagraph, storeMedia),
            type = DocumentType.RICH_TEXT
        )
        val syncDocument = storeDocument.toSyncDocument() as Document.RichTextDocument

        assertThat(syncDocument.blocks.size, iz(storeDocument.size()))
        assertThat(syncDocument.blocks.component1() is SyncParagraph, iz(true))

        val syncParagraph = syncDocument.blocks.component1() as SyncParagraph
        with(syncParagraph) {
            assertThat(id, iz(storeParagraph.localId))
            assertThat(blockStyles.unorderedList, iz(true))
            assertThat(blockStyles.rightToLeft, iz(true))
            assertThat(syncParagraph.content.size, iz(2))
            with(syncParagraph.content.component1()) {
                assertThat(this is ParagraphChunk.RichText, iz(true))
                val richText = this as ParagraphChunk.RichText
                assertThat(richText, iz(not(nullValue())))
                assertThat(richText.string, iz(storeContent.text.substring(storeSpan.start, storeSpan.end)))
                assertThat(richText.inlineStyles.size, iz(1))
                assertThat(richText.inlineStyles.component1(), iz(InlineStyle.Bold))
            }
            with(syncParagraph.content.component2()) {
                assertThat(this is ParagraphChunk.PlainText, iz(true))
                val plainText = this as ParagraphChunk.PlainText
                assertThat(plainText, iz(not(nullValue())))
                assertThat(
                    plainText.string,
                    iz(storeContent.text.substring(storeSpan.end, storeContent.text.length))
                )
            }
        }
        assertThat(syncDocument.blocks.component2() is SyncMedia, iz(true))
        val syncMedia = syncDocument.blocks.component2() as SyncMedia
        with(syncMedia) {
            assertThat(id, iz(storeMedia.localId))
            assertThat(source, iz(storeMedia.remoteUrl))
            assertThat(mimeType, iz(storeMedia.mimeType))
        }
    }

    @Test
    fun should_parse_Store_RemoteData_to_Sync_RemoteData() {
        val lastServerNote = StoreNote(localId = "id")
        val storeRemoteData = StoreRemoteData(
            id = "id", changeKey = "changeKey",
            lastServerVersion = lastServerNote, createdAt = "2018-01-31T16:45:05.0000000Z",
            lastModifiedAt = "2018-01-31T16:50:31.0000000Z"
        )
        val remoteNote = lastServerNote.toRemoteNote()
        val syncRemoteData = storeRemoteData.toSyncRemoteData()

        with(syncRemoteData) {
            assertThat(id, iz(storeRemoteData.id))
            assertThat(changeKey, iz(storeRemoteData.changeKey))
            assertThat(lastServerVersion, iz(remoteNote))
            assertThat(
                createdAt,
                iz(
                    parseMillisToISO8601String(storeRemoteData.createdAt)
                )
            )
            assertThat(
                lastModifiedAt,
                iz(
                    parseMillisToISO8601String(storeRemoteData.lastModifiedAt)
                )
            )
        }
    }

    @Test
    fun should_parse_Store_Note_to_Sync_Note() {
        val lastServerNote = StoreNote(localId = "id")
        val storeRemoteData = StoreRemoteData(
            id = "id", changeKey = "changeKey",
            lastServerVersion = lastServerNote, createdAt = "2018-01-31T16:45:05.0000000Z",
            lastModifiedAt = "2018-01-31T16:50:31.0000000Z"
        )
        val storeNote = StoreNote(
            localId = "localId",
            color = StoreColor.BLUE, document = StoreDocument(),
            remoteData = storeRemoteData
        )
        val remoteNote = lastServerNote.toRemoteNote()

        val syncNote = storeNote.toSyncNote()
        with(syncNote) {
            assertThat(id, iz(storeNote.localId))
            assertThat(color, iz(storeNote.color.toSyncColor()))
            assertThat(document.type, iz(Document.DocumentType.RICH_TEXT))
            assertThat((document as Document.RichTextDocument).blocks.isEmpty(), iz(true))
            assertThat(remoteData, iz(not(nullValue())))
            with(remoteData!!) {
                assertThat(id, iz(storeRemoteData.id))
                assertThat(changeKey, iz(storeRemoteData.changeKey))
                assertThat(lastServerVersion, iz(remoteNote))
                assertThat(
                    createdAt,
                    iz(
                        parseMillisToISO8601String(storeRemoteData.createdAt)
                    )
                )
                assertThat(
                    lastModifiedAt,
                    iz(
                        parseMillisToISO8601String(storeRemoteData.lastModifiedAt)
                    )
                )
            }
        }
    }
}
