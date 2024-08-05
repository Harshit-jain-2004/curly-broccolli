package com.microsoft.notes.sideeffect.persistence.handler

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.google.gson.Gson
import com.microsoft.notes.models.Changes
import com.microsoft.notes.models.NoteUpdate
import com.microsoft.notes.sideeffect.persistence.AssertUtils.assert_Note
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.sideeffect.persistence.PreferenceKeys
import com.microsoft.notes.sideeffect.persistence.createTestNotesDB
import com.microsoft.notes.sideeffect.persistence.extensions.fromMediaJson
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceNote
import com.microsoft.notes.store.action.SyncResponseAction
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.microsoft.notes.models.Color as StoreColor
import com.microsoft.notes.models.Media as StoreMedia
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.models.RemoteData as StoreRemoteData
import com.microsoft.notes.richtext.scheme.Content as StoreContent
import com.microsoft.notes.richtext.scheme.Document as StoreDocument
import com.microsoft.notes.richtext.scheme.Paragraph as StoreParagraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle as StoreParagraphStyle
import com.microsoft.notes.richtext.scheme.Span as StoreSpan
import com.microsoft.notes.richtext.scheme.SpanStyle as StoreSpanStyle
import org.hamcrest.CoreMatchers.`is` as iz

@RunWith(AndroidJUnit4::class)
class UserSyncHandlerTest {

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
    fun should_ApplyChanges_toCreate() {
        val NUMBER_OF_PARAGRAPHS = 2
        val NUMBER_OF_SPANS = 2
        val NUMBER_OF_MEDIA = 2
        val deltaToken = "xyz"

        val note1 = createDummyStoreNote(
            suffix = 1, numberOfParagraphs = NUMBER_OF_PARAGRAPHS,
            numberOfSpans = NUMBER_OF_SPANS, numberOfMedia = NUMBER_OF_MEDIA
        )
        val note2 = createDummyStoreNote(
            suffix = 2, numberOfParagraphs = NUMBER_OF_PARAGRAPHS,
            numberOfSpans = NUMBER_OF_SPANS, numberOfMedia = NUMBER_OF_MEDIA
        )

        insertNoteIntoDB(note1, testNotesDB)
        insertNoteIntoDB(note2, testNotesDB)

        // This is the new Note we are adding
        val NEW_NOTE_SUFFIX = 3
        val newNote = createDummyStoreNote(
            NEW_NOTE_SUFFIX, numberOfParagraphs = NUMBER_OF_PARAGRAPHS,
            numberOfSpans = NUMBER_OF_SPANS, numberOfMedia = NUMBER_OF_MEDIA
        )

        val applyChangesToCreate = SyncResponseAction.ApplyChanges(
            Changes(toCreate = arrayListOf(newNote)),
            deltaToken = deltaToken,
            userID = TEST_USER_ID
        )
        SyncHandler.handle(
            action = applyChangesToCreate, notesDB = testNotesDB, findNote = { null },
            actionDispatcher = {}
        )

        // Check that we have a new note more (originally we had just 2)
        val resultNotes = testNotesDB.noteDao().getAll()
        assertThat(resultNotes.size, iz(3))

        val fetchedToken = testNotesDB.preferencesDao().get(PreferenceKeys.deltaToken)
        assertThat(fetchedToken, iz(deltaToken))

        // We check the Note fields
        val resultNote = testNotesDB.noteDao().getNoteById(newNote.localId)
        assertThat(resultNote, iz(not(nullValue())))
        assert_Note(resultNote!!, newNote)
    }

    @Test
    fun should_ApplyChanges_toReplace() {
        val NUMBER_OF_PARAGRAPHS = 1
        val NUMBER_OF_SPANS = 1
        val NUMBER_OF_MEDIA = 1
        val deltaToken = "xyz"

        val note1 = createDummyStoreNote(
            suffix = 1, numberOfParagraphs = NUMBER_OF_PARAGRAPHS,
            numberOfSpans = NUMBER_OF_SPANS, numberOfMedia = NUMBER_OF_MEDIA
        )
        val note2 = createDummyStoreNote(
            suffix = 2, numberOfParagraphs = NUMBER_OF_PARAGRAPHS,
            numberOfSpans = NUMBER_OF_SPANS, numberOfMedia = NUMBER_OF_MEDIA
        )

        insertNoteIntoDB(note1, testNotesDB)
        insertNoteIntoDB(note2, testNotesDB)

        val newMedia = StoreMedia(
            localId = MEDIA_ID + "new",
            remoteId = null,
            localUrl = LOCAL_URL + "new",
            mimeType = "img/png",
            altText = null,
            imageDimensions = null,
            lastModified = System.currentTimeMillis()
        )
        val updatedNote = note2.copy(media = note2.media + newMedia)

        val applyChangesToCreate = SyncResponseAction.ApplyChanges(
            Changes(
                toReplace = arrayListOf(
                    NoteUpdate(updatedNote, updatedNote.uiRevision)
                )
            ),
            deltaToken = deltaToken,
            userID = TEST_USER_ID
        )
        SyncHandler.handle(
            action = applyChangesToCreate, notesDB = testNotesDB, findNote = { null },
            actionDispatcher = {}
        )

        val fetchedToken = testNotesDB.preferencesDao().get(PreferenceKeys.deltaToken)
        assertThat(fetchedToken, iz(deltaToken))

        val resultNotes = testNotesDB.noteDao().getAll()
        assertThat(resultNotes.size, iz(2))

        val persistenceNote = testNotesDB.noteDao().getNoteById(noteId = updatedNote.localId)
        assertThat(persistenceNote, iz(not(nullValue())))
        if (persistenceNote == null) return

        val resultListMedia = persistenceNote.media.fromMediaJson()

        assertThat(resultListMedia.size, iz(2))

        // We test the last Media block that is the new added one.
        with(resultListMedia.component2()) {
            assertThat(localId, iz(newMedia.localId))
            assertThat(localUrl, iz(newMedia.localUrl))
            assertThat(mimeType, iz(newMedia.mimeType))
        }
    }

    @Test
    fun should_ApplyChanges_toDelete() {
        val NUMBER_OF_PARAGRAPHS = 1
        val NUMBER_OF_SPANS = 1
        val NUMBER_OF_MEDIA = 1
        val deltaToken = "xyz"

        val note1 = createDummyStoreNote(
            suffix = 1, numberOfParagraphs = NUMBER_OF_PARAGRAPHS,
            numberOfSpans = NUMBER_OF_SPANS, numberOfMedia = NUMBER_OF_MEDIA
        )
        val note2 = createDummyStoreNote(
            suffix = 2, numberOfParagraphs = NUMBER_OF_PARAGRAPHS,
            numberOfSpans = NUMBER_OF_SPANS, numberOfMedia = NUMBER_OF_MEDIA
        )

        insertNoteIntoDB(note1, testNotesDB)
        insertNoteIntoDB(note2, testNotesDB)

        val applyChangesToCreate = SyncResponseAction.ApplyChanges(
            Changes(toDelete = arrayListOf(note1)),
            deltaToken = deltaToken,
            userID = TEST_USER_ID
        )
        SyncHandler.handle(
            action = applyChangesToCreate, notesDB = testNotesDB, findNote = { null },
            actionDispatcher = {}
        )

        val resultNotes = testNotesDB.noteDao().getAll()
        assertThat(resultNotes.size, iz(1))

        val fetchedToken = testNotesDB.preferencesDao().get(PreferenceKeys.deltaToken)
        assertThat(fetchedToken, iz(deltaToken))

        // We shouldn't get the deleted note from the DB, it should ve null
        val deletedNoteResult = testNotesDB.noteDao().getNoteById(note1.localId)
        assertThat(deletedNoteResult, iz(nullValue()))
    }

    @Test
    fun should_update_RemoteData() {
        val NUMBER_OF_PARAGRAPHS = 1
        val NUMBER_OF_SPANS = 1
        val NUMBER_OF_MEDIA = 1

        val note1 = createDummyStoreNote(
            suffix = 1, numberOfParagraphs = NUMBER_OF_PARAGRAPHS,
            numberOfSpans = NUMBER_OF_SPANS, numberOfMedia = NUMBER_OF_MEDIA
        )

        insertNoteIntoDB(note1, testNotesDB)

        val remoteDataUpdatedAction = SyncResponseAction.RemoteDataUpdated(
            noteLocalId = note1.localId,
            remoteData = note1.remoteData!!.copy(changeKey = "new"), userID = TEST_USER_ID
        )
        SyncHandler.handle(
            action = remoteDataUpdatedAction, notesDB = testNotesDB, findNote = { null },
            actionDispatcher = {}
        )
        val persistenceRemoteData = testNotesDB.noteDao().getNoteById(note1.localId)?.remoteData
        val resultRemoteData = Gson().fromJson(persistenceRemoteData, StoreRemoteData::class.java)
        with(resultRemoteData) {
            assertThat(this, iz(not(nullValue())))
            assertThat(id, iz(note1.remoteData?.id))
            assertThat(changeKey, iz("new"))
            assertThat(lastServerVersion, iz(note1.remoteData?.lastServerVersion))
        }
    }

    @Test
    fun should_update_RemoteData_with_Media_downloaded() {
        val NUMBER_OF_PARAGRAPHS = 1
        val NUMBER_OF_SPANS = 1
        val NUMBER_OF_MEDIA = 1

        val note1 = createDummyStoreNote(
            suffix = 1, numberOfParagraphs = NUMBER_OF_PARAGRAPHS,
            numberOfSpans = NUMBER_OF_SPANS, numberOfMedia = NUMBER_OF_MEDIA
        )

        insertNoteIntoDB(note1, testNotesDB)
        val media = note1.media.component1()
        val mediaDownloadedAction = SyncResponseAction.MediaDownloaded(
            noteId = note1.localId,
            mediaRemoteId = "remoteId", localUrl = "new", mimeType = "img/png", userID = TEST_USER_ID
        )
        SyncHandler.handle(
            action = mediaDownloadedAction, notesDB = testNotesDB, findNote = { note1 },
            actionDispatcher = {}
        )

        val persistenceNote = testNotesDB.noteDao().getNoteById(noteId = note1.localId)
        assertThat(persistenceNote, iz(not(nullValue())))
        if (persistenceNote == null) return

        val resultListMedia = persistenceNote.media.fromMediaJson()

        assertThat(resultListMedia.size, iz(NUMBER_OF_MEDIA))
        with(resultListMedia.component1()) {
            assertThat(this, iz(not(nullValue())))
            assertThat(localId, iz(media.localId))
            assertThat(localUrl, iz(media.localUrl))
            assertThat(mimeType, iz(media.mimeType))
        }
    }

    private fun createDummyStoreNote(
        suffix: Int,
        numberOfParagraphs: Int = 1,
        numberOfSpans: Int = 1,
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

        val listOfMedia = (0 until numberOfMedia).mapIndexedTo(
            destination = arrayListOf(),
            transform = { index, _ ->
                StoreMedia(
                    localId = MEDIA_ID + index + suffix,
                    remoteId = null,
                    localUrl = LOCAL_URL + index + suffix,
                    mimeType = "img/png",
                    altText = null,
                    imageDimensions = null,
                    lastModified = System.currentTimeMillis()
                )
            }
        )

        val storeDocument = StoreDocument(listOfParagraphs)

        val storeRemoteData = StoreRemoteData(
            id = REMOTE_DATA_ID + suffix, changeKey = CHANGE_KEY + suffix,
            lastServerVersion = StoreNote(REMOTE_DATA_ID + suffix),
            createdAt = CREATED_AT, lastModifiedAt = LAST_MODIFIED_AT
        )

        return StoreNote(
            localId = NOTE_ID + suffix, remoteData = storeRemoteData,
            document = storeDocument, color = COLOR, isDeleted = IS_DELETED,
            localCreatedAt = LOCAL_CREATED_AT, documentModifiedAt = DOCUMENT_MODIFIED_AT,
            media = listOfMedia
        )
    }

    @Test
    fun should_permanently_delete_Note() {
        val storeNote1 = StoreNote(localId = NOTE_ID)
        val persistenceNote1 = storeNote1.toPersistenceNote()
        testNotesDB.noteDao().insert(listOf(persistenceNote1))

        var notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(1))

        val deleteNoteAction = SyncResponseAction.PermanentlyDeleteNote(noteLocalId = NOTE_ID, userID = TEST_USER_ID)
        SyncHandler.handle(
            action = deleteNoteAction, notesDB = testNotesDB, findNote = { null },
            actionDispatcher = {}
        )

        notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.isEmpty(), iz(true))
    }
}
