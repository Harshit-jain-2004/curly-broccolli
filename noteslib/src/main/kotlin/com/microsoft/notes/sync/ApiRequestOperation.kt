package com.microsoft.notes.sync

import com.microsoft.notes.sync.SyncRequestTelemetry.SyncRequestType
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.sync.models.localOnly.Note
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import java.io.Serializable
import java.util.UUID

enum class ApiRequestOperationType {
    Sync,
    NoteReferencesSync,
    MeetingNotesSync,
    SamsungNotesSync,
    CreateNote,
    UpdateNote,
    DeleteNote,
    DeleteNoteReference,
    DeleteSamsungNote,
    GetNoteForMerge,
    UploadMedia,
    DownloadMedia,
    DeleteMedia,
    UpdateMediaAltText,
    InvalidUpdateNote,
    InvalidDeleteNote,
    InvalidDeleteSamsungNote,
    InvalidUploadMedia,
    InvalidDeleteMedia,
    InvalidUpdateMediaAltText
}

interface IJsonConverter<out T : ApiRequestOperation> {
    fun fromMap(map: Map<String, Any>): T?
    fun migrate(json: Any, old: Int, new: Int): Any
}

sealed class ApiRequestOperation : Serializable {
    abstract val uniqueId: String
    // requestId is a CorrelationVector (https://osgwiki.com/wiki/CorrelationVector), assigned just before the
    // request and is used in telemetry and request headers.
    @Transient
    lateinit var requestId: String

    @Transient
    lateinit var realTimeSessionId: String

    @Transient
    var isProcessing = false

    abstract val telemetryBundle: SyncRequestTelemetry

    abstract val type: ApiRequestOperationType

    companion object {
        const val SCHEMA_VERSION = 2

        fun priority(operation: ApiRequestOperation): Priority {
            return when (operation) {
                is ValidApiRequestOperation.CreateNote -> Priority.Critical
                is ValidApiRequestOperation.GetNoteForMerge -> Priority.High
                is ValidApiRequestOperation.NoteReferencesSync -> Priority.Low
                is ValidApiRequestOperation.MeetingNotesSync -> Priority.Low
                is ValidApiRequestOperation.SamsungNotesSync -> Priority.Low
                is ValidApiRequestOperation.UpdateNote -> Priority.Medium
                is ValidApiRequestOperation.DeleteNote -> Priority.Medium
                is ValidApiRequestOperation.DeleteNoteReference -> Priority.Medium
                is ValidApiRequestOperation.DeleteSamsungNote -> Priority.Medium
                is ValidApiRequestOperation.Sync -> Priority.Low
                is ValidApiRequestOperation.UploadMedia -> Priority.Low
                is ValidApiRequestOperation.DownloadMedia -> Priority.Low
                is ValidApiRequestOperation.DeleteMedia -> Priority.Low
                is ValidApiRequestOperation.UpdateMediaAltText -> Priority.Low
                is InvalidApiRequestOperation -> Priority.Invalid
            }
        }

        fun fromMap(map: Map<String, Any>): ApiRequestOperation? {
            val type = map["type"] as? String ?: return null
            return getApiRequestOperationInstantiator(type)?.fromMap(map)
        }

        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            val map = (json as? Map<String, Any> ?: return json).toMutableMap()

            val type = map["type"] as? String ?: return map

            return getApiRequestOperationInstantiator(type)?.migrate(map, old, new) ?: map
        }

        private fun getApiRequestOperationInstantiator(
            type: String
        ): IJsonConverter<ApiRequestOperation>? {
            return when (type) {
                ApiRequestOperationType.Sync.name -> {
                    ValidApiRequestOperation.Sync.JSONConverter()
                }
                ApiRequestOperationType.NoteReferencesSync.name -> {
                    ValidApiRequestOperation.NoteReferencesSync.JSONConverter()
                }
                ApiRequestOperationType.SamsungNotesSync.name -> {
                    ValidApiRequestOperation.SamsungNotesSync.JSONConverter()
                }
                ApiRequestOperationType.CreateNote.name -> {
                    ValidApiRequestOperation.CreateNote.JSONConverter()
                }
                ApiRequestOperationType.UpdateNote.name -> {
                    ValidApiRequestOperation.UpdateNote.JSONConverter()
                }
                ApiRequestOperationType.DeleteNote.name -> {
                    ValidApiRequestOperation.DeleteNote.JSONConverter()
                }
                ApiRequestOperationType.DeleteNoteReference.name -> {
                    ValidApiRequestOperation.DeleteNoteReference.JSONConverter()
                }
                ApiRequestOperationType.GetNoteForMerge.name -> {
                    ValidApiRequestOperation.GetNoteForMerge.JSONConverter()
                }
                ApiRequestOperationType.UploadMedia.name -> {
                    ValidApiRequestOperation.UploadMedia.JSONConverter()
                }
                ApiRequestOperationType.DownloadMedia.name -> {
                    ValidApiRequestOperation.DownloadMedia.JSONConverter()
                }
                ApiRequestOperationType.DeleteMedia.name -> {
                    ValidApiRequestOperation.DeleteMedia.JSONConverter()
                }
                ApiRequestOperationType.UpdateMediaAltText.name -> {
                    ValidApiRequestOperation.UpdateMediaAltText.JSONConverter()
                }
                ApiRequestOperationType.InvalidUpdateNote.name -> {
                    InvalidApiRequestOperation.InvalidUpdateNote.JSONConverter()
                }
                ApiRequestOperationType.InvalidDeleteNote.name -> {
                    InvalidApiRequestOperation.InvalidDeleteNote.JSONConverter()
                }
                ApiRequestOperationType.InvalidUploadMedia.name -> {
                    InvalidApiRequestOperation.InvalidUploadMedia.JSONConverter()
                }
                ApiRequestOperationType.InvalidDeleteMedia.name -> {
                    InvalidApiRequestOperation.InvalidDeleteMedia.JSONConverter()
                }
                ApiRequestOperationType.InvalidUpdateMediaAltText.name -> {
                    InvalidApiRequestOperation.InvalidUpdateMediaAltText.JSONConverter()
                }
                else -> null
            }
        }
    }

    sealed class ValidApiRequestOperation : ApiRequestOperation() {

        data class Sync(
            val deltaToken: Token.Delta?,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) :
            ValidApiRequestOperation(), Serializable {

            override val type: ApiRequestOperationType = ApiRequestOperationType.Sync

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this)
            ) { eventMarker ->
                when (deltaToken) {
                    null -> false
                    else -> eventMarker != EventMarkers.SyncRequestFailed
                }
            }

            @Suppress("UnsafeCast")
            class JSONConverter : IJsonConverter<Sync> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): Sync? {
                    val deltaToken = if (map["deltaToken"] as? Map<String, Any> != null) {
                        Token.Delta.fromMap(map["deltaToken"] as Map<String, Any>)
                    } else {
                        null
                    }
                    return Sync(deltaToken)
                }

                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    return json
                }
            }
        }

        data class NoteReferencesSync(
            val deltaToken: Token.Delta?,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) : ValidApiRequestOperation(), Serializable {

            override val type: ApiRequestOperationType = ApiRequestOperationType.NoteReferencesSync

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this)
            ) { eventMarker ->
                when (deltaToken) {
                    null -> false
                    else -> eventMarker != EventMarkers.SyncRequestFailed
                }
            }

            @Suppress("UnsafeCast")
            class JSONConverter : IJsonConverter<NoteReferencesSync> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): NoteReferencesSync? {
                    val deltaToken = if (map["deltaToken"] as? Map<String, Any> != null) {
                        Token.Delta.fromMap(map["deltaToken"] as Map<String, Any>)
                    } else {
                        null
                    }
                    return NoteReferencesSync(deltaToken)
                }

                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    return json
                }
            }
        }

        data class MeetingNotesSync(
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) : ValidApiRequestOperation(), Serializable {
            override val type: ApiRequestOperationType = ApiRequestOperationType.MeetingNotesSync

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this)
            ) { eventMarker ->
                eventMarker != EventMarkers.SyncRequestFailed
            }

            @Suppress("UnsafeCast")
            class JSONConverter : IJsonConverter<MeetingNotesSync> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): MeetingNotesSync = MeetingNotesSync()

                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json
                    return json
                }
            }
        }

        data class SamsungNotesSync(
            val deltaToken: Token.Delta?,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) : ValidApiRequestOperation(), Serializable {

            override val type: ApiRequestOperationType = ApiRequestOperationType.SamsungNotesSync

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this)
            ) { eventMarker ->
                when (deltaToken) {
                    null -> false
                    else -> eventMarker != EventMarkers.SyncRequestFailed
                }
            }

            @Suppress("UnsafeCast")
            class JSONConverter : IJsonConverter<SamsungNotesSync> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): SamsungNotesSync? {
                    val deltaToken = if (map["deltaToken"] as? Map<String, Any> != null) {
                        Token.Delta.fromMap(map["deltaToken"] as Map<String, Any>)
                    } else {
                        null
                    }
                    return SamsungNotesSync(deltaToken)
                }

                override fun migrate(json: Any, old: Int, new: Int): Any = json
            }
        }

        data class CreateNote(
            val note: Note,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) : ValidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.CreateNote

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = note.id, filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<CreateNote> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): CreateNote? {
                    val note = Note.fromMap(map["note"] as? Map<String, Any> ?: return null) ?: return null
                    return CreateNote(note)
                }

                @Suppress("UNCHECKED_CAST")
                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    val map = (json as? Map<String, Any> ?: return json).toMutableMap()
                    val note = map["note"]
                    if (note != null) {
                        map["note"] = Note.migrate(note, old, new)
                    }

                    return map
                }
            }
        }

        data class UpdateNote(
            val note: Note,
            val uiBaseRevision: Long,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) :
            ValidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.UpdateNote

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this), noteId = note.id
            )

            class JSONConverter : IJsonConverter<UpdateNote> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): UpdateNote? {
                    val note = Note.fromMap(map["note"] as? Map<String, Any> ?: return null) ?: return null
                    val uiBaseRevision = (map["uiBaseRevision"] as? Double)?.toLong() ?: return null
                    return UpdateNote(
                        note,
                        uiBaseRevision
                    )
                }

                @Suppress("UNCHECKED_CAST")
                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    val map = (json as? Map<String, Any> ?: return json).toMutableMap()
                    val note = map["note"]
                    if (note != null) {
                        map["note"] = Note.migrate(note, old, new)
                    }

                    return map
                }
            }
        }

        data class DeleteNote(
            val localId: String,
            val remoteId: String,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) :
            ValidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.DeleteNote

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = remoteId, filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<DeleteNote> {
                override fun fromMap(map: Map<String, Any>): DeleteNote? {
                    val localId = map["localId"] as? String ?: return null
                    val remoteId = map["remoteId"] as? String ?: return null
                    return DeleteNote(
                        localId,
                        remoteId
                    )
                }

                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    return json
                }
            }
        }

        data class DeleteNoteReference(
            val localId: String,
            val remoteId: String,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) : ValidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.DeleteNoteReference

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = remoteId, filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<DeleteNoteReference> {
                override fun fromMap(map: Map<String, Any>): DeleteNoteReference? {
                    val localId = map["localId"] as? String ?: return null
                    val remoteId = map["remoteId"] as? String ?: return null
                    return DeleteNoteReference(localId, remoteId)
                }

                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    return json
                }
            }
        }

        data class DeleteSamsungNote(
            val localId: String,
            val remoteId: String,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) : ValidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.DeleteSamsungNote

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = remoteId, filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<DeleteSamsungNote> {
                override fun fromMap(map: Map<String, Any>): DeleteSamsungNote? {
                    val localId = map["localId"] as? String ?: return null
                    val remoteId = map["remoteId"] as? String ?: return null
                    return DeleteSamsungNote(localId, remoteId)
                }

                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    return json
                }
            }
        }

        data class GetNoteForMerge(
            val note: Note,
            val uiBaseRevision: Long,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) :
            ValidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.GetNoteForMerge

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = note.id, filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<GetNoteForMerge> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): GetNoteForMerge? {
                    val note = Note.fromMap(map["note"] as? Map<String, Any> ?: return null) ?: return null
                    val uiBaseRevision = (map["uiBaseRevision"] as? Double)?.toLong() ?: return null
                    return GetNoteForMerge(
                        note, uiBaseRevision
                    )
                }

                @Suppress("UNCHECKED_CAST")
                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    val map = (json as? Map<String, Any> ?: return json).toMutableMap()
                    val note = map["note"]
                    if (note != null) {
                        map["note"] = Note.migrate(note, old, new)
                    }

                    return map
                }
            }
        }

        data class UploadMedia(
            val note: Note,
            val mediaLocalId: String,
            val localUrl: String,
            val mimeType: String,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) :
            ValidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.UploadMedia

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = note.id,
                metaData = mutableListOf(Pair(NotesSDKTelemetryKeys.NoteProperty.IMAGE_LOCAL_ID, mediaLocalId)),
                filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<UploadMedia> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): UploadMedia? {
                    val note = Note.fromMap(map["note"] as? Map<String, Any> ?: return null) ?: return null
                    val mediaLocalId = map["mediaLocalId"] as? String ?: return null
                    val localUrl = map["localUrl"] as? String ?: return null
                    val mimeType = map["mimeType"] as? String ?: return null
                    return UploadMedia(
                        note,
                        mediaLocalId, localUrl, mimeType
                    )
                }

                @Suppress("UNCHECKED_CAST")
                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    val map = (json as? Map<String, Any> ?: return json).toMutableMap()
                    val note = map["note"]
                    if (note != null) {
                        map["note"] = Note.migrate(note, old, new)
                    }

                    return map
                }
            }
        }

        data class DownloadMedia(
            val note: Note,
            val mediaRemoteId: String,
            val mimeType: String,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) :
            ValidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.DownloadMedia

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = note.id,
                metaData = mutableListOf(
                    Pair(NotesSDKTelemetryKeys.NoteProperty.IMAGE_REMOTE_ID, mediaRemoteId)
                )
            )

            class JSONConverter : IJsonConverter<DownloadMedia> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): DownloadMedia? {
                    val note = Note.fromMap(map["note"] as? Map<String, Any> ?: return null) ?: return null
                    val mediaRemoteId = map["mediaRemoteId"] as? String ?: return null
                    val mimeType = map["mimeType"] as? String ?: return null
                    return DownloadMedia(
                        note,
                        mediaRemoteId, mimeType
                    )
                }

                @Suppress("UNCHECKED_CAST")
                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    val map = (json as? Map<String, Any> ?: return json).toMutableMap()
                    val note = map["note"]
                    if (note != null) {
                        map["note"] = Note.migrate(note, old, new)
                    }

                    return map
                }
            }
        }

        data class DeleteMedia(
            val localNoteId: String,
            val remoteNoteId: String,
            val localMediaId: String,
            val remoteMediaId: String,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) :
            ValidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.DeleteMedia

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = remoteNoteId,
                filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<DeleteMedia> {
                override fun fromMap(map: Map<String, Any>): DeleteMedia? {
                    val localNoteId = map["localNoteId"] as? String ?: return null
                    val remoteNoteId = map["remoteNoteId"] as? String ?: return null
                    val localMediaId = map["localMediaId"] as? String ?: return null
                    val remoteMediaId = map["remoteMediaId"] as? String ?: return null
                    return DeleteMedia(
                        localNoteId, remoteNoteId, localMediaId, remoteMediaId
                    )
                }

                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    return json
                }
            }
        }

        data class UpdateMediaAltText(
            val note: Note,
            val localMediaId: String,
            val remoteMediaId: String,
            val altText: String?,
            val uiBaseRevision: Long,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) :
            ValidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.UpdateMediaAltText

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = note.id,
                filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<UpdateMediaAltText> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): UpdateMediaAltText? {
                    val note = Note.fromMap(map["note"] as? Map<String, Any> ?: return null) ?: return null
                    val localMediaId = map["localMediaId"] as? String ?: return null
                    val remoteMediaId = map["remoteMediaId"] as? String ?: return null
                    val altText = map["altText"] as? String ?: return null
                    val uiBaseRevision = (map["uiBaseRevision"] as? Double)?.toLong() ?: return null
                    return UpdateMediaAltText(
                        note, localMediaId, remoteMediaId, altText, uiBaseRevision
                    )
                }

                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    return json
                }
            }
        }
    }

    sealed class InvalidApiRequestOperation : ApiRequestOperation() {
        data class InvalidUpdateNote(
            val note: Note,
            val uiBaseRevision: Long,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) :
            InvalidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.InvalidUpdateNote

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = note.id, filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<InvalidUpdateNote> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): InvalidUpdateNote? {
                    val note = Note.fromMap(map["note"] as? Map<String, Any> ?: return null) ?: return null
                    val uiBaseRevision = (map["uiBaseRevision"] as? Double)?.toLong() ?: return null
                    return InvalidUpdateNote(
                        note, uiBaseRevision
                    )
                }

                @Suppress("UNCHECKED_CAST")
                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    val map = (json as? Map<String, Any> ?: return json).toMutableMap()
                    val note = map["note"]
                    if (note != null) {
                        map["note"] = Note.migrate(note, old, new)
                    }

                    return map
                }
            }
        }

        data class InvalidDeleteNote(
            val localId: String,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) :
            InvalidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.InvalidDeleteNote

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = localId, filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<InvalidDeleteNote> {
                override fun fromMap(map: Map<String, Any>): InvalidDeleteNote? {
                    val localId = map["localId"] as? String ?: return null
                    return InvalidDeleteNote(
                        localId
                    )
                }

                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    return json
                }
            }
        }

        data class InvalidUploadMedia(
            val note: Note,
            val mediaLocalId: String,
            val localUrl: String,
            val mimeType: String,
            @Transient override val uniqueId: String =
                UUID.randomUUID().toString()
        ) :
            InvalidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.InvalidUploadMedia

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = note.id,
                metaData = mutableListOf(Pair(NotesSDKTelemetryKeys.NoteProperty.IMAGE_LOCAL_ID, mediaLocalId)),
                filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<InvalidUploadMedia> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): InvalidUploadMedia? {
                    val note = Note.fromMap(map["note"] as? Map<String, Any> ?: return null) ?: return null
                    val mediaLocalId = map["mediaLocalId"] as? String ?: return null
                    val localUrl = map["localUrl"] as? String ?: return null
                    val mimeType = map["mimeType"] as? String ?: return null
                    return InvalidUploadMedia(
                        note, mediaLocalId, localUrl, mimeType
                    )
                }

                @Suppress("UNCHECKED_CAST")
                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    val map = (json as? Map<String, Any> ?: return json).toMutableMap()
                    val note = map["note"]
                    if (note != null) {
                        map["note"] = Note.migrate(note, old, new)
                    }

                    return map
                }
            }
        }

        data class InvalidDeleteMedia(
            val noteLocalId: String,
            val mediaLocalId: String,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) :
            InvalidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.InvalidDeleteMedia

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = noteLocalId, filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<InvalidDeleteMedia> {
                override fun fromMap(map: Map<String, Any>): InvalidDeleteMedia? {
                    val noteLocalId = map["noteLocalId"] as? String ?: return null
                    val mediaLocalId = map["mediaLocalId"] as? String ?: return null
                    return InvalidDeleteMedia(
                        noteLocalId, mediaLocalId
                    )
                }

                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    return json
                }
            }
        }

        data class InvalidUpdateMediaAltText(
            val note: Note,
            val mediaLocalId: String,
            val altText: String?,
            val uiBaseRevision: Long,
            @Transient override val uniqueId: String = UUID.randomUUID().toString()
        ) :
            InvalidApiRequestOperation(), Serializable {
            override val type = ApiRequestOperationType.InvalidUpdateMediaAltText

            @Transient
            override val telemetryBundle: SyncRequestTelemetry = SyncRequestTelemetry(
                requestType = SyncRequestType.requestType(this),
                noteId = note.id, filterOutEventMarker = { false }
            )

            class JSONConverter : IJsonConverter<InvalidUpdateMediaAltText> {
                @Suppress("UNCHECKED_CAST")
                override fun fromMap(map: Map<String, Any>): InvalidUpdateMediaAltText? {
                    val note = Note.fromMap(map["note"] as? Map<String, Any> ?: return null) ?: return null
                    val mediaLocalId = map["mediaLocalId"] as? String ?: return null
                    val altText = map["altText"] as? String ?: return null
                    val uiBaseRevision = (map["uiBaseRevision"] as? Double)?.toLong() ?: return null
                    return InvalidUpdateMediaAltText(
                        note, mediaLocalId, altText, uiBaseRevision
                    )
                }

                @Suppress("UNCHECKED_CAST")
                override fun migrate(json: Any, old: Int, new: Int): Any {
                    if (old <= 0 || old >= new) return json

                    val map = (json as? Map<String, Any> ?: return json).toMutableMap()
                    val note = map["note"]
                    if (note != null) {
                        map["note"] = Note.migrate(note, old, new)
                    }

                    return map
                }
            }
        }
    }
}

enum class Priority : Serializable {
    Critical,
    High,
    Medium,
    Low,
    Invalid
}
