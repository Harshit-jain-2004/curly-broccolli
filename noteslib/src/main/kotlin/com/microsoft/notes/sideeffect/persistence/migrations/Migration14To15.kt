package com.microsoft.notes.sideeffect.persistence.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val VERSION_FROM = 14
private const val VERSION_TO = 15

object Migration14To15 : Migration(VERSION_FROM, VERSION_TO) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Note ADD COLUMN reminder TEXT")
    }
}
