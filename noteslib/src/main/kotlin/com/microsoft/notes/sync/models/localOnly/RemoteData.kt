package com.microsoft.notes.sync.models.localOnly

import com.microsoft.notes.sync.JSON
import com.microsoft.notes.sync.models.RemoteNote
import com.microsoft.notes.utils.utils.parseMillisToISO8601String
import java.io.Serializable

data class RemoteData(
    val id: String,
    val changeKey: String,
    val lastServerVersion: RemoteNote?,
    val createdAt: String,
    val lastModifiedAt: String
) : Serializable {

    constructor(
        id: String,
        changeKey: String,
        lastServerVersion: RemoteNote?,
        createdAt: Long,
        lastModifiedAt: Long
    ) : this(
        id, changeKey, lastServerVersion,
        parseMillisToISO8601String(createdAt),
        parseMillisToISO8601String(lastModifiedAt)
    )

    fun toJSON(): JSON.JObject {
        return JSON.JObject(
            hashMapOf(
                "id" to JSON.JString(this.id),
                "changeKey" to JSON.JString(this.changeKey),
                "lastServerVersion" to RemoteNote.toJSON(lastServerVersion),
                "createdAt" to JSON.JString(this.createdAt),
                "lastModifiedAt" to JSON.JString(this.lastModifiedAt)
            )
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST", "UnsafeCast")
        fun fromMap(map: Map<String, Any>): RemoteData? {
            val id = map["id"] as? String ?: return null
            val changeKey = map["changeKey"] as? String ?: return null
            val createdAt = map["createdAt"] as? String ?: return null
            val lastModifiedAt = map["lastModifiedAt"] as? String ?: return null
            val lastServerVersion = if (map["lastServerVersion"] as? Map<String, Any> != null) {
                RemoteNote.fromMap(map["lastServerVersion"] as Map<String, Any>)
            } else {
                null
            }

            return RemoteData(
                id = id, changeKey = changeKey, lastServerVersion = lastServerVersion,
                createdAt = createdAt, lastModifiedAt = lastModifiedAt
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old >= new) return json

            val map = (json as? Map<String, Any> ?: return json).toMutableMap()

            val lastServerVersion = map["lastServerVersion"]
            if (lastServerVersion != null) {
                map["lastServerVersion"] = RemoteNote.migrate(lastServerVersion, old, new)
            }

            return map
        }
    }
}
