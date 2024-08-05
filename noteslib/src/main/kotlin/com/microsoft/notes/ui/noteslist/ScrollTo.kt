package com.microsoft.notes.ui.noteslist

sealed class ScrollTo {
    object Top : ScrollTo()
    object Bottom : ScrollTo()
    class Custom(val position: Int) : ScrollTo()
    object NoScroll : ScrollTo()
}
