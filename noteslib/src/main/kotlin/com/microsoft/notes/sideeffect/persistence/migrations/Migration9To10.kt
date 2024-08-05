package com.microsoft.notes.sideeffect.persistence.migrations

import android.database.sqlite.SQLiteException
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val VERSION_FROM = 9
private const val VERSION_TO = 10

object Migration9To10 : Migration(VERSION_FROM, VERSION_TO) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            createMeetingNoteTable(db)
        } catch (ex: SQLiteException) {
            db.execSQL("DROP TABLE IF EXISTS `MeetingNote`")
            createMeetingNoteTable(db)
        }
    }

    private fun createMeetingNoteTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `MeetingNote` (" +
                "`localId` TEXT NOT NULL, " +
                "`remoteId` TEXT NOT NULL, " +
                "`fileName` TEXT NOT NULL, " +
                "`createdTime` INTEGER NOT NULL, " +
                "`lastModifiedTime` INTEGER NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`staticTeaser` TEXT NOT NULL, " +
                "`accessUrl` TEXT NOT NULL, " +
                "`containerUrl` TEXT NOT NULL, " +
                "`containerTitle` TEXT NOT NULL, " +
                "`docId` INTEGER NOT NULL, " +
                "`fileUrl` TEXT NOT NULL, " +
                "`driveId` TEXT NOT NULL, " +
                "`itemId` TEXT NOT NULL, " +
                "`modifiedBy` TEXT NOT NULL, " +
                "`modifiedByDisplayName` TEXT NOT NULL, " +
                "`meetingId` TEXT, " +
                "`meetingSubject` TEXT, " +
                "`meetingStartTime` INTEGER, " +
                "`meetingEndTime` INTEGER, " +
                "`meetingOrganizer` TEXT, " +
                "`seriesMasterId` TEXT, " +
                "`occuranceId` TEXT, " +
                "PRIMARY KEY(`localId`))"
        )
    }
}
