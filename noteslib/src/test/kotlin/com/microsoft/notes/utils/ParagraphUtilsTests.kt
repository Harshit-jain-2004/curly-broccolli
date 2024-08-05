package com.microsoft.notes.utils

import com.microsoft.notes.utils.utils.toParagraphs
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ParagraphUtilsTests {
    val firstLineOfText = "Text1"
    val secondLineOfText = "Text2"
    val thirdLineOfText = "Text3"

    @Test
    fun should_return_empty_list_of_paragraph_for_empty_list_of_text() {
        val paragraphs = toParagraphs(listOf())
        assert(paragraphs.size == 0)
    }

    @Test
    fun should_split_empty_line() {
        val testString = ""
        val paragraphs = toParagraphs(listOf(testString))
        assert(paragraphs.size == 1)
        assertThat(paragraphs.get(0).content.text, iz(""))
    }

    @Test
    fun should_split_empty_line_with_line_separator() {
        val testString = System.lineSeparator()
        val paragraphs = toParagraphs(listOf(testString))
        assert(paragraphs.size == 2)
        assertThat(paragraphs.get(0).content.text, iz(""))
        assertThat(paragraphs.get(1).content.text, iz(""))
    }

    @Test
    fun should_split_one_line_with_line_separator_on_start() {
        val testString = System.lineSeparator() + firstLineOfText
        val paragraphs = toParagraphs(listOf(testString))
        assert(paragraphs.size == 2)
        assertThat(paragraphs.get(0).content.text, iz(""))
        assertThat(paragraphs.get(1).content.text, iz(firstLineOfText))
    }

    @Test
    fun should_split_one_line_with_line_separator_on_end() {
        val testString = firstLineOfText + System.lineSeparator()
        val paragraphs = toParagraphs(listOf(testString))
        assert(paragraphs.size == 2)
        assertThat(paragraphs.get(0).content.text, iz(firstLineOfText))
        assertThat(paragraphs.get(1).content.text, iz(""))
    }

    @Test
    fun should_split_one_line_with_line_separator_on_both_ends() {
        val testString = System.lineSeparator() + firstLineOfText + System.lineSeparator()
        val paragraphs = toParagraphs(listOf(testString))
        assert(paragraphs.size == 3)
        assertThat(paragraphs.get(0).content.text, iz(""))
        assertThat(paragraphs.get(1).content.text, iz(firstLineOfText))
        assertThat(paragraphs.get(2).content.text, iz(""))
    }

    @Test
    fun should_split_line() {
        val testString = firstLineOfText + System.lineSeparator() + secondLineOfText + System.lineSeparator() +
            thirdLineOfText
        val paragraphs = toParagraphs(listOf(testString))
        assert(paragraphs.size == 3)
        assertThat(paragraphs.get(0).content.text, iz(firstLineOfText))
        assertThat(paragraphs.get(1).content.text, iz(secondLineOfText))
        assertThat(paragraphs.get(2).content.text, iz(thirdLineOfText))
    }

    @Test
    fun should_return_multiple_paragraphs_for_multiple_lines() {
        val paragraphs = toParagraphs(listOf(firstLineOfText, secondLineOfText))
        assert(paragraphs.size == 2)
        assertThat(paragraphs.get(0).content.text, iz(firstLineOfText))
        assertThat(paragraphs.get(1).content.text, iz(secondLineOfText))
    }
}
