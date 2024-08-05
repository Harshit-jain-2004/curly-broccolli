package com.microsoft.notes.sideeffect.persistence.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val VERSION_FROM = 6
private const val VERSION_TO = 7

object Migration6To7 : Migration(VERSION_FROM, VERSION_TO) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `Backup` (" +
                "`id` TEXT NOT NULL, " +
                "`remoteId` TEXT, " +
                "`type` TEXT NOT NULL, " +
                "`pageSourceId` TEXT, " +
                "`pagePartialSourceId` TEXT, " +
                "`pageLocalId` TEXT, " +
                "`sectionSourceId` TEXT, " +
                "`sectionLocalId` TEXT, " +
                "`isLocalOnlyPage` INTEGER NOT NULL, " +
                "`isDeleted` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`lastModifiedAt` INTEGER NOT NULL, " +
                "`weight` REAL, " +
                "`title` TEXT, " +
                "`previewText` TEXT NOT NULL, " +
                "`previewImageUrl` TEXT, " +
                "`color` INTEGER, " +
                "`notebookUrl` TEXT, " +
                "`webUrl` TEXT, " +
                "`clientUrl` TEXT, " +
                "`containerName` TEXT, " +
                "`rootContainerName` TEXT, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT INTO Backup " +
                "(`id`, `remoteId`, `type`, `pageSourceId`, `createdAt`, `lastModifiedAt`, `weight`, " +
                "`title`, `previewText`, `previewImageUrl`, `color`, `notebookUrl`, `webUrl`, `clientUrl`, " +
                "`containerName`, `rootContainerName`, `pagePartialSourceId`, `pageLocalId`," +
                " `sectionSourceId`, `sectionLocalId`, `isLocalOnlyPage`, `isDeleted`) " +
                "SELECT id, " +
                "remoteId, " +
                "type, " +
                "sourceId, " +
                "createdAt, " +
                "lastModifiedAt, " +
                "weight, " +
                "title, " +
                "previewText, " +
                "previewImageUrl, " +
                "color, " +
                "null, " +
                "webUrl, " +
                "clientUrl, " +
                "containerName, " +
                "rootContainerName, null, null, null, null, 0, 0 " +
                "FROM NoteReference"
        )
        db.execSQL("DROP TABLE NoteReference")
        db.execSQL("ALTER TABLE Backup RENAME TO NoteReference")
        db.execSQL("ALTER TABLE Note ADD title TEXT")
    }
}
