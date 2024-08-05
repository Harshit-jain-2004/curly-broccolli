package com.microsoft.notes.notesReference

import com.microsoft.notes.notesReference.models.NoteRefLocalChanges
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ChangesCalculatorTest {

    @Test
    fun calculateChangesForLocalOnlyNoteRefsCleanUpTest() {
        val notesWithOnlyLocalId = NoteRefSignalTranslatorTest.generateNoteReferences(
            size = 3,
            firstNoteId = 0,
            hasLocal = true,
            hasFullSource = false,
            hasPartial = false
        ).map { it.copy(isLocalOnlyPage = true) }

        val noteWithBothLocalAndSourceId_LocalOnlyPage = NoteRefSignalTranslatorTest.generateNoteRef(
            ind = 3,
            hasLocal = true,
            hasFullSource = true,
            hasPartial = false
        ).copy(isLocalOnlyPage = true)

        val noteWithBothLocalAndSourceId_NotLocalOnlyPage = NoteRefSignalTranslatorTest.generateNoteRef(
            ind = 3,
            hasLocal = true,
            hasFullSource = true,
            hasPartial = false
        ).copy(isLocalOnlyPage = false)

        val localNotes = notesWithOnlyLocalId + noteWithBothLocalAndSourceId_LocalOnlyPage + noteWithBothLocalAndSourceId_NotLocalOnlyPage
        val validPageLocalIds = listOf(notesWithOnlyLocalId[2].pageLocalId!!)
        val changes = calculateChangesForLocalOnlyNoteRefsCleanUp(localNotes, validPageLocalIds)

        val expected = NoteRefLocalChanges(toDelete = listOf(notesWithOnlyLocalId[0], notesWithOnlyLocalId[1], noteWithBothLocalAndSourceId_LocalOnlyPage))
        assertThat(changes, iz(expected))
    }
}
