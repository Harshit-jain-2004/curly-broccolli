package com.microsoft.notes.sideeffect.persistence.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val VERSION_FROM = 1
private const val VERSION_TO = 2

object Migration1To2 : Migration(VERSION_FROM, VERSION_TO) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `Preference`" +
                " (`id` TEXT NOT NULL, `value` TEXT, PRIMARY KEY(`id`))"
        )
    }
}
