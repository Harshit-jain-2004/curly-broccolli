package com.microsoft.notes.sideeffect.sync.mapper

import com.microsoft.notes.richtext.scheme.asParagraph
import com.microsoft.notes.richtext.scheme.isMedia
import com.microsoft.notes.richtext.scheme.isParagraph
import com.microsoft.notes.richtext.scheme.mediaList
import com.microsoft.notes.richtext.scheme.paragraphList
import com.microsoft.notes.richtext.scheme.size
import com.microsoft.notes.sync.models.InlineStyle
import com.microsoft.notes.sync.models.ParagraphChunk
import com.microsoft.notes.sync.models.RemoteMetadataContext
import com.microsoft.notes.sync.models.RemoteNote
import com.microsoft.notes.sync.models.RemoteNoteMetadata
import com.microsoft.notes.utils.utils.parseISO8601StringToMillis
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.richtext.scheme.Document as StoreDocument
import com.microsoft.notes.richtext.scheme.ParagraphStyle as StoreParagraphStyle
import com.microsoft.notes.sync.models.Block.InlineMedia as SyncMedia
import com.microsoft.notes.sync.models.Block.Paragraph as SyncParagraph
import com.microsoft.notes.sync.models.BlockStyle as SyncBlockStyle
import com.microsoft.notes.sync.models.Document.RichTextDocument as SyncRichTextDocument
import com.microsoft.notes.sync.models.localOnly.Note.Color as LocalOnlyColor
import org.hamcrest.CoreMatchers.`is` as iz

class SyncToStoreMapperTest {

    @Test
    fun should_parse_remote_Color_to_store_Color() {
        for (index in LocalOnlyColor.values().indices) {
            val remoteColor = LocalOnlyColor.values()[index]
            val storeColor = remoteColor.value.toStoreColor()
            assertThat(remoteColor.value, iz(storeColor.value))
        }
    }

    @Test
    fun should_parse_RemoteNote_correctly() {
        val plainTextChunk = ParagraphChunk.PlainText(string = "chunk1")
        val richTextChunk = ParagraphChunk.RichText(string = "chunk1", inlineStyles = listOf(InlineStyle.Bold))
        val remoteContent = listOf(plainTextChunk, richTextChunk)
        val remoteBlock = SyncParagraph(
            id = "block1", content = remoteContent,
            blockStyles = SyncBlockStyle(unorderedList = true)
        )
        val blocks = listOf(remoteBlock)
        val remoteDocument = SyncRichTextDocument(blocks)
        val remoteMetadata = RemoteNoteMetadata(
            context = RemoteMetadataContext(
                displayName = "testing module", host = "testing-module", hostIcon = null, url = null
            )
        )
        val remoteNote = RemoteNote(
            id = "id",
            changeKey = "blabla",
            document = remoteDocument,
            color = LocalOnlyColor.BLUE.value,
            createdWithLocalId = "123",
            createdAt = "2018-01-31T16:45:05.0000000Z",
            lastModifiedAt = "2018-01-31T16:50:31.0000000Z",
            createdByApp = "App",
            documentModifiedAt = "2018-02-01T16:50:31.0000000Z",
            // todo implement
            media = listOf(),
            metadata = remoteMetadata
        )
        val localNote = StoreNote(localId = "localNoteId")
        val storeNote = remoteNote.toStoreNote(localNote.localId)

        assertThat(storeNote.localId, iz(localNote.localId))
        with(storeNote.remoteData) {
            assertThat(this, iz(not(nullValue())))
            assertThat(this!!.id, iz(remoteNote.id))
            assertThat(changeKey, iz(remoteNote.changeKey))
            assertThat(
                createdAt,
                iz(
                    parseISO8601StringToMillis(remoteNote.createdAt)
                )
            )
            assertThat(
                lastModifiedAt,
                iz(
                    parseISO8601StringToMillis(remoteNote.lastModifiedAt)
                )
            )
        }
        assertThat(
            storeNote.documentModifiedAt,
            iz(
                parseISO8601StringToMillis(remoteNote.documentModifiedAt!!)
            )
        )
        assertThat(storeNote.createdByApp, iz(remoteNote.createdByApp))
        assertThat(storeNote.color, iz(remoteNote.color.toStoreColor()))
        assertThat(storeNote.isDocumentEmpty, iz(false))
        assertThat(storeNote.document.size(), iz((remoteNote.document as SyncRichTextDocument).blocks.size))

        val block = storeNote.document.blocks.component1()
        with(block) {
            assertThat(isParagraph(), iz(true))
            assertThat(localId, iz(not(nullValue())))
            val paragraph = asParagraph()
            with(paragraph) {
                assertThat(localId, iz(not(nullValue())))
                assertThat(style, iz(StoreParagraphStyle(true)))
                with(content) {
                    assertThat(text, iz(plainTextChunk.string + richTextChunk.string))
                    assertThat(spans.isNotEmpty(), iz(true))
                    assertThat(spans.size, iz(1))
                    with(spans.component1()) {
                        assertThat(style.bold, iz(true))
                        assertThat(style.italic, iz(false))
                        assertThat(style.underline, iz(false))
                        assertThat(start, iz(plainTextChunk.string.length))
                        assertThat(end, iz(plainTextChunk.string.length + richTextChunk.string.length))
                        assertThat(flag, iz(0))
                    }
                }
            }
        }

        val metadata = storeNote.metadata
        assertThat(metadata.context!!.displayName, iz(remoteNote.metadata.context?.displayName))
        assertThat(metadata.context!!.host, iz(remoteNote.metadata.context?.host))
    }

    @Test
    fun should_parse_remote_Inline_Media_correctly() {
        val remoteMedia = SyncMedia(
            id = "mediaId",
            source = "media/source",
            mimeType = "mime/type",
            altText = "alttext"
        )
        val storeMedia = remoteMedia.toStoreInlineMedia(localUrl = null)
        with(storeMedia) {
            assertThat(localId, iz(remoteMedia.id))
            assertThat(localUrl, iz(nullValue()))
            assertThat(remoteUrl, iz(remoteMedia.source))
            assertThat(altText, iz(remoteMedia.altText))
        }
    }

    @Test
    fun should_parse_remote_Paragraph_to_Store_Paragraph() {
        val plainTextChunk = ParagraphChunk.PlainText(string = "chunk1")
        val richTextChunk = ParagraphChunk.RichText(string = "chunk1", inlineStyles = listOf(InlineStyle.Bold))
        val remoteContent = listOf(plainTextChunk, richTextChunk)
        val remoteParagraph = SyncParagraph(
            id = "block1", content = remoteContent,
            blockStyles = SyncBlockStyle(unorderedList = true)
        )

        val storeParagraph = remoteParagraph.toStoreParagraph()
        with(storeParagraph) {
            assertThat(style.unorderedList, iz(true))
            assertThat(style.rightToLeft, iz(false))
            assertThat(localId, iz(remoteParagraph.id))
            with(content) {
                assertThat(text, iz(plainTextChunk.string + richTextChunk.string))
                assertThat(spans.isNotEmpty(), iz(true))
                assertThat(spans.size, iz(richTextChunk.inlineStyles.size))
                with(spans.component1()) {
                    assertThat(style.bold, iz(true))
                    assertThat(style.italic, iz(false))
                    assertThat(style.underline, iz(false))
                    assertThat(start, iz(plainTextChunk.string.length))
                    assertThat(end, iz(plainTextChunk.string.length + richTextChunk.string.length))
                    assertThat(flag, iz(0))
                }
            }
        }
    }

    @Test
    fun should_parse_remote_Document_to_Store_Document() {
        val plainTextChunk = ParagraphChunk.PlainText(string = "chunk1")
        val richTextChunk = ParagraphChunk.RichText(string = "chunk1", inlineStyles = listOf(InlineStyle.Bold))
        val remoteContent = listOf(plainTextChunk, richTextChunk)
        val remoteParagraph = SyncParagraph(
            id = "block1", content = remoteContent,
            blockStyles = SyncBlockStyle(unorderedList = true)
        )
        val remoteMedia = SyncMedia(
            id = "mediaId",
            source = "media/source",
            mimeType = "mime/type",
            altText = "alttext"
        )
        val blocks = listOf(remoteParagraph, remoteMedia)

        val remoteDocument = SyncRichTextDocument(blocks)
        val storeDocument = remoteDocument.toStoreDocument(StoreDocument())

        with(storeDocument) {
            assertThat(size(), iz(remoteDocument.blocks.size))
            assertThat(paragraphList().size, iz(1))
            assertThat(mediaList().size, iz(1))
            assertThat(storeDocument.blocks.component1().isParagraph(), iz(true))
            assertThat(storeDocument.blocks.component2().isMedia(), iz(true))
            assertThat(paragraphList().component1().isParagraph(), iz(true))

            with(paragraphList().component1()) {
                assertThat(style.unorderedList, iz(true))
                assertThat(style.rightToLeft, iz(false))
                assertThat(localId, iz(remoteParagraph.id))
                with(content) {
                    assertThat(text, iz(plainTextChunk.string + richTextChunk.string))
                    assertThat(spans.isNotEmpty(), iz(true))
                    assertThat(spans.size, iz(richTextChunk.inlineStyles.size))
                    with(spans.component1()) {
                        assertThat(style.bold, iz(true))
                        assertThat(style.italic, iz(false))
                        assertThat(style.underline, iz(false))
                        assertThat(start, iz(plainTextChunk.string.length))
                        assertThat(end, iz(plainTextChunk.string.length + richTextChunk.string.length))
                        assertThat(flag, iz(0))
                    }
                }
            }
            with(mediaList().component1()) {
                assertThat(localId, iz(remoteMedia.id))
                assertThat(localUrl, iz(nullValue()))
                assertThat(remoteUrl, iz(remoteMedia.source))
                assertThat(altText, iz(remoteMedia.altText))
            }
        }
    }
}
