package com.microsoft.notes.models.extensions

import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.notesReference.models.NoteRefSourceId
import com.microsoft.notes.noteslib.R
import java.net.MalformedURLException
import java.net.URL

fun NoteReference.isSPOItem(): Boolean =
    (pageSourceId as? NoteRefSourceId.FullSourceId)?.fullSourceId?.startsWith("SPO") ?: false

fun NoteReference.getResourceUrlForSPOItem(): String {
    val strUrl = webUrl ?: return ""
    return try {
        val url = URL(strUrl)
        "${url.protocol}://${url.host}"
    } catch (e: MalformedURLException) {
        ""
    }
}

fun NoteReference.getHostUrl(): String {
    val strUrl = webUrl ?: return ""
    return try {
        val url = URL(strUrl)
        url.host
    } catch (e: MalformedURLException) {
        ""
    }
}

/*
  NoteRefColor mappings should be in sync with color mappings defined in NotesFabric @
  https://o365exchange.visualstudio.com/O365%20Core/_git/NotesFabric?path=/sources/dev/Api/Api/OneNotePagePreviews.fs
 */
enum class NoteRefColor(val value: Int) {
    WHITE(0),
    BLUE(1),
    TEAL(2),
    GREEN(3),
    RED(4),
    BLUEMIST(5),
    CYAN(6),
    APPLE(7),
    REDCHALK(8),
    PURPLEMIST(9),
    SILVER(10),
    LEMONLIME(11),
    TAN(12),
    PURPLE(13),
    MAGENTA(14),
    YELLOW(15),
    ORANGE(16);

    companion object {
        fun getDefault() = WHITE

        fun fromColorInt(color: Int?): NoteRefColor {
            return when (color) {
                0xFFFFFF -> WHITE
                0xFAF3EC -> BLUE
                0xF6F8EE -> TEAL
                0xF3F9F1 -> GREEN
                0xEEEEFC -> RED
                0xF9F1EE -> BLUEMIST
                0xFAF8F0 -> CYAN
                0xEDF9F2 -> APPLE
                0xF5F5FD -> REDCHALK
                0xF9F1F2 -> PURPLEMIST
                0xF9F7F5 -> SILVER
                0xEBF9F5 -> LEMONLIME
                0xE6EDF6 -> TAN
                0xF8F1F9 -> PURPLE
                0xF7EFFD -> MAGENTA
                0xE6F8FE -> YELLOW
                0xEAF3FC -> ORANGE
                else -> getDefault()
            }
        }

        private val colorMap = values().associateBy(NoteRefColor::value)

        fun fromInt(noteRefColor: Int?): NoteRefColor = colorMap[noteRefColor] ?: getDefault()
    }
}

fun NoteRefColor.getColorLight(): Int {
    return when (this) {
        NoteRefColor.WHITE -> R.color.note_reference_note_color_white
        NoteRefColor.BLUE -> R.color.note_reference_note_color_blue
        NoteRefColor.TEAL -> R.color.note_reference_note_color_teal
        NoteRefColor.GREEN -> R.color.note_reference_note_color_green
        NoteRefColor.RED -> R.color.note_reference_note_color_red
        NoteRefColor.BLUEMIST -> R.color.note_reference_note_color_blue_mist
        NoteRefColor.CYAN -> R.color.note_reference_note_color_cyan
        NoteRefColor.APPLE -> R.color.note_reference_note_color_apple
        NoteRefColor.REDCHALK -> R.color.note_reference_note_color_red_chalk
        NoteRefColor.PURPLEMIST -> R.color.note_reference_note_color_purple_mist
        NoteRefColor.SILVER -> R.color.note_reference_note_color_silver
        NoteRefColor.LEMONLIME -> R.color.note_reference_note_color_lemon_lime
        NoteRefColor.TAN -> R.color.note_reference_note_color_tan
        NoteRefColor.PURPLE -> R.color.note_reference_note_color_purple
        NoteRefColor.MAGENTA -> R.color.note_reference_note_color_magenta
        NoteRefColor.YELLOW -> R.color.note_reference_note_color_yellow
        NoteRefColor.ORANGE -> R.color.note_reference_note_color_orange
    }
}

fun NoteRefColor.getColorDark(): Int {
    return when (this) {
        NoteRefColor.WHITE -> R.color.note_reference_note_color_grey
        NoteRefColor.BLUE -> R.color.note_reference_note_color_blue_dark
        NoteRefColor.TEAL -> R.color.note_reference_note_color_teal_dark
        NoteRefColor.GREEN -> R.color.note_reference_note_color_green_dark
        NoteRefColor.RED -> R.color.note_reference_note_color_red_dark
        NoteRefColor.BLUEMIST -> R.color.note_reference_note_color_blue_mist_dark
        NoteRefColor.CYAN -> R.color.note_reference_note_color_cyan_dark
        NoteRefColor.APPLE -> R.color.note_reference_note_color_apple_dark
        NoteRefColor.REDCHALK -> R.color.note_reference_note_color_red_chalk_dark
        NoteRefColor.PURPLEMIST -> R.color.note_reference_note_color_purple_mist_dark
        NoteRefColor.SILVER -> R.color.note_reference_note_color_silver_dark
        NoteRefColor.LEMONLIME -> R.color.note_reference_note_color_lemon_lime_dark
        NoteRefColor.TAN -> R.color.note_reference_note_color_tan_dark
        NoteRefColor.PURPLE -> R.color.note_reference_note_color_purple_dark
        NoteRefColor.MAGENTA -> R.color.note_reference_note_color_magenta_dark
        NoteRefColor.YELLOW -> R.color.note_reference_note_color_yellow_dark
        NoteRefColor.ORANGE -> R.color.note_reference_note_color_orange_dark
    }
}
