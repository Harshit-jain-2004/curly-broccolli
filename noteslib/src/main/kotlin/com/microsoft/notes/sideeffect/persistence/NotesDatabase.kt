package com.microsoft.notes.sideeffect.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.microsoft.notes.sideeffect.persistence.dao.MeetingNoteDao
import com.microsoft.notes.sideeffect.persistence.dao.NoteDao
import com.microsoft.notes.sideeffect.persistence.dao.NoteReferenceDao
import com.microsoft.notes.sideeffect.persistence.dao.PreferencesDao
import com.microsoft.notes.sideeffect.persistence.migrations.Migration10To11
import com.microsoft.notes.sideeffect.persistence.migrations.Migration11To12
import com.microsoft.notes.sideeffect.persistence.migrations.Migration12To13
import com.microsoft.notes.sideeffect.persistence.migrations.Migration13To14
import com.microsoft.notes.sideeffect.persistence.migrations.Migration14To15
import com.microsoft.notes.sideeffect.persistence.migrations.Migration1To2
import com.microsoft.notes.sideeffect.persistence.migrations.Migration2To3
import com.microsoft.notes.sideeffect.persistence.migrations.Migration3To4
import com.microsoft.notes.sideeffect.persistence.migrations.Migration4To5
import com.microsoft.notes.sideeffect.persistence.migrations.Migration5To6
import com.microsoft.notes.sideeffect.persistence.migrations.Migration6To7
import com.microsoft.notes.sideeffect.persistence.migrations.Migration7To8
import com.microsoft.notes.sideeffect.persistence.migrations.Migration8To9
import com.microsoft.notes.sideeffect.persistence.migrations.Migration9To10

fun createNotesDB(
    context: Context,
    dbName: String
): NotesDatabase =
    Room.databaseBuilder(context, NotesDatabase::class.java, dbName)
        .addMigrations(
            Migration1To2, Migration2To3, Migration3To4, Migration4To5, Migration5To6,
            Migration6To7, Migration7To8, Migration8To9, Migration9To10, Migration10To11, Migration11To12, Migration12To13,
            Migration13To14, Migration14To15
        )
        .fallbackToDestructiveMigration()
        .build()

@Database(version = 15, entities = arrayOf(Note::class, NoteReference::class, Preference::class, MeetingNote::class))
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun noteReferenceDao(): NoteReferenceDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun meetingNoteDao(): MeetingNoteDao
}
