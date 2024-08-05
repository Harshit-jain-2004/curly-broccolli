package com.microsoft.notes.sync.models

import com.microsoft.notes.sync.JSON
import java.io.Serializable

data class ImageDimensions(
    val height: String,
    val width: String
) : Serializable {

    companion object {
        const val HEIGHT = "height"
        const val WIDTH = "width"

        fun fromJSON(json: JSON.JObject): ImageDimensions? {
            val height = json.get<JSON.JString>(HEIGHT)?.string ?: return null
            val width = json.get<JSON.JString>(WIDTH)?.string ?: return null
            return ImageDimensions(height = height, width = width)
        }

        fun fromMap(map: Map<String, Any>): ImageDimensions? {
            val height = map.get(HEIGHT) as? String ?: return null
            val width = map.get(WIDTH) as? String ?: return null
            return ImageDimensions(height = height, width = width)
        }

        fun toJSON(imageDimensions: ImageDimensions): JSON.JObject {
            return JSON.JObject(
                hashMapOf(
                    HEIGHT to JSON.JString(imageDimensions.height),
                    WIDTH to JSON.JString(imageDimensions.width)
                )
            )
        }
    }
}

data class NoteReferenceMedia(
    val mediaID: String,
    val mediaType: String
) : Serializable {
    companion object {
        private const val MEDIA_ID = "id"
        private const val MEDIA_TYPE = "type"

        fun fromJSON(json: JSON.JObject): NoteReferenceMedia? {
            val mediaID = json.get<JSON.JString>(MEDIA_ID)?.string ?: return null
            val mediaType = json.get<JSON.JString>(MEDIA_TYPE)?.string ?: return null

            return NoteReferenceMedia(
                mediaID = mediaID,
                mediaType = mediaType
            )
        }

        fun toJSON(noteReferenceMedia: NoteReferenceMedia): JSON.JObject {
            return JSON.JObject(
                hashMapOf(
                    MEDIA_ID to JSON.JString(noteReferenceMedia.mediaID),
                    MEDIA_TYPE to JSON.JString(noteReferenceMedia.mediaType)
                )
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            return json
        }
    }
}

data class Media(
    val id: String,
    val createdWithLocalId: String,
    val mimeType: String,
    val lastModified: String,
    val altText: String?,
    val imageDimensions: ImageDimensions?
) : Serializable {

    companion object {
        private const val ID = "id"
        private const val CREATED_WITH_LOCAL_ID = "createdWithLocalId"
        private const val MIME_TYPE = "mimeType"
        private const val LAST_MODIFIED = "lastModified"
        private const val ALT_TEXT = "altText"
        private const val IMAGE_DIMENSIONS = "imageDimensions"

        fun fromJSON(json: JSON.JObject): Media? {
            val id = json.get<JSON.JString>(ID)?.string ?: return null
            val createdWithLocalId = json.get<JSON.JString>(CREATED_WITH_LOCAL_ID)?.string ?: return null
            val mimeType = json.get<JSON.JString>(MIME_TYPE)?.string ?: return null
            val lastModified = json.get<JSON.JString>(LAST_MODIFIED)?.string ?: return null
            val altText = json.get<JSON.JString>(ALT_TEXT)?.string
            val imageDimensions = json.get<JSON.JObject>(IMAGE_DIMENSIONS)

            return Media(
                id = id,
                createdWithLocalId = createdWithLocalId,
                mimeType = mimeType,
                lastModified = lastModified,
                altText = altText,
                imageDimensions = when (imageDimensions) {
                    null -> null
                    else -> ImageDimensions.fromJSON(imageDimensions)
                }
            )
        }

        fun toJSON(media: Media): JSON.JObject {
            return JSON.JObject(
                hashMapOf(
                    ID to JSON.JString(media.id),
                    CREATED_WITH_LOCAL_ID to JSON.JString(media.createdWithLocalId),
                    MIME_TYPE to JSON.JString(media.mimeType),
                    LAST_MODIFIED to JSON.JString(media.lastModified),
                    ALT_TEXT to when {
                        media.altText != null -> JSON.JString(media.altText)
                        else -> JSON.JNull()
                    },
                    IMAGE_DIMENSIONS to when {
                        media.imageDimensions != null -> ImageDimensions.toJSON(media.imageDimensions)
                        else -> JSON.JNull()
                    }
                )
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            return json
        }
    }
}
