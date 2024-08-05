package com.microsoft.notes.richtext.render

import com.microsoft.notes.richtext.scheme.mediaListCount
import com.microsoft.notes.richtext.scheme.paragraphList
import com.microsoft.notes.richtext.scheme.paragraphListCount
import com.microsoft.notes.richtext.scheme.size
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class StringExtensionsTest {
    val LINE_1 = "a"
    val LINE_2 = "b"
    val LINE_3 = "c"
    val EMPTY_NEW_LINE = ""
    val WHITE_SPACE = " "
    val NEW_LINE = "\n"

    @Test
    fun should_get_lines_ended_by_new_line() {
        val text = LINE_1 + NEW_LINE +
            LINE_2 + NEW_LINE +
            LINE_3 + NEW_LINE

        val lines = text.lines()

        assertThat(lines, iz(notNullValue()))
        assertThat(lines.size, iz(4))
        assertThat(lines[0], iz(LINE_1))
        assertThat(lines[1], iz(LINE_2))
        assertThat(lines[2], iz(LINE_3))
        assertThat(lines[3], iz(EMPTY_NEW_LINE))
    }

    @Test
    fun should_get_one_line_when_no_new_lines() {
        val text = LINE_1 + LINE_2 + LINE_3

        val lines = text.lines()

        assertThat(lines, iz(notNullValue()))
        assertThat(lines.size, iz(1))
        assertThat(lines[0], iz(LINE_1 + LINE_2 + LINE_3))
    }

    @Test
    fun should_get_one_line_from_empty_text() {
        val text = ""

        val lines = text.lines()
        assertThat(lines, iz(notNullValue()))
        assertThat(lines.size, iz(1))
        assertThat(lines[0], iz(EMPTY_NEW_LINE))
    }

    @Test
    fun should_get_empty_lines_from_empty_text_with_new_lines() {
        val text = NEW_LINE + NEW_LINE

        val lines = text.lines()
        assertThat(lines, iz(notNullValue()))
        assertThat(lines.size, iz(3))
        assertThat(lines[0], iz(EMPTY_NEW_LINE))
        assertThat(lines[1], iz(EMPTY_NEW_LINE))
        assertThat(lines[2], iz(EMPTY_NEW_LINE))
    }

    @Test
    fun should_get_empty_lines_from_empty_text() {
        val text = EMPTY_NEW_LINE + NEW_LINE + WHITE_SPACE

        val lines = text.lines()
        assertThat(lines, iz(notNullValue()))
        assertThat(lines.size, iz(2))
        assertThat(lines[0], iz(EMPTY_NEW_LINE))
        assertThat(lines[1], iz(WHITE_SPACE))
    }

    @Test
    fun should_get_lines_with_content_and_empty() {
        val text = LINE_1 + NEW_LINE +
            LINE_2 + NEW_LINE +
            EMPTY_NEW_LINE + NEW_LINE +
            LINE_3

        val lines = text.lines()

        assertThat(lines, iz(notNullValue()))
        assertThat(lines.size, iz(4))
        assertThat(lines[0], iz(LINE_1))
        assertThat(lines[1], iz(LINE_2))
        assertThat(lines[2], iz(EMPTY_NEW_LINE))
        assertThat(lines[3], iz(LINE_3))
    }

    @Test
    fun should_get_list_of_Paragraphs() {
        val text = LINE_1 + NEW_LINE +
            EMPTY_NEW_LINE + NEW_LINE +
            LINE_2 + NEW_LINE +
            EMPTY_NEW_LINE + NEW_LINE +
            LINE_3

        val paragraphs = text.splitIntoParagraphs()

        assertThat(paragraphs, iz(notNullValue()))
        assertThat(paragraphs.size, iz(5))
        assertThat(paragraphs[0].content.text, iz(LINE_1))
        assertThat(paragraphs[1].content.text, iz(EMPTY_NEW_LINE))
        assertThat(paragraphs[2].content.text, iz(LINE_2))
        assertThat(paragraphs[3].content.text, iz(EMPTY_NEW_LINE))
        assertThat(paragraphs[4].content.text, iz(LINE_3))
    }

    @Test
    fun should_create_Document() {
        val text = LINE_1 + NEW_LINE +
            EMPTY_NEW_LINE + NEW_LINE +
            LINE_2 + NEW_LINE +
            EMPTY_NEW_LINE + NEW_LINE +
            LINE_3

        val document = text.toDocument()

        assertThat(document, iz(notNullValue()))
        assertThat(document.size(), iz(5))
        assertThat(document.paragraphListCount(), iz(5))
        assertThat(document.mediaListCount(), iz(0))
        with(document.paragraphList()) {
            assertThat(get(0).content.text, iz(LINE_1))
            assertThat(get(1).content.text, iz(EMPTY_NEW_LINE))
            assertThat(get(2).content.text, iz(LINE_2))
            assertThat(get(3).content.text, iz(EMPTY_NEW_LINE))
            assertThat(get(4).content.text, iz(LINE_3))
        }
    }
}
