package com.microsoft.notes.sync

import com.microsoft.notes.sync.NotesClientHost.ExpirableHost.Companion.SOFT_EXPIRY_IN_MILLISECONDS

sealed class NotesClientHost(url: String) : ClientHost(url) {
    companion object {
        const val DEFAULT_HOST = "https://substrate.office.com"
        const val DEFAULT_PATH = "/notesfabric"
    }
    class StaticHost(url: String) : NotesClientHost(url) {
        companion object {
            val default = StaticHost(DEFAULT_HOST + DEFAULT_PATH)
        }
    }

    class ExpirableHost(url: String, val epochAcquiredTimeInMs: Long) : NotesClientHost(url) {
        companion object {
            val default = ExpirableHost(DEFAULT_HOST + DEFAULT_PATH, 0L)
            const val SOFT_EXPIRY_IN_MILLISECONDS = 12 * 60 * 60 * 1000 // 12 hours

            @Suppress("UNCHECKED_CAST", "UnsafeCast")
            fun fromMap(map: Map<String, Any>): ExpirableHost? {
                val url = map["url"] as? String ?: return null
                val epochAcquiredTimeInMs = (map["epochAcquiredTimeInMs"] as? Long) ?: return null
                return ExpirableHost(url, epochAcquiredTimeInMs)
            }
        }
    }
}

fun NotesClientHost.isExpired(): Boolean {
    return when (this) {
        is NotesClientHost.StaticHost -> return false
        is NotesClientHost.ExpirableHost -> System.currentTimeMillis() > (
            epochAcquiredTimeInMs +
                SOFT_EXPIRY_IN_MILLISECONDS
            )
    }
}
