package com.microsoft.notes.noteslib

/**
 * All input values are res ids
 */
data class NotesThemeOverride(
    val backgroundColor: Int,
    val optionToolbarBackgroundColor: Int,
    val optionIconColor: Int,
    val optionTextColor: Int,
    val optionSecondaryTextColor: Int,
    val optionIconBackgroundDrawable: Int,
    val optionBottomSheetIconColor: Int,
    val primaryAppColor: Int,
    val feedBackgroundColor: Int,
    val dividerColor: Int,
    val searchHighlightForeground: Int,
    val searchHighlightBackground: Int,
    val feedTimestampColor: Int,
    val stickyNoteCanvasThemeOverride: StickyNoteCanvasThemeOverride? = null,
    val noteRefCanvasThemeOverride: NoteRefCanvasThemeOverride? = null,
    val samsungNoteCanvasThemeOverride: SamsungNoteCanvasThemeOverride? = null,
    val sortFilterBottomSheetThemeOverride: SortFilterBottomSheetThemeOverride? = null
) {
    /**
     * Used in conjunction to override the note canvas colors. Typically used to implement a dark theme.
     */
    data class StickyNoteCanvasThemeOverride(
        val bodyColor: Int,
        val textAndInkColor: Int,
        val textHintColor: Int,
        val noteBorderColor: Int,
        val metadataColor: Int
    )

    data class NoteRefCanvasThemeOverride(
        val bodyColor: Int,
        val textAndInkColor: Int,
        val secondaryTextColor: Int,
        val noteBorderColor: Int
    )

    data class SamsungNoteCanvasThemeOverride(
        val cardBg: Int,
        val cardTitleColor: Int,
        val cardDetailsColor: Int,
        val contentBg: Int,
        val cardBorderColor: Int,
        val contentColor: Int,
        val timeStampDividerColor: Int,
        val timeStampTextColor: Int,
        val forceDarkModeInContentHTML: Boolean
    )

    data class SortFilterBottomSheetThemeOverride(
        val selectedOptionChipColor: Int,
        val sortArrowUpDrawable: Int,
        val sortArrowDownDrawable: Int
    )

    companion object {
        @JvmStatic
        val default = NotesThemeOverride(
            backgroundColor = R.color.sn_noteslist_background,
            optionToolbarBackgroundColor = R.color.sn_content_background,
            optionIconColor = R.color.secondary_text_color_light,
            optionTextColor = R.color.primary_text_color_light,
            optionSecondaryTextColor = R.color.secondary_text_color_light,
            optionIconBackgroundDrawable = R.drawable.sn_button_bg,
            optionBottomSheetIconColor = R.color.sn_bottom_sheet_row_text,
            primaryAppColor = R.color.sn_primary_color,
            feedBackgroundColor = R.color.feed_background_light,
            dividerColor = R.color.divider_light,
            searchHighlightForeground = R.color.sn_search_highlight_foreground_light,
            searchHighlightBackground = R.color.sn_search_highlight_background_light,
            feedTimestampColor = R.color.note_reference_timestamp_color_light,
            sortFilterBottomSheetThemeOverride = SortFilterBottomSheetThemeOverride(
                selectedOptionChipColor = R.color.bottom_sheet_selected_chip_color,
                sortArrowUpDrawable = R.drawable.icon_arrowup,
                sortArrowDownDrawable = R.drawable.icon_arrowdown
            )
        )
    }
}
