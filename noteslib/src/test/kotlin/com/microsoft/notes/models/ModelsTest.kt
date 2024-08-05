package com.microsoft.notes.models

import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.mediaListCount
import com.microsoft.notes.richtext.scheme.paragraphListCount
import com.microsoft.notes.utils.utils.parseISO8601StringToMillis
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ModelsTest {
    private val LOCAL_ID = "localId"

    private fun isContentEqual(note: Note, other: Note): Boolean {
        with(note) {
            return document == other.document &&
                isDeleted == other.isDeleted &&
                color == other.color
        }
    }

    @Test
    fun should_Paragraph_list_be_empty() {
        val document = Document()
        assertThat(document.paragraphListCount(), iz(0))
    }

    @Test
    fun should_Paragraph_list_not_be_empty() {
        val content = Content(text = "text")
        val paragraph = Paragraph(LOCAL_ID, content = content)
        val document = Document(listOf(paragraph))
        assertThat(document.paragraphListCount(), iz(1))
    }

    @Test
    fun should_Media_list_be_empty() {
        val document = Document()
        assertThat(document.mediaListCount(), iz(0))
    }

    @Test
    fun should_Media_list_not_be_empty() {
        val media = InlineMedia(LOCAL_ID)
        val document = Document(listOf(media))
        assertThat(document.mediaListCount(), iz(1))
    }

    @Test
    fun should_Paragraph_and_Media_list_be_empty() {
        val document = Document()

        assertThat(document.paragraphListCount(), iz(0))
        assertThat(document.mediaListCount(), iz(0))
    }

    @Test
    fun should_Paragraph_and_Media_list_not_be_empty() {
        val content = Content(text = "text")
        val paragraph = Paragraph(LOCAL_ID, content = content)
        val media = InlineMedia(LOCAL_ID)
        val document = Document(listOf(paragraph, media))

        assertThat(document.paragraphListCount(), iz(1))
        assertThat(document.mediaListCount(), iz(1))
    }

    @Test
    fun should_Note_have_empty_Document() {
        val note = Note(localId = LOCAL_ID)
        assertThat(note.isDocumentEmpty, iz(true))
    }

    @Test
    fun should_Note_not_be_empty_with_a_Document() {
        val content = Content(text = "text")
        val paragraph = Paragraph(LOCAL_ID, content = content)
        val media = InlineMedia(LOCAL_ID)
        val document = Document(listOf(paragraph, media))
        val note = Note(localId = LOCAL_ID, document = document)

        assertThat(note.isDocumentEmpty, iz(false))
    }

    @Test
    fun should_Note_have_empty_Paragraph_and_Media_list() {
        val note = Note(localId = LOCAL_ID)

        assertThat(note.isParagraphListEmpty, iz(true))
        assertThat(note.isMediaListEmpty, iz(true))
    }

    @Test
    fun should_Note_not_have_empty_Paragraph_and_Media_list() {
        val content = Content(text = "text")
        val paragraph = Paragraph(LOCAL_ID, content = content)
        val media = Media(
            localId = "test-id",
            localUrl = null,
            altText = null,
            mimeType = "some/type",
            imageDimensions = null,
            lastModified = System.currentTimeMillis()

        )
        val document = Document(listOf(paragraph))
        val note = Note(localId = LOCAL_ID, document = document, media = listOf(media))

        assertThat(note.isParagraphListEmpty, iz(false))
        assertThat(note.isMediaListEmpty, iz(false))
    }

    @Test
    fun should_Note_have_the_same_content_than_other_Note_instance() {
        val content = Content(text = "text")
        val paragraph = Paragraph(LOCAL_ID, content = content)
        val media = InlineMedia(LOCAL_ID)
        val document = Document(listOf(paragraph, media))
        val note1 = Note(localId = LOCAL_ID, document = document)
        val note2 = Note(document = document)

        assertThat(note1.equals(note2), iz(false))
        assertThat(note1.localId, iz(not(note2.localId)))
        assertThat(this.isContentEqual(note1, note2), iz(true))
    }

    @Test
    fun should_Note_have_the_same_content_than_same_Note_instance() {
        val content = Content(text = "text")
        val paragraph = Paragraph(LOCAL_ID, content = content)
        val media = InlineMedia(LOCAL_ID)
        val document = Document(listOf(paragraph, media))
        val note1 = Note(localId = LOCAL_ID, document = document)
        val note2 = note1

        assertThat(note1.equals(note2), iz(true))
        assertThat(this.isContentEqual(note1, note2), iz(true))
    }

    @Test
    fun should_Note_be_empty() {
        val note = Note()

        assertThat(note.isEmpty, iz(true))
    }

    @Test
    fun should_Note_not_be_empty_if_contains_text() {
        val content = Content(text = "text")
        val paragraph = Paragraph(LOCAL_ID, content = content)
        val media = InlineMedia(LOCAL_ID)
        val document = Document(listOf(paragraph, media))
        val note = Note(localId = LOCAL_ID, document = document)

        assertThat(note.isEmpty, iz(false))
    }

    @Test
    fun should_Note_be_empty_if_contains_spaces() {
        val content = Content(text = "     ")
        val paragraph = Paragraph(LOCAL_ID, content = content)
        val document = Document(listOf(paragraph))
        val note = Note(localId = LOCAL_ID, document = document)

        assertThat(note.isEmpty, iz(true))
    }

    @Test
    fun should_Note_be_empty_if_contains_newlines() {
        val content = Content(text = "\n")
        val paragraph = Paragraph(LOCAL_ID, content = content)
        val document = Document(listOf(paragraph))
        val note = Note(localId = LOCAL_ID, document = document)

        assertThat(note.isEmpty, iz(true))
    }

    @Test
    fun should_Note_has_no_text() {
        val note = Note()

        assertThat(note.hasNoText, iz(true))
    }

    @Test
    fun should_Note_has_no_text_when_has_Document_with_Media() {
        val note = Note(
            document = Document(
                listOf(
                    InlineMedia(localId = "mediaId", localUrl = "localUrl/")
                )
            )
        )

        assertThat(note.hasNoText, iz(true))
    }

    @Test
    fun should_Note_has_no_text_when_has_Document_with_empty_Paragraph() {
        // TODO we shouldn't have this case in the near future, since we can not have empty Paragraphs
        // instead empty we will delete that Paragraph. But for now is good this test to check
        // that having empty Paragraphs we don't have text.
        val note = Note(
            document = Document(
                listOf(
                    Paragraph(localId = "paragraphId", content = Content(text = ""))
                )
            )
        )

        assertThat(note.hasNoText, iz(true))
    }

    @Test
    fun should_Note_has_text_when_has_at_least_one_Paragraph() {
        val content = Content(text = "text")
        val paragraph = Paragraph(LOCAL_ID, content = content)
        val document = Document(listOf(paragraph))
        val note = Note(document = document)

        assertThat(note.hasNoText, iz(false))
    }

    @Test
    fun should_parse_ISO8601String_to_Millis() {
        val ISO8601String = "2018-02-02T16:26:09.0000000Z"
        val unixTimeStamp: Long = 1517588769000
        val millisResult = parseISO8601StringToMillis(ISO8601String)
        assertThat(millisResult, iz(unixTimeStamp))
    }

    @Test
    fun should_return_light_font_color() {
        val note = Note(
            document = Document(listOf(Paragraph(localId = "paragraphId", content = Content(text = "")))),
            color = Color.GREY
        )
        assertThat(note.fontColor, iz(FontColor.DARK))
    }

    @Test
    fun should_return_dark_font_color() {
        val note = Note(
            document = Document(listOf(Paragraph(localId = "paragraphId", content = Content(text = "")))),
            color = Color.CHARCOAL
        )
        assertThat(note.fontColor, iz(FontColor.LIGHT))
    }

    @Test
    fun should_return_light_only_when_charcoal() {
        assertThat(
            Color.GREY.getFontColor(), iz(FontColor.DARK)
        )
        assertThat(
            Color.BLUE.getFontColor(), iz(FontColor.DARK)
        )
        assertThat(
            Color.GREEN.getFontColor(), iz(FontColor.DARK)
        )
        assertThat(
            Color.YELLOW.getFontColor(),
            iz(
                FontColor.DARK
            )
        )
        assertThat(
            Color.PINK.getFontColor(), iz(FontColor.DARK)
        )
        assertThat(
            Color.PURPLE.getFontColor(),
            iz(
                FontColor.DARK
            )
        )
        assertThat(
            Color.CHARCOAL.getFontColor(),
            iz(
                FontColor.LIGHT
            )
        )
    }
}
