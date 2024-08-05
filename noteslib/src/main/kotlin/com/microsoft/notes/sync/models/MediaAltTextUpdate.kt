package com.microsoft.notes.sync.models

import com.microsoft.notes.sync.JSON
import java.io.Serializable

data class MediaAltTextUpdate(
    val changeKey: String,
    val id: String,
    val createdWithLocalId: String,
    val mimeType: String,
    val lastModified: String,
    val altText: String?,
    val imageDimensions: ImageDimensions?
) : Serializable {

    companion object {
        private const val CHANGE_KEY = "changeKey"
        private const val ID = "id"
        private const val CREATED_WITH_LOCAL_ID = "createdWithLocalId"
        private const val MIME_TYPE = "mimeType"
        private const val LAST_MODIFIED = "lastModified"
        private const val ALT_TEXT = "altText"
        private const val IMAGE_DIMENSIONS = "imageDimensions"

        fun fromJSON(json: JSON): MediaAltTextUpdate? {
            val obj = json as? JSON.JObject ?: return null

            val changeKey = obj.get<JSON.JString>(CHANGE_KEY)?.string ?: return null
            val id = obj.get<JSON.JString>(ID)?.string ?: return null
            val createdWithLocalId = obj.get<JSON.JString>(CREATED_WITH_LOCAL_ID)?.string ?: return null
            val mimeType = obj.get<JSON.JString>(MIME_TYPE)?.string ?: return null
            val lastModified = obj.get<JSON.JString>(LAST_MODIFIED)?.string ?: return null
            val altText = obj.get<JSON.JString>(ALT_TEXT)?.string
            val imageDimensions = obj.get<JSON.JObject>(IMAGE_DIMENSIONS)

            return MediaAltTextUpdate(
                changeKey = changeKey,
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
    }
}
