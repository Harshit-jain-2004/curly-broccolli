package com.microsoft.notes.sideeffect.persistence.mapper

import com.microsoft.notes.sideeffect.persistence.extensions.toJson
import org.junit.Assert.assertThat
import org.junit.Test
import com.microsoft.notes.models.Color as StoreColor
import com.microsoft.notes.models.Media as StoreMedia
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.models.RemoteData as StoreRemoteData
import com.microsoft.notes.richtext.scheme.Content as StoreContent
import com.microsoft.notes.richtext.scheme.Document as StoreDocument
import com.microsoft.notes.richtext.scheme.InkPoint as StoreInkPoint
import com.microsoft.notes.richtext.scheme.InlineMedia as StoreInlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph as StoreParagraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle as StoreParagraphStyle
import com.microsoft.notes.richtext.scheme.Span as StoreSpan
import com.microsoft.notes.richtext.scheme.SpanStyle as StoreSpanStyle
import com.microsoft.notes.richtext.scheme.Stroke as StoreStroke
import org.hamcrest.CoreMatchers.`is` as iz

class StoreToPersistenceMapperTest {

    private val NOTE_ID = "noteId"
    private val PARAGRAPH_ID = "paragraphId"
    private val STROKE_ID = "strokeId"
    private val MEDIA_ID = "mediaId"
    private val REMOTE_DATA_ID = "remoteId"
    private val CHANGE_KEY = "change_key"
    private val CREATED_AT: Long = 1517417105000
    private val LAST_MODIFIED_AT: Long = 1517417431000
    private val LOCAL_CREATED_AT: Long = 1517417105000
    private val DOCUMENT_MODIFIED_AT: Long = 1517417431000
    private val TEXT = "text"
    private val LOCAL_URL = "localUrl/"
    private val SPAN_START = 0
    private val SPAN_END = 5
    private val SPAN_FLAG = 10
    private val MIME_TYPE = "img/png"
    private val COLOR = StoreColor.GREEN
    private val IS_DELETED = false
    private val INK_PRESSURE = 0.4

    @Test
    fun should_parse_Document_with_InlineMedia_Block() {
        val storeMedia = StoreInlineMedia(localId = MEDIA_ID, localUrl = LOCAL_URL, mimeType = MIME_TYPE)
        val storeDocument = StoreDocument(listOf(storeMedia))
        val persistenceDocument = storeDocument.toJson()

        AssertUtils.assert_Document(persistenceDocument = persistenceDocument, storeDocument = storeDocument)
    }

    @Test
    fun should_parse_Document_with_Paragraph_Block() {
        val storeParagraphStyle = StoreParagraphStyle(true)
        val storeSpan1 = StoreSpan(StoreSpanStyle.BOLD, SPAN_START, SPAN_END, SPAN_FLAG)
        val storeSpan2 = StoreSpan(StoreSpanStyle.ITALIC, SPAN_START, SPAN_END, SPAN_FLAG)
        val storeContent = StoreContent(text = TEXT, spans = listOf(storeSpan1, storeSpan2))

        val storeParagraph = StoreParagraph(
            localId = PARAGRAPH_ID, style = storeParagraphStyle,
            content = storeContent
        )
        val storeDocument = StoreDocument(listOf(storeParagraph))

        val persistenceDocument = storeDocument.toJson()

        AssertUtils.assert_Document(persistenceDocument = persistenceDocument, storeDocument = storeDocument)
    }

    @Test
    fun should_parse_Color() {
        val storeColor = StoreColor.BLUE
        val persistenceColor = storeColor.toPersistenceColor()

        assertThat(persistenceColor, iz(StoreColor.BLUE.value))
    }

    @Test
    fun should_parse_RemoteData() {
        val storeRemoteData = StoreRemoteData(
            id = REMOTE_DATA_ID, changeKey = CHANGE_KEY,
            lastServerVersion = StoreNote(), createdAt = CREATED_AT, lastModifiedAt = LAST_MODIFIED_AT
        )
        val persistenceRemoteData = storeRemoteData.toJson()

        AssertUtils.assert_RemoteData(persistenceRemoteData = persistenceRemoteData, remoteData = storeRemoteData)
    }

    @Test
    fun should_parse_Note_with_Media() {
        val storeMedia = StoreMedia(
            localId = MEDIA_ID,
            remoteId = null,
            localUrl = LOCAL_URL,
            mimeType = MIME_TYPE,
            altText = null,
            imageDimensions = null,
            lastModified = System.currentTimeMillis()
        )
        val storeDocument = StoreDocument()

        val storeRemoteData = StoreRemoteData(
            id = REMOTE_DATA_ID,
            changeKey = CHANGE_KEY,
            lastServerVersion = StoreNote(localId = REMOTE_DATA_ID),
            createdAt = CREATED_AT,
            lastModifiedAt = LAST_MODIFIED_AT
        )

        val storeNote = StoreNote(
            localId = NOTE_ID,
            remoteData = storeRemoteData,
            document = storeDocument,
            media = listOf(storeMedia),
            color = COLOR,
            isDeleted = IS_DELETED,
            localCreatedAt = LOCAL_CREATED_AT,
            documentModifiedAt = DOCUMENT_MODIFIED_AT
        )

        val persistenceNote = storeNote.toPersistenceNote()
        AssertUtils.assert_Note(persistenceNote = persistenceNote, storeNote = storeNote)
    }

    @Test
    fun should_parse_Note() {
        val storeParagraphStyle = StoreParagraphStyle(true)
        val storeSpan1 = StoreSpan(StoreSpanStyle.BOLD, SPAN_START, SPAN_END, SPAN_FLAG)
        val storeSpan2 = StoreSpan(StoreSpanStyle.ITALIC, SPAN_START, SPAN_END, SPAN_FLAG)
        val storeContent = StoreContent(text = TEXT, spans = listOf(storeSpan1, storeSpan2))
        val storeParagraph = StoreParagraph(
            localId = PARAGRAPH_ID, style = storeParagraphStyle,
            content = storeContent
        )

        val storeMedia = StoreInlineMedia(localId = MEDIA_ID, localUrl = LOCAL_URL, mimeType = MIME_TYPE)

        val storeDocument = StoreDocument(listOf(storeParagraph, storeMedia))

        val storeRemoteData = StoreRemoteData(
            id = REMOTE_DATA_ID, changeKey = CHANGE_KEY,
            lastServerVersion = StoreNote(localId = REMOTE_DATA_ID),
            createdAt = CREATED_AT,
            lastModifiedAt = LAST_MODIFIED_AT
        )

        val storeNote = StoreNote(
            localId = NOTE_ID, remoteData = storeRemoteData,
            document = storeDocument, color = COLOR, isDeleted = IS_DELETED,
            localCreatedAt = LOCAL_CREATED_AT, documentModifiedAt = DOCUMENT_MODIFIED_AT
        )

        val persistenceNote = storeNote.toPersistenceNote()
        AssertUtils.assert_Note(persistenceNote = persistenceNote, storeNote = storeNote)
    }

    @Test
    fun should_parse_Ink_Note() {
        val storeInkPoint1 = StoreInkPoint(x = 0.0, y = 0.0, p = INK_PRESSURE)
        val storeInkPoint2 = StoreInkPoint(x = 1.0, y = 1.0, p = INK_PRESSURE)
        val storePoints = listOf(storeInkPoint1, storeInkPoint2)
        val storeStroke1 = StoreStroke(id = STROKE_ID + "1", points = storePoints)
        val storeStroke2 = StoreStroke(id = STROKE_ID + "2", points = storePoints)

        val storeDocument = StoreDocument(
            blocks = listOf(StoreParagraph()),
            strokes = listOf(storeStroke1, storeStroke2)
        )

        val storeRemoteData = StoreRemoteData(
            id = REMOTE_DATA_ID, changeKey = CHANGE_KEY,
            lastServerVersion = StoreNote(localId = REMOTE_DATA_ID),
            createdAt = CREATED_AT,
            lastModifiedAt = LAST_MODIFIED_AT
        )

        val storeNote = StoreNote(
            localId = NOTE_ID, remoteData = storeRemoteData,
            document = storeDocument, color = COLOR, isDeleted = IS_DELETED,
            localCreatedAt = LOCAL_CREATED_AT, documentModifiedAt = DOCUMENT_MODIFIED_AT
        )

        val persistenceNote = storeNote.toPersistenceNote()
        AssertUtils.assert_Note(persistenceNote = persistenceNote, storeNote = storeNote)
    }
}
