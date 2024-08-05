package com.microsoft.notes.sideeffect.persistence

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.InstrumentationRegistry
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceNote
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceNoteReference
import com.microsoft.notes.sideeffect.persistence.migrations.Migration10To11
import com.microsoft.notes.sideeffect.persistence.migrations.Migration11To12
import com.microsoft.notes.sideeffect.persistence.migrations.Migration1To2
import com.microsoft.notes.sideeffect.persistence.migrations.Migration2To3
import com.microsoft.notes.sideeffect.persistence.migrations.Migration3To4
import com.microsoft.notes.sideeffect.persistence.migrations.Migration4To5
import com.microsoft.notes.sideeffect.persistence.migrations.Migration5To6
import com.microsoft.notes.sideeffect.persistence.migrations.Migration6To7
import com.microsoft.notes.sideeffect.persistence.migrations.Migration7To8
import com.microsoft.notes.sideeffect.persistence.migrations.Migration8To9
import com.microsoft.notes.sideeffect.persistence.migrations.Migration9To10
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class MigrationTest {
    val TEST_DB_NAME = "Notes_Migration_Test"
    val VERSION_1 = 1
    val VERSION_2 = 2
    val VERSION_3 = 3
    val VERSION_4 = 4
    val VERSION_5 = 5
    val VERSION_6 = 6
    val VERSION_7 = 7
    val VERSION_8 = 8
    val VERSION_9 = 9
    val VERSION_10 = 10
    val VERSION_11 = 11
    val VERSION_12 = 12

    @get:Rule
    val migrationTestHelper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NotesDatabase::class.java.canonicalName, FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun should_migrate_1_to_2() {
        // The change here was that we added a new Table, "Preferences"

        val dbV1 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_1)
        dbV1.close()

        migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_2, true, Migration1To2)

        val notesDatabase = getMigratedNotesDatabase1to2()
        val id = "duumyKey"
        val value = "dummyValue"
        notesDatabase.preferencesDao().insertOrUpdate(Preference(id, value))

        assertThat(notesDatabase.preferencesDao().get(id), iz(value))
    }

    private fun getMigratedNotesDatabase1to2(): NotesDatabase {
        val database: NotesDatabase = Room.databaseBuilder(
            InstrumentationRegistry.getTargetContext(), NotesDatabase::class.java, TEST_DB_NAME
        )
            .addMigrations(Migration1To2)
            .fallbackToDestructiveMigration()
            .build()

        migrationTestHelper.closeWhenFinished(database)
        return database
    }

    @Test
    fun should_migrate_2_to_3() {
        // The change here was that we renamed the field "localModifiedAt" to "documentModifiedAt"
        val dbV2 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_2)
        val noteV2 = Note(
            localId = "noteV2",
            color = Color.BLUE,
            localCreatedAt = 10.toLong(),
            documentModifiedAt = 12.toLong(),
            document = Document(
                listOf(Paragraph(content = Content(text = "hello world!")))
            )
        ).toPersistenceNote()

        val values = ContentValues()
        with(noteV2) {
            values.put("id", id)
            values.put("isDeleted", isDeleted)
            values.put("color", color)
            values.put("localCreatedAt", localCreatedAt)
            values.put("localLastModifiedAt", documentModifiedAt)
            values.put("remoteData", remoteData)
            values.put("document", document)
            values.put("createdByApp", createdByApp)

            dbV2.insert("Note", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV2.close()
        }

        val dbV3 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_3, true, Migration2To3)
        val c = dbV3.query("SELECT * FROM Note WHERE id = 'noteV2'")

        assert(c.moveToFirst())
        assertThat(c.getInt(4), iz(12))

        dbV3.close()
    }

    @Test
    fun should_migrate_3_to_4() {
        // The change here was that we added the field "media"
        val dbV3 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_3)
        val noteV3 = Note(
            localId = "noteV3",
            color = Color.BLUE,
            localCreatedAt = 10.toLong(),
            documentModifiedAt = 12.toLong(),
            document = Document(
                listOf(Paragraph(content = Content(text = "hello world!")))
            )
        ).toPersistenceNote()

        val values = ContentValues()
        with(noteV3) {
            values.put("id", id)
            values.put("isDeleted", isDeleted)
            values.put("color", color)
            values.put("localCreatedAt", localCreatedAt)
            values.put("documentModifiedAt", documentModifiedAt)
            values.put("remoteData", remoteData)
            values.put("document", document)
            values.put("createdByApp", createdByApp)

            dbV3.insert("Note", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV3.close()
        }

        val dbV4 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_4, true, Migration3To4)

        val cOldNote = dbV4.query("SELECT * FROM Note WHERE id = 'noteV3'")
        assertThat(cOldNote.count, iz(1))
        assert(cOldNote.moveToFirst())
        assertThat(cOldNote.getLong(cOldNote.getColumnIndex("documentModifiedAt")), iz(12.toLong()))

        val noteV4 = Note(
            localId = "noteV4",
            color = Color.BLUE,
            localCreatedAt = 10.toLong(),
            documentModifiedAt = 12.toLong(),
            document = Document(
                listOf(Paragraph(content = Content(text = "hello world!")))
            ),
            media = listOf()
        ).toPersistenceNote()

        val newValues = ContentValues()
        with(noteV4) {
            newValues.put("id", id)
            newValues.put("isDeleted", isDeleted)
            newValues.put("color", color)
            newValues.put("localCreatedAt", localCreatedAt)
            newValues.put("documentModifiedAt", documentModifiedAt)
            newValues.put("remoteData", remoteData)
            newValues.put("document", document)
            newValues.put("media", media)
            newValues.put("createdByApp", createdByApp)

            dbV4.insert("Note", SQLiteDatabase.CONFLICT_REPLACE, newValues)
        }

        val cNotes = dbV4.query("SELECT * FROM Note")
        assertThat(cNotes.count, iz(2))

        val cNewNote = dbV4.query("SELECT * FROM Note WHERE id = 'noteV4'")
        assertThat(cNewNote.count, iz(1))
        assert(cNewNote.moveToFirst())
        assertThat(cNewNote.getLong(cNewNote.getColumnIndex("documentModifiedAt")), iz(12.toLong()))

        dbV4.close()
    }

    @Test
    fun should_migrate_4_to_5() {
        // The change here was that we added the ExternalNote table
        val dbV4 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_4)
        val noteV4 = Note(localId = "noteV4").toPersistenceNote()

        val values = ContentValues()
        with(noteV4) {
            values.put("id", id)
            values.put("isDeleted", isDeleted)
            values.put("color", color)
            values.put("localCreatedAt", localCreatedAt)
            values.put("documentModifiedAt", documentModifiedAt)
            values.put("remoteData", remoteData)
            values.put("document", document)
            values.put("createdByApp", createdByApp)
            values.put("media", media)

            dbV4.insert("Note", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV4.close()
        }

        val dbV5 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_5, true, Migration4To5)
        var c = dbV5.query("SELECT * FROM Note WHERE id = 'noteV4'")
        assertThat(c.count, iz(1))

        c = dbV5.query("SELECT * FROM ExternalNote")
        assertThat(c.count, iz(0))

        dbV5.close()
    }

    // error case handling test
    @Test
    fun migrate_4_to_5_with_ExternalNote_table_present() {
        val dbV4 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_4)
        dbV4.execSQL("CREATE TABLE `ExternalNote` (`id` TEXT NOT NULL, PRIMARY KEY(`id`))")
        val noteReferenceId = "noteV4"

        val values = ContentValues()
        values.put("id", noteReferenceId)
        dbV4.insert("ExternalNote", SQLiteDatabase.CONFLICT_REPLACE, values)
        dbV4.close()

        val dbV5 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_5, true, Migration4To5)
        val c = dbV5.query("SELECT * FROM ExternalNote")
        assertThat(c.count, iz(0)) // table dropped
        dbV5.close()
    }

    @Test
    fun should_migrate_5_to_6_verify_Notes_table_not_affected() {
        // The change here was that we renamed table ExternalNote to NoteReference
        val dbV5 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_5)
        val noteV5 = Note(localId = "noteV5").toPersistenceNote()

        val values = ContentValues()
        with(noteV5) {
            values.put("id", id)
            values.put("isDeleted", isDeleted)
            values.put("color", color)
            values.put("localCreatedAt", localCreatedAt)
            values.put("documentModifiedAt", documentModifiedAt)
            values.put("remoteData", remoteData)
            values.put("document", document)
            values.put("createdByApp", createdByApp)
            values.put("media", media)

            dbV5.insert("Note", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV5.close()
        }

        val dbV6 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_6, true, Migration5To6)
        var c = dbV6.query("SELECT * FROM Note WHERE id = 'noteV5'")
        assertThat(c.count, iz(1))

        c = dbV6.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(0))

        dbV6.close()
    }

    @Test
    fun should_migrate_5_to_6_verify_NoteReference_table_not_affected() {
        val dbV5 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_5)
        val NOTEREFERENCEID = "noteV5"
        val externalNoteV5 = NoteReference(localId = NOTEREFERENCEID).toPersistenceNoteReference()

        val values = ContentValues()
        with(externalNoteV5) {
            values.put("id", id)
            values.put("remoteId", remoteId)
            values.put("type", type)
            values.put("sourceId", "id1")
            values.put("createdAt", createdAt)
            values.put("lastModifiedAt", lastModifiedAt)
            values.put("weight", weight)
            values.put("title", title)
            values.put("previewText", previewText)
            values.put("previewImageUrl", previewImageUrl)
            values.put("color", color)
            values.put("webUrl", webUrl)
            values.put("clientUrl", clientUrl)

            dbV5.insert("ExternalNote", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV5.close()
        }

        val dbV6 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_6, true, Migration5To6)
        val c = dbV6.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(1))
        assert(c.moveToFirst())
        assertThat(c.getString(0), iz(NOTEREFERENCEID))

        dbV6.close()
    }

    // error case handling test
    @Test
    fun migrate_5_to_6_with_NoteReference_table_present() {
        val dbV5 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_5)
        dbV5.execSQL("CREATE TABLE `NoteReference` (`id` TEXT NOT NULL, PRIMARY KEY(`id`))")

        val dbV6 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_6, true, Migration5To6)
        val c = dbV6.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(0)) // table dropped
        dbV6.close()
    }

    @Test
    fun should_migrate_6_to_7() {
        val dbV6 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_6)
        val NOTEREFERENCEID = "noteV6"
        val noteReferenceV6 = NoteReference(localId = NOTEREFERENCEID).toPersistenceNoteReference()

        val values = ContentValues()
        with(noteReferenceV6) {
            values.put("id", id)
            values.put("remoteId", remoteId)
            values.put("type", type)
            values.put("sourceId", "id1")
            values.put("createdAt", createdAt)
            values.put("lastModifiedAt", lastModifiedAt)
            values.put("weight", weight)
            values.put("title", title)
            values.put("previewText", previewText)
            values.put("previewImageUrl", previewImageUrl)
            values.put("color", color)
            values.put("webUrl", webUrl)
            values.put("clientUrl", clientUrl)

            dbV6.insert("NoteReference", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV6.close()
        }

        val dbV7 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_7, true, Migration6To7)
        val c = dbV7.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(1))
        assert(c.moveToFirst())
        assertThat(c.getString(0), iz(NOTEREFERENCEID))

        dbV7.close()
    }

    @Test
    fun should_migrate_6_to_7_verify_Note_table_not_affected() {
        val dbV6 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_6)
        val noteId = "noteV6"
        val noteV6 = Note(localId = noteId).toPersistenceNote()

        val values = ContentValues()
        with(noteV6) {
            values.put("id", id)
            values.put("isDeleted", isDeleted)
            values.put("color", color)
            values.put("localCreatedAt", localCreatedAt)
            values.put("documentModifiedAt", documentModifiedAt)
            values.put("remoteData", remoteData)
            values.put("document", document)
            values.put("createdByApp", createdByApp)
            values.put("media", media)

            dbV6.insert("Note", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV6.close()
        }

        val dbV7 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_7, true, Migration6To7)
        val c = dbV7.query("SELECT * FROM Note")
        assertThat(c.count, iz(1))
        assert(c.moveToFirst())
        assertThat(c.getString(0), iz(noteId))

        dbV7.close()
    }

    @Test
    fun should_migrate_7_to_8() {
        // The change here was that we added the field "isMediaPresent"
        // and alter 'color' column data type from Int to Text in NoteReference table
        val dbV7 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_7)
        val NOTEREFERENCEID = "noteV7"
        val noteReferenceV7 = NoteReference(localId = NOTEREFERENCEID).toPersistenceNoteReference()

        val values = ContentValues()
        with(noteReferenceV7) {
            values.put("id", id)
            values.put("remoteId", remoteId)
            values.put("type", type)
            values.put("pageSourceId", "id1")
            values.put("isLocalOnlyPage", isLocalOnlyPage)
            values.put("isDeleted", isDeleted)
            values.put("createdAt", createdAt)
            values.put("lastModifiedAt", lastModifiedAt)
            values.put("weight", weight)
            values.put("title", title)
            values.put("previewText", previewText)
            values.put("previewImageUrl", previewImageUrl)
            values.put("color", color)
            values.put("webUrl", webUrl)
            values.put("clientUrl", clientUrl)

            dbV7.insert("NoteReference", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV7.close()
        }

        val dbV8 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_8, true, Migration7To8)
        var c = dbV8.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(1))
        assert(c.moveToFirst())
        assertThat(c.getString(0), iz(NOTEREFERENCEID))

        val noteReferenceV8 = NoteReference(localId = "noteV8").toPersistenceNoteReference()

        val newValues = ContentValues()
        with(noteReferenceV8) {
            newValues.put("id", id)
            newValues.put("remoteId", remoteId)
            newValues.put("type", type)
            newValues.put("pageSourceId", "id1")
            newValues.put("isLocalOnlyPage", isLocalOnlyPage)
            newValues.put("isDeleted", isDeleted)
            newValues.put("createdAt", createdAt)
            newValues.put("lastModifiedAt", lastModifiedAt)
            newValues.put("weight", weight)
            newValues.put("title", title)
            newValues.put("previewText", previewText)
            newValues.put("previewImageUrl", previewImageUrl)
            newValues.put("color", color)
            newValues.put("webUrl", webUrl)
            newValues.put("clientUrl", clientUrl)
            newValues.put("isMediaPresent", isMediaPresent)
            newValues.put("previewRichText", previewRichText)

            dbV8.insert("NoteReference", SQLiteDatabase.CONFLICT_REPLACE, newValues)
        }

        c = dbV8.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(2))

        c = dbV8.query("SELECT * FROM NoteReference WHERE id = 'noteV8'")
        assertThat(c.count, iz(1))

        dbV8.close()
    }

    @Test
    fun should_migrate_7_to_8_verify_Notes_table_not_affected() {
        val dbV7 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_7)
        val noteV7 = Note(localId = "noteV7").toPersistenceNote()

        val values = ContentValues()
        with(noteV7) {
            values.put("id", id)
            values.put("isDeleted", isDeleted)
            values.put("color", color)
            values.put("localCreatedAt", localCreatedAt)
            values.put("documentModifiedAt", documentModifiedAt)
            values.put("remoteData", remoteData)
            values.put("document", document)
            values.put("createdByApp", createdByApp)
            values.put("media", media)

            dbV7.insert("Note", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV7.close()
        }

        val dbV8 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_8, true, Migration7To8)
        var c = dbV8.query("SELECT * FROM Note WHERE id = 'noteV7'")
        assertThat(c.count, iz(1))

        c = dbV8.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(0))

        dbV8.close()
    }

    @Test
    fun should_migrate_8_to_9() {
        // The change here was that we added the field "rootContainerSourceId" in NoteReference table
        val dbV8 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_8)
        val NOTEREFERENCEID = "noteV8"
        val noteReferenceV8 = NoteReference(localId = NOTEREFERENCEID).toPersistenceNoteReference()

        val values = ContentValues()
        with(noteReferenceV8) {
            values.put("remoteId", remoteId)
            values.put("id", id)
            values.put("type", type)
            values.put("pageSourceId", "id1")
            values.put("isLocalOnlyPage", isLocalOnlyPage)
            values.put("isDeleted", isDeleted)
            values.put("createdAt", createdAt)
            values.put("lastModifiedAt", lastModifiedAt)
            values.put("weight", weight)
            values.put("title", title)
            values.put("previewText", previewText)
            values.put("previewImageUrl", previewImageUrl)
            values.put("color", color)
            values.put("webUrl", webUrl)
            values.put("clientUrl", clientUrl)
            values.put("isMediaPresent", isMediaPresent)
            values.put("previewRichText", previewRichText)

            dbV8.insert("NoteReference", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV8.close()
        }

        val dbV9 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_9, true, Migration8To9)
        var c = dbV9.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(1))
        assert(c.moveToFirst())
        assertThat(c.getString(0), iz(NOTEREFERENCEID))

        val noteReferenceV9 = NoteReference(localId = "noteV9").toPersistenceNoteReference()

        val newValues = ContentValues()
        with(noteReferenceV9) {
            newValues.put("id", id)
            newValues.put("remoteId", remoteId)
            newValues.put("type", type)
            newValues.put("pageSourceId", "id1")
            newValues.put("isLocalOnlyPage", isLocalOnlyPage)
            newValues.put("isDeleted", isDeleted)
            newValues.put("createdAt", createdAt)
            newValues.put("lastModifiedAt", lastModifiedAt)
            newValues.put("weight", weight)
            newValues.put("title", title)
            newValues.put("previewText", previewText)
            newValues.put("previewImageUrl", previewImageUrl)
            newValues.put("color", color)
            newValues.put("webUrl", webUrl)
            newValues.put("clientUrl", clientUrl)
            newValues.put("rootContainerSourceId", rootContainerSourceId)
            newValues.put("isMediaPresent", isMediaPresent)
            newValues.put("previewRichText", previewRichText)

            dbV9.insert("NoteReference", SQLiteDatabase.CONFLICT_REPLACE, newValues)
        }

        c = dbV9.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(2))

        c = dbV9.query("SELECT * FROM NoteReference WHERE id = 'noteV9'")
        assertThat(c.count, iz(1))

        dbV9.close()
    }

    @Test
    fun should_migrate_8_to_9_verify_Notes_table_not_affected() {
        val dbV8 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_8)
        val noteV8 = Note(localId = "noteV8").toPersistenceNote()

        val values = ContentValues()
        with(noteV8) {
            values.put("id", id)
            values.put("isDeleted", isDeleted)
            values.put("color", color)
            values.put("localCreatedAt", localCreatedAt)
            values.put("documentModifiedAt", documentModifiedAt)
            values.put("remoteData", remoteData)
            values.put("document", document)
            values.put("createdByApp", createdByApp)
            values.put("media", media)

            dbV8.insert("Note", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV8.close()
        }

        val dbV9 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_9, true, Migration8To9)
        var c = dbV9.query("SELECT * FROM Note WHERE id = 'noteV8'")
        assertThat(c.count, iz(1))

        c = dbV9.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(0))

        dbV9.close()
    }

    // error case handling test
    @Test
    fun migrate_9_to_10_with_MeetingNote_table_present() {
        val dbV9 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_9)
        dbV9.execSQL("CREATE TABLE 'MeetingNote' ('localId' TEXT NOT NULL, PRIMARY KEY('localId'))")

        val dbV10 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_10, true, Migration9To10)
        val c = dbV10.query("SELECT * FROM MeetingNote")
        assertThat(c.count, iz(0))
        dbV10.close()
    }

    @Test
    fun should_migrate_9_to_10_verify_Notes_table_not_affected() {
        // We added the Meeting Notes table in this migration
        val dbV9 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_9)
        val noteV9 = Note(localId = "noteV9").toPersistenceNote()

        val values = ContentValues()
        with(noteV9) {
            values.put("id", id)
            values.put("isDeleted", isDeleted)
            values.put("color", color)
            values.put("localCreatedAt", localCreatedAt)
            values.put("documentModifiedAt", documentModifiedAt)
            values.put("remoteData", remoteData)
            values.put("document", document)
            values.put("createdByApp", createdByApp)
            values.put("media", media)

            dbV9.insert("Note", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV9.close()
        }

        val dbV10 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_10, true, Migration9To10)
        var c = dbV10.query("SELECT * FROM Note WHERE id = 'noteV9'")
        assertThat(c.count, iz(1))

        c = dbV10.query("SELECT * FROM MeetingNote")
        assertThat(c.count, iz(0))

        dbV10.close()
    }

    @Test
    fun should_migrate_9_to_10_verify_Notes_Reference_table_not_affected() {
        // We added the Meeting Notes table in this migration
        val dbV9 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_9)
        val noteReferenceV9 = NoteReference(localId = "noteReferenceV9").toPersistenceNoteReference()

        val newValues = ContentValues()
        with(noteReferenceV9) {
            newValues.put("id", id)
            newValues.put("remoteId", remoteId)
            newValues.put("type", type)
            newValues.put("pageSourceId", "id1")
            newValues.put("isLocalOnlyPage", isLocalOnlyPage)
            newValues.put("isDeleted", isDeleted)
            newValues.put("createdAt", createdAt)
            newValues.put("lastModifiedAt", lastModifiedAt)
            newValues.put("weight", weight)
            newValues.put("title", title)
            newValues.put("previewText", previewText)
            newValues.put("previewImageUrl", previewImageUrl)
            newValues.put("color", color)
            newValues.put("webUrl", webUrl)
            newValues.put("clientUrl", clientUrl)
            newValues.put("rootContainerSourceId", rootContainerSourceId)
            newValues.put("isMediaPresent", isMediaPresent)
            newValues.put("previewRichText", previewRichText)

            dbV9.insert("NoteReference", SQLiteDatabase.CONFLICT_REPLACE, newValues)
        }

        val dbV10 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_10, true, Migration9To10)
        var c = dbV10.query("SELECT * FROM NoteReference WHERE id = 'noteReferenceV9'")
        assertThat(c.count, iz(1))

        c = dbV10.query("SELECT * FROM MeetingNote")
        assertThat(c.count, iz(0))

        dbV10.close()
    }

    @Test
    fun should_migrate_10_to_11() {
        // The change here was that we added the field "isPinned" in NoteReference table
        val dbV10 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_10)
        val NOTEREFERENCEID = "noteV10"
        val noteReferenceV10 = NoteReference(localId = NOTEREFERENCEID).toPersistenceNoteReference()

        val values = ContentValues()
        with(noteReferenceV10) {
            values.put("remoteId", remoteId)
            values.put("id", id)
            values.put("type", type)
            values.put("pageSourceId", "id1")
            values.put("isLocalOnlyPage", isLocalOnlyPage)
            values.put("isDeleted", isDeleted)
            values.put("createdAt", createdAt)
            values.put("lastModifiedAt", lastModifiedAt)
            values.put("weight", weight)
            values.put("title", title)
            values.put("previewText", previewText)
            values.put("previewImageUrl", previewImageUrl)
            values.put("color", color)
            values.put("webUrl", webUrl)
            values.put("clientUrl", clientUrl)
            values.put("rootContainerSourceId", rootContainerSourceId)
            values.put("isMediaPresent", isMediaPresent)
            values.put("previewRichText", previewRichText)

            dbV10.insert("NoteReference", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV10.close()
        }

        val dbV11 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_11, true, Migration10To11)
        var c = dbV11.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(1))
        assert(c.moveToFirst())
        assertThat(c.getString(0), iz(NOTEREFERENCEID))

        val noteReferenceV11 = NoteReference(localId = "noteV11").toPersistenceNoteReference()

        val newValues = ContentValues()
        with(noteReferenceV11) {
            newValues.put("id", id)
            newValues.put("remoteId", remoteId)
            newValues.put("type", type)
            newValues.put("pageSourceId", "id1")
            newValues.put("isLocalOnlyPage", isLocalOnlyPage)
            newValues.put("isDeleted", isDeleted)
            newValues.put("createdAt", createdAt)
            newValues.put("lastModifiedAt", lastModifiedAt)
            newValues.put("weight", weight)
            newValues.put("title", title)
            newValues.put("previewText", previewText)
            newValues.put("previewImageUrl", previewImageUrl)
            newValues.put("color", color)
            newValues.put("webUrl", webUrl)
            newValues.put("clientUrl", clientUrl)
            newValues.put("rootContainerSourceId", rootContainerSourceId)
            newValues.put("isMediaPresent", isMediaPresent)
            newValues.put("previewRichText", previewRichText)
            newValues.put("isPinned", isPinned)
            dbV11.insert("NoteReference", SQLiteDatabase.CONFLICT_REPLACE, newValues)
        }

        c = dbV11.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(2))

        c = dbV11.query("SELECT * FROM NoteReference WHERE id = 'noteV11'")
        assertThat(c.count, iz(1))

        dbV11.close()
    }

    @Test
    fun should_migrate_10_to_11_verify_Notes_table_not_affected() {
        val dbV10 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_10)
        val noteV10 = Note(localId = "noteV10").toPersistenceNote()

        val values = ContentValues()
        with(noteV10) {
            values.put("id", id)
            values.put("isDeleted", isDeleted)
            values.put("color", color)
            values.put("localCreatedAt", localCreatedAt)
            values.put("documentModifiedAt", documentModifiedAt)
            values.put("remoteData", remoteData)
            values.put("document", document)
            values.put("createdByApp", createdByApp)
            values.put("media", media)

            dbV10.insert("Note", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV10.close()
        }
        val dbV11 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_11, true, Migration10To11)
        var c = dbV11.query("SELECT * FROM Note WHERE id = 'noteV10'")
        assertThat(c.count, iz(1))

        c = dbV11.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(0))

        dbV11.close()
    }

    @Test
    fun should_migrate_11_to_12() {
        // The change here was that we added the field "pinnedAt" in NoteReference table
        val dbV11 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_11)
        val NOTEREFERENCEID = "noteV11"
        val noteReferenceV11 = NoteReference(localId = NOTEREFERENCEID).toPersistenceNoteReference()

        val values = ContentValues()
        with(noteReferenceV11) {
            values.put("remoteId", remoteId)
            values.put("id", id)
            values.put("type", type)
            values.put("pageSourceId", "id1")
            values.put("isLocalOnlyPage", isLocalOnlyPage)
            values.put("isDeleted", isDeleted)
            values.put("createdAt", createdAt)
            values.put("lastModifiedAt", lastModifiedAt)
            values.put("weight", weight)
            values.put("title", title)
            values.put("previewText", previewText)
            values.put("previewImageUrl", previewImageUrl)
            values.put("color", color)
            values.put("webUrl", webUrl)
            values.put("clientUrl", clientUrl)
            values.put("rootContainerSourceId", rootContainerSourceId)
            values.put("isMediaPresent", isMediaPresent)
            values.put("previewRichText", previewRichText)
            values.put("isPinned", isPinned)

            dbV11.insert("NoteReference", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV11.close()
        }

        val dbV12 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_12, true, Migration11To12)
        var c = dbV12.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(1))
        assert(c.moveToFirst())
        assertThat(c.getString(0), iz(NOTEREFERENCEID))

        val noteReferenceV12 = NoteReference(localId = "noteV12").toPersistenceNoteReference()

        val newValues = ContentValues()
        with(noteReferenceV12) {
            newValues.put("id", id)
            newValues.put("remoteId", remoteId)
            newValues.put("type", type)
            newValues.put("pageSourceId", "id1")
            newValues.put("isLocalOnlyPage", isLocalOnlyPage)
            newValues.put("isDeleted", isDeleted)
            newValues.put("createdAt", createdAt)
            newValues.put("lastModifiedAt", lastModifiedAt)
            newValues.put("weight", weight)
            newValues.put("title", title)
            newValues.put("previewText", previewText)
            newValues.put("previewImageUrl", previewImageUrl)
            newValues.put("color", color)
            newValues.put("webUrl", webUrl)
            newValues.put("clientUrl", clientUrl)
            newValues.put("rootContainerSourceId", rootContainerSourceId)
            newValues.put("isMediaPresent", isMediaPresent)
            newValues.put("previewRichText", previewRichText)
            newValues.put("isPinned", isPinned)
            newValues.put("pinnedAt", pinnedAt)
            dbV12.insert("NoteReference", SQLiteDatabase.CONFLICT_REPLACE, newValues)
        }

        c = dbV12.query("SELECT * FROM NoteReference")
        assertThat(c.count, iz(2))

        c = dbV12.query("SELECT * FROM NoteReference WHERE id = 'noteV12'")
        assertThat(c.count, iz(1))

        dbV12.close()
    }

    @Test
    fun should_migrate_11_to_12_verify_Notes_table_not_affected() {
        // The change here was that the fields pinnedAt and isPinned are added
        val dbV11 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_11)
        val noteV11 = Note(localId = "noteV11").toPersistenceNote()

        val values = ContentValues()
        with(noteV11) {
            values.put("id", id)
            values.put("isDeleted", isDeleted)
            values.put("color", color)
            values.put("localCreatedAt", localCreatedAt)
            values.put("documentModifiedAt", documentModifiedAt)
            values.put("remoteData", remoteData)
            values.put("document", document)
            values.put("createdByApp", createdByApp)
            values.put("media", media)

            dbV11.insert("Note", SQLiteDatabase.CONFLICT_REPLACE, values)
            dbV11.close()
        }
        val dbV12 = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, VERSION_12, true, Migration11To12)
        var c = dbV12.query("SELECT * FROM Note")
        assertThat(c.count, iz(1))
        assert(c.moveToFirst())
        assertThat(c.getString(0), iz("noteV11"))

        val noteV12 = Note(localId = "noteV12").toPersistenceNote()

        val newValues = ContentValues()
        with(noteV12) {
            newValues.put("id", id)
            newValues.put("isDeleted", isDeleted)
            newValues.put("color", color)
            newValues.put("localCreatedAt", localCreatedAt)
            newValues.put("documentModifiedAt", documentModifiedAt)
            newValues.put("remoteData", remoteData)
            newValues.put("document", document)
            newValues.put("createdByApp", createdByApp)
            newValues.put("media", media)
            newValues.put("isPinned", isPinned)
            newValues.put("pinnedAt", pinnedAt)
            dbV12.insert("Note", SQLiteDatabase.CONFLICT_REPLACE, newValues)
        }

        c = dbV12.query("SELECT * FROM Note")
        assertThat(c.count, iz(2))

        c = dbV12.query("SELECT * FROM Note WHERE id = 'noteV12'")
        assertThat(c.count, iz(1))

        dbV12.close()
    }

    @Test
    fun should_migrate_all() {
        val dbV1 = migrationTestHelper.createDatabase(TEST_DB_NAME, VERSION_1)
        dbV1.close()

        migrationTestHelper.runMigrationsAndValidate(
            TEST_DB_NAME, VERSION_8, true,
            Migration1To2,
            Migration2To3,
            Migration3To4,
            Migration4To5,
            Migration5To6,
            Migration6To7,
            Migration7To8,
            Migration8To9,
            Migration9To10,
            Migration10To11,
            Migration11To12
        )
    }
}
