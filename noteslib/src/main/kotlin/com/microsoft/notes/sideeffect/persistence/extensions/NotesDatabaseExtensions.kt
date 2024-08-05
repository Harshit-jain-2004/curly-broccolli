package com.microsoft.notes.sideeffect.persistence.extensions

import com.microsoft.notes.sideeffect.persistence.NotesDatabase

fun NotesDatabase.deleteAll() {
    preferencesDao().deleteAll()
    noteReferenceDao().deleteAll()
    noteDao().deleteAll()
}
