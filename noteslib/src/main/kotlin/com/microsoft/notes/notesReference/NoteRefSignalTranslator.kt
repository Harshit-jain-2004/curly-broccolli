package com.microsoft.notes.notesReference

import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.NoteReferenceMedia
import com.microsoft.notes.models.NoteReferenceUpdate
import com.microsoft.notes.models.extensions.NoteRefColor
import com.microsoft.notes.notesReference.models.NoteRefLocalChanges
import com.microsoft.notes.notesReference.models.NoteRefSourceId
import com.microsoft.notes.notesReference.models.PageChangeSignalMetaData
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import com.microsoft.notes.utils.utils.parseISO8601StringToMillis
import java.lang.IllegalStateException

fun translatePageChangedSignal(
    localNotes: List<NoteReference>,
    pageLocalId: String,
    pageSourceId: NoteRefSourceId?,
    metaData: PageChangeSignalMetaData,
    isDebugMode: Boolean = false
): NoteRefLocalChanges = translatePageChangedSignalInternal(
    localNotes = localNotes,
    pageLocalId = pageLocalId,
    pageSourceId = pageSourceId,
    metaData = metaData,
    acceptSignalPageContentWhenSameLMT = true,
    isDebugMode = isDebugMode
)

private fun translatePageChangedSignalInternal(
    localNotes: List<NoteReference>,
    pageLocalId: String,
    pageSourceId: NoteRefSourceId?,
    metaData: PageChangeSignalMetaData,
    acceptSignalPageContentWhenSameLMT: Boolean,
    isDebugMode: Boolean = false
): NoteRefLocalChanges {

    var changes = NoteRefLocalChanges()
    if (pageLocalId.isEmpty()) {
        return changes
    }

    val notesList = getValidNoteReferencesForUser(localNotes)
    val (foundNote, duplicateNotes) = notesList.findMatchingNotes(pageLocalId, pageSourceId, isDebugMode)
    duplicateNotes?.forEach {
        changes = changes.appendToDelete(it)
    }

    val lastModifiedAt = parseISO8601StringToMillis(metaData.lastModifiedDateTime)

    foundNote?.let {
        if (lastModifiedAt >= foundNote.lastModifiedAt) {
            var replacementNote = foundNote.copy(
                pageLocalId = pageLocalId,
                pageSourceId = getBetterSourceId(pageSourceId, foundNote.pageSourceId)
            )

            if (lastModifiedAt > foundNote.lastModifiedAt || acceptSignalPageContentWhenSameLMT) {
                replacementNote = replacementNote.mergePageMetaData(metaData)
            }
            changes = changes.appendToReplace(NoteReferenceUpdate(replacementNote))
        }
    } ?: run {
        changes = changes.appendToCreate(
            NoteReference(
                pageLocalId = pageLocalId,
                pageSourceId = pageSourceId,
                isLocalOnlyPage = true
            ).mergePageMetaData(metaData)
        )
    }

    return changes
}

fun translatePageDeletedSignal(
    localNotes: List<NoteReference>,
    pageLocalId: String,
    pageSourceId: NoteRefSourceId?,
    isDebugMode: Boolean = false
): NoteRefLocalChanges {
    var changes = NoteRefLocalChanges()
    if (pageLocalId.isEmpty()) {
        return changes
    }

    val notesList = getValidNoteReferencesForUser(localNotes)
    val (foundNote, duplicateNotes) = notesList.findMatchingNotes(pageLocalId, pageSourceId, isDebugMode)
    duplicateNotes?.forEach {
        changes = changes.appendToDelete(it)
    }

    foundNote?.let {
        changes = if (it.pageSourceId is NoteRefSourceId.FullSourceId) {
            changes.appendToMarkAsDeleted(it)
        } else {
            changes.appendToDelete(it)
        }
    }
    return changes
}

fun translateSectionChangedSignal(
    localNotes: List<NoteReference>,
    sectionLocalId: String,
    sectionSourceId: NoteRefSourceId?,
    sectionName: String
): NoteRefLocalChanges {
    if (sectionLocalId.isEmpty()) {
        return NoteRefLocalChanges()
    }

    val toReplace = mutableListOf<NoteReferenceUpdate>()

    val notesList = getValidNoteReferencesForUser(localNotes)
    notesList.forEach {
        if (it.noteBelongsToSection(sectionLocalId, sectionSourceId)) {
            toReplace.add(
                NoteReferenceUpdate(
                    it.copy(
                        sectionLocalId = sectionLocalId,
                        sectionSourceId =
                        if (sectionSourceId is NoteRefSourceId.FullSourceId) sectionSourceId
                        else it.sectionSourceId,
                        containerName = sectionName
                    )
                )
            )
        }
    }
    return NoteRefLocalChanges(toReplace = toReplace)
}

fun translateSectionDeletedSignal(
    localNotes: List<NoteReference>,
    sectionLocalId: String,
    sectionSourceId: NoteRefSourceId?
): NoteRefLocalChanges {
    if (sectionLocalId.isEmpty()) {
        return NoteRefLocalChanges()
    }

    val toDelete = mutableListOf<NoteReference>()
    val toMarkAsDeleted = mutableListOf<NoteReference>()

    val notesList = getValidNoteReferencesForUser(localNotes)
    notesList.forEach {
        if (it.noteBelongsToSection(sectionLocalId, sectionSourceId)) {
            if (it.pageSourceId is NoteRefSourceId.FullSourceId) {
                toMarkAsDeleted.add(it)
            } else {
                toDelete.add(it)
            }
        }
    }
    return NoteRefLocalChanges(toDelete = toDelete, toMarkAsDeleted = toMarkAsDeleted)
}

fun translateAppendPageIfNeededSignal(
    localNotes: List<NoteReference>,
    pageLocalId: String,
    pageSourceId: NoteRefSourceId?,
    metaData: PageChangeSignalMetaData,
    isDebugMode: Boolean = false
): NoteRefLocalChanges = translatePageChangedSignalInternal(
    localNotes = localNotes,
    pageLocalId = pageLocalId,
    pageSourceId = pageSourceId,
    metaData = metaData,
    acceptSignalPageContentWhenSameLMT = false,
    isDebugMode = isDebugMode
)

//  a new note originated from PageChangeSignal may have localId same as that of a deleted note
// so filter the noteReferences before operating on them
private fun getValidNoteReferencesForUser(localNotes: List<NoteReference>) =
    localNotes.filter { !it.isDeleted }

/*
 returns Pair(Matched Note, Duplicate Notes)
 If no note is found, 'Matched Note' is null
 If two or more notes are found, one mapping to pageSourceId is returned in 'Matched Note', others mapping to
 pageLocalId are returned in 'Duplicate Note'
  (in a consistent state, max two notes can exist in list corresponding to a set of pageIds)
 */
private fun List<NoteReference>.findMatchingNotes(pageLocalId: String, pageSourceId: NoteRefSourceId?, isDebugMode: Boolean):
    Pair<NoteReference?, List<NoteReference>?> {

    val matchedNotes = this.filter {
        it.pageLocalId == pageLocalId ||
            (
                it.pageSourceId is NoteRefSourceId.FullSourceId && it.webUrl != null &&
                    pageSourceId?.isSameId(it.pageSourceId, it.webUrl) == true
                )
    }

    if (matchedNotes.isEmpty()) {
        return Pair(null, null)
    }

    return when (matchedNotes.size) {
        0 -> Pair(null, null)
        1 -> Pair(matchedNotes[0], null)
        2 ->
            // keep the note with matched SourceId and delete the other (duplicate)
            if (matchedNotes[0].pageSourceId is NoteRefSourceId.FullSourceId) {
                Pair(matchedNotes[0], listOf(matchedNotes[1]))
            } else {
                Pair(matchedNotes[1], listOf(matchedNotes[0]))
            }
        else -> {
            NotesLibrary.getInstance().recordTelemetry(
                EventMarkers.DuplicatePageSourceId,
                Pair(NotesSDKTelemetryKeys.FeedProperty.MATCHED_NOTE_REFERENCE_SIZE, matchedNotes.size.toString())
            )
            if (isDebugMode) {
                throw IllegalStateException("more than two notes found corresponding to same pageIds")
            }
            val noteToKeep = (
                matchedNotes.firstOrNull { it.pageSourceId is NoteRefSourceId.FullSourceId }
                ) ?: matchedNotes[0]
            return Pair(noteToKeep, matchedNotes - noteToKeep)
        }
    }
}

private fun NoteReference.noteBelongsToSection(sectionLocalId: String, sectionSourceId: NoteRefSourceId?):
    Boolean {
    return this.sectionLocalId == sectionLocalId ||
        (
            this.sectionSourceId != null && this.webUrl != null &&
                sectionSourceId?.isSameId(this.sectionSourceId, this.webUrl) == true
            )
}

private fun getBetterSourceId(id1: NoteRefSourceId?, id2: NoteRefSourceId?): NoteRefSourceId? {
    if (getSourceIdRank(id1) <= getSourceIdRank(id2)) {
        return id1
    }
    return id2
}

private fun getSourceIdRank(id: NoteRefSourceId?): Int {
    return when (id) {
        is NoteRefSourceId.FullSourceId -> 1
        is NoteRefSourceId.PartialSourceId -> 2
        null -> 3
    }
}

private fun NoteReference.mergePageMetaData(metaData: PageChangeSignalMetaData): NoteReference =
    this.copy(
        sectionLocalId = metaData.sectionLocalId,
        sectionSourceId = metaData.sectionSourceId ?: this.sectionSourceId,
        color = NoteRefColor.fromColorInt(metaData.color?.toIntOrNull()),
        webUrl = metaData.webUrl,
        clientUrl = metaData.clientUrl,
        createdAt = parseISO8601StringToMillis(metaData.createdDateTime),
        lastModifiedAt = parseISO8601StringToMillis(metaData.lastModifiedDateTime),
        title = metaData.title,
        containerName = metaData.sectionName,
        rootContainerName = metaData.notebookName,
        previewImageUrl = metaData.previewImageUrl,
        previewText = metaData.previewText,
        isMediaPresent = metaData.isMediaPresent,
        previewRichText = metaData.previewRichText,
        media = mergeMedia(metaData.media, this.media)
    )

private fun mergeMedia(newMedia: List<NoteReferenceMedia>?, currentMedia: List<NoteReferenceMedia>?): List<NoteReferenceMedia>? {

    if (newMedia.isNullOrEmpty() && !currentMedia.isNullOrEmpty()) {
        NotesLibrary.getInstance().deleteCachedImage(currentMedia[0].mediaID)
        return newMedia
    }
    if (newMedia?.size != 0 && currentMedia?.size != 0) {
        if (newMedia?.get(0)?.mediaID == currentMedia?.get(0)?.mediaID) {
            // when same media comes from local sigs, keep the existing media to avoid recalculating local image url
            return currentMedia
        } else {
            // when media id is updated, deleted the existing cached image.
            currentMedia?.get(0)?.mediaID?.let { NotesLibrary.getInstance().deleteCachedImage(it) }
        }
    }
    return newMedia
}
