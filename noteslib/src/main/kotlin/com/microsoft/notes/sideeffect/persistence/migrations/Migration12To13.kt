package com.microsoft.notes.sideeffect.persistence.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val VERSION_FROM = 12
private const val VERSION_TO = 13

object Migration12To13 : Migration(VERSION_FROM, VERSION_TO) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE NoteReference ADD COLUMN media TEXT")
    }
}
