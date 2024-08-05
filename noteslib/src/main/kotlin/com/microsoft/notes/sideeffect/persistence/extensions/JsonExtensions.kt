package com.microsoft.notes.sideeffect.persistence.extensions

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.microsoft.notes.models.Media
import com.microsoft.notes.models.NoteReferenceMedia
import com.microsoft.notes.models.ReminderWrapper
import com.microsoft.notes.models.RemoteData
import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.BlockType
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.NoteContext
import com.microsoft.notes.richtext.scheme.Paragraph
import java.io.InputStreamReader
import java.lang.reflect.Type

// We need custom deserializer to parse sealed classes
val gsonDeserializer: Gson by lazy { buildGsonDeserializer() }

val gsonSerializer: Gson by lazy { Gson() }

fun RemoteData.toJson(): String = gsonSerializer.toJson(this)

fun Document.toJson(): String = gsonSerializer.toJson(this)

fun List<Media>.toJson(): String = gsonSerializer.toJson(this)
fun List<NoteReferenceMedia>.toNoteReferenceMediaJson(): String = gsonSerializer.toJson(this)

fun NoteContext.toJson(): String = gsonSerializer.toJson(this)

fun ReminderWrapper.toJson(): String = gsonSerializer.toJson(this)

fun String.fromRemoteDataJson(): RemoteData = deserialize(RemoteData::class.java) as RemoteData

fun String.fromDocumentJson(): Document? = deserialize(Document::class.java) as Document?

fun String.fromMediaJson(): List<Media> {
    val reader = InputStreamReader(this.byteInputStream())
    val deserialized =
        gsonDeserializer.fromJson(this, Array<Media>::class.java).toMutableList()
    reader.close()
    return deserialized
}

fun String.fromNoteReferenceMediaJson(): List<NoteReferenceMedia> {
    val reader = InputStreamReader(this.byteInputStream())
    val deserialized =
        gsonDeserializer.fromJson(this, Array<NoteReferenceMedia>::class.java).toMutableList()
    reader.close()
    return deserialized
}

fun String.fromMetadataContextJson(): NoteContext = deserialize(NoteContext::class.java) as NoteContext

fun String.fromMetadataReminderWrapperJson(): ReminderWrapper = deserialize(ReminderWrapper::class.java) as ReminderWrapper

fun <T> String.deserialize(classOfT: Class<T>): Any? {
    val reader = InputStreamReader(this.byteInputStream())
    val deserialized = gsonDeserializer.fromJson<Any>(reader, classOfT)
    reader.close()
    return deserialized
}

private fun buildGsonDeserializer(): Gson = GsonBuilder()
    .registerTypeAdapter(
        Block::class.java,
        JsonDeserializer<Block> { jsonElement: JsonElement, _: Type,
            jsonDeserializationContext: JsonDeserializationContext ->
            if (jsonElement.asJsonObject.get("blockType").asString == BlockType.Paragraph.name) {
                jsonDeserializationContext.deserialize(
                    jsonElement,
                    Paragraph::class.java
                )
            } else {
                jsonDeserializationContext.deserialize(
                    jsonElement,
                    InlineMedia::class.java
                )
            }
        }
    ).create()
