package com.microsoft.notes.sideeffect.persistence.handler

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.RemoteData
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.DocumentType
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import com.microsoft.notes.richtext.scheme.asMedia
import com.microsoft.notes.richtext.scheme.asParagraph
import com.microsoft.notes.richtext.scheme.isParagraph
import com.microsoft.notes.richtext.scheme.size
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.sideeffect.persistence.createTestNotesDB
import com.microsoft.notes.sideeffect.persistence.extensions.toJson
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceColor
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.sideeffect.persistence.Note as PersistenceNote
import org.hamcrest.CoreMatchers.`is` as iz

@RunWith(AndroidJUnit4::class)
class ReadHandlerTest {

    private val NOTE_ID = "noteId"
    private val PARAGRAPH_ID = "paragraphId"
    private val MEDIA_ID = "mediaId"
    private val REMOTE_DATA_ID = "remoteId"
    private val CHANGE_KEY = "change_key"
    private val CREATED_AT: Long = 1517417105000
    private val LAST_MODIFIED_AT: Long = 1517417431000
    private val TEXT = "text"
    private val LOCAL_URL = "localUrl/"
    private val REMOTE_URL = "remoteUrl/"
    private val SPAN_START = 0
    private val SPAN_END = 5
    private val SPAN_FLAG = 10
    private val MIME_TYPE = "img/png"
    private val COLOR = Color.GREEN
    private val IS_DELETED = false

    private lateinit var testNotesDB: NotesDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getTargetContext()
        testNotesDB = createTestNotesDB(context)
    }

    @After
    fun clean() {
        testNotesDB.close()
    }

    @Test
    fun should_fetch_all_Notes() {
        val NUM_NOTES = 3
        // Create and insert dummy notes
        val lastServerNote = StoreNote(localId = REMOTE_DATA_ID)
        (0 until NUM_NOTES).forEach { index -> createAndInsertModelsIntoDB(index, lastServerNote) }

        ReadHandler.setDBPageSize(1)

        ReadHandler.fetchAllNotesFromDBIn2Steps(
            notesDB = testNotesDB
        ) { _, notesCollection, _, _ ->
            assertThat(notesCollection.size, anyOf(iz(NUM_NOTES), iz(0)))

            var suffix = NUM_NOTES - 1
            (0 until notesCollection.size).forEach { index ->
                val storeNote = notesCollection[index]

                // Assert the Note direct fields
                with(storeNote) {
                    assertThat(localId, iz(NOTE_ID + suffix))
                    assertThat(color, iz(COLOR))
                    assertThat(isDeleted, iz(IS_DELETED))
                    assertThat(localCreatedAt, iz(suffix.toLong()))
                    assertThat(documentModifiedAt, iz(suffix.toLong()))

                    // Assert the remote data
                    assertThat(remoteData, iz(not(nullValue())))
                    with(remoteData!!) {
                        assertThat(id, iz(REMOTE_DATA_ID + suffix))
                        assertThat(changeKey, iz(CHANGE_KEY + suffix))
                        assertThat(lastServerVersion, iz(lastServerNote))
                        assertThat(createdAt, iz(CREATED_AT))
                        assertThat(lastModifiedAt, iz(LAST_MODIFIED_AT))
                    }

                    // Assert the document and we go through each block to see if it keep the order we gave
                    assertThat(document.size(), iz(2))
                    with(document.blocks) {

                        // Element 1 should be a Paragraph
                        assertThat(component1().isParagraph(), iz(true))
                        val paragraph = component1().asParagraph()
                        with(paragraph) {
                            assertThat(localId, iz(PARAGRAPH_ID + suffix))
                            assertThat(style.unorderedList, iz(true))
                            assertThat(content.text, iz(TEXT + suffix))

                            // It should have 2 spans
                            assertThat(content.spans.size, iz(2))
                            with(content.spans.component1()) {
                                assertThat(style.bold, iz(true))
                                assertThat(style.italic, iz(false))
                                assertThat(style.underline, iz(false))
                                assertThat(style.strikethrough, iz(false))
                                assertThat(start, iz(SPAN_START + suffix))
                                assertThat(end, iz(SPAN_END + suffix))
                                assertThat(flag, iz(SPAN_FLAG + suffix))
                            }
                            with(content.spans.component2()) {
                                assertThat(style.bold, iz(true))
                                assertThat(style.italic, iz(false))
                                assertThat(style.underline, iz(false))
                                assertThat(style.strikethrough, iz(false))
                                assertThat(start, iz(SPAN_START + 2 + suffix))
                                assertThat(end, iz(SPAN_END + 2 + suffix))
                                assertThat(flag, iz(SPAN_FLAG + 2 + suffix))
                            }
                        }
                        // Now the last block should be Media
                        val media = component2().asMedia()
                        with(media) {
                            assertThat(localId, iz(MEDIA_ID + suffix))
                            assertThat(localUrl, iz(LOCAL_URL + suffix))
                            assertThat(remoteUrl, iz(REMOTE_URL + suffix))
                            assertThat(mimeType, iz(MIME_TYPE))
                        }
                    }
                }
                suffix--
            }
        }
    }

    @Test
    fun should_fetch_all_Notes_when_same_documentLastModified() {
        val NUM_NOTES = 3
        val DOCUMENT_MODIFIED_AT = 0L
        // Create and insert dummy notes
        val lastServerNote = StoreNote(localId = REMOTE_DATA_ID)
        (0 until NUM_NOTES).forEach { index ->
            createAndInsertModelsIntoDB(index, lastServerNote, DOCUMENT_MODIFIED_AT)
        }

        ReadHandler.setDBPageSize(1)

        ReadHandler.fetchAllNotesFromDBIn2Steps(
            notesDB = testNotesDB
        ) { _, notesCollection, _, _ ->
            assertThat(notesCollection.size, iz(NUM_NOTES))
            assertThat(notesCollection.map { it.localId }.distinct().size, iz(NUM_NOTES))

            (0 until notesCollection.size).forEach { index ->
                val storeNote = notesCollection[index]
                assertThat(storeNote.documentModifiedAt, iz(DOCUMENT_MODIFIED_AT))
            }
        }
    }

    @Test
    fun should_fetch_notes_in_steps() {
        val NUM_NOTES = 13
        val NUM_BATCH = 10
        // Create and insert dummy notes
        val lastServerNote = StoreNote(localId = REMOTE_DATA_ID)
        (0 until NUM_NOTES).forEach { index -> createAndInsertModelsIntoDB(index, lastServerNote) }

        ReadHandler.fetchAllNotesFromDBIn2Steps(
            notesDB = testNotesDB
        ) { _, notesCollection, _, _ ->
            assertThat(notesCollection.size, anyOf(iz(NUM_BATCH), iz(NUM_NOTES - NUM_BATCH)))
        }
    }

    private fun createAndInsertModelsIntoDB(
        suffix: Int,
        lastServerNote: StoreNote,
        documentModifiedAt: Long? = null
    ) {
        val noteId = NOTE_ID + suffix

        val remoteData = RemoteData(
            id = REMOTE_DATA_ID + suffix,
            changeKey = CHANGE_KEY + suffix,
            lastServerVersion = lastServerNote,
            createdAt = CREATED_AT,
            lastModifiedAt = LAST_MODIFIED_AT
        )
        val inlineMedia = InlineMedia(
            localId = MEDIA_ID + suffix,
            localUrl = LOCAL_URL + suffix,
            remoteUrl = REMOTE_URL + suffix,
            mimeType = MIME_TYPE
        )

        val media = listOf<Media>()

        val style = ParagraphStyle(true)
        val span1 = Span(
            style = SpanStyle.BOLD,
            start = SPAN_START + suffix,
            end = SPAN_END + suffix,
            flag = SPAN_FLAG + suffix
        )
        val span2 = Span(
            style = SpanStyle.BOLD,
            start = SPAN_START + 2 + suffix,
            end = SPAN_END + 2 + suffix,
            flag = SPAN_FLAG + 2 + suffix
        )
        val content = Content(
            text = TEXT + suffix,
            spans = listOf(span1, span2)
        )
        val block = Paragraph(
            localId = PARAGRAPH_ID + suffix,
            style = style,
            content = content
        )
        val document = Document(blocks = listOf(block, inlineMedia))
        val note = PersistenceNote(
            id = noteId,
            color = COLOR.toPersistenceColor(),
            isDeleted = IS_DELETED,
            localCreatedAt = suffix.toLong(),
            documentModifiedAt = documentModifiedAt ?: suffix.toLong(),
            document = document.toJson(),
            remoteData = remoteData.toJson(),
            media = media.toJson(),
            pinnedAt = null
        )

        with(testNotesDB) {
            noteDao().insert(note)
        }
    }

    @Test
    fun should_calculate_percentile_correctly_1st_set() {
        val notesCollection = arrayOf(2, 8, 6, 4).toIntArray()
        val percentile = ReadHandler.calculatePercentile(notesCollection)

        assertThat(percentile[25], iz(2))
        assertThat(percentile[50], iz(4))
        assertThat(percentile[75], iz(6))
        assertThat(percentile[100], iz(8))
    }

    @Test
    fun should_calculate_percentile_correctly_2nd_set() {
        val notesCollection = arrayOf(100, 10, 5, 23, 75, 80).toIntArray()
        val percentile = ReadHandler.calculatePercentile(notesCollection)

        assertThat(percentile[25], iz(10))
        assertThat(percentile[50], iz(23))
        assertThat(percentile[75], iz(75))
        assertThat(percentile[100], iz(100))
    }

    @Test
    fun should_calculate_percentile_correctly_given_a_notes_collection() {
        val note1_with_0_paragraphs = StoreNote(document = Document())
        val note1_with_1_paragraphs = StoreNote(
            document = Document(blocks = listOf(Paragraph()))
        )
        val note2_with_1_paragraphs = StoreNote(
            document = Document(blocks = listOf(Paragraph()))
        )
        val note1_with_2_paragraphs = StoreNote(
            document = Document(blocks = listOf(Paragraph(), Paragraph()))
        )
        val note2_with_2_paragraphs = StoreNote(
            document = Document(blocks = listOf(Paragraph(), Paragraph()))
        )
        val note1_with_3_paragraphs = StoreNote(
            document = Document(blocks = listOf(Paragraph(), Paragraph(), Paragraph()))
        )
        val note2_with_3_paragraphs = StoreNote(
            document = Document(blocks = listOf(Paragraph(), Paragraph(), Paragraph()))
        )

        val notesCollection = listOf(
            note1_with_0_paragraphs,
            note1_with_1_paragraphs, note2_with_1_paragraphs,
            note1_with_2_paragraphs, note2_with_2_paragraphs,
            note1_with_3_paragraphs, note2_with_3_paragraphs
        )

        ReadHandler.calculateTelemetryAttributes(notesCollection) { _, _, percentileMap, _ ->
            assertThat(percentileMap[25], iz(1))
            assertThat(percentileMap[50], iz(2))
            assertThat(percentileMap[75], iz(2))
            assertThat(percentileMap[100], iz(3))
        }
    }

    @Test
    fun should_group_notes_by_type_correctly() {
        val richTextNote0 = StoreNote(document = Document())
        val richTextNote1 = StoreNote(
            document = Document(
                blocks = listOf(Paragraph(content = Content("abc"))),
                type = DocumentType.RICH_TEXT
            )
        )
        val richTextNote2 = StoreNote(
            document = Document(
                blocks = listOf(Paragraph(content = Content("abc"))),
                type = DocumentType.RICH_TEXT
            )
        )
        val richTextNote3 = StoreNote(
            document = Document(
                blocks = listOf(Paragraph(content = Content("abc"))),
                type = DocumentType.RICH_TEXT
            )
        )

        val inkNote1 = StoreNote(
            document = Document(
                blocks = listOf(InlineMedia(localUrl = "abc")),
                type = DocumentType.RENDERED_INK
            )
        )
        val inkNote2 = StoreNote(
            document = Document(
                blocks = listOf(InlineMedia(localUrl = "abc")),
                type = DocumentType.RENDERED_INK
            )
        )

        val futureNote1 = StoreNote(
            document = Document(
                blocks = listOf(Paragraph(content = Content("abc"))),
                type = DocumentType.FUTURE
            )
        )

        val imageNote = StoreNote(
            document = Document(),
            media = listOf(Media("123", "", null, null))
        )

        val textWithImageNote1 = StoreNote(
            document = Document(
                blocks = listOf(Paragraph(content = Content("abc"))),
                type = DocumentType.RICH_TEXT
            ),
            media = listOf(
                Media("123", "", null, null),
                Media("123", "", null, null),
                Media("123", "", null, null)
            )
        )
        val textWithImageNote2 = StoreNote(
            document = Document(
                blocks = listOf(Paragraph(content = Content("abc"))),
                type = DocumentType.RICH_TEXT
            ),
            media = listOf(
                Media("123", "", null, null),
                Media("123", "", null, null)
            )
        )

        val notesCollection = arrayListOf(
            richTextNote0, richTextNote1, richTextNote2, richTextNote3,
            inkNote1, inkNote2, imageNote, textWithImageNote1, textWithImageNote2, futureNote1
        )

        ReadHandler.calculateTelemetryAttributes(notesCollection) { _, noteTypeMap, _, _ ->
            assertThat(noteTypeMap["Empty"], iz(1))
            assertThat(noteTypeMap["Text"], iz(3))
            assertThat(noteTypeMap["Image"], iz(1))
            assertThat(noteTypeMap["TextWithImage"], iz(2))
            assertThat(noteTypeMap["Ink"], iz(2))
            assertThat(noteTypeMap["Future"], iz(1))
        }
    }

    @Test
    fun should_group_notes_by_color_correctly() {
        val noteBlue1 = StoreNote(color = Color.BLUE)
        val noteBlue2 = StoreNote(color = Color.BLUE)
        val noteCharcoal1 = StoreNote(color = Color.CHARCOAL)
        val noteCharcoal2 = StoreNote(color = Color.CHARCOAL)
        val noteCharcoal3 = StoreNote(color = Color.CHARCOAL)
        val noteGreen = StoreNote(color = Color.GREEN)
        val notePurple = StoreNote(color = Color.PURPLE)
        val noteYellow1 = StoreNote(color = Color.YELLOW)
        val noteYellow2 = StoreNote(color = Color.YELLOW)
        val noteGrey = StoreNote(color = Color.GREY)

        val notesCollection = arrayListOf(
            noteBlue1, noteBlue2,
            noteCharcoal1, noteCharcoal2, noteCharcoal3,
            noteGreen,
            notePurple,
            noteYellow1, noteYellow2,
            noteGrey
        )

        ReadHandler.calculateTelemetryAttributes(notesCollection) { colorMap, _, _, _ ->
            assertThat(colorMap["Blue"], iz(2))
            assertThat(colorMap["Charcoal"], iz(3))
            assertThat(colorMap["Green"], iz(1))
            assertThat(colorMap["Pink"], iz(0))
            assertThat(colorMap["Purple"], iz(1))
            assertThat(colorMap["Yellow"], iz(2))
            assertThat(colorMap["Grey"], iz(1))
        }
    }

    @Test
    fun should_handle_empty_list() {
        val notesCollection = emptyList<StoreNote>()

        ReadHandler.calculateTelemetryAttributes(notesCollection) { colorMap, noteTypeMap, paragraphPercentileMap, imagePercentileMap ->
            assertThat(colorMap["Blue"], iz(0))
            assertThat(colorMap["Charcoal"], iz(0))
            assertThat(colorMap["Green"], iz(0))
            assertThat(colorMap["Pink"], iz(0))
            assertThat(colorMap["Purple"], iz(0))
            assertThat(colorMap["Yellow"], iz(0))
            assertThat(colorMap["Grey"], iz(0))

            assertThat(noteTypeMap["Empty"], iz(0))
            assertThat(noteTypeMap["Text"], iz(0))
            assertThat(noteTypeMap["Image"], iz(0))
            assertThat(noteTypeMap["TextWithImage"], iz(0))
            assertThat(noteTypeMap["Ink"], iz(0))
            assertThat(noteTypeMap["Future"], iz(0))

            assertThat(paragraphPercentileMap[25], iz(0))
            assertThat(paragraphPercentileMap[50], iz(0))
            assertThat(paragraphPercentileMap[75], iz(0))
            assertThat(paragraphPercentileMap[100], iz(0))

            assertThat(imagePercentileMap[25], iz(0))
            assertThat(imagePercentileMap[50], iz(0))
            assertThat(imagePercentileMap[75], iz(0))
            assertThat(imagePercentileMap[100], iz(0))
        }
    }

    @Test
    fun should_handle_empty_note() {
        val emptyNote = StoreNote()
        val notesCollection = arrayListOf(emptyNote)

        ReadHandler.calculateTelemetryAttributes(notesCollection) { colorMap, noteTypeMap, paragraphPercentileMap, imagePercentileMap ->
            assertThat(colorMap["Blue"], iz(0))
            assertThat(colorMap["Charcoal"], iz(0))
            assertThat(colorMap["Green"], iz(0))
            assertThat(colorMap["Pink"], iz(0))
            assertThat(colorMap["Purple"], iz(0))
            assertThat(colorMap["Yellow"], iz(1))
            assertThat(colorMap["Grey"], iz(0))

            assertThat(noteTypeMap["Empty"], iz(1))
            assertThat(noteTypeMap["Text"], iz(0))
            assertThat(noteTypeMap["Image"], iz(0))
            assertThat(noteTypeMap["TextWithImage"], iz(0))
            assertThat(noteTypeMap["Ink"], iz(0))
            assertThat(noteTypeMap["Future"], iz(0))

            assertThat(paragraphPercentileMap[25], iz(0))
            assertThat(paragraphPercentileMap[50], iz(0))
            assertThat(paragraphPercentileMap[75], iz(0))
            assertThat(paragraphPercentileMap[100], iz(0))

            assertThat(imagePercentileMap[25], iz(0))
            assertThat(imagePercentileMap[50], iz(0))
            assertThat(imagePercentileMap[75], iz(0))
            assertThat(imagePercentileMap[100], iz(0))
        }
    }
}
