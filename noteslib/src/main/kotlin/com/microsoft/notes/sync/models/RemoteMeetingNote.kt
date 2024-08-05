package com.microsoft.notes.sync.models

import com.microsoft.notes.sync.JSON
import com.microsoft.notes.sync.filterOutNulls
import com.microsoft.notes.utils.utils.parseISO8601StringToMillis
import java.io.Serializable

data class RemoteMeetingNote(
    val id: String,
    val fileName: String,
    val createdTime: Long,
    val lastModifiedTime: Long,
    val visualizationData: RemoteMeetingNoteVisualizationData,
    val sharepointItem: RemoteMeetingNoteSharepointItem
) : Serializable {

    companion object {
        private const val ID = "Id"
        private const val FILE_NAME = "FileName"
        private const val CREATED_TIME = "FileCreatedTime"
        private const val LAST_MODIFIED_TIME = "FileModifiedTime"
        private const val VISUALIZATION = "Visualization"
        private const val SHAREPOINT_ITEM = "SharePointItem"

        fun fromJSON(json: JSON): RemoteMeetingNote? {
            val obj = json as? JSON.JObject ?: return null
            val id = obj.get<JSON.JString>(ID)?.string ?: return null
            val fileName = obj.get<JSON.JString>(FILE_NAME)?.string ?: return null

            val fileCreated = obj.get<JSON.JString>(CREATED_TIME)?.string ?: return null
            val fileCreatedIso8601Time =
                fileCreated.substring(0, fileCreated.length - 1).plus(".0000000Z")
            val fileCreatedTimeLong: Long = parseISO8601StringToMillis(fileCreatedIso8601Time)

            val lastModified = obj.get<JSON.JString>(LAST_MODIFIED_TIME)?.string ?: return null
            val lastModifiedIso8601Time =
                lastModified.substring(0, lastModified.length - 1).plus(".0000000Z")
            val lastModifiedTimeLong: Long = parseISO8601StringToMillis(lastModifiedIso8601Time)

            val sharepointItem = RemoteMeetingNoteSharepointItem.fromJSON(
                obj.get<JSON.JObject>(
                    SHAREPOINT_ITEM
                ) ?: return null
            ) ?: return null

            val visualization = RemoteMeetingNoteVisualizationData.fromJSON(
                obj.get<JSON.JObject>
                (VISUALIZATION) ?: return null
            ) ?: return null

            return RemoteMeetingNote(
                id = id,
                fileName = fileName,
                createdTime = fileCreatedTimeLong,
                lastModifiedTime = lastModifiedTimeLong,
                visualizationData = visualization,
                sharepointItem = sharepointItem
            )
        }
    }
}

data class RemoteMeetingNoteSharepointItem(
    val docId: Long,
    val fileUrl: String,
    val siteId: String,
    val webId: String,
    val listId: String,
    val modifiedBy: String,
    val modifiedByDisplayName: String
) : Serializable {

    companion object {
        private const val DOC_ID = "DocId"
        private const val FILE_URL = "FileUrl"
        private const val MODIFIED_BY = "ModifiedBy"
        private const val SITE_ID = "SiteId"
        private const val WEB_ID = "WebId"
        private const val LIST_ID = "ListId"
        private const val MODIFIED_BY_DISPLAY_NAME = "ModifiedByDisplayName"

        fun fromJSON(json: JSON.JObject): RemoteMeetingNoteSharepointItem? {
            val docId = json.get<JSON.JNumber>(DOC_ID)?.number?.toLong() ?: return null
            val fileUrl = json.get<JSON.JString>(FILE_URL)?.string ?: return null
            val modifiedBy = json.get<JSON.JString>(MODIFIED_BY)?.string ?: return null
            val modifiedByDisplayName = json.get<JSON.JString>(MODIFIED_BY_DISPLAY_NAME)?.string ?: return null

            val siteId = json.get<JSON.JString>(SITE_ID)?.string ?: return null
            val webId = json.get<JSON.JString>(WEB_ID)?.string ?: return null
            val listId = json.get<JSON.JString>(LIST_ID)?.string ?: return null

            return RemoteMeetingNoteSharepointItem(
                docId = docId,
                fileUrl = fileUrl,
                siteId = siteId,
                webId = webId,
                listId = listId,
                modifiedBy = modifiedBy,
                modifiedByDisplayName = modifiedByDisplayName
            )
        }
    }
}

data class RemoteMeetingNoteVisualizationData(
    val title: String,
    val type: String,
    val staticTeaser: String,
    val accessUrl: String,
    val containerUrl: String,
    val containerTitle: String
) : Serializable {

    companion object {
        private const val TITLE = "Title"
        private const val TYPE = "Type"
        private const val STATIC_TEASER = "StaticTeaser"
        private const val ACCESS_URL = "AccessUrl"
        private const val CONTAINER_URL = "ContainerUrl"
        private const val CONTAINER_TITLE = "ContainerTitle"

        fun fromJSON(json: JSON.JObject): RemoteMeetingNoteVisualizationData? {

            val title = json.get<JSON.JString>(TITLE)?.string ?: return null
            val type = json.get<JSON.JString>(TYPE)?.string ?: return null
            val staticTeaser = json.get<JSON.JString>(STATIC_TEASER)?.string ?: return null
            val accessUrl = json.get<JSON.JString>(ACCESS_URL)?.string ?: return null
            val containerUrl = json.get<JSON.JString>(CONTAINER_URL)?.string ?: return null
            if (!containerUrl.endsWith("/Documents/Meetings"))
                return null

            val containerTitle = json.get<JSON.JString>(CONTAINER_TITLE)?.string ?: return null

            return RemoteMeetingNoteVisualizationData(
                title,
                type,
                staticTeaser,
                accessUrl,
                containerUrl,
                containerTitle
            )
        }
    }
}

data class RemoteMeetingNoteFromCollab(
    val id: String,
    val url: String
) : Serializable {

    companion object {
        private const val RESOURCES = "resources"
        private const val TYPE = "type"
        private const val NOTES = "notes"
        private const val ID = "id"
        private const val LOCATION = "location"

        fun fromJSON(json: JSON): RemoteMeetingNoteFromCollab? {
            val obj = json as? JSON.JObject ?: return null
            val resources = obj.get<JSON.JArray>(RESOURCES)?.toList() ?: return null

            var notesResource: JSON.JObject? = null
            for (resource in resources) {
                if ((resource as? JSON.JObject)?.get<JSON.JString>(TYPE)?.string == NOTES) {
                    notesResource = resource
                    break
                }
            }
            if (notesResource == null) return null

            val id = notesResource.get<JSON.JString>(ID)?.string ?: return null
            val url = notesResource.get<JSON.JString>(LOCATION)?.string ?: return null

            return RemoteMeetingNoteFromCollab(
                id = id,
                url = url
            )
        }
    }
}

fun getListOfRemoteMeetingNoteFromCollab(json: JSON): List<RemoteMeetingNoteFromCollab>? {
    val obj = json as? JSON.JArray
    val valueParser = RemoteMeetingNoteFromCollab.Companion::fromJSON
    return obj?.toList()?.map { valueParser(it) }?.filterOutNulls()
}
