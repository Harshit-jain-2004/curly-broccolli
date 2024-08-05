package com.microsoft.notes.models.extensions

import com.microsoft.notes.models.MeetingNote
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.NoteReference

inline fun <T> Iterable<T>.firstOrDefault(predicate: (T) -> Boolean, default: T): T {
    this.forEach { if (predicate(it)) return it }
    return default
}

inline fun <T> Iterable<T>.findAndMap(find: (T) -> Boolean, map: (T) -> T): List<T> =
    map { if (find(it)) map(it) else it }

private const val FORMATTING = ":\t"

fun List<Note>.describe(): String {
    val bldr = StringBuilder()
    bldr.append("NotesList:: Count : $size")
    this.forEach {
        bldr.append("$FORMATTING ${it.localId}")
    }
    return bldr.toString()
}

fun List<NoteReference>.describeNoteReferencesList(): String {
    val bldr = StringBuilder()
    bldr.append("NotesList:: Count : $size")
    this.forEach {
        bldr.append("$FORMATTING ${it.localId}")
    }
    return bldr.toString()
}

fun List<MeetingNote>.describeMeetingNotesList(): String {
    val bldr = StringBuilder()
    bldr.append("MeetingNotesList:: Count : $size")
    this.forEach {
        bldr.append("$FORMATTING ${it.localId}")
    }
    return bldr.toString()
}
