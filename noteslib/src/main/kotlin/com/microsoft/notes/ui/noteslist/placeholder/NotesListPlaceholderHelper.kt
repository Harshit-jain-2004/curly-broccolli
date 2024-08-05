package com.microsoft.notes.ui.noteslist.placeholder

import android.text.SpannableString
import com.microsoft.notes.ui.noteslist.NotesListComponent

/**
 * This class is used to hold placeholder values in fragments, it helps with reducing code duplication as
 * multiple fragments need same empty state handling.
 */
class NotesListPlaceholderHelper {
    private var notesList: NotesListComponent? = null

    private var image: Int? = null
    private var imageContentDescription: String? = null
    private var title: SpannableString? = null
    private var titleContentDescription: String? = null
    private var titleStyle: Int? = null
    private var subtitle: SpannableString? = null
    private var subtitleContentDescription: String? = null
    private var subtitleStyle: Int? = null

    fun setNotesList(notesList: NotesListComponent) {
        this.notesList = notesList
        setPlaceholder()
    }

    fun setPlaceholder(
        image: Int? = null,
        imageContentDescription: String? = null,
        title: SpannableString? = null,
        titleContentDescription: String? = null,
        titleStyle: Int? = null,
        subtitle: SpannableString? = null,
        subtitleContentDescription: String? = null,
        subtitleStyle: Int? = null
    ) {
        this.image = image
        this.imageContentDescription = imageContentDescription
        this.title = title
        this.titleContentDescription = titleContentDescription
        this.titleStyle = titleStyle
        this.subtitle = subtitle
        this.subtitleContentDescription = subtitleContentDescription
        this.subtitleStyle = subtitleStyle
        setPlaceholder()
    }

    private fun setPlaceholder() {
        notesList?.setPlaceholder(
            image, imageContentDescription,
            title, titleContentDescription, titleStyle,
            subtitle, subtitleContentDescription, subtitleStyle
        )
    }
}
