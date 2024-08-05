package com.microsoft.notes.richtext.scheme

import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class DocumentTest {
    val LOCAL_ID = "localId"
    val TEXT = "Hello"
    val LOCAL_URL = "localUrl"

    @Test
    fun should_create_RenderedInkDocument() {
        val renderedInkDocument = Document(
            blocks = listOf(InlineMedia(LOCAL_ID, LOCAL_URL)),
            type = DocumentType.RENDERED_INK
        )

        assertThat(renderedInkDocument.readOnly, iz(true))
    }

    @Test
    fun should_create_FutureDocument() {
        val futureDocument = Document(
            type = DocumentType.FUTURE
        )
        assertThat(futureDocument.blocks.isEmpty(), iz(true))
        assertThat(futureDocument.readOnly, iz(true))
    }

    @Test
    fun should_create_empty_Document() {
        val document = Document()

        assertThat(document, iz(notNullValue()))
        assertThat(document.blocks, iz(emptyList()))
    }

    @Test
    fun should_create_Document_with_one_block() {
        val CONTENT = Content(TEXT)
        val PARAGRAPH = Paragraph(LOCAL_ID, content = CONTENT)
        val document = Document(listOf(PARAGRAPH))

        assertThat(document, iz(notNullValue()))
        assertThat(document.blocks.size, iz(1))
        assertThat(document.blocks.component1(), iz(instanceOf(Paragraph::class.java)))
        assertThat(document.blocks.component1() as Paragraph, iz(PARAGRAPH))
    }

    @Test
    fun should_create_Document_with_some_blocks() {
        val CONTENT = Content(TEXT)
        val PARAGRAPH = Paragraph(LOCAL_ID, content = CONTENT)
        val MEDIA = InlineMedia(LOCAL_ID, localUrl = LOCAL_URL)
        val document = Document(listOf(PARAGRAPH, MEDIA))

        assertThat(document, iz(notNullValue()))
        assertThat(document.blocks.size, iz(2))
        assertThat(document.blocks.component1(), iz(instanceOf(Paragraph::class.java)))
        assertThat(document.blocks.component1() as Paragraph, iz(PARAGRAPH))
        assertThat(document.blocks.component2(), iz(instanceOf(InlineMedia::class.java)))
        assertThat(document.blocks.component2() as InlineMedia, iz(MEDIA))
    }
}
