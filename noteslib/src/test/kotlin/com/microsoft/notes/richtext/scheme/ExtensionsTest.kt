package com.microsoft.notes.richtext.scheme

import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ExtensionsTest {

    private val LOCAL_ID = "localId"
    private val TEXT = "text"
    private val MEDIA_LOCAL_URL = "mediaLocalUrl/"
    private val MEDIA_REMOTE_URL = "mediaRemoteUrl/"

    @Test
    fun should_paragraphListCount_be_empty() {
        val document = Document()
        assertThat(document.paragraphListCount(), iz(0))
    }

    @Test
    fun should_paragraphListCount_be_not_empty() {
        val paragraph = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = TEXT)
        )
        val document = Document(listOf(paragraph))

        assertThat(document.paragraphListCount(), iz(1))
    }

    @Test
    fun should_mediaListCount_equals_be_empty() {
        val document = Document()
        assertThat(document.mediaListCount(), iz(0))
    }

    @Test
    fun should_mediaListCount_be_not_empty() {
        val media = InlineMedia(localId = LOCAL_ID, localUrl = MEDIA_LOCAL_URL)
        val document = Document(listOf(media))

        assertThat(document.mediaListCount(), iz(1))
    }

    @Test
    fun should_Document_have_correct_size() {
        val paragraph = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = TEXT)
        )
        val document = Document(listOf(paragraph, paragraph, paragraph))

        assertThat(document.size(), iz(3))
    }

    @Test
    fun should_Document_be_empty() {
        val document = Document()

        assertThat(document.isEmpty(), iz(true))
    }

    @Test
    fun should_return_MediaList_from_Document() {
        val NUM_ITEMS = 10
        val blocks = (0 until NUM_ITEMS).mapIndexed {
                index, _ ->
            if (index % 2 == 0) InlineMedia(
                localId = LOCAL_ID + index,
                localUrl = MEDIA_LOCAL_URL + index
            )
            else Paragraph(
                localId = LOCAL_ID + index,
                content = Content(text = TEXT + index)
            )
        }
        val document = Document(blocks)
        assertThat(document.size(), iz(NUM_ITEMS))
        assertThat(document.mediaListCount(), iz(NUM_ITEMS / 2))

        val mediaList = document.mediaList()
        (mediaList).forEachIndexed {
                index, media ->
            assertThat(media.localId, iz(LOCAL_ID + index * 2))
            assertThat(media.localUrl, iz(MEDIA_LOCAL_URL + index * 2))
        }
    }

    @Test
    fun should_return_ParagraphList_from_Document() {
        val NUM_ITEMS = 10
        val blocks = (0 until NUM_ITEMS).mapIndexed {
                index, _ ->
            if (index % 2 == 0) InlineMedia(
                localId = LOCAL_ID + index,
                localUrl = MEDIA_LOCAL_URL + index
            )
            else Paragraph(
                localId = LOCAL_ID + index,
                content = Content(text = TEXT + index)
            )
        }
        val document = Document(blocks)
        assertThat(document.size(), iz(NUM_ITEMS))
        assertThat(document.paragraphListCount(), iz(NUM_ITEMS / 2))

        val paragraphList = document.paragraphList()
        (paragraphList).forEachIndexed {
                index, paragraph ->
            assertThat(paragraph.localId, iz(LOCAL_ID + (index + index + 1)))
            assertThat(paragraph.content.text, iz(TEXT + (index + index + 1)))
        }
    }

    @Test
    fun should_return_String_representation() {
        val paragraph1 = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = "Hello ")
        )
        val paragraph2 = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = "how ")
        )
        val paragraph3 = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = "are ")
        )
        val paragraph4 = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = "you?\n")
        )
        val document = Document(
            listOf(paragraph1, paragraph2, paragraph3, paragraph4)
        )

        assertThat(document.asString(), iz("Hello \nhow \nare \nyou?\n"))
    }

    @Test
    fun should_return_String_representation_having_Media_blocks_in_Document() {
        val paragraph1 = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = "Hello ")
        )
        val paragraph2 = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = "how ")
        )
        val media = InlineMedia(localId = LOCAL_ID, localUrl = MEDIA_LOCAL_URL)
        val paragraph3 = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = "are ")
        )
        val paragraph4 = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = "you?\n")
        )
        val document = Document(
            listOf(paragraph1, paragraph2, media, paragraph3, paragraph4)
        )

        assertThat(document.asString(), iz("Hello \nhow \nare \nyou?\n"))
    }

    @Test
    fun should_say_that_Block_is_Media() {
        val media = InlineMedia(localId = LOCAL_ID, localUrl = MEDIA_LOCAL_URL)
        val document = Document(listOf(media))

        val block = document.blocks.first()
        assertThat(block.isParagraph(), iz(false))
        assertThat(block.isMedia(), iz(true))
    }

    @Test
    fun should_say_that_Block_is_Paragraph() {
        val paragraph = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = TEXT)
        )
        val document = Document(listOf(paragraph))

        val block = document.blocks.first()
        assertThat(block.isMedia(), iz(false))
        assertThat(block.isParagraph(), iz(true))
    }

    @Test
    fun should_get_a_Block_as_Media() {
        val media = InlineMedia(localId = LOCAL_ID, localUrl = MEDIA_LOCAL_URL)
        val document = Document(listOf(media))

        val block = document.blocks.first().asMedia()
        assertThat(block, iz(media))
    }

    @Test(expected = ClassCastException::class)
    fun should_throw_a_ClassCastException_when_try_to_have_a_Block_as_Media_but_is_a_Paragraph() {
        val paragraph = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = TEXT)
        )
        val document = Document(listOf(paragraph))

        document.blocks.first().asMedia()
    }

    @Test
    fun should_get_a_Block_as_Paragraph() {
        val paragraph = Paragraph(
            localId = LOCAL_ID,
            content = Content(text = TEXT)
        )
        val document = Document(listOf(paragraph))

        val block = document.blocks.first().asParagraph()
        assertThat(block, iz(paragraph))
    }

    @Test(expected = ClassCastException::class)
    fun should_throw_a_ClassCastException_when_try_to_have_a_Block_as_Paragraph_but_is_Media() {
        val media = InlineMedia(localId = LOCAL_ID, localUrl = MEDIA_LOCAL_URL)
        val document = Document(listOf(media))

        document.blocks.first().asParagraph()
    }

    @Test
    fun should_have_localUrl_when_have_it() {
        val media = InlineMedia(localId = LOCAL_ID, localUrl = MEDIA_LOCAL_URL)

        assertThat(media.getUrlOrNull(), iz(MEDIA_LOCAL_URL))
    }

    @Test
    fun should_have_remoteUrl_when_have_it() {
        val media = InlineMedia(
            localId = LOCAL_ID,
            remoteUrl = MEDIA_REMOTE_URL
        )

        assertThat(media.getUrlOrNull(), iz(MEDIA_REMOTE_URL))
    }

    @Test
    fun should_have_localUrl_when_have_both_urls() {
        val media = InlineMedia(
            localId = LOCAL_ID, localUrl = MEDIA_LOCAL_URL,
            remoteUrl = MEDIA_REMOTE_URL
        )

        assertThat(media.getUrlOrNull(), iz(MEDIA_LOCAL_URL))
    }

    @Test
    fun should_have_null_when_have_not_urls() {
        val media = InlineMedia(localId = LOCAL_ID)

        assertThat(media.getUrlOrNull(), iz(nullValue()))
    }
}
