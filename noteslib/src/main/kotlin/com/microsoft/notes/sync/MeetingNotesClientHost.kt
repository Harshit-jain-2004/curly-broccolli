package com.microsoft.notes.sync

sealed class MeetingNotesClientHost(url: String) : ClientHost(url) {
    companion object {
        const val SUBSTRATE_HOST = "https://substrate.office.com"
        const val OUTLOOK_HOST = "https://outlook.office.com"
        const val WORKING_SET_API_PATH = "/api/beta/me/WorkingSetFiles"
        const val EVENTS_API_PATH = "/api/v2.0/me/calendarView"
        const val COLLAB_API_PATH = "/Collab/v1"
    }

    class StaticHost(url: String) : MeetingNotesClientHost(url) {
        companion object {
            val workingSetAPIHost = StaticHost(SUBSTRATE_HOST + WORKING_SET_API_PATH)
            val eventsAPIHost = StaticHost(OUTLOOK_HOST + EVENTS_API_PATH)
            val collabAPIHost = StaticHost(SUBSTRATE_HOST + COLLAB_API_PATH)
        }
    }
}
