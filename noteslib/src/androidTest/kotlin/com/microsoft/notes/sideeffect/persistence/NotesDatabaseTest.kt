package com.microsoft.notes.sideeffect.persistence

import android.content.Context
import androidx.room.Room

fun createTestNotesDB(context: Context): NotesDatabase =
    Room.inMemoryDatabaseBuilder(context, NotesDatabase::class.java).build()
