package com.microsoft.notes.models

import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.DocumentType
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.utils.logging.NoteType
import org.junit.Assert
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ModelExtensionsTest {

    @Test
    fun `should parse note to telemetry note type Text`() {
        val note = Note(
            document = Document(listOf(Paragraph(content = Content(text = "Hello world!"))))
        )

        val noteTypeResult = note.toTelemetryNoteType()
        Assert.assertThat(noteTypeResult, iz(NoteType.Text))
    }

    @Test
    fun `should parse note to telemetry note type Image`() {
        val note = Note(
            media = listOf(Media("", "", null, null))
        )

        val noteTypeResult = note.toTelemetryNoteType()
        Assert.assertThat(noteTypeResult, iz(NoteType.Image))
    }

    @Test
    fun `should parse note to telemetry note type TextWithImage`() {
        val note = Note(
            document = Document(listOf(Paragraph(content = Content(text = "Hello world!")))),
            media = listOf(Media("", "", null, null))
        )

        val noteTypeResult = note.toTelemetryNoteType()
        Assert.assertThat(noteTypeResult, iz(NoteType.TextWithImage))
    }

    @Test
    fun `should parse note to telemetry note type Empty`() {
        val note = Note()

        val noteTypeResult = note.toTelemetryNoteType()
        Assert.assertThat(noteTypeResult, iz(NoteType.Empty))
    }

    @Test
    fun `should parse note to telemetry note type Ink`() {
        val note = Note(document = Document(type = DocumentType.RENDERED_INK))

        val noteTypeResult = note.toTelemetryNoteType()
        Assert.assertThat(noteTypeResult, iz(NoteType.Ink))
    }

    @Test
    fun `should parse note to telemetry note type Future`() {
        val note = Note(document = Document(type = DocumentType.FUTURE))

        val noteTypeResult = note.toTelemetryNoteType()
        Assert.assertThat(noteTypeResult, iz(NoteType.Future))
    }
}
