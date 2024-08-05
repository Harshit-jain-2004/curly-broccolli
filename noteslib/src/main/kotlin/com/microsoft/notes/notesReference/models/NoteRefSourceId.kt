package com.microsoft.notes.notesReference.models

sealed class NoteRefSourceId {

    data class FullSourceId(val fullSourceId: String) : NoteRefSourceId() {

        override fun isSameId(sourceId: FullSourceId, pageWebUrl: String): Boolean =
            this.fullSourceId == sourceId.fullSourceId
    }

    data class PartialSourceId(val partialId: String, val nbUrl: String) : NoteRefSourceId() {

        override fun isSameId(sourceId: FullSourceId, pageWebUrl: String): Boolean =
            sourceId.fullSourceId.endsWith(this.partialId) && pageWebUrl.startsWith(this.nbUrl)
    }

    abstract fun isSameId(sourceId: FullSourceId, pageWebUrl: String): Boolean
}
