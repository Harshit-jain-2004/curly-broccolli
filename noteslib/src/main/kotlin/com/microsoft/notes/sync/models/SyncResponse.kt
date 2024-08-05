package com.microsoft.notes.sync.models

import com.microsoft.notes.sync.JSON
import com.microsoft.notes.sync.filterOutNulls

data class SyncResponse<out T>(val token: Token, val value: List<T>) {
    companion object {
        fun <T> fromJSON(json: JSON, valueParser: (JSON) -> T?, tokenParser: (JSON) -> Token?): SyncResponse<T>? {
            val token = tokenParser(json)
            val obj = json as? JSON.JObject
            val value = obj?.get<JSON.JArray>("value")?.toList()?.map { valueParser(it) }?.filterOutNulls()

            if (token != null && value != null) {
                return SyncResponse(token = token, value = value)
            } else {
                return null
            }
        }
    }
}
