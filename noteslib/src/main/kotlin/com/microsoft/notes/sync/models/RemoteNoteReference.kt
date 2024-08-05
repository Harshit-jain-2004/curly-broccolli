package com.microsoft.notes.sync.models

import com.microsoft.notes.notesReference.models.NoteRefSourceId
import com.microsoft.notes.sync.JSON
import com.microsoft.notes.sync.sequence
import com.microsoft.notes.utils.utils.parseISO8601StringToMillis
import java.io.Serializable

data class RemoteNoteReference(
    val id: String,
    val createdAt: String,
    val lastModified: String,
    val weight: Float? = null,
    val metaData: RemoteNoteReferenceMetaData,
    val visualizationData: RemoteNoteReferenceVisualizationData

) : Serializable {

    companion object {
        private const val ID = "id"
        private const val CREATED_AT = "createdAt"
        private const val LAST_MODIFIED = "lastModified"
        private const val WEIGHT = "weight"
        private const val METADATA = "metadata"
        private const val VISUALIZATION = "visualization"

        fun fromJSON(json: JSON): RemoteNoteReference? {
            val obj = json as? JSON.JObject ?: return null
            val metadata = RemoteNoteReferenceMetaData.fromJSON(obj.get<JSON.JObject>(METADATA) ?: return null)
                ?: return null
            val visualization = RemoteNoteReferenceVisualizationData.fromJSON(
                obj.get<JSON.JObject>
                (VISUALIZATION) ?: return null
            ) ?: return null
            val id = obj.get<JSON.JString>(ID)?.string ?: return null
            val createdAt = obj.get<JSON.JString>(CREATED_AT)?.string ?: return null
            val lastModified = obj.get<JSON.JString>(LAST_MODIFIED)?.string ?: return null
            val weight = obj.get<JSON.JNumber>(WEIGHT)?.number ?: return null

            return RemoteNoteReference(
                id = id,
                createdAt = createdAt,
                lastModified = lastModified,
                weight = weight.toFloat(),
                metaData = metadata,
                visualizationData = visualization
            )
        }
    }
}

data class RemoteNoteReferenceMetaData(
    val createdAt: Long,
    val id: NoteRefSourceId.FullSourceId,
    val lastModified: Long,
    val type: String,
    val webUrl: String,
    val clientUrl: String
) : Serializable {

    companion object {
        private const val CREATEDAT = "createdAt"
        private const val ID = "id"
        private const val LASTMODIFIED = "lastModified"
        private const val TYPE = "type"
        private const val WEBURL = "webUrl"
        private const val CLIENTURL = "clientUrl"

        fun fromJSON(json: JSON.JObject): RemoteNoteReferenceMetaData? {
            val createdAt = json.get<JSON.JString>(CREATEDAT)?.string ?: return null
            val id = json.get<JSON.JString>(ID)?.string ?: return null
            val lastModified = json.get<JSON.JString>(LASTMODIFIED)?.string ?: return null
            val type = json.get<JSON.JString>(TYPE)?.string ?: return null
            val webUrl = json.get<JSON.JString>(WEBURL)?.string ?: return null
            val clientUrl = json.get<JSON.JString>(CLIENTURL)?.string ?: "" // todo remove default once NoteReferences EndPoint returns value

            return RemoteNoteReferenceMetaData(
                createdAt = parseISO8601StringToMillis(createdAt),
                id = NoteRefSourceId.FullSourceId(id),
                lastModified = parseISO8601StringToMillis(lastModified),
                type = type,
                webUrl = webUrl,
                clientUrl = clientUrl
            )
        }
    }
}

data class RemoteNoteReferenceVisualizationData(
    val color: Int?,
    val previewText: String,
    val previewRichText: String?,
    val title: String,
    val containers: List<Container>,
    val media: List<NoteReferenceMedia>
) : Serializable {

    companion object {
        const val PREVIEWTEXT = "previewText"
        const val PREVIEWRICHTEXT = "previewRichText"
        const val TITLE = "title"
        const val COLOR = "color"
        private const val CONTAINER = "containers"
        private const val DISPLAYNAME = "displayName"
        private const val ID = "id"
        private const val MEDIA = "media"
        private const val TYPE = "type"

        fun fromJSON(json: JSON.JObject): RemoteNoteReferenceVisualizationData? {
            val previewText = json.get<JSON.JString>(PREVIEWTEXT)?.string ?: return null
            val previewRichText = json.get<JSON.JString>(PREVIEWRICHTEXT)?.string
            val title = json.get<JSON.JString>(TITLE)?.string ?: return null
            val color = json.get<JSON.JNumber>(COLOR)?.number?.toInt()
            /* The containerName contains source name elements at following indices:
            0: Section Name
            1: Section Group
            2: Notebook Name
            It's possible to have multiple Section Groups, hence always use first and last element to be safe.*/
            val containers = (
                json.get<JSON.JArray>(CONTAINER)?.array?.map {
                    Container(
                        name = (it as JSON.JObject).get<JSON.JString>(DISPLAYNAME)?.string
                            ?: "",
                        id = it.get<JSON.JString>(ID)?.string
                            ?: ""
                    )
                }
                )?.toList()
                ?: emptyList()

            val media = json.get<JSON.JArray>(MEDIA)?.array?.map {
                NoteReferenceMedia.fromJSON(it as JSON.JObject)
            }?.toList()?.sequence() ?: emptyList()

            return RemoteNoteReferenceVisualizationData(
                color = color,
                previewText = previewText,
                previewRichText = previewRichText,
                title = title,
                containers = containers,
                media = media
            )
        }
    }
}

data class Container(
    val name: String,
    val id: String
)
