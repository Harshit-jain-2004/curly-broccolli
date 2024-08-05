package com.microsoft.notes.models

import com.microsoft.notes.models.extensions.NoteRefColor
import com.microsoft.notes.models.extensions.describe
import com.microsoft.notes.models.extensions.describeMeetingNotesList
import com.microsoft.notes.models.extensions.describeNoteReferencesList
import com.microsoft.notes.models.extensions.sortedByLastModified
import com.microsoft.notes.notesReference.models.NoteRefSourceId
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.DocumentType
import com.microsoft.notes.richtext.scheme.NoteMetadata
import com.microsoft.notes.richtext.scheme.asString
import com.microsoft.notes.richtext.scheme.isEmpty
import com.microsoft.notes.richtext.scheme.mediaListCount
import com.microsoft.notes.richtext.scheme.paragraphListCount
import com.microsoft.notes.sideeffect.persistence.extensions.gsonSerializer
import com.microsoft.notes.sideeffect.persistence.mapper.toPersistenceNote
import com.microsoft.notes.utils.logging.NoteColor
import com.microsoft.notes.utils.logging.NoteType
import com.microsoft.notes.utils.utils.parseISO8601StringToMillis
import java.io.Serializable

const val LOCAL_ID = "localId"
const val DEFAULT_DURATION: Long = 86400000 // milliseconds in a day

fun generateLocalId(): String = com.microsoft.notes.richtext.scheme.generateLocalId()

enum class Color(val value: Int) {
    YELLOW(1),
    GREEN(2),
    PINK(3),
    PURPLE(4),
    BLUE(5),
    GREY(0),
    CHARCOAL(6);

    companion object {
        val NUMBER_OF_COLORS = Color.values().size

        @JvmStatic
        fun getDefault() = YELLOW
    }
}

enum class FontColor(val value: Int) {
    DARK(0),
    LIGHT(1)
}

fun Color.getFontColor() = if (this == Color.CHARCOAL) {
    FontColor.LIGHT
} else {
    FontColor.DARK
}

fun Color.toTelemetryColor(): NoteColor {
    return when (this) {
        Color.GREY -> NoteColor.Grey
        Color.YELLOW -> NoteColor.Yellow
        Color.GREEN -> NoteColor.Green
        Color.PINK -> NoteColor.Pink
        Color.PURPLE -> NoteColor.Purple
        Color.BLUE -> NoteColor.Blue
        Color.CHARCOAL -> NoteColor.Charcoal
    }
}

fun Note.toTelemetryNoteType(): NoteType {
    if (isEmpty)
        return NoteType.Empty

    return when (document.type) {
        DocumentType.RICH_TEXT -> when {
            document.isEmpty() -> NoteType.Image
            isMediaListEmpty -> NoteType.Text
            else -> NoteType.TextWithImage
        }
        DocumentType.RENDERED_INK -> NoteType.Ink
        DocumentType.INK -> NoteType.Ink
        DocumentType.FUTURE -> NoteType.Future
        DocumentType.SAMSUNG_NOTE -> NoteType.SamsungNote
    }
}

data class RemoteData(
    val id: String,
    val changeKey: String,
    val lastServerVersion: Note,
    val createdAt: Long,
    val lastModifiedAt: Long
) : Serializable {
    constructor(
        id: String,
        changeKey: String,
        lastServerVersion: Note,
        createdAt: String,
        lastModifiedAt: String
    ) : this(
        id, changeKey, lastServerVersion,
        createdAt = parseISO8601StringToMillis(createdAt),
        lastModifiedAt = parseISO8601StringToMillis(lastModifiedAt)
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            val map = (json as? Map<String, Any> ?: return json).toMutableMap()

            if (old < 2 && new >= 2) {
                val lastServerVersion = map["lastServerVersion"] as? Map<String, Any> ?: return map
                map["lastServerVersion"] = Note.migrate(
                    lastServerVersion, old,
                    new
                )
            }

            return map
        }
    }
}

data class ImageDimensions(
    val height: Long,
    val width: Long
)

data class NoteReferenceMedia(
    val mediaID: String,
    val mediaType: String,
    val localImageUrl: String?
) : Serializable

data class Media(
    val localId: String,
    val remoteId: String? = null,
    val localUrl: String? = null,
    val mimeType: String,
    val altText: String? = null,
    val imageDimensions: ImageDimensions? = null,
    val lastModified: Long
) : Serializable {

    val hasRemoteId: Boolean
        get() = remoteId != null

    val hasLocalUrl: Boolean
        get() = localUrl != null

    val hasImageDimensions: Boolean
        get() = imageDimensions != null

    constructor(
        localUrl: String,
        mimeType: String,
        altText: String?,
        imageDimensions: ImageDimensions?
    ) : this(
        localId = generateLocalId(),
        remoteId = null,
        localUrl = localUrl,
        mimeType = mimeType,
        altText = altText,
        imageDimensions = imageDimensions,
        lastModified = System.currentTimeMillis()
    )

    constructor(
        localId: String,
        remoteId: String? = null,
        localUrl: String? = null,
        mimeType: String,
        altText: String? = null,
        imageDimensions: ImageDimensions? = null,
        lastModified: String
    ) : this(
        localId = localId,
        remoteId = remoteId,
        localUrl = localUrl,
        mimeType = mimeType,
        altText = altText,
        imageDimensions = imageDimensions,
        lastModified = parseISO8601StringToMillis(lastModified)
    )
}

data class Note(
    val localId: String = generateLocalId(),
    val remoteData: RemoteData? = null,
    val document: Document = Document(),
    val media: List<Media> = listOf(),
    val isDeleted: Boolean = false,
    val color: Color = Color.getDefault(),
    val localCreatedAt: Long = System.currentTimeMillis(),
    val documentModifiedAt: Long = System.currentTimeMillis(),
    val uiRevision: Long = 0,
    val uiShadow: Note? = null,
    val createdByApp: String? = null,
    val title: String? = null,
    val isPinned: Boolean = false,
    val pinnedAt: Long? = null,
    val metadata: NoteMetadata = NoteMetadata()
) : Serializable {

    val fontColor: FontColor
        get() = color.getFontColor()

    val mediaCount: Int
        get() = document.mediaListCount() + media.size

    fun serialize(): String = gsonSerializer.toJson(this.toPersistenceNote())

    val mediaCountWithoutInlineMedia: Int
        get() = media.size

    // Rendered ink and future are never considered empty
    val isDocumentEmpty: Boolean
        get() = when (document.type) {
            DocumentType.RICH_TEXT -> hasNoText && isMediaListEmpty
            DocumentType.INK -> hasNoStrokes
            DocumentType.SAMSUNG_NOTE -> false
            else -> false
        }

    val isParagraphListEmpty: Boolean
        get() = document.paragraphListCount() == 0

    val isMediaListEmpty: Boolean
        get() = media.isEmpty() && document.mediaListCount() == 0

    // TODO: fix this for SamsungNote
    val hasNoText: Boolean
        get() = isParagraphListEmpty || document.asString().trim().isEmpty()

    val hasNoBodyPreview: Boolean
        get() = document.bodyPreview.trim().isEmpty()

    val hasNoStrokes: Boolean
        get() = document.strokes.isEmpty()

    val isEmpty: Boolean
        get() = isDocumentEmpty && !isDeleted

    val isRichTextNote: Boolean
        get() = document.type == DocumentType.RICH_TEXT

    val isRenderedInkNote: Boolean
        get() = document.type == DocumentType.RENDERED_INK

    val isInkNote: Boolean
        get() = document.type == DocumentType.INK

    val isVoiceNote: Boolean
        get() {
            if (isMediaListEmpty) return false
            else {
                for (item in media) {
                    if (item.mimeType.contains("gif")) {
                        return true
                    }
                }
                return false
            }
        }

    val isFutureNote: Boolean
        get() = document.type == DocumentType.FUTURE

    val sortedMedia: List<Media>
        get() = media.sortedByLastModified()

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun migrate(json: Any, old: Int, new: Int): Any {
            if (old <= 0 || old >= new) return json

            val map = (json as? Map<String, Any> ?: return json).toMutableMap()

            if (old < 2 && new >= 2) {
                val document = map["document"] as? Map<String, Any> ?: return map
                map["document"] = Document.migrate(document, old, new)
            }

            return map
        }
    }
}

data class NoteUpdate(val noteFromServer: Note, val uiRevision: Long = 0)

data class Changes(
    val toCreate: List<Note> = emptyList(),
    val toReplace: List<NoteUpdate> = emptyList(),
    val toDelete: List<Note> = emptyList()
) {

    companion object {
        const val DELIMITER = "\n: "
    }

    fun count(): Int = toCreate.size + toReplace.size + toDelete.size

    fun isEmpty(): Boolean = toCreate.isEmpty() && toReplace.isEmpty() && toDelete.isEmpty()

    fun appendToCreate(created: Note): Changes = Changes(
        toCreate + created, toReplace, toDelete
    )

    fun appendToReplace(replaced: NoteUpdate): Changes = Changes(
        toCreate, toReplace + replaced, toDelete
    )

    fun appendToDelete(deleted: Note): Changes = Changes(
        toCreate, toReplace, toDelete + deleted
    )

    fun appendToDelete(deleted: List<Note>): Changes = Changes(
        toCreate, toReplace, toDelete + deleted
    )

    override fun toString(): String {
        val stringBldr = StringBuilder()
        stringBldr.append(this::class.java.simpleName)
            .append("${DELIMITER}toCreate:  ${toCreate.describe()}")
            .append("${DELIMITER}toDelete:  ${toDelete.describe()}")
            .append("${DELIMITER}toReplace: ${toReplace.map { it.noteFromServer }.describe()}")

        return stringBldr.toString()
    }
}

data class NoteReference(
    val localId: String = generateLocalId(),
    val remoteId: String? = null,
    val type: String = "",
    val pageSourceId: NoteRefSourceId? = null,
    val pageLocalId: String? = null,
    val sectionSourceId: NoteRefSourceId.FullSourceId? = null,
    val sectionLocalId: String? = null,
    val isLocalOnlyPage: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val weight: Float? = null,
    val title: String? = null,
    val previewText: String = "",
    val previewImageUrl: String? = null,
    val color: NoteRefColor = NoteRefColor.getDefault(),
    val webUrl: String? = null,
    val clientUrl: String? = null,
    val containerName: String? = null,
    val rootContainerName: String? = null,
    val rootContainerSourceId: NoteRefSourceId.FullSourceId? = null,
    val isMediaPresent: Int? = null,
    val previewRichText: String? = null,
    val isPinned: Boolean = false,
    val pinnedAt: Long? = null,
    val media: List<NoteReferenceMedia>? = null /* Today we support single media only hence list will contain only single element */
)

data class NoteReferenceUpdate(val remoteNote: NoteReference)

data class NoteReferenceChanges(
    val toCreate: List<NoteReference> = emptyList(),
    val toReplace: List<NoteReferenceUpdate> = emptyList(),
    val toDelete: List<NoteReference> = emptyList()
) {

    companion object {
        const val DELIMITER = "\n: "
    }

    fun count(): Int = toCreate.size + toReplace.size + toDelete.size

    fun isEmpty(): Boolean = toCreate.isEmpty() && toReplace.isEmpty() && toDelete.isEmpty()

    fun appendToCreate(created: NoteReference): NoteReferenceChanges = copy(toCreate = toCreate + created)

    fun appendToReplace(replaced: NoteReferenceUpdate): NoteReferenceChanges = copy(toReplace = toReplace + replaced)

    fun appendToDelete(deleted: NoteReference): NoteReferenceChanges = copy(toDelete = toDelete + deleted)

    override fun toString(): String {
        val stringBldr = StringBuilder()
        stringBldr.append(this::class.java.simpleName)
            .append("${DELIMITER}toCreate:  ${toCreate.describeNoteReferencesList()}")
            .append("${DELIMITER}toDelete:  ${toDelete.describeNoteReferencesList()}")
            .append("${DELIMITER}toReplace: ${toReplace.map { it.remoteNote }.describeNoteReferencesList()}")

        return stringBldr.toString()
    }
}

data class MeetingNote(
    val localId: String = generateLocalId(),
    val remoteId: String,
    val fileName: String,
    val createdTime: Long = System.currentTimeMillis(),
    val lastModifiedTime: Long = System.currentTimeMillis(),
    val title: String,
    val type: String,
    val staticTeaser: String,
    val accessUrl: String,
    val containerUrl: String,
    val containerTitle: String,
    val docId: Long,
    val fileUrl: String,
    val driveId: String,
    val itemId: String,
    val modifiedBy: String,
    val modifiedByDisplayName: String,
    val meetingId: String?,
    val meetingSubject: String?,
    val meetingStartTime: Long?,
    val meetingEndTime: Long?,
    val meetingOrganizer: String?,
    val seriesMasterId: String?,
    val occuranceId: String?
)

data class MeetingNoteUpdate(val remoteNote: MeetingNote)

data class MeetingNoteChanges(
    val toCreate: List<MeetingNote> = emptyList(),
    val toReplace: List<MeetingNoteUpdate> = emptyList(),
    val toDelete: List<MeetingNote> = emptyList()
) {
    companion object {
        const val DELIMITER = "\n: "
    }

    fun count(): Int = toCreate.size + toReplace.size + toDelete.size

    fun isEmpty(): Boolean = toCreate.isEmpty() && toReplace.isEmpty() && toDelete.isEmpty()

    fun appendToCreate(created: MeetingNote): MeetingNoteChanges = copy(toCreate = toCreate + created)

    fun appendToReplace(replaced: MeetingNoteUpdate): MeetingNoteChanges = copy(toReplace = toReplace + replaced)

    fun appendToDelete(deleted: MeetingNote): MeetingNoteChanges = copy(toDelete = toDelete + deleted)

    override fun toString(): String {
        val stringBldr = StringBuilder()
        stringBldr.append(this::class.java.simpleName)
            .append("${DELIMITER}toCreate:  ${toCreate.describeMeetingNotesList()}")
            .append("${DELIMITER}toDelete:  ${toDelete.describeMeetingNotesList()}")
            .append("${DELIMITER}toReplace: ${toReplace.map { it.remoteNote }.describeMeetingNotesList()}")

        return stringBldr.toString()
    }
}

data class TimeDuration(
    val startTimeInEpochMillis: Long,
    val endTimeInEpochMillis: Long
) {
    companion object {
        var now = System.currentTimeMillis()
        val default = TimeDuration(now, now + DEFAULT_DURATION)
    }
}

data class ScheduledAlarm(
    val alarmLocalId: String, // noteId+alarmDateTime
    val noteId: String,
    val alarmDateTime: Long,
    val isLogged: Boolean = false
) : Serializable

data class ReminderWrapper(
    var timeReminder: Reminder.TimeReminder? = null
) : Serializable

sealed class Reminder(val reminderLocalId: String = generateLocalId()) {
    class TimeReminder(
        val reminderDateTime: Long,
        val recurrenceType: RecurrenceType
    ) : Reminder()
}

enum class RecurrenceType {
    SINGLE,
    DAILY,
    WEEKLY,
    WEEKDAYS,
    MONTHLY,
    YEARLY,
    CUSTOM
}
