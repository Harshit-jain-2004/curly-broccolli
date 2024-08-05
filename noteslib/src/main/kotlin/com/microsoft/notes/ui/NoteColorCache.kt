package com.microsoft.notes.ui

import android.content.Context
import com.microsoft.notes.models.Color
import com.microsoft.notes.utils.utils.Constants.NOTE_COLOR_PREFERENCES

interface NoteColorCache {
    fun getNoteColorPreference(): Color?
    fun setNoteColorPreference(color: Color)
}

class NoteColorSharedPreferences(
    private val context: Context
) : NoteColorCache {
    companion object {
        const val NULL_COLOR = -1
    }

    override fun getNoteColorPreference(): Color? {
        val sharedPrefs = context.getSharedPreferences(NOTE_COLOR_PREFERENCES, Context.MODE_PRIVATE)
        val colorInt = sharedPrefs.getInt(PREFS_KEY, NULL_COLOR)
        if (colorInt == NULL_COLOR || colorInt < 0) return null
        return Color.values().firstOrNull { it.value == colorInt }
    }

    override fun setNoteColorPreference(color: Color) {
        val sharedPrefs = context.getSharedPreferences(NOTE_COLOR_PREFERENCES, Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putInt(PREFS_KEY, color.value)
            .apply()
    }
}

const val PREFS_KEY = "preferredColor"
