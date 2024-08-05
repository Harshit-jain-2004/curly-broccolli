package com.microsoft.notes.richtext.scheme

import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class BlockTest {

    @Test
    fun should_create_Paragraph_with_default_parameters() {
        val LOCAL_ID = "localId"
        val TEXT = "Hello"

        val CONTENT = Content(TEXT)
        val paragraph = Paragraph(localId = LOCAL_ID, content = CONTENT)

        assertThat(paragraph.localId, iz(LOCAL_ID))
        assertThat(paragraph.style, iz(ParagraphStyle()))
        assertThat(paragraph.content, iz(CONTENT))
    }

    @Test
    fun should_create_Paragraph_with_all_defined_parameters() {
        val LOCAL_ID = "localId"
        val TEXT = "Hello"

        val STYLE = ParagraphStyle(true)
        val CONTENT = Content(TEXT)
        val paragraph = Paragraph(
            localId = LOCAL_ID, style = STYLE,
            content = CONTENT
        )

        assertThat(paragraph.localId, iz(LOCAL_ID))
        assertThat(paragraph.style, iz(STYLE))
        assertThat(paragraph.content, iz(CONTENT))
    }

    @Test
    fun should_create_Media_with_default_parameters() {
        val LOCAL_ID = "localId"
        val LOCAL_URL = "localUrl"

        val media = InlineMedia(localId = LOCAL_ID, localUrl = LOCAL_URL)

        assertThat(media.localId, iz(LOCAL_ID))
        assertThat(media.localUrl, iz(LOCAL_URL))
        assertThat(media.remoteUrl, iz(nullValue()))
    }

    @Test
    fun should_create_Media_with_all_defined_parameters() {
        val LOCAL_ID = "localId"
        val LOCAL_URL = "localUrl"
        val REMOTE_URL = "remoteUrl"

        val media = InlineMedia(
            localId = LOCAL_ID, localUrl = LOCAL_URL,
            remoteUrl = REMOTE_URL
        )

        assertThat(media.localId, iz(LOCAL_ID))
        assertThat(media.localUrl, iz(LOCAL_URL))
        assertThat(media.remoteUrl, iz(REMOTE_URL))
    }
}
