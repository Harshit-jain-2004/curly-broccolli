package com.microsoft.notes.sideeffect.persistence.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val VERSION_FROM = 11
private const val VERSION_TO = 12

object Migration11To12 : Migration(VERSION_FROM, VERSION_TO) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Note ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE NoteReference ADD COLUMN pinnedAt INTEGER")
        db.execSQL("ALTER TABLE Note ADD COLUMN pinnedAt INTEGER")
    }
}
