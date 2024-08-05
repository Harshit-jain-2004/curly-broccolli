package com.microsoft.notes.sideeffect.persistence.handler

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.microsoft.notes.sideeffect.persistence.AssertUtils
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.sideeffect.persistence.createTestNotesDB
import com.microsoft.notes.store.action.CreationAction
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.microsoft.notes.models.Color as StoreColor
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.models.RemoteData as StoreRemoteData
import com.microsoft.notes.richtext.scheme.Content as StoreContent
import com.microsoft.notes.richtext.scheme.Document as StoreDocument
import com.microsoft.notes.richtext.scheme.InlineMedia as StoreMedia
import com.microsoft.notes.richtext.scheme.Paragraph as StoreParagraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle as StoreParagraphStyle
import com.microsoft.notes.richtext.scheme.Span as StoreSpan
import com.microsoft.notes.richtext.scheme.SpanStyle as StoreSpanStyle
import org.hamcrest.CoreMatchers.`is` as iz

@RunWith(AndroidJUnit4::class)
class CreationHandlerTest {

    private val NOTE_ID = "noteId"
    private val PARAGRAPH_ID = "paragraphId"
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
    private val COLOR = StoreColor.GREEN
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
    fun should_create_Note() {
        val storeParagraphStyle = StoreParagraphStyle(true)
        val storeSpan1 = StoreSpan(StoreSpanStyle.BOLD, SPAN_START, SPAN_END, SPAN_FLAG)
        val storeSpan2 = StoreSpan(StoreSpanStyle.ITALIC, SPAN_START, SPAN_END, SPAN_FLAG)
        val storeContent = StoreContent(text = TEXT, spans = listOf(storeSpan1, storeSpan2))
        val storeParagraph = StoreParagraph(
            localId = PARAGRAPH_ID, style = storeParagraphStyle,
            content = storeContent
        )

        val storeMedia = StoreMedia(localId = MEDIA_ID, localUrl = LOCAL_URL)

        val storeDocument = StoreDocument(listOf(storeParagraph, storeMedia))

        val storeRemoteData = StoreRemoteData(
            id = REMOTE_DATA_ID, changeKey = CHANGE_KEY,
            lastServerVersion = StoreNote(localId = REMOTE_DATA_ID), createdAt = CREATED_AT,
            lastModifiedAt = LAST_MODIFIED_AT
        )

        val storeNote = StoreNote(
            localId = NOTE_ID, remoteData = storeRemoteData,
            document = storeDocument, color = COLOR, isDeleted = IS_DELETED,
            localCreatedAt = LOCAL_CREATED_AT, documentModifiedAt = DOCUMENT_MODIFIED_AT
        )

        val addNoteAction = CreationAction.AddNoteAction(storeNote, TEST_USER_ID)
        CreationHandler.handle(
            action = addNoteAction, notesDB = testNotesDB, findNote = { null },
            actionDispatcher = {}
        )

        val notesListResult = testNotesDB.noteDao().getAll()
        assertThat(notesListResult.isNotEmpty(), iz(true))
        assertThat(notesListResult.size, iz(1))

        AssertUtils.assert_Note(persistenceNote = notesListResult[0], storeNote = storeNote)
    }
}
