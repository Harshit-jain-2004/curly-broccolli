package com.microsoft.notes.sideeffect.persistence.migrations

import android.database.sqlite.SQLiteException
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val VERSION_FROM = 5
private const val VERSION_TO = 6

object Migration5To6 : Migration(VERSION_FROM, VERSION_TO) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE ExternalNote RENAME TO NoteReference")
        } catch (ex: SQLiteException) {
            db.execSQL("DROP TABLE IF EXISTS `ExternalNote`")
            /* ideally this table should not exist but adding this due to crashes */
            db.execSQL("DROP TABLE IF EXISTS `NoteReference`")
            createNoteReferenceTable(db)
        }
    }

    private fun createNoteReferenceTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `NoteReference` (" +
                "`id` TEXT NOT NULL, " +
                "`remoteId` TEXT, " +
                "`type` TEXT NOT NULL, " +
                "`sourceId` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`lastModifiedAt` INTEGER NOT NULL, " +
                "`weight` REAL, " +
                "`title` TEXT, " +
                "`previewText` TEXT NOT NULL, " +
                "`previewImageUrl` TEXT, " +
                "`color` INTEGER, " +
                "`webUrl` TEXT, " +
                "`clientUrl` TEXT, " +
                "`containerName` TEXT, " +
                "`rootContainerName` TEXT, " +
                "PRIMARY KEY(`id`))"
        )
    }
}
