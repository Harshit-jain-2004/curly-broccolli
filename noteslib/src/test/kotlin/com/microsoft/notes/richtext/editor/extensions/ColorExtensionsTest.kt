package com.microsoft.notes.richtext.editor.extensions

import com.microsoft.notes.models.Color
import com.microsoft.notes.noteslib.R
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ColorExtensionsTest {

    @Test
    fun should_parse_Yellow_Color_to_correct_Int_resources() {
        assertThat(Color.YELLOW.toLighterColorResource(), iz(R.color.sn_note_color_yellow_lighter))
        assertThat(Color.YELLOW.toMediumColorResource(), iz(R.color.sn_note_color_yellow_medium))
        assertThat(Color.YELLOW.toLinkTextColorResource(), iz(R.color.sn_link_color_yellow_note))
    }

    @Test
    fun should_parse_Blue_Color_to_correct_Int_resources() {
        assertThat(Color.BLUE.toLighterColorResource(), iz(R.color.sn_note_color_blue_lighter))
        assertThat(Color.BLUE.toMediumColorResource(), iz(R.color.sn_note_color_blue_medium))
        assertThat(Color.BLUE.toLinkTextColorResource(), iz(R.color.sn_link_color_blue_note))
    }

    @Test
    fun should_parse_Green_Color_to_correct_Int_resources() {
        assertThat(Color.GREEN.toLighterColorResource(), iz(R.color.sn_note_color_green_lighter))
        assertThat(Color.GREEN.toMediumColorResource(), iz(R.color.sn_note_color_green_medium))
        assertThat(Color.GREEN.toLinkTextColorResource(), iz(R.color.sn_link_color_green_note))
    }

    @Test
    fun should_parse_Pink_Color_to_correct_Int_resources() {
        assertThat(Color.PINK.toLighterColorResource(), iz(R.color.sn_note_color_pink_lighter))
        assertThat(Color.PINK.toMediumColorResource(), iz(R.color.sn_note_color_pink_medium))
        assertThat(Color.PINK.toLinkTextColorResource(), iz(R.color.sn_link_color_pink_note))
    }

    @Test
    fun should_parse_Purple_Color_to_correct_Int_resources() {
        assertThat(Color.PURPLE.toLighterColorResource(), iz(R.color.sn_note_color_purple_lighter))
        assertThat(Color.PURPLE.toMediumColorResource(), iz(R.color.sn_note_color_purple_medium))
        assertThat(Color.PURPLE.toLinkTextColorResource(), iz(R.color.sn_link_color_purple_note))
    }

    @Test
    fun should_parse_Grey_Color_to_correct_Int_resources() {
        assertThat(Color.GREY.toLighterColorResource(), iz(R.color.sn_note_color_grey_lighter))
        assertThat(Color.GREY.toMediumColorResource(), iz(R.color.sn_note_color_grey_medium))
        assertThat(Color.GREY.toLinkTextColorResource(), iz(R.color.sn_link_color_grey_note))
    }

    @Test
    fun should_parse_Charcoal_Color_to_correct_Int_resources() {
        assertThat(Color.CHARCOAL.toLighterColorResource(), iz(R.color.sn_note_color_charcoal_lighter))
        assertThat(Color.CHARCOAL.toMediumColorResource(), iz(R.color.sn_note_color_charcoal_medium))
        assertThat(Color.CHARCOAL.toLinkTextColorResource(), iz(R.color.sn_link_color_charcoal_note))
    }
}
