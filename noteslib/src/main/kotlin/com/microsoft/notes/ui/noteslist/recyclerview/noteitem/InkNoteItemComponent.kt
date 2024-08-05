package com.microsoft.notes.ui.noteslist.recyclerview.noteitem

import android.content.Context
import android.util.AttributeSet
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.styled.toInkContextColor
import com.microsoft.notes.ui.note.ink.InkView

open class InkNoteItemComponent(context: Context, attrs: AttributeSet) : NoteItemComponent(context, attrs) {
    protected open val inkView: InkView? by lazy { findViewById<InkView?>(R.id.ink) }

    override fun bindNote(
        note: Note,
        keywordsToHighlight: List<String>?,
        isSelectionEnabled: Boolean,
        isItemSelected: Boolean,
        showDateTime: Boolean,
        showSource: Boolean,
        showSourceText: Boolean,
        isFeedUiRefreshEnabled: Boolean
    ) {
        super.bindNote(
            note, keywordsToHighlight, isSelectionEnabled, isItemSelected, showDateTime,
            showSource, showSourceText, isFeedUiRefreshEnabled
        )

        inkView?.setDocumentAndUpdateScaleFactor(note.document)
        applyInkColor(note)
    }

    override fun applyTheme(isSelectionEnabled: Boolean, isItemSelected: Boolean) {
        super.applyTheme(isSelectionEnabled, isItemSelected)
        sourceNote?.let { applyInkColor(it) }
    }

    private fun applyInkColor(note: Note) {
        inkView?.inkPaint?.color = note.color.toInkContextColor(context, themeOverride)
    }
}
