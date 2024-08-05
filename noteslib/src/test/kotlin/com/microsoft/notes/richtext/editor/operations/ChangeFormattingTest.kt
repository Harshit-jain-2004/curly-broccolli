package com.microsoft.notes.richtext.editor.operations

import android.text.Spannable
import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.editor.FormattingProperty
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Range
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import com.microsoft.notes.richtext.scheme.asParagraph
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ChangeFormattingTest {

    val FLAG = 0

    @Test
    fun should_set_bold_to_true() {
        val style = SpanStyle.DEFAULT
        val newStyle = style.setProperty(FormattingProperty.BOLD, true)
        assertThat(newStyle, iz(SpanStyle(bold = true)))
    }

    @Test
    fun should_set_bold_to_false() {
        val style = SpanStyle(bold = true, italic = true, underline = true, strikethrough = true)
        val newStyle = style.setProperty(FormattingProperty.BOLD, false)
        assertThat(newStyle, iz(SpanStyle(italic = true, underline = true, strikethrough = true)))
    }

    @Test
    fun should_set_italic_to_true() {
        val style = SpanStyle.DEFAULT
        val newStyle = style.setProperty(FormattingProperty.ITALIC, true)
        assertThat(newStyle, iz(SpanStyle(italic = true)))
    }

    @Test
    fun should_set_italic_to_false() {
        val style = SpanStyle(bold = true, italic = true, underline = true)
        val newStyle = style.setProperty(FormattingProperty.ITALIC, false)
        assertThat(newStyle, iz(SpanStyle(bold = true, underline = true)))
    }

    @Test
    fun should_set_underline_to_true() {
        val style = SpanStyle.DEFAULT
        val newStyle = style.setProperty(FormattingProperty.UNDERLINE, true)
        assertThat(newStyle, iz(SpanStyle(underline = true)))
    }

    @Test
    fun should_set_underline_to_false() {
        val style = SpanStyle(bold = true, italic = true, underline = true, strikethrough = true)
        val newStyle = style.setProperty(FormattingProperty.UNDERLINE, false)
        assertThat(newStyle, iz(SpanStyle(bold = true, italic = true, strikethrough = true)))
    }

    @Test
    fun should_set_strikethrough_to_false() {
        val style = SpanStyle(bold = true, italic = true, underline = true, strikethrough = true)
        val newStyle = style.setProperty(FormattingProperty.STRIKETHROUGH, false)
        assertThat(newStyle, iz(SpanStyle(bold = true, italic = true, underline = true)))
    }

    @Test
    fun should_set_strikethrough_to_true() {
        val style = SpanStyle.DEFAULT
        val newStyle = style.setProperty(FormattingProperty.STRIKETHROUGH, true)
        assertThat(newStyle, iz(SpanStyle(strikethrough = true)))
    }

    @Test
    fun should_remove_consecutive_empty_styles() {
        val styles = listOf(
            Span(SpanStyle.DEFAULT, 0, 0, FLAG),
            Span(SpanStyle.DEFAULT, 0, 0, FLAG)
        )
        val newStyles = styles.removeConsecutiveZeroLengthStyles()
        assertThat(newStyles, iz(styles.subList(1, 2)))
    }

    @Test
    fun should_remove_consecutive_empty_styles_no_ops_if_no_empty_styles() {
        val styles = listOf(
            Span(SpanStyle.DEFAULT, 0, 1, FLAG),
            Span(SpanStyle.DEFAULT, 1, 2, FLAG)
        )
        val newStyles = styles.removeConsecutiveZeroLengthStyles()
        assertThat(newStyles, iz(styles))
    }

    @Test
    fun should_remove_only_consecutive_empty_styles() {
        val styles = listOf(
            Span(SpanStyle.BOLD, 0, 1, FLAG),
            Span(SpanStyle.DEFAULT, 1, 1, FLAG),
            Span(SpanStyle.ITALIC, 1, 1, FLAG),
            Span(SpanStyle.UNDERLINE, 1, 1, FLAG),
            Span(SpanStyle.STRIKETHROUGH, 1, 1, FLAG),
            Span(SpanStyle.DEFAULT, 1, 5, FLAG)
        )
        val newStyles = styles.removeConsecutiveZeroLengthStyles()
        assertThat(newStyles, iz(listOf(styles[0], styles[4], styles[5])))
    }

    @Test
    fun should_fillStyleGaps_on_empty_is_only_default() {
        val styles = emptyList<Span>()
        val newStyles = styles.fillStyleGaps(10)
        assertThat(newStyles, iz(listOf(Span(SpanStyle.DEFAULT, 0, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))))
    }

    @Test
    fun should_fillStyleGaps_no_ops_with_no_gaps() {
        val styles = listOf(
            Span(SpanStyle.BOLD, 0, 1, FLAG),
            Span(SpanStyle.ITALIC, 1, 2, FLAG)
        )
        val newStyles = styles.fillStyleGaps(2)
        assertThat(newStyles, iz(styles))
    }

    @Test
    fun should_fillStyleGaps_inserts_default_formatting_in_gaps() {
        val styles = listOf(
            Span(SpanStyle.BOLD, 1, 2, FLAG),
            Span(SpanStyle.ITALIC, 3, 4, FLAG)
        )
        val newStyles = styles.fillStyleGaps(5)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    styles[0],
                    Span(SpanStyle.DEFAULT, 2, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    styles[1],
                    Span(SpanStyle.DEFAULT, 4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_range_matches_span_exactly() {
        val styles = listOf(Span(SpanStyle.BOLD, 2, 4, FLAG))
        val newStyles = styles.changeFormatting(2, 4, FormattingProperty.ITALIC, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD_ITALIC, 2, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_range_cancels_out_span() {
        val styles = listOf(Span(SpanStyle.BOLD, 2, 4, FLAG))
        val newStyles = styles.changeFormatting(2, 4, FormattingProperty.BOLD, false)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_range_overlaps_span_start() {
        val styles = listOf(Span(SpanStyle.BOLD, 2, 4, FLAG))
        val newStyles = styles.changeFormatting(1, 3, FormattingProperty.ITALIC, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.ITALIC, 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD_ITALIC, 2, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 3, 4, FLAG)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_range_adjacent_to_span_start() {
        val styles = listOf(Span(SpanStyle.BOLD, 2, 4, FLAG))
        val newStyles = styles.changeFormatting(2, 3, FormattingProperty.ITALIC, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD_ITALIC, 2, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 3, 4, FLAG)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_range_overlaps_span_end() {
        val styles = listOf(Span(SpanStyle.BOLD, 2, 4, FLAG))
        val newStyles = styles.changeFormatting(3, 5, FormattingProperty.ITALIC, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 2, 3, FLAG),
                    Span(SpanStyle.BOLD_ITALIC, 3, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.ITALIC, 4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_range_adjacent_to_span_end() {
        val styles = listOf(Span(SpanStyle.BOLD, 2, 4, FLAG))
        val newStyles = styles.changeFormatting(3, 4, FormattingProperty.ITALIC, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 2, 3, FLAG),
                    Span(SpanStyle.BOLD_ITALIC, 3, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_range_outside_of_span() {
        val styles = listOf(Span(SpanStyle.BOLD, 2, 4, FLAG))
        val newStyles = styles.changeFormatting(1, 2, FormattingProperty.ITALIC, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.ITALIC, 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 2, 4, FLAG)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_range_fully_encloses_span() {
        val styles = listOf(Span(SpanStyle.BOLD, 2, 4, FLAG))
        val newStyles = styles.changeFormatting(1, 5, FormattingProperty.ITALIC, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.ITALIC, 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD_ITALIC, 2, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.ITALIC, 4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_empty_selection_between_two_nonempty_spans() {
        val styles = listOf(
            Span(SpanStyle.BOLD, 0, 3, FLAG),
            Span(SpanStyle.ITALIC, 3, 6, FLAG)
        )
        val newStyles = styles.changeFormatting(3, 3, FormattingProperty.UNDERLINE, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, FLAG),
                    Span(SpanStyle(bold = true, underline = true), 3, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.ITALIC, 3, 6, FLAG)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_empty_selection_within_nonempty_span() {
        val styles = listOf(Span(SpanStyle.BOLD, 0, 5, FLAG))
        val newStyles = styles.changeFormatting(3, 3, FormattingProperty.UNDERLINE, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, FLAG),
                    Span(SpanStyle(bold = true, underline = true), 3, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 3, 5, FLAG)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_empty_selection_not_within_span() {
        val styles = listOf(Span(SpanStyle.BOLD, 0, 5, FLAG))
        val newStyles = styles.changeFormatting(6, 6, FormattingProperty.UNDERLINE, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 5, FLAG),
                    Span(SpanStyle.DEFAULT, 5, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.UNDERLINE, 6, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_empty_selection_at_start_of_span() {
        val styles = listOf(Span(SpanStyle.BOLD, 3, 5, FLAG))
        val newStyles = styles.changeFormatting(3, 3, FormattingProperty.UNDERLINE, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.UNDERLINE, 3, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 3, 5, FLAG)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_empty_selection_at_end_of_span() {
        val styles = listOf(Span(SpanStyle.BOLD, 3, 5, FLAG))
        val newStyles = styles.changeFormatting(5, 5, FormattingProperty.UNDERLINE, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 3, 5, FLAG),
                    Span(SpanStyle(bold = true, underline = true), 5, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_empty_selection_at_start_of_paragraph() {
        val styles = listOf(Span(SpanStyle.BOLD, 0, 3, FLAG))
        val newStyles = styles.changeFormatting(0, 0, FormattingProperty.UNDERLINE, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle(bold = true, underline = true), 0, 0, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 0, 3, FLAG)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_empty_Selection_at_empty_span_at_start_of_paragraph() {
        val styles = listOf(
            Span(SpanStyle.ITALIC, 0, 0, FLAG),
            Span(SpanStyle.BOLD, 0, 3, FLAG)
        )
        val newStyles = styles.changeFormatting(0, 0, FormattingProperty.UNDERLINE, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle(italic = true, underline = true), 0, 0, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 0, 3, FLAG)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_empty_selection_at_start_of_empty_paragraph() {
        val styles = emptyList<Span>()
        val newStyles = styles.changeFormatting(0, 0, FormattingProperty.UNDERLINE, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.UNDERLINE, 0, 0, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_empty_selection_on_another_empty_span_of_different_style() {
        val styles = listOf(
            Span(SpanStyle.DEFAULT, 0, 3, FLAG),
            Span(SpanStyle.BOLD, 3, 3, FLAG)
        )
        val newStyles = styles.changeFormatting(3, 3, FormattingProperty.ITALIC, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 3, FLAG),
                    Span(SpanStyle(bold = true, italic = true), 3, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_empty_selection_on_another_empty_span_of_same_style() {
        val styles = listOf(
            Span(SpanStyle.DEFAULT, 0, 3, FLAG),
            Span(SpanStyle.BOLD, 3, 3, FLAG)
        )
        val newStyles = styles.changeFormatting(3, 3, FormattingProperty.BOLD, false)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 3, FLAG),
                    Span(SpanStyle.DEFAULT, 3, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_empty_selection_on_another_empty_span_of_same_style_merge_into_same_flag() {
        val styles = listOf(
            Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 3, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newStyles = styles.changeFormatting(3, 3, FormattingProperty.BOLD, false)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_merge_adjacent_styles_with_same_properties_on_style_add_after_another_span() {
        val styles = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 3, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newStyles = styles.changeFormatting(3, 6, FormattingProperty.BOLD, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_merge_adjacent_styles_with_same_properties_on_style_removal_after_another_span() {
        val styles = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle(bold = true, underline = true), 3, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newStyles = styles.changeFormatting(3, 6, FormattingProperty.UNDERLINE, false)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_merge_adjacent_styles_with_same_properties_on_style_add_before_another_span() {
        val styles = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle(bold = true, underline = true), 3, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newStyles = styles.changeFormatting(0, 3, FormattingProperty.UNDERLINE, true)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle(bold = true, underline = true), 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_merge_adjacent_styles_with_same_properties_on_style_removal_before_another_span() {
        val styles = listOf(
            Span(SpanStyle(bold = true, italic = true), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 3, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newStyles = styles.changeFormatting(0, 3, FormattingProperty.ITALIC, false)
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_splitBySelection_selection_contained_by_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(7, 8)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 7, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 8, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_splitBySelection_selection_start_of_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(5, 7)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_splitBySelection_selection_end_of_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(8, 10)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 5, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 8, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_splitBySelection_selection_before_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(3, 5)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitBySelection_selection_after_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(10, 12)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitBySelection_selection_start_of_paragraph() {
        val span = Span(SpanStyle.BOLD, 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(0, 3)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_splitBySelection_empty_selection_start_of_paragraph() {
        val span = Span(SpanStyle.BOLD, 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(0, 0)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitBySelection_empty_selection_contained_by_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(7, 7)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_splitBySelection_empty_selection_start_of_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(5, 5)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitBySelection_empty_selection_end_of_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(10, 10)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitBySelection_empty_selection_before_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(4, 4)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitBySelection_empty_selection_after_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(11, 11)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitBySelection_empty_selection_on_empty_span() {
        val span = Span(SpanStyle.BOLD, 5, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(5, 5)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitBySelection_selection_containing_empty_span() {
        val span = Span(SpanStyle.BOLD, 5, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitBySelection(3, 7)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitByIndex_within_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitByIndex(7)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_splitByIndex_start_of_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitByIndex(5)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitByIndex_end_of_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitByIndex(10)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitByIndex_before_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitByIndex(4)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitByIndex_after_span() {
        val span = Span(SpanStyle.BOLD, 5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitByIndex(11)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_splitByIndex_start_of_span_and_start_of_paragraph() {
        val span = Span(SpanStyle.BOLD, 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val newSpans = span.splitByIndex(0)
        assertThat(newSpans, iz(listOf(span)))
    }

    @Test
    fun should_changeFormattingAssumingSplit_span_equality() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(3, 5, FormattingProperty.BOLD, true)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle(bold = true, italic = true), 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_span_equality_remove_style() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(3, 5, FormattingProperty.ITALIC, false)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.DEFAULT, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_span_set_contains_empty_span() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 5, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(3, 7, FormattingProperty.BOLD, true)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle(bold = true, italic = true), 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 5, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle(bold = true, underline = true), 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_span_set_contains_span_with_style_already_applied() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(3, 7, FormattingProperty.UNDERLINE, true)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle(italic = true, underline = true), 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_span_set_remove_style_contains_span_with_style_already_unapplied() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(3, 7, FormattingProperty.ITALIC, false)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.DEFAULT, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_span_equality_start_of_paragraph() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(0, 3, FormattingProperty.ITALIC, true)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle(bold = true, italic = true), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_span_equality_start_of_paragraph_remove_style() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(0, 3, FormattingProperty.BOLD, false)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_span_equality_no_merge() {
        val spans = listOf(
            Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(0, 3, FormattingProperty.ITALIC, true)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.ITALIC, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.UNDERLINE, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_empty_selection_no_empty_span() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(3, FormattingProperty.UNDERLINE, true)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle(bold = true, underline = true), 3, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_empty_selection_no_empty_span_remove_style() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(3, FormattingProperty.BOLD, false)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.DEFAULT, 3, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_empty_selection_no_empty_span_and_no_succeeding_style() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(3, FormattingProperty.UNDERLINE, true)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle(bold = true, underline = true), 3, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_empty_selection_no_empty_span_remove_style_and_no_succeeding_style() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(3, FormattingProperty.BOLD, false)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.DEFAULT, 3, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_empty_selection_beginning_of_block() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(0, FormattingProperty.UNDERLINE, true)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle(bold = true, underline = true), 0, 0, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_empty_selection_beginning_of_block_remove_style() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(0, FormattingProperty.BOLD, false)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 0, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_empty_selection_missing_base_span_between_spans() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.UNDERLINE, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(5, FormattingProperty.BOLD, true)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 5, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.UNDERLINE, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_changeFormattingAssumingSplit_empty_selection_missing_base_span_after_spans() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.UNDERLINE, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newSpans = spans.changeFormattingAssumingSplit(10, FormattingProperty.BOLD, true)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.UNDERLINE, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                    Span(SpanStyle.BOLD, 10, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_partitionGivenSelection_matching_span_borders() {
        val spans = listOf(
            Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val partitioned = spans.partitionGivenSelection(5, 7)
        assertThat(
            partitioned,
            iz(
                Triple(
                    listOf(Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)),
                    listOf(
                        Span(SpanStyle.DEFAULT, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                        Span(SpanStyle.DEFAULT, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                        Span(SpanStyle.DEFAULT, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ),
                    emptyList()
                )
            )
        )
    }

    @Test
    fun should_partitionGivenSelection_selection_is_superset() {
        val spans = listOf(
            Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val partitioned = spans.partitionGivenSelection(0, 10)
        assertThat(
            partitioned,
            iz(
                Triple(
                    emptyList(),
                    spans,
                    emptyList()
                )
            )
        )
    }

    @Test
    fun should_partitionGivenSelection_start_of_paragraph_matching_span_borders() {
        val spans = listOf(
            Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val partitioned = spans.partitionGivenSelection(0, 5)
        assertThat(
            partitioned,
            iz(
                Triple(
                    emptyList(),
                    listOf(
                        Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                        Span(SpanStyle.DEFAULT, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                        Span(SpanStyle.DEFAULT, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ),
                    listOf(Span(SpanStyle.DEFAULT, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                )
            )
        )
    }

    @Test
    fun should_partitionGivenSelection_not_matching_span_borders() {
        val spans = listOf(
            Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val partitioned = spans.partitionGivenSelection(2, 6)
        assertThat(
            partitioned,
            iz(
                Triple(
                    emptyList(),
                    listOf(
                        Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                        Span(SpanStyle.DEFAULT, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                        Span(SpanStyle.DEFAULT, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ),
                    listOf(Span(SpanStyle.DEFAULT, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                )
            )
        )
    }

    @Test
    fun should_partitionGivenSelection_within_span() {
        val spans = listOf(
            Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val partitioned = spans.partitionGivenSelection(4, 4)
        assertThat(
            partitioned,
            iz(
                Triple(
                    listOf(Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)),
                    listOf(Span(SpanStyle.DEFAULT, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)),
                    listOf(
                        Span(SpanStyle.DEFAULT, 5, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                        Span(SpanStyle.DEFAULT, 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    )
                )
            )
        )
    }

    @Test
    fun should_changeFormatting_on_specified_paragraph() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two")),
                    Paragraph(content = Content("three"))
                )
            )
        )
        val newState = state.changeFormatting(
            state.document.blocks[1].asParagraph(), 0, 3,
            FormattingProperty.BOLD, true
        )
        assertThat(newState.document.blocks[0], iz(state.document.blocks[0]))
        assertThat(newState.document.blocks[2], iz(state.document.blocks[2]))
        with(newState.document.blocks[1].asParagraph()) {
            assertThat(
                content,
                iz(
                    Content(
                        "two",
                        listOf(
                            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        )
                    )
                )
            )
        }
    }

    @Test
    fun should_changeFormatting_on_single_paragraph_selection() {
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two")),
                    Paragraph(content = Content("three"))
                ),
                range = Range(startBlock = 1, startOffset = 1, endBlock = 1, endOffset = 2)
            )
        )
        val newState = state.changeFormatting(FormattingProperty.BOLD, true)
        assertThat(newState.document.blocks[0], iz(state.document.blocks[0]))
        assertThat(newState.document.blocks[2], iz(state.document.blocks[2]))
        with(newState.document.blocks[1].asParagraph()) {
            assertThat(
                content,
                iz(
                    Content(
                        "two",
                        listOf(
                            Span(SpanStyle.DEFAULT, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                            Span(SpanStyle.BOLD, 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        )
                    )
                )
            )
        }
    }

    @Test
    fun should_changeFormatting_on_multi_paragraph_selection() {
        val state = EditorState(
            Document(
                blocks = listOf(
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two")),
                    Paragraph(content = Content("three"))
                ),
                range = Range(startBlock = 0, startOffset = 1, endBlock = 2, endOffset = 4)
            )
        )
        val newState = state.changeFormatting(FormattingProperty.BOLD, true)
        with(newState.document.blocks[0].asParagraph()) {
            assertThat(
                content,
                iz(
                    Content(
                        "one",
                        listOf(
                            Span(SpanStyle.DEFAULT, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                            Span(SpanStyle.BOLD, 1, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        )
                    )
                )
            )
        }
        with(newState.document.blocks[1].asParagraph()) {
            assertThat(
                content,
                iz(
                    Content(
                        "two",
                        listOf(
                            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        )
                    )
                )
            )
        }
        with(newState.document.blocks[2].asParagraph()) {
            assertThat(
                content,
                iz(
                    Content(
                        "three",
                        listOf(
                            Span(SpanStyle.BOLD, 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        )
                    )
                )
            )
        }
    }

    @Test
    fun should_mergeEqualAdjacentSpans_on_adjacent_equal_spans() {
        val styles = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 3, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newStyles = styles.mergeEqualAdjacentSpans()
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_mergeEqualAdjacentSpans_on_partial_overlap() {
        val styles = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 2, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newStyles = styles.mergeEqualAdjacentSpans()
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_mergeEqualAdjacentSpans_on_total_overlap() {
        val styles = listOf(
            Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newStyles = styles.mergeEqualAdjacentSpans()
        assertThat(
            newStyles,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                )
            )
        )
    }

    @Test
    fun should_mergeEqualAdjacentSpans_preserve_empty_spans() {
        val styles = listOf(
            Span(SpanStyle.ITALIC, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 3, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 3, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newStyles = styles.mergeEqualAdjacentSpans()
        assertThat(newStyles, iz(styles))
    }

    @Test
    fun should_mergeEqualAdjacentSpans_empty_list() {
        val styles = emptyList<Span>()
        val newStyles = styles.mergeEqualAdjacentSpans()
        assertThat(newStyles, iz(emptyList()))
    }

    @Test
    fun should_mergeEqualAdjacentSpans_empty_span_after_same_styled_non_empty_span() {
        val styles = listOf(
            Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.DEFAULT, 3, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newStyles = styles.mergeEqualAdjacentSpans()
        assertThat(newStyles, iz(listOf(Span(SpanStyle.DEFAULT, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))))
    }

    @Test
    fun should_mergeEqualAdjacentSpans_empty_span_at_beginning_with_adjacent_equal_span() {
        val styles = listOf(
            Span(SpanStyle.BOLD, 0, 0, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.BOLD, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newStyles = styles.mergeEqualAdjacentSpans()
        assertThat(newStyles, iz(listOf(Span(SpanStyle.BOLD, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))))
    }

    @Test
    fun should_mergeEqualAdjacentSpans_empty_span_at_beginning_with_adjacent_non_equal_span() {
        val styles = listOf(
            Span(SpanStyle.BOLD, 0, 0, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
            Span(SpanStyle.ITALIC, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        )
        val newStyles = styles.mergeEqualAdjacentSpans()
        assertThat(newStyles, iz(styles))
    }
}
