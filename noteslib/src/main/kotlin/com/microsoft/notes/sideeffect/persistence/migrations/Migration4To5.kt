package com.microsoft.notes.sideeffect.persistence.migrations

import android.database.sqlite.SQLiteException
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val VERSION_FROM = 4
private const val VERSION_TO = 5

object Migration4To5 : Migration(VERSION_FROM, VERSION_TO) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            createExternalNoteTable(db)
        } catch (ex: SQLiteException) {
            /* ideally this table should not exist but adding this due to crashes */
            db.execSQL("DROP TABLE IF EXISTS `ExternalNote`") // dropping table to prevent corrupt data
            createExternalNoteTable(db)
        }
    }

    private fun createExternalNoteTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `ExternalNote` (" +
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
