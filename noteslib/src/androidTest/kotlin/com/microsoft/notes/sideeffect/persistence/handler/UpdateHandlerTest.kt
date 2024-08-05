package com.microsoft.notes.sideeffect.persistence.handler

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.extensions.justParagraph
import com.microsoft.notes.sideeffect.persistence.AssertUtils
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.sideeffect.persistence.createTestNotesDB
import com.microsoft.notes.sideeffect.persistence.extensions.fromDocumentJson
import com.microsoft.notes.sideeffect.persistence.extensions.fromMediaJson
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceColor
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceNote
import com.microsoft.notes.store.action.CreationAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithColorAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithDocumentAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithRemovedMediaAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithUpdateMediaAltTextAction
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.microsoft.notes.models.Color as StoreColor
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.richtext.scheme.Content as StoreContent
import com.microsoft.notes.richtext.scheme.Document as StoreDocument
import com.microsoft.notes.richtext.scheme.InlineMedia as StoreMedia
import com.microsoft.notes.richtext.scheme.Paragraph as StoreParagraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle as StoreParagraphStyle
import com.microsoft.notes.richtext.scheme.Span as StoreSpan
import com.microsoft.notes.richtext.scheme.SpanStyle as StoreSpanStyle
import org.hamcrest.CoreMatchers.`is` as iz

@RunWith(AndroidJUnit4::class)
class UpdateHandlerTest {
    private val NOTE_ID = "noteId"
    private val PARAGRAPH_ID = "paragraphId"
    private val MEDIA_ID = "mediaId"
    private val LOCAL_CREATED_AT: Long = 1517417105000
    private val DOCUMENT_MODIFIED_AT: Long = 1517417431000
    private val TEXT = "text"
    private val NEW_TEXT = "newText"
    private val LOCAL_URL = "localUrl/"
    private val SPAN_START = 0
    private val SPAN_END = 5
    private val SPAN_FLAG = 10
    private val COLOR = StoreColor.GREEN
    private val NEW_COLOR = StoreColor.BLUE
    private val IS_DELETED = false
    private val TEST_USER_ID = "test@outlook.com"

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
    fun should_update_Note_with_Document() {
        // Build a Note with one Paragpraph and one Media.
        var storeParagraphStyle = StoreParagraphStyle(true)
        var storeSpan1 = StoreSpan(StoreSpanStyle.BOLD, SPAN_START, SPAN_END, SPAN_FLAG)
        var storeSpan2 = StoreSpan(StoreSpanStyle.ITALIC, SPAN_START, SPAN_END, SPAN_FLAG)
        var storeContent = StoreContent(text = TEXT, spans = listOf(storeSpan1, storeSpan2))
        var storeParagraph = StoreParagraph(
            localId = PARAGRAPH_ID, style = storeParagraphStyle,
            content = storeContent
        )

        val storeMedia = StoreMedia(localId = MEDIA_ID, localUrl = LOCAL_URL)

        var storeDocument = StoreDocument(listOf(storeParagraph, storeMedia))
        val storeNote = StoreNote(
            localId = NOTE_ID,
            document = storeDocument, color = COLOR, isDeleted = IS_DELETED, localCreatedAt = LOCAL_CREATED_AT,
            documentModifiedAt = DOCUMENT_MODIFIED_AT
        )

        // We will insert the Note into the DB.
        val addNoteAction = CreationAction.AddNoteAction(note = storeNote, userID = TEST_USER_ID)
        CreationHandler.handle(
            action = addNoteAction, notesDB = testNotesDB,
            findNote =
            { null },
            actionDispatcher = {}
        )

        // Create a new Paragraph that will be added to the current Document
        val newStoreParagraphStyle = StoreParagraphStyle(false)
        val newStoreSpan1 = StoreSpan(StoreSpanStyle.UNDERLINE, SPAN_START, SPAN_END, SPAN_FLAG)
        val newStoreSpan2 = StoreSpan(StoreSpanStyle.ITALIC, SPAN_START, SPAN_END, SPAN_FLAG)
        val newStoreContent = StoreContent(text = NEW_TEXT, spans = listOf(newStoreSpan1, newStoreSpan2))
        val newStoreParagraph = StoreParagraph(
            localId = PARAGRAPH_ID + 2, style = newStoreParagraphStyle,
            content = newStoreContent
        )

        val newStoreDocument = storeDocument.copy(blocks = storeDocument.blocks + newStoreParagraph)
        val updatedNote = storeNote.copy(document = newStoreDocument)

        // We are going to update the Document into the DB
        val updateDocumentAction = UpdateNoteWithDocumentAction(
            noteLocalId = storeNote.localId,
            updatedDocument = updatedNote.document,
            documentModifiedAt = DOCUMENT_MODIFIED_AT + 1, uiRevision = 1, userID = TEST_USER_ID
        )
        with(updateDocumentAction) {
            UpdateHandler.handle(action = updateDocumentAction, notesDB = testNotesDB, findNote = {
                Note(
                    localId = noteLocalId, document = updatedDocument, documentModifiedAt = documentModifiedAt,
                    uiRevision = 1
                )
            }, actionDispatcher = {})
        }

        // We should have just one Note
        val notesListResult = testNotesDB.noteDao().getAll()
        assertThat(notesListResult.size, iz(1))
        assertThat(notesListResult.component1().documentModifiedAt, iz(DOCUMENT_MODIFIED_AT + 1))

        // Now we should have 2 Paragraphs instead 1
        val persistenceDocument = notesListResult.component1().document
        val paragraphListResult = persistenceDocument.fromDocumentJson()?.justParagraph()
        assertThat(paragraphListResult?.size, iz(2))

        AssertUtils.assert_Document(
            persistenceDocument = persistenceDocument,
            storeDocument =
            updatedNote.document
        )
    }

    @Test
    fun should_update_Note_with_Color() {
        val storeNote = StoreNote(
            localId = NOTE_ID, color = COLOR, localCreatedAt = LOCAL_CREATED_AT,
            documentModifiedAt = DOCUMENT_MODIFIED_AT
        )
        val persistenceNote = storeNote.toPersistenceNote()
        testNotesDB.noteDao().insert(persistenceNote)

        val updateColorAction = UpdateNoteWithColorAction(
            noteLocalId = storeNote.localId, color = NEW_COLOR,
            uiRevision = 1, userID = TEST_USER_ID
        )

        with(updateColorAction) {
            UpdateHandler.handle(
                action = updateColorAction, notesDB = testNotesDB,
                findNote = {
                    Note(localId = noteLocalId, color = color, uiRevision = 1)
                }, actionDispatcher = {}
            )
        }

        val notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(1))

        assertThat(notesResult.component1().color, iz(NEW_COLOR.toPersistenceColor()))
        assertThat(notesResult.component1().documentModifiedAt, iz(DOCUMENT_MODIFIED_AT))
    }

    @Test
    fun should_remove_Note_Media() {
        val NOTE_ID = "id1"
        val MEDIA_ID_1 = "media1"
        val DOCUMENT_MODIFIED = 1L
        val media = Media(MEDIA_ID_1, null, null, "image/jpg", null, null, DOCUMENT_MODIFIED)
        val note = Note(localId = NOTE_ID, documentModifiedAt = DOCUMENT_MODIFIED, media = listOf(media))

        testNotesDB.noteDao().insert(note.toPersistenceNote())

        val removeMediaAction = UpdateNoteWithRemovedMediaAction(MEDIA_ID_1, media, DOCUMENT_MODIFIED, TEST_USER_ID)
        UpdateHandler.handle(
            action = removeMediaAction, notesDB = testNotesDB, findNote = { note },
            actionDispatcher = {}
        )

        val notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(1))
        with(notesResult) {
            assertThat(component1().media.fromMediaJson(), iz(emptyList()))
        }
    }

    @Test
    fun should_remove_Note_Media_given_multiple_Media() {
        val NOTE_ID = "id1"
        val MEDIA_ID_1 = "media1"
        val MEDIA_ID_2 = "media2"
        val DOCUMENT_MODIFIED = 1L
        val media1 = Media(MEDIA_ID_1, null, null, "image/jpg", null, null, DOCUMENT_MODIFIED)
        val media2 = Media(MEDIA_ID_2, null, null, "image/jpg", null, null, DOCUMENT_MODIFIED)
        val note = Note(localId = NOTE_ID, documentModifiedAt = DOCUMENT_MODIFIED, media = listOf(media1, media2))

        testNotesDB.noteDao().insert(note.toPersistenceNote())

        val removeMediaAction = UpdateNoteWithRemovedMediaAction(MEDIA_ID_1, media1, DOCUMENT_MODIFIED, TEST_USER_ID)
        UpdateHandler.handle(
            action = removeMediaAction, notesDB = testNotesDB, findNote = { note },
            actionDispatcher = {}
        )

        val notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(1))
        with(notesResult) {
            assertThat(component1().media.fromMediaJson(), iz(listOf(media2)))
        }
    }

    @Test
    fun should_not_remove_Note_Media_given_missing_Media() {
        val NOTE_ID = "id1"
        val MEDIA_ID_1 = "media1"
        val MEDIA_ID_2 = "media2"
        val DOCUMENT_MODIFIED = 1L
        val media1 = Media(MEDIA_ID_1, null, null, "image/jpg", null, null, DOCUMENT_MODIFIED)
        val media2 = Media(MEDIA_ID_2, null, null, "image/jpg", null, null, DOCUMENT_MODIFIED)
        val note = Note(localId = NOTE_ID, documentModifiedAt = DOCUMENT_MODIFIED, media = listOf(media1, media2))

        testNotesDB.noteDao().insert(note.toPersistenceNote())

        val removeMediaAction = UpdateNoteWithRemovedMediaAction(
            "missing_id",
            Media(
                "missing_id",
                "image/jpg", null, null
            ),
            DOCUMENT_MODIFIED, TEST_USER_ID
        )
        UpdateHandler.handle(
            action = removeMediaAction, notesDB = testNotesDB, findNote = { note },
            actionDispatcher = {}
        )

        val notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(1))
        with(notesResult) {
            assertThat(component1().media.fromMediaJson(), iz(listOf(media1, media2)))
        }
    }

    @Test
    fun should_update_Note_Media_alt_text() {
        val NOTE_ID = "id1"
        val MEDIA_ID_1 = "media1"
        val DOCUMENT_MODIFIED = 1L
        val media = Media(MEDIA_ID_1, null, null, "image/jpg", null, null, DOCUMENT_MODIFIED)
        val note = Note(localId = NOTE_ID, documentModifiedAt = DOCUMENT_MODIFIED, media = listOf(media))

        testNotesDB.noteDao().insert(note.toPersistenceNote())

        val NEW_ALT_TEXT = "foo"
        val updateAltTextAction = UpdateNoteWithUpdateMediaAltTextAction(
            NOTE_ID, MEDIA_ID_1, NEW_ALT_TEXT,
            DOCUMENT_MODIFIED, TEST_USER_ID
        )
        UpdateHandler.handle(
            action = updateAltTextAction, notesDB = testNotesDB, findNote = { note },
            actionDispatcher = {}
        )

        val notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(1))
        with(notesResult) {
            assertThat(component1().media.fromMediaJson(), iz(listOf(media.copy(altText = NEW_ALT_TEXT))))
        }
    }

    @Test
    fun should_update_Note_Media_alt_text_given_multiple_Media() {
        val NOTE_ID = "id1"
        val MEDIA_ID_1 = "media1"
        val MEDIA_ID_2 = "media2"
        val DOCUMENT_MODIFIED = 1L
        val media1 = Media(MEDIA_ID_1, null, null, "image/jpg", null, null, DOCUMENT_MODIFIED)
        val media2 = Media(MEDIA_ID_2, null, null, "image/jpg", null, null, DOCUMENT_MODIFIED)
        val note = Note(localId = NOTE_ID, documentModifiedAt = DOCUMENT_MODIFIED, media = listOf(media1, media2))

        testNotesDB.noteDao().insert(note.toPersistenceNote())

        val NEW_ALT_TEXT = "foo"
        val updateAltTextAction = UpdateNoteWithUpdateMediaAltTextAction(
            NOTE_ID, MEDIA_ID_1, NEW_ALT_TEXT,
            DOCUMENT_MODIFIED, TEST_USER_ID
        )
        UpdateHandler.handle(
            action = updateAltTextAction, notesDB = testNotesDB, findNote = { note },
            actionDispatcher = {}
        )

        val notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(1))
        with(notesResult) {
            assertThat(
                component1().media.fromMediaJson(),
                iz(listOf(media1.copy(altText = NEW_ALT_TEXT), media2))
            )
        }
    }

    @Test
    fun should_update_Note_Media_alt_text_given_missing_Media() {
        val NOTE_ID = "id1"
        val MEDIA_ID_1 = "media1"
        val MEDIA_ID_2 = "media2"
        val DOCUMENT_MODIFIED = 1L
        val media1 = Media(MEDIA_ID_1, null, null, "image/jpg", null, null, DOCUMENT_MODIFIED)
        val media2 = Media(MEDIA_ID_2, null, null, "image/jpg", null, null, DOCUMENT_MODIFIED)
        val note = Note(localId = NOTE_ID, documentModifiedAt = DOCUMENT_MODIFIED, media = listOf(media1, media2))

        testNotesDB.noteDao().insert(note.toPersistenceNote())

        val NEW_ALT_TEXT = "foo"
        val updateAltTextAction = UpdateNoteWithUpdateMediaAltTextAction(
            NOTE_ID, "missing_id", NEW_ALT_TEXT,
            DOCUMENT_MODIFIED, TEST_USER_ID
        )
        UpdateHandler.handle(
            action = updateAltTextAction, notesDB = testNotesDB, findNote = { note },
            actionDispatcher = {}
        )

        val notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(1))
        with(notesResult) {
            assertThat(
                component1().media.fromMediaJson(),
                iz(listOf(media1, media2))
            )
        }
    }
}
