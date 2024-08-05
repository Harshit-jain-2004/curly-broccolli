package com.microsoft.notes.utils.logging

enum class SNMarkerConstants(val value: String) {
    NotesFetchDBStart("NotesFetchDBStart"),
    NotesFetchDBEnd("NotesFetchDBEnd"),
    /**
     * Call made from UI to fetch notes
     * for showing them on UI
     */
    NotesFetchUIStart("NotesFetchUIStart"),
    /**
     * Notes rendered on UI
     */
    NotesFetchUIEnd("NotesFetchUIEnd"),
}

interface SNMarkerListener {
    fun handleEvent(markerConstants: SNMarkerConstants)
}

class SNMarker {
    companion object {
        private val listeners = mutableSetOf<SNMarkerListener>()
        fun addListener(listener: SNMarkerListener) {
            synchronized(listeners) {
                listeners.add(listener)
            }
        }

        fun logMarker(markerConstants: SNMarkerConstants) {
            synchronized(listeners) {
                for (listener in listeners) {
                    listener.handleEvent(markerConstants)
                }
            }
        }
    }
}
