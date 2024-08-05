package com.microsoft.notes.sync

import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.JsonParser
import com.microsoft.notes.utils.logging.NotesLogger
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import okio.BufferedSource
import java.io.IOException

sealed class JSON {
    companion object {
        private const val CLASS_TAG = "JSON"
        private val gson: Gson by lazy {
            Gson()
        }

        var isGsonEnabled: Boolean = false
            internal set(value) {
                when (value) {
                    true -> {
                        notesLogger?.d(
                            CLASS_TAG, "Using Gson as parser"
                        )
                        notesLogger?.recordTelemetry(
                            EventMarkers.SyncFeatureFlag,
                            Pair(
                                NotesSDKTelemetryKeys.SyncProperty.JSON_PARSER,
                                JsonParser.GsonJsonParserInit.toString()
                            )
                        )
                    }
                    false -> {
                        notesLogger?.d(
                            CLASS_TAG, "Using custom parser"
                        )
                        notesLogger?.recordTelemetry(
                            EventMarkers.SyncFeatureFlag,
                            Pair(
                                NotesSDKTelemetryKeys.SyncProperty.JSON_PARSER,
                                JsonParser.CustomJsonParserInit.toString()
                            )
                        )
                    }
                }
                field = value
            }
        internal var notesLogger: NotesLogger? = null

        fun read(json: BufferedSource): ApiResult<JSON> {
            val reader = JsonReader.of(json)
            return try {
                ApiResult.Success(read(reader))
            } catch (e: Exception) {
                return catchJsonException(e)
            }
        }

        private fun catchJsonException(e: Exception): ApiResult.Failure<JSON> {
            notesLogger?.d(
                CLASS_TAG, "Json exception error. Message: ${e.message} cause: ${e.cause} \""
            )
            notesLogger?.recordTelemetry(
                EventMarkers.SyncJsonError,
                Pair(
                    NotesSDKTelemetryKeys.SyncProperty.JSON_PARSER,
                    JsonParser.JsonParserException.toString() + " type: ${e::class.java.canonicalName} "
                )
            )
            when (e) {
                is IOException /* thrown by Moshi/custom parser */ -> {
                    return ApiResult.Failure(
                        ApiError.NonJSONError(e.message ?: "Json Error while parsing")
                    )
                }
                else /* can be thrown by our reader (JSonDataException) or by any unknown cause */ -> {
                    throw e
                }
            }
        }

        private fun read(reader: JsonReader): JSON {
            val next = reader.peek()
            return when (next) {
                JsonReader.Token.BEGIN_ARRAY -> readArray(reader)
                JsonReader.Token.BEGIN_OBJECT -> readObject(reader)
                JsonReader.Token.STRING -> readString(reader)
                JsonReader.Token.NUMBER -> readNumber(reader)
                JsonReader.Token.BOOLEAN -> readBoolean(reader)
                JsonReader.Token.NULL -> readNull(reader)
                else -> throw JsonDataException()
            }
        }

        private fun readNull(reader: JsonReader): JNull {
            reader.nextNull<Any>()
            return JNull()
        }

        private fun readArray(reader: JsonReader): JArray {
            reader.beginArray()
            val elements = mutableListOf<JSON>()
            while (reader.hasNext()) {
                elements.add(read(reader))
            }
            reader.endArray()
            return JArray(elements)
        }

        private fun readObject(reader: JsonReader): JObject {
            reader.beginObject()
            val elements = mutableMapOf<String, JSON>()
            while (reader.hasNext()) {
                val key = reader.nextName()
                elements.put(key, read(reader))
            }
            reader.endObject()
            return JObject(elements)
        }

        private fun readString(reader: JsonReader): JString = JString(reader.nextString())

        private fun readNumber(reader: JsonReader): JNumber = JNumber(reader.nextDouble())

        private fun readBoolean(reader: JsonReader): JBoolean = JBoolean(reader.nextBoolean())
    }

    data class JObject(val map: Map<String, JSON>) : JSON() {
        inline fun <reified T> get(key: String): T? = map[key] as? T

        override fun toString(): String {
            val inside = map.map { (key, value) -> """"$key":$value""" }.joinToString(",")
            return "{$inside}"
        }
    }

    data class JArray(val array: List<JSON>) : JSON() {
        fun toList(): List<JSON> = array

        override fun toString(): String {
            val inside = array.joinToString(",")
            return "[$inside]"
        }
    }

    data class JString(val string: String) : JSON() {
        override fun toString(): String {
            return when {
                isGsonEnabled -> usingGsonParser()
                else -> usingCustomParser()
            }
        }

        private fun usingCustomParser(): String {
            val escapedString = string
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
            return "\"$escapedString\""
        }

        private fun usingGsonParser(): String {
            try {
                return gson.toJson(string)
            } catch (e: JsonIOException) {
                notesLogger?.d(
                    CLASS_TAG,
                    "Json exception error. Message: ${e.message} cause: ${e.cause} " +
                        "type: ${e::class.java.canonicalName} "
                )
                notesLogger?.recordTelemetry(
                    EventMarkers.SyncJsonError,
                    Pair(
                        NotesSDKTelemetryKeys.SyncProperty.JSON_PARSER,
                        JsonParser.JsonParserException.toString() + " type: ${e::class.java.canonicalName} "
                    )
                )
                throw e
            }
        }
    }

    class JNull : JSON() {
        override fun toString(): String = "null"
    }

    data class JNumber(val number: Double) : JSON() {
        override fun toString(): String = this.number.toString()
    }

    data class JBoolean(val boolean: Boolean) : JSON() {
        override fun toString(): String = this.boolean.toString()
    }
}
