package com.microsoft.notes.noteslib.extensions

import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.NoteReferenceChanges
import com.microsoft.notes.models.generateLocalId
import com.microsoft.notes.notesReference.models.NoteRefSourceId
import com.microsoft.notes.sideeffect.sync.mapper.toStoreNoteReference
import com.microsoft.notes.sync.models.Container
import com.microsoft.notes.sync.models.NoteReferenceMedia
import com.microsoft.notes.sync.models.RemoteNoteReference
import com.microsoft.notes.sync.models.RemoteNoteReferenceMetaData
import com.microsoft.notes.sync.models.RemoteNoteReferenceVisualizationData
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class NoteReferenceChangesForHybridFeedTest {

    companion object {
        const val fullSourceIdPrefix = "xyz"
        const val nbUrl = "abdefg"
    }

    @Test
    fun `note reference create changes test with same FullSourceId`() {

        val localNoteReference1 = NoteReference(pageSourceId = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + "0"))
        val localNoteReference2 = NoteReference(pageSourceId = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + "1"))

        val localNoteReferences = listOf(localNoteReference1, localNoteReference2)
        val remoteNoteReferences = generateDummyRemoteNoteReferences(2)

        assertThat(calculateNoteReferencesChangesForHybrid(localNoteReferences, remoteNoteReferences).toCreate.isEmpty(), iz(true))
    }

    @Test
    fun `note reference create changes test with different FullSourceId`() {

        val localNoteReference1 = NoteReference(pageSourceId = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + "0"))
        val localNoteReference2 = NoteReference(pageSourceId = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + "1"))

        val localNoteReferences = listOf(localNoteReference1, localNoteReference2)
        val remoteNoteReferences = generateDummyRemoteNoteReferences(1, 9)

        var changes: NoteReferenceChanges = NoteReferenceChanges()
        remoteNoteReferences.forEach {
            changes = changes.appendToCreate(
                it.toStoreNoteReference(
                    localNoteReferenceId = generateLocalId(),
                    pageLocalId = null, sectionLocalId = null, isDeleted = false, isPinned = false, pinnedAt = null, localNoteReferenceMedia = null
                )
            )
        }

        assertThat(calculateNoteReferencesChangesForHybrid(localNoteReferences, remoteNoteReferences).toCreate.isEmpty(), iz(false))
    }

    @Test
    fun `note reference create changes test with same partialSourceId`() {

        val localNoteReference1 = NoteReference(pageSourceId = NoteRefSourceId.PartialSourceId("0", nbUrl))

        val localNoteReferences = listOf(localNoteReference1)
        val remoteNoteReferences = generateDummyRemoteNoteReferences(1)

        assertThat(calculateNoteReferencesChangesForHybrid(localNoteReferences, remoteNoteReferences).toCreate.isEmpty(), iz(true))
    }

    @Test
    fun `note reference create changes test with different partialSourceId`() {

        val localNoteReference1 = NoteReference(pageSourceId = NoteRefSourceId.PartialSourceId("0", nbUrl))

        val localNoteReferences = listOf(localNoteReference1)
        val remoteNoteReferences = generateDummyRemoteNoteReferences(1, 1)

        assertThat(calculateNoteReferencesChangesForHybrid(localNoteReferences, remoteNoteReferences).toCreate.isEmpty(), iz(false))
    }

    @Test
    fun `note reference update changes test with last modified of remote note greater`() {

        val localNoteReference1 = NoteReference(remoteId = "0", pageSourceId = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + "0"), lastModifiedAt = 1)

        val localNoteReferences = listOf(localNoteReference1)
        val remoteNoteReferences = generateDummyRemoteNoteReferences(1)

        assertThat(calculateNoteReferencesChangesForHybrid(localNoteReferences, remoteNoteReferences).toReplace[0].remoteNote.isLocalOnlyPage, iz(false))
    }

    @Test
    fun `note reference delete changes test with same FullSourceId`() {

        val localNoteReference1 = NoteReference(pageSourceId = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + "0"))
        val localNoteReference2 = NoteReference(pageSourceId = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + "1"))

        val localNoteReferences = listOf(localNoteReference1, localNoteReference2)
        val remoteNoteReferences = generateDummyRemoteNoteReferences(6)

        assertThat(calculateNoteReferencesChangesForHybrid(localNoteReferences, remoteNoteReferences).toDelete.isEmpty(), iz(true))
    }

    @Test
    fun `note reference delete changes test with different FullSourceId`() {

        val localNoteReference1 = NoteReference(pageSourceId = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + "10"))
        val localNoteReference2 = NoteReference(pageSourceId = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + "11"))

        val localNoteReferences = listOf(localNoteReference1, localNoteReference2)
        val remoteNoteReferences = generateDummyRemoteNoteReferences(6)

        val deletedNotes = listOf(localNoteReference1, localNoteReference2)

        assertThat(calculateNoteReferencesChangesForHybrid(localNoteReferences, remoteNoteReferences).toDelete, iz(deletedNotes))
    }

    @Test
    fun `note reference delete changes test with isLocalPage set to true`() {

        val localNoteReference1 = NoteReference(isLocalOnlyPage = true)
        val localNoteReference2 = NoteReference(pageSourceId = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + "1"))

        val localNoteReferences = listOf(localNoteReference1, localNoteReference2)
        val remoteNoteReferences = generateDummyRemoteNoteReferences(6)

        assertThat(calculateNoteReferencesChangesForHybrid(localNoteReferences, remoteNoteReferences).toDelete.isEmpty(), iz(true))
    }

    @Test
    fun `note reference delete changes test with isLocalPage set to false`() {

        val localNoteReference1 = NoteReference(pageSourceId = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + "9"))
        val localNoteReference2 = NoteReference(pageSourceId = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + "1"))

        val localNoteReferences = listOf(localNoteReference1, localNoteReference2)
        val remoteNoteReferences = generateDummyRemoteNoteReferences(6)

        val deletedNotes = listOf(localNoteReference1)

        assertThat(calculateNoteReferencesChangesForHybrid(localNoteReferences, remoteNoteReferences).toDelete, iz(deletedNotes))
    }

    private fun generateDummyRemoteNoteReferences(size: Int, firstNoteId: Int = 0): List<RemoteNoteReference> =
        (firstNoteId until firstNoteId + size).map {
            RemoteNoteReference(
                it.toString(),
                createdAt = "",
                lastModified = "",
                metaData = RemoteNoteReferenceMetaData(
                    createdAt = 999, lastModified = 999,
                    id = NoteRefSourceId.FullSourceId(fullSourceIdPrefix + it.toString()), type = "", webUrl = nbUrl + "hijkl",
                    clientUrl = ""
                ),
                visualizationData = RemoteNoteReferenceVisualizationData(
                    color = 0, previewText = "",
                    previewRichText = "", title = "", containers = listOf(Container("", "")), media = listOf(NoteReferenceMedia("", ""))
                )
            )
        }
}
