package com.microsoft.notes.store.reducer

import com.microsoft.notes.models.Color
import com.microsoft.notes.models.LOCAL_ID
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.RemoteData
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.mediaListCount
import com.microsoft.notes.richtext.scheme.paragraphList
import com.microsoft.notes.richtext.scheme.paragraphListCount
import com.microsoft.notes.richtext.scheme.size
import com.microsoft.notes.store.NotesList
import com.microsoft.notes.store.State
import com.microsoft.notes.store.UserState
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithColorAction
import com.microsoft.notes.store.action.UpdateAction.UpdateActionWithId.UpdateNoteWithDocumentAction
import com.microsoft.notes.store.addNotes
import com.microsoft.notes.store.addUserState
import com.microsoft.notes.store.getNotesCollectionForUser
import com.microsoft.notes.store.withNotesLoaded
import com.microsoft.notes.utils.logging.TestConstants
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class UpdateReducerTest {
    val NOTE_LOCAL_ID_1 = "localId1"
    val NOTE_LOCAL_ID_2 = "localId2"
    val CONTENT_TEXT = "contentText"

    val testNote1 = Note(localId = NOTE_LOCAL_ID_1, color = Color.BLUE)
    val testNote2 = Note(localId = NOTE_LOCAL_ID_2, color = Color.YELLOW)

    @Test
    fun setNoteColorState_should_update_Note() {
        val state = State().withNotesLoaded(false, TestConstants.TEST_USER_ID).addNotes(
            listOf(testNote1),
            TestConstants.TEST_USER_ID
        )
        val action = UpdateNoteWithColorAction(
            noteLocalId = testNote1.localId,
            color = Color.YELLOW,
            uiRevision = 1,
            userID = TestConstants.TEST_USER_ID
        )
        val newState = UpdateReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(1))
            assertThat(userIDForLocalNoteID(testNote1.localId), iz(TestConstants.TEST_USER_ID))
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).first().color, iz(Color.YELLOW))
        }
    }

    @Test
    fun should_update_note_with_Document_when_it_didnt_have_Paragraph() {
        val testNote = Note(
            localId = NOTE_LOCAL_ID_1,
            color = Color.BLUE,
            uiShadow = Note(localId = NOTE_LOCAL_ID_1),
            remoteData = RemoteData("", "", Note(), 0, 0)
        )
        val testUserState = UserState(
            notesList = NotesList.createNotesList(
                notesCollection = listOf(testNote),
                notesLoaded = false
            )
        )
        val state = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val newParagraph = Paragraph(localId = LOCAL_ID, content = Content(text = CONTENT_TEXT))

        val action = UpdateNoteWithDocumentAction(
            noteLocalId = NOTE_LOCAL_ID_1,
            updatedDocument = Document(listOf(newParagraph)),
            uiRevision = 1,
            userID = TestConstants.TEST_USER_ID
        )
        val newState = UpdateReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(1))

            fun verifyNote(note: Note) {
                assertThat(note.remoteData, iz(testNote.remoteData))
                assertThat(note.remoteData, iz(not(nullValue())))
                assertThat(note.uiShadow, iz(not(nullValue())))
                with(note.document) {
                    assertThat(note.document.size(), iz(1))
                    assertThat(paragraphListCount(), iz(1))
                    assertThat(paragraphList().first(), iz(newParagraph))
                }
            }
            verifyNote(getNotesCollectionForUser(TestConstants.TEST_USER_ID).first())
            assertThat(userIDForLocalNoteID(NOTE_LOCAL_ID_1), iz(TestConstants.TEST_USER_ID))
        }
    }

    @Test
    fun should_update_note_with_Document_when_already_has_Paragraph_and_is_the_same_we_want_to_update() {
        val paragraph = Paragraph(localId = LOCAL_ID, content = Content(text = CONTENT_TEXT))
        val testNote = Note(
            localId = NOTE_LOCAL_ID_1,
            document = Document(listOf(paragraph)),
            color = Color.BLUE
        )
        val testUserState = UserState(
            notesList = NotesList.createNotesList(
                notesCollection = listOf(testNote),
                notesLoaded = false
            )
        )
        val state = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val NEW_CONTENT = "new content"
        val newParagraph = Paragraph(localId = LOCAL_ID, content = Content(text = NEW_CONTENT))

        val action = UpdateNoteWithDocumentAction(
            noteLocalId = NOTE_LOCAL_ID_1,
            updatedDocument = Document(listOf(newParagraph)),
            uiRevision = 1,
            userID = TestConstants.TEST_USER_ID
        )
        val newState = UpdateReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(1))

            fun verifyNote(note: Note) {
                with(note.document) {
                    assertThat(size(), iz(1))
                    assertThat(paragraphListCount(), iz(1))
                    assertThat(paragraphList().first(), iz(newParagraph))
                }
            }
            verifyNote(getNotesCollectionForUser(TestConstants.TEST_USER_ID).first())
            assertThat(userIDForLocalNoteID(NOTE_LOCAL_ID_1), iz(TestConstants.TEST_USER_ID))
        }
    }

    @Test
    fun should_update_note_with_Document_when_we_have_another_Block_that_is_a_Paragraph() {
        val oldParagraph = Paragraph(localId = "other Paragraph", content = Content(text = "other content"))
        val testNote = Note(
            localId = NOTE_LOCAL_ID_1,
            document = Document(listOf(oldParagraph)),
            color = Color.BLUE
        )
        val testUserState = UserState(
            notesList = NotesList.createNotesList(
                notesCollection = listOf(testNote),
                notesLoaded = false
            )
        )
        val state = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val NEW_PARAGRAPH_ID = "new Paragraph id"
        val newParagraph = Paragraph(localId = NEW_PARAGRAPH_ID, content = Content(text = CONTENT_TEXT))

        val action = UpdateNoteWithDocumentAction(
            noteLocalId = NOTE_LOCAL_ID_1,
            updatedDocument = Document(listOf(newParagraph)),
            uiRevision = 1,
            userID = TestConstants.TEST_USER_ID
        )

        val newState = UpdateReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(1))

            fun verifyNote(note: Note) {
                with(note.document) {
                    assertThat(size(), iz(1))
                    assertThat(paragraphListCount(), iz(1))

                    val newCreatedParagraph = paragraphList().find { it.localId == newParagraph.localId }
                    assertThat(newCreatedParagraph, iz(not(nullValue())))
                    assertThat(newCreatedParagraph, iz(newParagraph))

                    val oldFoundParagraph = paragraphList().find { it.localId == oldParagraph.localId }
                    assertThat(oldFoundParagraph, iz(nullValue()))
                }
            }
            verifyNote(getNotesCollectionForUser(TestConstants.TEST_USER_ID).first())
            assertThat(userIDForLocalNoteID(NOTE_LOCAL_ID_1), iz(TestConstants.TEST_USER_ID))
        }
    }

    @Test
    fun should_update_note_with_Document_when_we_have_another_Block_that_is_Media() {
        val media = InlineMedia(localId = "media localId", localUrl = "/localUrl")
        val testNote = Note(
            localId = NOTE_LOCAL_ID_1,
            document = Document(listOf(media)),
            color = Color.BLUE
        )
        val testUserState = UserState(
            notesList = NotesList.createNotesList(
                notesCollection = listOf(testNote),
                notesLoaded = false
            )
        )
        val state = State().addUserState(testUserState, TestConstants.TEST_USER_ID)

        val NEW_PARAGRAPH_ID = "new Paragraph id"
        val newParagraph = Paragraph(localId = NEW_PARAGRAPH_ID, content = Content(text = CONTENT_TEXT))

        val action = UpdateNoteWithDocumentAction(
            noteLocalId = NOTE_LOCAL_ID_1,
            updatedDocument = Document(listOf(newParagraph)),
            uiRevision = 1,
            userID = TestConstants.TEST_USER_ID
        )

        val newState = UpdateReducer.reduce(action, currentState = state, isDebugMode = true)

        with(newState) {
            assertThat(getNotesCollectionForUser(TestConstants.TEST_USER_ID).size, iz(1))

            fun verifyNote(note: Note) {
                with(note.document) {
                    assertThat(size(), iz(1))
                    assertThat(mediaListCount(), iz(0))
                    assertThat(paragraphListCount(), iz(1))

                    val newCreatedParagraph = paragraphList().find { it.localId == newParagraph.localId }
                    assertThat(newCreatedParagraph, iz(not(nullValue())))
                    assertThat(newCreatedParagraph, iz(newParagraph))
                }
            }
            verifyNote(getNotesCollectionForUser(TestConstants.TEST_USER_ID).first())
            assertThat(userIDForLocalNoteID(NOTE_LOCAL_ID_1), iz(TestConstants.TEST_USER_ID))
        }
    }
}
