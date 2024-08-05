package com.microsoft.notes.ui.noteslist.recyclerview.noteitem

import android.content.Context
import android.util.AttributeSet
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.note.ink.PreviewInkView

class InkNoteItemUIRefreshComponent(context: Context, attrs: AttributeSet) : InkNoteItemComponent(context, attrs) {
    override val inkView: PreviewInkView? by lazy { findViewById<PreviewInkView?>(R.id.previewInk) }
}
