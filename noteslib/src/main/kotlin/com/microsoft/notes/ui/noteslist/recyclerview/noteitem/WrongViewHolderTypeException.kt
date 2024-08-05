package com.microsoft.notes.ui.noteslist.recyclerview.noteitem

class WrongViewHolderTypeException : ClassNotFoundException() {
    override val message: String?
        get() = "Wrong view holder!!"
}
