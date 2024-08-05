package com.microsoft.notes.sideeffect.persistence.handler

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.microsoft.notes.sideeffect.persistence.NotesDatabase
import com.microsoft.notes.sideeffect.persistence.createTestNotesDB
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceNote
import com.microsoft.notes.store.action.DeleteAction
import com.microsoft.notes.store.action.DeleteAction.MarkNoteAsDeletedAction
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.microsoft.notes.models.Note as StoreNote
import org.hamcrest.CoreMatchers.`is` as iz

@RunWith(AndroidJUnit4::class)
class DeleteHandlerTest {
    private val NOTE_ID = "noteId"
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
    fun should_delete_all_Notes() {
        val storeNote1 = StoreNote(localId = NOTE_ID + 1)
        val storeNote2 = StoreNote(localId = NOTE_ID + 2)
        val storeNote3 = StoreNote(localId = NOTE_ID + 3)

        val persistenceNote1 = storeNote1.toPersistenceNote()
        val persistenceNote2 = storeNote2.toPersistenceNote()
        val persistenceNote3 = storeNote3.toPersistenceNote()

        testNotesDB.noteDao().insert(listOf(persistenceNote1, persistenceNote2, persistenceNote3))
        var notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(3))

        val deleteAllNotesAction = DeleteAction.DeleteAllNotesAction(TEST_USER_ID)
        DeleteHandler.handle(
            action = deleteAllNotesAction, notesDB = testNotesDB, findNote = { null },
            actionDispatcher = {}
        )

        notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(0))
    }

    @Test
    fun should_mark_Note_as_deleted() {
        val storeNote1 = StoreNote(localId = NOTE_ID)
        val persistenceNote1 = storeNote1.toPersistenceNote()
        assertThat(persistenceNote1.isDeleted, iz(false))

        testNotesDB.noteDao().insert(listOf(persistenceNote1))

        var notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(1))
        assertThat(notesResult.component1().isDeleted, iz(false))

        val deleteNoteAction = MarkNoteAsDeletedAction(localId = NOTE_ID, remoteId = null, userID = TEST_USER_ID)
        DeleteHandler.handle(
            action = deleteNoteAction, notesDB = testNotesDB, findNote = { null },
            actionDispatcher = {}
        )

        notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(1))
        assertThat(notesResult.component1().isDeleted, iz(true))
    }

    @Test
    fun should_unmark_Note_as_deleted() {
        val storeNote1 = StoreNote(localId = NOTE_ID, isDeleted = true)
        val persistenceNote1 = storeNote1.toPersistenceNote()
        assertThat(persistenceNote1.isDeleted, iz(true))

        testNotesDB.noteDao().insert(listOf(persistenceNote1))

        var notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(1))
        assertThat(notesResult.component1().isDeleted, iz(true))

        val deleteNoteAction = DeleteAction.UnmarkNoteAsDeletedAction(
            localId = NOTE_ID, remoteId = null,
            userID =
            TEST_USER_ID
        )
        DeleteHandler.handle(
            action = deleteNoteAction, notesDB = testNotesDB, findNote = { null },
            actionDispatcher = {}
        )

        notesResult = testNotesDB.noteDao().getAll()
        assertThat(notesResult.size, iz(1))
        assertThat(notesResult.component1().isDeleted, iz(false))
    }
}
