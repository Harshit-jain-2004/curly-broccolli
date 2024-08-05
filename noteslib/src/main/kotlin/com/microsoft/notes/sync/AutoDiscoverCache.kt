package com.microsoft.notes.sync

import android.content.Context
import com.microsoft.notes.utils.utils.Constants.AUTODISCOVER_HOST_URLS

interface AutoDiscoverCache {
    fun getExpirableHost(userID: String): NotesClientHost.ExpirableHost?
    fun setExpirableHost(userID: String, host: NotesClientHost.ExpirableHost)
}

class AutoDiscoverSharedPreferences(
    private val context: Context
) : AutoDiscoverCache {
    companion object {
        const val NULL_ACQUIRED_TIME = 0L
    }

    override fun getExpirableHost(userID: String): NotesClientHost.ExpirableHost? {
        val sharedPref = context.getSharedPreferences(AUTODISCOVER_HOST_URLS, Context.MODE_PRIVATE)
        val hostURL = sharedPref.getString(getHostUrlKey(userID), null) ?: return null
        val epochAcquiredTimeInMs = sharedPref.getLong(getEpochAcquiredTimeInMsKey(userID), NULL_ACQUIRED_TIME)
        if (epochAcquiredTimeInMs == NULL_ACQUIRED_TIME) return null
        return NotesClientHost.ExpirableHost(hostURL, epochAcquiredTimeInMs)
    }

    override fun setExpirableHost(userID: String, host: NotesClientHost.ExpirableHost) {
        val sharedPref = context.getSharedPreferences(AUTODISCOVER_HOST_URLS, Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString(getHostUrlKey(userID), host.url)
            .putLong(getEpochAcquiredTimeInMsKey(userID), host.epochAcquiredTimeInMs)
            .apply()
    }
}

private fun getHostUrlKey(userID: String): String = "$userID|hostURL"
private fun getEpochAcquiredTimeInMsKey(userID: String) = "$userID|epochAcquiredTimeInMs"
