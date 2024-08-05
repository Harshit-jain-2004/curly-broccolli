package com.microsoft.notes.richtext.editor.extensions

import com.microsoft.notes.models.Color
import com.microsoft.notes.noteslib.R

fun Color.toLighterColorResource() = when (this) {
    Color.YELLOW -> R.color.sn_note_color_yellow_lighter
    Color.GREEN -> R.color.sn_note_color_green_lighter
    Color.PINK -> R.color.sn_note_color_pink_lighter
    Color.PURPLE -> R.color.sn_note_color_purple_lighter
    Color.BLUE -> R.color.sn_note_color_blue_lighter
    Color.GREY -> R.color.sn_note_color_grey_lighter
    Color.CHARCOAL -> R.color.sn_note_color_charcoal_lighter
}

fun Color.toDarkColorResource() = when (this) {
    Color.YELLOW -> R.color.sn_note_color_yellow_dark
    Color.GREEN -> R.color.sn_note_color_green_dark
    Color.PINK -> R.color.sn_note_color_pink_dark
    Color.PURPLE -> R.color.sn_note_color_purple_dark
    Color.BLUE -> R.color.sn_note_color_blue_dark
    Color.GREY -> R.color.sn_note_color_grey_dark
    Color.CHARCOAL -> R.color.sn_note_color_charcoal_dark
}

fun Color.toMediumColorResource() = when (this) {
    Color.YELLOW -> R.color.sn_note_color_yellow_medium
    Color.GREEN -> R.color.sn_note_color_green_medium
    Color.PINK -> R.color.sn_note_color_pink_medium
    Color.PURPLE -> R.color.sn_note_color_purple_medium
    Color.BLUE -> R.color.sn_note_color_blue_medium
    Color.GREY -> R.color.sn_note_color_grey_medium
    Color.CHARCOAL -> R.color.sn_note_color_charcoal_medium
}

fun Color.toLinkTextColorResource() = when (this) {
    Color.YELLOW -> R.color.sn_link_color_yellow_note
    Color.GREEN -> R.color.sn_link_color_green_note
    Color.PINK -> R.color.sn_link_color_pink_note
    Color.PURPLE -> R.color.sn_link_color_purple_note
    Color.BLUE -> R.color.sn_link_color_blue_note
    Color.GREY -> R.color.sn_link_color_grey_note
    Color.CHARCOAL -> R.color.sn_link_color_charcoal_note
}

fun Color.toInkColorResource() = when (this) {
    Color.YELLOW -> R.color.sn_ink_color_yellow_note
    Color.GREEN -> R.color.sn_ink_color_green_note
    Color.PINK -> R.color.sn_ink_color_pink_note
    Color.PURPLE -> R.color.sn_ink_color_purple_note
    Color.BLUE -> R.color.sn_ink_color_blue_note
    Color.GREY -> R.color.sn_ink_color_grey_note
    Color.CHARCOAL -> R.color.sn_ink_color_charcoal_note
}

fun Color.toSNIconColorResource() = this.toInkColorResource()

fun Color.toIconColorNightResource() = when (this) {
    Color.YELLOW -> R.color.sn_night_icon_color_yellow
    Color.GREEN -> R.color.sn_night_icon_color_green
    Color.PINK -> R.color.sn_night_icon_color_pink
    Color.PURPLE -> R.color.sn_night_icon_color_purple
    Color.BLUE -> R.color.sn_night_icon_color_blue
    Color.GREY -> R.color.sn_night_icon_color_grey
    Color.CHARCOAL -> R.color.sn_night_icon_color_charcoal
}

fun Color.toInkColorNightResource() = when (this) {
    Color.YELLOW -> R.color.sn_night_ink_color_yellow
    Color.GREEN -> R.color.sn_night_ink_color_green
    Color.PINK -> R.color.sn_night_ink_color_pink
    Color.PURPLE -> R.color.sn_night_ink_color_purple
    Color.BLUE -> R.color.sn_night_ink_color_blue
    Color.GREY -> R.color.sn_night_ink_color_grey
    Color.CHARCOAL -> R.color.sn_night_ink_color_charcoal
}

fun Color.toSNCardColorNightResource() = when (this) {
    Color.YELLOW -> R.color.sn_night_card_color_yellow
    Color.GREEN -> R.color.sn_night_card_color_green
    Color.PINK -> R.color.sn_night_card_color_pink
    Color.PURPLE -> R.color.sn_night_card_color_purple
    Color.BLUE -> R.color.sn_night_card_color_blue
    Color.GREY -> R.color.sn_night_card_color_grey
    Color.CHARCOAL -> R.color.sn_night_card_color_charcoal
}

fun Color.toDarkHighLightColorResource() = when (this) {
    Color.YELLOW -> R.color.sn_highlight_color_yellow_dark
    Color.GREEN -> R.color.sn_highlight_color_green_dark
    Color.PINK -> R.color.sn_highlight_color_pink_dark
    Color.PURPLE -> R.color.sn_highlight_color_purple_dark
    Color.BLUE -> R.color.sn_highlight_color_blue_dark
    Color.GREY -> R.color.sn_highlight_color_grey_dark
    Color.CHARCOAL -> R.color.sn_highlight_color_charcoal_dark
}

fun Color.toLightHighLightColorResource() = when (this) {
    Color.YELLOW -> R.color.sn_highlight_color_yellow_light
    Color.GREEN -> R.color.sn_highlight_color_green_light
    Color.PINK -> R.color.sn_highlight_color_pink_light
    Color.PURPLE -> R.color.sn_highlight_color_purple_light
    Color.BLUE -> R.color.sn_highlight_color_blue_light
    Color.GREY -> R.color.sn_highlight_color_grey_light
    Color.CHARCOAL -> R.color.sn_highlight_color_charcoal_light
}

fun Color.toDarkTextHandleColorResource() = this.toIconColorNightResource()

fun Color.toLightTextHandleColorResource() = this.toSNIconColorResource()
