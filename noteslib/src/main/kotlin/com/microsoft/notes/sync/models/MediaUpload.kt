package com.microsoft.notes.sync.models

import com.microsoft.notes.sync.JSON

data class MediaUpload(val remoteId: String) {
    companion object {
        private const val ID = "id"
        fun fromJSON(json: JSON): MediaUpload? {
            val obj = json as? JSON.JObject ?: return null
            val remoteId = obj.get<JSON.JString>(ID)?.string ?: return null
            return MediaUpload(remoteId)
        }
    }
}
