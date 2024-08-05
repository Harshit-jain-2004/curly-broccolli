package com.microsoft.notes.sync.models.localOnly

import com.microsoft.notes.sync.models.Document
import com.microsoft.notes.sync.models.Media
import com.microsoft.notes.sync.models.RemoteNoteMetadata
import java.io.Serializable

data class Note(
    val id: String,
    val remoteData: RemoteData?,
    val document: Document,
    val media: List<Media>,
    val color: Color,
    val createdByApp: String?,
    val documentModifiedAt: String?,
    val metadata: RemoteNoteMetadata?
) : Serializable {

    enum class Color(val value: Int) {
        GREY(0),
        YELLOW(1),
        GREEN(2),
        PINK(3),
        PURPLE(4),
        BLUE(5),
        CHARCOAL(6);

        companion object {
            private val colorMap = Color.values().associateBy(Color::value)
            fun fromInt(color: Int): Color? = colorMap[color]
        }
    }

    companion object {
        private const val MEDIA = "media"

        @Suppress("UNCHECKED_CAST", "UnsafeCast")
        fun fromMap(map: Map<String, Any>): Note? {
            val id = map["id"] as? String ?: return null
            val remoteData = if (map["remoteData"] as? Map<String, Any> != null) {
                RemoteData.fromMap(map["remoteData"] as Map<String, Any>)
            } else {
                null
            }
            val document = Document.fromMap(map["document"] as? Map<String, Any> ?: return null) ?: return null
            val media = map[MEDIA] as? List<Media> ?: return null
            val color = Color.valueOf(map["color"] as? String ?: return null)
            val createdByApp = map["createdByApp"] as? String
            val documentModifiedAt = map["documentModifiedAt"] as? String
            val metadata = (map["metadata"] as? Map<String, Any>)?.let { RemoteNoteMetadata.fromMap(it) }
            return Note(
                id = id, remoteData = remoteData, document = document, media = media,
                color = color, createdByApp = createdByApp, documentModifiedAt = documentModifiedAt,
                metadata = metadata
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            val map = (json as? Map<String, Any> ?: return json).toMutableMap()

            val remoteData = map["remoteData"]
            if (remoteData != null) {
                map["remoteData"] = RemoteData.migrate(remoteData, old, new)
            }

            val document = map["document"]
            if (document != null) {
                map["document"] = Document.migrate(document, old, new)
            }

            if (old < 2 && new >= 2) {
                map["media"] = listOf<Media>()
            }

            return map
        }
    }
}
