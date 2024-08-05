package com.microsoft.notes.sideeffect.persistence.mapper

import com.google.gson.Gson
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
import com.microsoft.notes.sideeffect.persistence.Note as PersistenceNote
import org.hamcrest.CoreMatchers.`is` as iz

class PersistenceToStoreMapperTest {

    private val NOTE_ID = "noteId"
    private val PARAGRAPH_ID = "paragraphId"
    private val STROKE_ID = "strokeId"
    private val MEDIA_ID = "mediaId"
    private val REMOTE_DATA_ID = "remoteId"
    private val CONTENT_ID = "contentId"
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
    fun should_parse_RemoteData() {
        val persistenceRemoteData = StoreRemoteData(
            id = REMOTE_DATA_ID,
            changeKey = CHANGE_KEY, lastServerVersion = StoreNote(REMOTE_DATA_ID),
            createdAt = CREATED_AT, lastModifiedAt = LAST_MODIFIED_AT
        ).toJson()

        val storeRemoteData = Gson().fromJson(persistenceRemoteData, StoreRemoteData::class.java)
        AssertUtils.assert_RemoteData(persistenceRemoteData, storeRemoteData)
    }

    @Test
    fun should_parse_Color() {
        val persistenceColor = 4
        val storeColor = persistenceColor.toStoreColor()

        assert_Color(storeColor, persistenceColor)
    }

    private fun assert_Color(storeColor: StoreColor, persistenceColor: Int) {
        assertThat(storeColor.value, iz(persistenceColor))
    }

    @Test
    fun should_parse_Note() {
        val tempNote = createDummyTextStoreNote(
            suffix = 2,
            numberOfParagraphs = 3,
            numberOfSpans = 1,
            numberOfMedia = 2
        )

        val persistenceNote = PersistenceNote(
            id = NOTE_ID,
            isDeleted = IS_DELETED,
            color = COLOR.toPersistenceColor(),
            localCreatedAt = LOCAL_CREATED_AT,
            documentModifiedAt = DOCUMENT_MODIFIED_AT,
            document = tempNote.document.toJson(),
            remoteData = tempNote.remoteData?.toJson(),
            media = tempNote.media.toJson(),
            pinnedAt = null
        )

        val storeNote = listOf(persistenceNote).toStoreNoteList().component1()

        assert_Note(persistenceNote = persistenceNote, storeNote = storeNote)
    }

    @Test
    fun should_parse_Ink_Note() {
        val tempNote = createDummyInkStoreNote(
            suffix = 2,
            numberOfStrokes = 3,
            numberOfPointsPerStroke = 2
        )

        val persistenceNote = PersistenceNote(
            id = NOTE_ID,
            isDeleted = IS_DELETED,
            color = COLOR.toPersistenceColor(),
            localCreatedAt = LOCAL_CREATED_AT,
            documentModifiedAt = DOCUMENT_MODIFIED_AT,
            document = tempNote.document.toJson(),
            remoteData = tempNote.remoteData?.toJson(),
            media = tempNote.media.toJson(),
            pinnedAt = null
        )

        val storeNote = listOf(persistenceNote).toStoreNoteList().component1()

        assert_Note(persistenceNote = persistenceNote, storeNote = storeNote)
    }

    @Test
    fun should_map_to_List_with_position() {
        val listOfItems = listOf("abcd", "abc", "ab", "a")
        val copyListOfItems = listOfItems.toMutableList()
        copyListOfItems.sortBy { it.length }

        with(copyListOfItems) {
            assertThat(size, iz(listOfItems.size))
            assertThat(component1(), iz(listOfItems.component4()))
            assertThat(component2(), iz(listOfItems.component3()))
            assertThat(component3(), iz(listOfItems.component2()))
            assertThat(component4(), iz(listOfItems.component1()))
        }
    }

    private fun createDummyTextStoreNote(
        suffix: Int,
        numberOfParagraphs: Int = 1,
        numberOfSpans: Int = 1,
        numberOfInlineMedia: Int = 1,
        numberOfMedia: Int = 1
    ): StoreNote {
        val storeParagraphStyle = StoreParagraphStyle(true)

        val listOfSpans = (0 until numberOfSpans).mapTo(
            destination = arrayListOf(),
            transform = {
                StoreSpan(StoreSpanStyle.BOLD, SPAN_START + suffix, SPAN_END + suffix, SPAN_FLAG + suffix)
            }
        )
        val storeContent = StoreContent(text = TEXT + suffix, spans = listOfSpans)

        val listOfParagraphs = (0 until numberOfParagraphs).mapIndexedTo(
            destination = arrayListOf(),
            transform = { index, _ ->
                StoreParagraph(
                    localId = PARAGRAPH_ID + index + suffix, style = storeParagraphStyle,
                    content = storeContent
                )
            }
        )

        val listOfInlineMedia = (0 until numberOfInlineMedia).mapIndexedTo(
            destination = arrayListOf(),
            transform = { index, _ ->
                StoreInlineMedia(localId = MEDIA_ID + index + suffix, localUrl = LOCAL_URL + index + suffix)
            }
        )

        val storeDocument = StoreDocument(listOfParagraphs + listOfInlineMedia)

        val listOfMedia = (0 until numberOfMedia).mapIndexedTo(
            destination = arrayListOf(),
            transform = { index, _ ->
                StoreMedia(
                    localId = MEDIA_ID + index + suffix,
                    remoteId = null,
                    localUrl = LOCAL_URL + index + suffix,
                    mimeType = MIME_TYPE + index + suffix,
                    altText = null,
                    imageDimensions = null,
                    lastModified = LAST_MODIFIED_AT
                )
            }
        )

        val storeRemoteData = StoreRemoteData(
            id = REMOTE_DATA_ID + suffix, changeKey = CHANGE_KEY + suffix,
            lastServerVersion = StoreNote(REMOTE_DATA_ID),
            createdAt = CREATED_AT, lastModifiedAt = LAST_MODIFIED_AT
        )

        return StoreNote(
            localId = NOTE_ID + suffix, remoteData = storeRemoteData,
            document = storeDocument, color = COLOR, isDeleted = IS_DELETED,
            localCreatedAt = LOCAL_CREATED_AT, documentModifiedAt = DOCUMENT_MODIFIED_AT,
            media = listOfMedia
        )
    }

    private fun createDummyInkStoreNote(
        suffix: Int = 1,
        numberOfStrokes: Int = 1,
        numberOfPointsPerStroke: Int = 2
    ): StoreNote {
        val listOfPoints = (0 until numberOfPointsPerStroke).mapIndexedTo(
            destination = arrayListOf(),
            transform = { index, _ ->
                StoreInkPoint(x = index.toDouble(), y = index.toDouble(), p = INK_PRESSURE)
            }
        )

        val listOfStrokes = (0 until numberOfStrokes).mapIndexedTo(
            destination = arrayListOf(),
            transform = { index, _ ->
                StoreStroke(id = STROKE_ID + index + suffix, points = listOfPoints)
            }
        )

        val storeDocument = StoreDocument(blocks = listOf(StoreParagraph()), strokes = listOfStrokes)

        val storeRemoteData = StoreRemoteData(
            id = REMOTE_DATA_ID, changeKey = CHANGE_KEY + suffix,
            lastServerVersion = StoreNote(REMOTE_DATA_ID),
            createdAt = CREATED_AT, lastModifiedAt = LAST_MODIFIED_AT
        )

        return StoreNote(
            localId = NOTE_ID, remoteData = storeRemoteData,
            document = storeDocument, color = COLOR, isDeleted = IS_DELETED,
            localCreatedAt = LOCAL_CREATED_AT, documentModifiedAt = DOCUMENT_MODIFIED_AT,
            media = emptyList()
        )
    }

    private fun assert_Note(persistenceNote: PersistenceNote, storeNote: StoreNote) {
        with(persistenceNote) {
            assertThat(id, iz(storeNote.localId))
            assertThat(isDeleted, iz(storeNote.isDeleted))
            assertThat(color, iz(storeNote.color.value))
            assertThat(localCreatedAt, iz(storeNote.localCreatedAt))
            assertThat(documentModifiedAt, iz(storeNote.documentModifiedAt))
            AssertUtils.assert_Media(media, storeNote.media)
            AssertUtils.assert_Document(document, storeNote.document)
            AssertUtils.assert_RemoteData(remoteData, storeNote.remoteData)
        }
    }
}
