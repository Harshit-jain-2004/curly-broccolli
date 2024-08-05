package com.microsoft.notes.threeWayMerge

import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.DocumentType
import com.microsoft.notes.richtext.scheme.InlineMedia
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class InkTest {

    @Test
    fun `should return Primary Ink Note when applying 3wm to inked notes`() {
        val inkBase = Note(
            document = Document(
                type = DocumentType.RENDERED_INK,
                blocks = listOf(InlineMedia(localId = "localId_Base", localUrl = "localUrl_Base"))
            )
        )
        val inkPrimary = Note(
            document = Document(
                type = DocumentType.RENDERED_INK,
                blocks = listOf(InlineMedia(localId = "localId_Primary", localUrl = "localUrl_Primary"))
            )
        )
        val inkSecondary = Note(
            document = Document(
                type = DocumentType.RENDERED_INK,
                blocks = listOf(InlineMedia(localId = "localId_Secondary", localUrl = "localUrl_Secondary"))
            )
        )

        val inkResult = threeWayMerge(
            base = inkBase, primary = inkPrimary,
            secondary = inkSecondary
        )

        assertThat(inkResult, iz(inkPrimary))
    }
}
