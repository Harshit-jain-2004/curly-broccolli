package com.microsoft.notes.sync.models

import com.microsoft.notes.sync.JSON
import com.microsoft.notes.sync.sequence
import java.io.Serializable

data class RemoteNote(
    val id: String,
    val changeKey: String,
    val document: Document,
    val color: Int,
    val media: List<Media>,
    val createdWithLocalId: String,
    val createdAt: String,
    val lastModifiedAt: String,
    val createdByApp: String?,
    val documentModifiedAt: String?,
    val title: String? = null,
    val metadata: RemoteNoteMetadata
) : Serializable {

    companion object {
        private const val ID = "id"
        private const val CHANGE_KEY = "changeKey"
        private const val COLOR = "color"
        private const val CREATED_WITH_LOCAL_ID = "createdWithLocalId"
        private const val DOCUMENT = "document"
        private const val CREATED_AT = "createdAt"
        private const val LAST_MODIFIED = "lastModified"
        private const val LAST_MODIFIED_AT = "lastModifiedAt"
        private const val CREATED_BY_APP = "createdByApp"
        private const val DOCUMENT_MODIFIED_AT = "documentModifiedAt"
        private const val MEDIA = "media"
        private const val TITLE = "title"
        private const val METADATA = "metadata"

        fun fromJSON(json: JSON): RemoteNote? {
            val obj = json as? JSON.JObject ?: return null
            val id = obj.get<JSON.JString>(ID)?.string ?: return null
            val changeKey = obj.get<JSON.JString>(CHANGE_KEY)?.string ?: return null
            val color = obj.get<JSON.JNumber>(COLOR)?.number ?: return null
            val createdWithLocalId = obj.get<JSON.JString>(CREATED_WITH_LOCAL_ID)?.string ?: return null
            val document = Document.fromJSON(obj.get<JSON.JObject>(DOCUMENT) ?: return null) ?: return null
            val createdAt = obj.get<JSON.JString>(CREATED_AT)?.string ?: return null
            val lastModifiedAt = obj.get<JSON.JString>(LAST_MODIFIED)?.string ?: return null
            val createdByApp = obj.get<JSON.JString>(CREATED_BY_APP)?.string
            val documentModifiedAt = obj.get<JSON.JString>(DOCUMENT_MODIFIED_AT)?.string
            val media = json.get<JSON.JArray>(MEDIA)?.toList()?.map {
                // cast necessary due to toList not being generic (returns JSON)
                Media.fromJSON(it as JSON.JObject)
            }?.sequence() ?: return null
            val title = obj.get<JSON.JString>(TITLE)?.string
            val metadata = obj.get<JSON.JObject>(METADATA)?.let { RemoteNoteMetadata.fromJSON(it) } ?: RemoteNoteMetadata()

            return RemoteNote(
                id = id,
                changeKey = changeKey,
                document = document,
                color = color.toInt(),
                createdWithLocalId = createdWithLocalId,
                createdAt = createdAt,
                lastModifiedAt = lastModifiedAt,
                createdByApp = createdByApp,
                documentModifiedAt = documentModifiedAt,
                media = media,
                title = title,
                metadata = metadata
            )
        }

        fun toJSON(remoteNote: RemoteNote?): JSON {
            return if (remoteNote != null) {
                JSON.JObject(
                    mapOf(
                        ID to JSON.JString(remoteNote.id),
                        CHANGE_KEY to JSON.JString(remoteNote.changeKey),
                        COLOR to JSON.JNumber(remoteNote.color.toDouble()),
                        CREATED_AT to JSON.JString(remoteNote.createdAt),
                        CREATED_WITH_LOCAL_ID to JSON.JString(remoteNote.createdWithLocalId),
                        DOCUMENT to remoteNote.document.toJSON(),
                        LAST_MODIFIED to JSON.JString(remoteNote.lastModifiedAt),
                        CREATED_BY_APP to when {
                            remoteNote.createdByApp != null -> JSON.JString(remoteNote.createdByApp)
                            else -> JSON.JNull()
                        },
                        DOCUMENT_MODIFIED_AT to when {
                            remoteNote.documentModifiedAt != null -> JSON.JString(remoteNote.documentModifiedAt)
                            else -> JSON.JNull()
                        },
                        MEDIA to JSON.JArray(remoteNote.media.map { Media.toJSON(it) }),
                        TITLE to when {
                            remoteNote.title != null -> JSON.JString(remoteNote.title)
                            else -> JSON.JNull()
                        }
                    )
                )
            } else {
                JSON.JObject(emptyMap())
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any>): RemoteNote? {
            val id = map[ID] as? String ?: return null
            val changeKey = map[CHANGE_KEY] as? String ?: return null
            val document = Document.fromMap(map[DOCUMENT] as? Map<String, Any> ?: return null) ?: return null
            val color = (map[COLOR] as? Double ?: return null).toInt()
            val createdWithLocalId = map[CREATED_WITH_LOCAL_ID] as? String ?: return null
            val createdAt = map[CREATED_AT] as? String ?: return null
            val lastModifiedAt = map[LAST_MODIFIED_AT] as? String ?: return null
            val createdByApp = map[CREATED_BY_APP] as? String
            val documentModifiedAt = map[DOCUMENT_MODIFIED_AT] as? String
            val media = map[MEDIA] as? List<Media> ?: emptyList()
            val title = map[TITLE] as? String
            val metadata = (map[METADATA] as? Map<String, Any>)?.let { RemoteNoteMetadata.fromMap(it) } ?: RemoteNoteMetadata()

            return RemoteNote(
                id = id, changeKey = changeKey, document = document,
                color = color, createdWithLocalId = createdWithLocalId,
                createdAt = createdAt, lastModifiedAt = lastModifiedAt,
                createdByApp = createdByApp, documentModifiedAt = documentModifiedAt,
                media = media,
                title = title,
                metadata = metadata
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            val map = (json as? Map<String, Any> ?: return json).toMutableMap()

            val document = map[DOCUMENT]
            if (document != null) {
                map[DOCUMENT] = Document.migrate(document, old, new)
            }

            return map
        }
    }
}

data class RemoteNoteMetadata(
    val context: RemoteMetadataContext? = null
) : Serializable {

    companion object {
        private const val CONTEXT = "context"
        // reminders to be added here

        fun fromJSON(json: JSON): RemoteNoteMetadata? {
            val obj = json as? JSON.JObject ?: return null
            val context = RemoteMetadataContext.fromJSON(
                obj.get<JSON.JObject>(
                    CONTEXT
                ) ?: return null
            ) ?: return null

            return RemoteNoteMetadata(context = context)
        }

        fun toJSON(metaData: RemoteNoteMetadata): JSON.JObject {
            return JSON.JObject(
                hashMapOf(
                    "context" to RemoteMetadataContext.toJSON(metaData.context)
                )
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any>): RemoteNoteMetadata? {
            val context = RemoteMetadataContext.fromMap(
                map[CONTEXT] as? Map<String, Any> ?: return null
            ) ?: return null

            return RemoteNoteMetadata(context = context)
        }
    }
}

data class RemoteMetadataContext(
    val host: String,
    val hostIcon: String?,
    val displayName: String,
    val url: String?
) : Serializable {
    companion object {
        private const val HOST = "host"
        private const val HOST_ICON = "hostIcon"
        private const val DISPLAY_NAME = "displayName"
        private const val URL = "url"

        fun fromJSON(json: JSON.JObject): RemoteMetadataContext? {
            val obj = json as? JSON.JObject ?: return null
            val host = obj.get<JSON.JString>(HOST)?.string ?: return null
            val hostIcon = obj.get<JSON.JString>(HOST_ICON)?.string
            val displayName = obj.get<JSON.JString>(DISPLAY_NAME)?.string ?: return null
            val url = obj.get<JSON.JString>(URL)?.string
            return RemoteMetadataContext(
                host,
                hostIcon,
                displayName,
                url
            )
        }

        fun toJSON(remoteMetadataContext: RemoteMetadataContext?): JSON {
            return if (remoteMetadataContext != null) {
                JSON.JObject(
                    mapOf(
                        HOST to JSON.JString(remoteMetadataContext.host),
                        HOST_ICON to when {
                            remoteMetadataContext.hostIcon != null -> JSON.JString(remoteMetadataContext.hostIcon)
                            else -> JSON.JNull()
                        },
                        DISPLAY_NAME to JSON.JString(remoteMetadataContext.displayName),
                        URL to when {
                            remoteMetadataContext.url != null -> JSON.JString(remoteMetadataContext.url)
                            else -> JSON.JNull()
                        }
                    )
                )
            } else {
                JSON.JObject(emptyMap())
            }
        }

        fun fromMap(map: Map<String, Any>): RemoteMetadataContext? {
            val host = map[HOST] as? String ?: return null
            val hostIcon = map[HOST_ICON] as? String?
            val displayName = map[DISPLAY_NAME] as? String ?: return null
            val url = map[URL] as? String?
            return RemoteMetadataContext(
                host,
                hostIcon,
                displayName,
                url
            )
        }
    }
}
