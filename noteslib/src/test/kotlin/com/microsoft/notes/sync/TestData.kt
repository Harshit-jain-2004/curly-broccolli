package com.microsoft.notes.sync

import com.microsoft.notes.sync.models.Block.InlineMedia
import com.microsoft.notes.sync.models.Block.Paragraph
import com.microsoft.notes.sync.models.BlockStyle
import com.microsoft.notes.sync.models.Document.RichTextDocument
import com.microsoft.notes.sync.models.InlineStyle
import com.microsoft.notes.sync.models.ParagraphChunk
import com.microsoft.notes.sync.models.RemoteMetadataContext
import com.microsoft.notes.sync.models.RemoteMetadataContext.Companion.toJSON
import com.microsoft.notes.sync.models.RemoteNote
import com.microsoft.notes.sync.models.RemoteNoteMetadata

fun remoteRichTextNote(id: String): RemoteNote {
    return RemoteNote(
        id = id,
        changeKey = "change-key",
        document = RichTextDocument(
            listOf(
                testParagraphBlock("remoteParagraphBlockId"),
                testMediaBlock("remoteMediaBlockId")
            )
        ),
        color = 0,
        createdWithLocalId = "localId",
        createdAt = "2018-01-31T16:45:05.0000000Z",
        lastModifiedAt = "2018-01-31T16:50:31.0000000Z",
        createdByApp = "Test",
        documentModifiedAt = "2018-01-31T16:50:31.0000000Z",
        // todo implement
        media = listOf(),
        metadata = testRemoteNoteMetadata()
    )
}

fun remoteRichTextNoteJSON(id: String): JSON.JObject {
    return JSON.JObject(
        hashMapOf(
            "id" to JSON.JString(id),
            "changeKey" to JSON.JString("change-key"),
            "document" to JSON.JObject(
                hashMapOf(
                    "type" to JSON.JString("document"),
                    "blocks" to JSON.JArray(
                        listOf(
                            testParagraphBlock("remoteParagraphBlockId").toJSON(),
                            testMediaBlock("remoteMediaBlockId").toJSON()
                        )
                    )
                )
            ),
            "color" to JSON.JNumber(0.0),
            "createdWithLocalId" to JSON.JString("localId"),
            "createdAt" to JSON.JString("2018-01-31T16:45:05.0000000Z"),
            "lastModified" to JSON.JString("2018-01-31T16:50:31.0000000Z"),
            "createdByApp" to JSON.JString("Test"),
            "documentModifiedAt" to JSON.JString("2018-01-31T16:50:31.0000000Z"),
            "media" to JSON.JArray(listOf()),
            "metadata" to JSON.JObject(
                hashMapOf("context" to toJSON(testRemoteNoteMetadata().context))
            )
        )
    )
}

fun richTextDocument(): RichTextDocument {
    return RichTextDocument(
        listOf(
            testParagraphBlock("testParagraphBlockId"),
            testMediaBlock("testMediaBlockId")
        )
    )
}

fun testParagraphBlock(id: String): Paragraph {
    return Paragraph(
        id = id,
        content = listOf(
            ParagraphChunk.PlainText(string = "plainText"),
            ParagraphChunk.RichText(
                string = "richTextWithStyle1",
                inlineStyles = listOf(
                    InlineStyle.Bold, InlineStyle.Underlined,
                    InlineStyle.Strikethrough
                )
            ),
            ParagraphChunk.RichText(
                string = "richTextWithoutInlineStyle",
                inlineStyles = listOf()
            ),
            ParagraphChunk.RichText(
                string = "richTextWithInlineStyle2",
                inlineStyles = listOf(InlineStyle.Italic)
            )
        ),
        blockStyles = BlockStyle(unorderedList = true)
    )
}

fun testMediaBlock(id: String): InlineMedia {
    return InlineMedia(
        id = id,
        source = "/media/testMediaRemoteUrl",
        mimeType = "image/png",
        altText = "alttext"
    )
}

fun testRemoteNoteMetadata(): RemoteNoteMetadata = RemoteNoteMetadata(
    context = RemoteMetadataContext(
        displayName = "Context",
        host = "module-testing", url = "https:\\test.sampleapp", hostIcon = null
    )
)
