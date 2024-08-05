package com.microsoft.notes.notesReference

import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.NoteReferenceUpdate
import com.microsoft.notes.notesReference.models.NoteRefLocalChanges
import com.microsoft.notes.notesReference.models.NoteRefSourceId
import com.microsoft.notes.notesReference.models.PageChangeSignalMetaData
import com.microsoft.notes.utils.utils.parseMillisToISO8601String
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class NoteRefSignalTranslatorTest {

    private lateinit var baseLocalNotes: List<NoteReference>
    private lateinit var notesWithOnlyLocalId: List<NoteReference>
    private lateinit var notesWithOnlyFullSourceId: List<NoteReference>
    private lateinit var notesWithPartialId: List<NoteReference>
    private lateinit var notesWithBothLocalAndSourceId: List<NoteReference>

    @Before
    fun setUp() {
        val size = 3
        notesWithOnlyLocalId = generateNoteReferences(
            size = size,
            firstNoteId = 0,
            hasLocal = true,
            hasFullSource = false,
            hasPartial = false
        )
        notesWithOnlyFullSourceId = generateNoteReferences(
            size = size,
            firstNoteId = 1 * size,
            hasLocal = false,
            hasFullSource = true,
            hasPartial = false
        )
        notesWithPartialId = generateNoteReferences(
            size = size,
            firstNoteId = 2 * size,
            hasLocal = true,
            hasFullSource = false,
            hasPartial = true
        )
        notesWithBothLocalAndSourceId = generateNoteReferences(
            size = size,
            firstNoteId = 3 * size,
            hasLocal = true,
            hasFullSource = true,
            hasPartial = false
        )
        baseLocalNotes = notesWithOnlyLocalId + notesWithOnlyFullSourceId + notesWithPartialId +
            notesWithBothLocalAndSourceId
    }

    @Test
    fun `pageChangedSignal same LMT test`() {
        val note = notesWithBothLocalAndSourceId[0]
        val newTitle = "NewTitle"

        val changes = translatePageChangedSignal(
            localNotes = baseLocalNotes,
            pageLocalId = note.pageLocalId!!,
            pageSourceId = note.pageSourceId,
            metaData = genPageMetaData().copy(title = newTitle, lastModifiedDateTime = parseMillisToISO8601String(note.lastModifiedAt))
        )
        assertThat(changes.count(), iz(1))
    }

    @Test
    fun `pageChangedSignal replace test`() {
        val note = notesWithBothLocalAndSourceId[0]
        val newTitle = "NewTitle"

        val changes = translatePageChangedSignal(
            localNotes = baseLocalNotes,
            pageLocalId = note.pageLocalId!!,
            pageSourceId = note.pageSourceId,
            metaData = genPageMetaData()
                .copy(title = newTitle, lastModifiedDateTime = parseMillisToISO8601String(note.lastModifiedAt + 1))
        )
        assertThat(changes.count(), iz(1))
        assertThat(changes.toReplace.size, iz(1))
        with(changes.toReplace[0].remoteNote) {
            assertThat(this.pageLocalId, iz(note.pageLocalId))
            assertThat(this.title, iz(newTitle))
        }
    }

    @Test
    fun `pageChangedSignal replace with matching sourceId test`() {
        val note = notesWithOnlyFullSourceId[0]
        val pageLocalId = "randomLocalId"
        val newTitle = "NewTitle"

        val changes = translatePageChangedSignal(
            localNotes = baseLocalNotes,
            pageLocalId = pageLocalId,
            pageSourceId = note.pageSourceId,
            metaData = genPageMetaData()
                .copy(title = newTitle, lastModifiedDateTime = parseMillisToISO8601String(note.lastModifiedAt + 1))
        )
        assertThat(changes.count(), iz(1))
        assertThat(changes.toReplace.size, iz(1))
        with(changes.toReplace[0].remoteNote) {
            assertThat(this.pageLocalId, iz(pageLocalId))
            assertThat(this.pageSourceId, iz(note.pageSourceId))
            assertThat(this.title, iz(newTitle))
        }
    }

    @Test
    fun `pageChangedSignal replace with matching partialId test`() {
        val ind = 100
        val note = generateNoteRef(ind = ind, hasLocal = false, hasPartial = false, hasFullSource = true)
        val pageLocalId = "randomLocalId"
        val newTitle = "NewTitle"
        val pagePartialSourceId = generatePartialSourceId("pageId $ind")

        val changes = translatePageChangedSignal(
            localNotes = baseLocalNotes + note,
            pageLocalId = pageLocalId,
            pageSourceId = pagePartialSourceId,
            metaData = genPageMetaData()
                .copy(title = newTitle, lastModifiedDateTime = parseMillisToISO8601String(note.lastModifiedAt + 1))
        )
        assertThat(changes.count(), iz(1))
        assertThat(changes.toReplace.size, iz(1))
        with(changes.toReplace[0].remoteNote) {
            assertThat(this.pageLocalId, iz(pageLocalId))
            assertThat(this.pageSourceId, iz(note.pageSourceId))
            assertThat(this.title, iz(newTitle))
        }
    }

    @Test
    fun `pageChangedSignal creation test`() {
        val pageLocalId = "randomLocalId"

        val changes = translatePageChangedSignal(
            localNotes = baseLocalNotes,
            pageLocalId = pageLocalId,
            pageSourceId = generateFullSourceId("randomSourceId"),
            metaData = genPageMetaData()
        )
        assertThat(changes.count(), iz(1))
        assertThat(changes.toCreate.size, iz(1))
        with(changes.toCreate[0]) {
            assertThat(this.pageLocalId, iz(pageLocalId))
            assertThat(this.isLocalOnlyPage, iz(true))
        }
    }

    @Test
    fun `pageDeleteSignal test`() {
        val note = notesWithBothLocalAndSourceId[0]

        val changes = translatePageDeletedSignal(
            localNotes = baseLocalNotes,
            pageLocalId = note.pageLocalId!!,
            pageSourceId = note.pageSourceId
        )
        val expected = NoteRefLocalChanges(toMarkAsDeleted = listOf(note))
        assertThat(changes, iz(expected))
    }

    @Test
    fun `pageDeleteSignal matching localId test`() {
        val note = notesWithOnlyLocalId[0]

        val changes = translatePageDeletedSignal(
            localNotes = baseLocalNotes,
            pageLocalId = note.pageLocalId!!,
            pageSourceId = null
        )
        val expected = NoteRefLocalChanges(toDelete = listOf(note))
        assertThat(changes, iz(expected))
    }

    @Test
    fun `pageDeleteSignal matching fullSourceId test`() {
        val note = notesWithOnlyFullSourceId[0]

        val changes = translatePageDeletedSignal(
            localNotes = baseLocalNotes,
            pageLocalId = "randomLocalId",
            pageSourceId = note.pageSourceId
        )
        val expected = NoteRefLocalChanges(toMarkAsDeleted = listOf(note))
        assertThat(changes, iz(expected))
    }

    @Test
    fun `pageDeleteSignal matching PartialSourceId test`() {
        val ind = 100
        val note = generateNoteRef(ind = ind, hasLocal = false, hasPartial = false, hasFullSource = true)

        val changes = translatePageDeletedSignal(
            localNotes = baseLocalNotes + note,
            pageLocalId = "randomLocalId",
            pageSourceId = generatePartialSourceId("pageId $ind")
        )
        val expected = NoteRefLocalChanges(toMarkAsDeleted = listOf(note))
        assertThat(changes, iz(expected))
    }

    @Test
    fun `sectionChangedSignal match localId test`() {
        val sectionLocalId = "sectionLocalId 100"
        val sectionSourceId = generateFullSourceId("section 100")
        val newSectionName = "Name100"

        val sectionNotes = listOf(
            generateNoteRef(ind = 100, hasLocal = true, hasPartial = false, hasFullSource = false).copy(sectionLocalId = sectionLocalId, sectionSourceId = null),
            generateNoteRef(ind = 101, hasLocal = true, hasPartial = false, hasFullSource = false).copy(sectionLocalId = sectionLocalId, sectionSourceId = null),
            generateNoteRef(ind = 102, hasLocal = false, hasPartial = false, hasFullSource = true).copy(sectionLocalId = null, sectionSourceId = sectionSourceId)
        )

        val changes = translateSectionChangedSignal(
            localNotes = baseLocalNotes + sectionNotes,
            sectionLocalId = sectionLocalId,
            sectionSourceId = null,
            sectionName = newSectionName
        )

        val expected = NoteRefLocalChanges(
            toReplace = listOf(
                NoteReferenceUpdate(sectionNotes[0].copy(containerName = newSectionName)),
                NoteReferenceUpdate(sectionNotes[1].copy(containerName = newSectionName))
            )
        )
        assertThat(changes, iz(expected))
    }

    @Test
    fun `sectionChangedSignal match SourceId test`() {
        val sectionLocalId = "sectionLocalId 100"
        val sectionSourceId = generateFullSourceId("section 100")
        val newSectionName = "Name100"

        val sectionNotes = listOf(
            generateNoteRef(ind = 100, hasLocal = true, hasPartial = false, hasFullSource = false).copy(sectionLocalId = sectionLocalId, sectionSourceId = null),
            generateNoteRef(ind = 101, hasLocal = true, hasPartial = false, hasFullSource = false).copy(sectionLocalId = sectionLocalId, sectionSourceId = null),
            generateNoteRef(ind = 102, hasLocal = false, hasPartial = false, hasFullSource = true).copy(sectionLocalId = null, sectionSourceId = sectionSourceId)
        )

        val changes = translateSectionChangedSignal(
            localNotes = baseLocalNotes + sectionNotes,
            sectionLocalId = sectionLocalId,
            sectionSourceId = sectionSourceId,
            sectionName = newSectionName
        )

        val expected = NoteRefLocalChanges(
            toReplace = listOf(
                NoteReferenceUpdate(sectionNotes[0].copy(containerName = newSectionName, sectionSourceId = sectionSourceId)),
                NoteReferenceUpdate(sectionNotes[1].copy(containerName = newSectionName, sectionSourceId = sectionSourceId)),
                NoteReferenceUpdate(sectionNotes[2].copy(containerName = newSectionName, sectionLocalId = sectionLocalId))
            )
        )
        assertThat(changes, iz(expected))
    }

    @Test
    fun `sectionChangedSignal match PartialSourceId test`() {
        val sectionLocalId = "sectionLocalId 100"
        val sectionSourceId = generateFullSourceId("section 100")
        val sectionPartialId = generatePartialSourceId("section 100")
        val newSectionName = "Name100"

        val sectionNotes = listOf(
            generateNoteRef(ind = 100, hasLocal = true, hasPartial = false, hasFullSource = false).copy(sectionLocalId = sectionLocalId, sectionSourceId = null),
            generateNoteRef(ind = 102, hasLocal = false, hasPartial = false, hasFullSource = true).copy(sectionLocalId = null, sectionSourceId = sectionSourceId)
        )

        val changes = translateSectionChangedSignal(
            localNotes = baseLocalNotes + sectionNotes,
            sectionLocalId = sectionLocalId,
            sectionSourceId = sectionPartialId,
            sectionName = newSectionName
        )

        val expected = NoteRefLocalChanges(
            toReplace = listOf(
                NoteReferenceUpdate(sectionNotes[0].copy(containerName = newSectionName)),
                NoteReferenceUpdate(sectionNotes[1].copy(containerName = newSectionName, sectionLocalId = sectionLocalId))
            )
        )
        assertThat(changes, iz(expected))
    }

    @Test
    fun `sectionDeleteSignal match localId test`() {
        val sectionLocalId = "sectionLocalId 100"
        val sectionSourceId = generateFullSourceId("section 100")

        val sectionNotes = listOf(
            generateNoteRef(ind = 100, hasLocal = true, hasPartial = false, hasFullSource = false).copy(sectionLocalId = sectionLocalId, sectionSourceId = null),
            generateNoteRef(ind = 101, hasLocal = true, hasPartial = false, hasFullSource = false).copy(sectionLocalId = sectionLocalId, sectionSourceId = null),
            generateNoteRef(ind = 102, hasLocal = false, hasPartial = false, hasFullSource = true).copy(sectionLocalId = null, sectionSourceId = sectionSourceId)
        )

        val changes = translateSectionDeletedSignal(
            localNotes = baseLocalNotes + sectionNotes,
            sectionLocalId = sectionLocalId,
            sectionSourceId = null
        )

        val expected = NoteRefLocalChanges(toDelete = listOf(sectionNotes[0], sectionNotes[1]))
        assertThat(changes, iz(expected))
    }

    @Test
    fun `sectionDeletedSignal match SourceId test`() {
        val sectionLocalId = "sectionLocalId 100"
        val sectionSourceId = generateFullSourceId("section 100")

        val sectionNotes = listOf(
            generateNoteRef(ind = 100, hasLocal = true, hasPartial = false, hasFullSource = false).copy(sectionLocalId = sectionLocalId, sectionSourceId = null),
            generateNoteRef(ind = 101, hasLocal = true, hasPartial = false, hasFullSource = false).copy(sectionLocalId = sectionLocalId, sectionSourceId = null),
            generateNoteRef(ind = 102, hasLocal = false, hasPartial = false, hasFullSource = true).copy(sectionLocalId = null, sectionSourceId = sectionSourceId)
        )

        val changes = translateSectionDeletedSignal(
            localNotes = baseLocalNotes + sectionNotes,
            sectionLocalId = sectionLocalId,
            sectionSourceId = sectionSourceId
        )

        val expected = NoteRefLocalChanges(
            toDelete = listOf(sectionNotes[0], sectionNotes[1]),
            toMarkAsDeleted = listOf(sectionNotes[2])
        )
        assertThat(changes, iz(expected))
    }

    @Test
    fun `sectionDeletedSignal match PartialSourceId test`() {
        val sectionLocalId = "sectionLocalId 100"
        val sectionSourceId = generateFullSourceId("section 100")
        val sectionPartialId = generatePartialSourceId("section 100")

        val sectionNotes = listOf(
            generateNoteRef(ind = 100, hasLocal = true, hasPartial = false, hasFullSource = false).copy(sectionLocalId = sectionLocalId, sectionSourceId = null),
            generateNoteRef(ind = 102, hasLocal = false, hasPartial = false, hasFullSource = true).copy(sectionLocalId = null, sectionSourceId = sectionSourceId)
        )

        val changes = translateSectionDeletedSignal(
            localNotes = baseLocalNotes + sectionNotes,
            sectionLocalId = sectionLocalId,
            sectionSourceId = sectionPartialId
        )

        val expected = NoteRefLocalChanges(
            toDelete = listOf(sectionNotes[0]),
            toMarkAsDeleted = listOf(sectionNotes[1])
        )
        assertThat(changes, iz(expected))
    }

    @Test
    fun `appendPageIfNeededSignal page already present same LMT test`() {
        val note = notesWithBothLocalAndSourceId[0]
        val newTitle = "NewTitle"

        val changes = translateAppendPageIfNeededSignal(
            localNotes = baseLocalNotes,
            pageLocalId = note.pageLocalId!!,
            pageSourceId = note.pageSourceId,
            metaData = genPageMetaData()
                .copy(title = newTitle, lastModifiedDateTime = parseMillisToISO8601String(note.lastModifiedAt))
        )
        assertThat(changes.count(), iz(1))
        assertThat(changes.toReplace.size, iz(1))
        with(changes.toReplace[0].remoteNote) {
            assertThat(this.pageLocalId, iz(note.pageLocalId))
            // verify title unchanged same LMT
            assertThat(this.title, iz(notesWithBothLocalAndSourceId[0].title))
        }
    }

    @Test
    fun `appendPageIfNeededSignal page already present diff LMT test`() {
        val note = notesWithBothLocalAndSourceId[0]
        val newTitle = "NewTitle"
        val newLMT = note.lastModifiedAt + 1

        val changes = translateAppendPageIfNeededSignal(
            localNotes = baseLocalNotes,
            pageLocalId = note.pageLocalId!!,
            pageSourceId = note.pageSourceId,
            metaData = genPageMetaData()
                .copy(title = newTitle, lastModifiedDateTime = parseMillisToISO8601String(newLMT))
        )
        assertThat(changes.count(), iz(1))
        assertThat(changes.toReplace.size, iz(1))
        with(changes.toReplace[0].remoteNote) {
            assertThat(this.pageLocalId, iz(note.pageLocalId))
            // verify title, LMT changes
            assertThat(this.title, iz(newTitle))
            assertThat(this.lastModifiedAt, iz(newLMT))
        }
    }

    @Test
    fun `appendPageIfNeededSignal page absent test`() {
        val pageLocalId = "randomLocalId"

        val changes = translateAppendPageIfNeededSignal(
            localNotes = baseLocalNotes,
            pageLocalId = pageLocalId,
            pageSourceId = generateFullSourceId("randomSourceId"),
            metaData = genPageMetaData()
        )
        assertThat(changes.count(), iz(1))
        assertThat(changes.toCreate.size, iz(1))
        with(changes.toCreate[0]) {
            assertThat(this.pageLocalId, iz(pageLocalId))
            assertThat(this.isLocalOnlyPage, iz(true))
        }
    }

    companion object {
        const val nbUrl = "notebookURL"

        fun generateNoteReferences(size: Int, firstNoteId: Int, hasFullSource: Boolean, hasLocal: Boolean, hasPartial: Boolean) =
            (firstNoteId until firstNoteId + size).map {
                generateNoteRef(
                    ind = it,
                    hasFullSource = hasFullSource,
                    hasLocal = hasLocal,
                    hasPartial = hasPartial
                )
            }

        fun generateNoteRef(ind: Int, hasLocal: Boolean, hasFullSource: Boolean, hasPartial: Boolean) = NoteReference(
            pageSourceId = when {
                hasFullSource -> generateFullSourceId("pageId $ind")
                hasPartial -> generatePartialSourceId("pageId $ind")
                else -> null
            },
            pageLocalId = if (hasLocal) "pageLocalId $ind" else null,
            sectionSourceId = if (hasFullSource) generateFullSourceId("section $ind") else null,
            sectionLocalId = if (hasLocal) "sectionLocalId $ind" else null,
            title = "Title $ind",
            type = "source",
            lastModifiedAt = System.currentTimeMillis(),
            webUrl = nbUrl + "webURL" + ind,
            clientUrl = "clientUrl$ind"
        )

        private fun genPageMetaData() = PageChangeSignalMetaData(
            sectionLocalId = "sectionLocalId",
            sectionSourceId = null,
            color = null,
            webUrl = "webUrl",
            clientUrl = "clientUrl",
            createdDateTime = "1",
            lastModifiedDateTime = parseMillisToISO8601String(10),
            title = "title",
            sectionName = "sectionName",
            notebookName = "noteBookName",
            previewImageUrl = "imageUrl",
            previewText = "previewText",
            isMediaPresent = 0,
            previewRichText = "",
            media = null
        )

        private fun generateFullSourceId(uniqueId: String) =
            NoteRefSourceId.FullSourceId("sourceIdPrefix$uniqueId")

        private fun generatePartialSourceId(uniqueId: String) =
            NoteRefSourceId.PartialSourceId(uniqueId, nbUrl)
    }
}
