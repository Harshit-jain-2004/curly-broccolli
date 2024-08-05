package com.microsoft.notes.sideeffect.persistence.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val VERSION_FROM = 10
private const val VERSION_TO = 11

object Migration10To11 : Migration(VERSION_FROM, VERSION_TO) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE NoteReference ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
    }
}
