package com.microsoft.notes.sideeffect.persistence.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val VERSION_FROM = 2
private const val VERSION_TO = 3

object Migration2To3 : Migration(VERSION_FROM, VERSION_TO) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TEMPORARY TABLE `Backup` (" +
                "`id` TEXT NOT NULL, " +
                "`isDeleted` INTEGER NOT NULL, " +
                "`color` INTEGER NOT NULL, " +
                "`localCreatedAt` INTEGER NOT NULL, " +
                "`documentModifiedAt` INTEGER NOT NULL, " +
                "`remoteData` TEXT, " +
                "`document` TEXT NOT NULL, " +
                "`createdByApp` TEXT, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT INTO Backup " +
                "SELECT id, " +
                "isDeleted, " +
                "color, " +
                "localCreatedAt, " +
                "localLastModifiedAt, " +
                "remoteData, " +
                "document, " +
                "createdByApp " +
                "FROM Note"
        )
        db.execSQL("DROP TABLE Note")
        db.execSQL(
            "CREATE TABLE `Note` (" +
                "`id` TEXT NOT NULL, " +
                "`isDeleted` INTEGER NOT NULL, " +
                "`color` INTEGER NOT NULL, " +
                "`localCreatedAt` INTEGER NOT NULL, " +
                "`documentModifiedAt` INTEGER NOT NULL, " +
                "`remoteData` TEXT, " +
                "`document` TEXT NOT NULL, " +
                "`createdByApp` TEXT, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT INTO Note " +
                "SELECT id, " +
                "isDeleted, " +
                "color, " +
                "localCreatedAt, " +
                "documentModifiedAt, " +
                "remoteData, " +
                "document, " +
                "createdByApp " +
                "FROM Backup"
        )
        db.execSQL("DROP TABLE Backup")
    }
}
