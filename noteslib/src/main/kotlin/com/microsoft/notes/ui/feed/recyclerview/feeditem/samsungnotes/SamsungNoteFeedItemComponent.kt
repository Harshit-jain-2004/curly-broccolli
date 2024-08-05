package com.microsoft.notes.ui.feed.recyclerview.feeditem.samsungnotes

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.NotesThemeOverride
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.styled.parseMillisToRFC1123String
import com.microsoft.notes.richtext.editor.styled.parseMillisToShortDateFormat
import com.microsoft.notes.ui.extensions.getHighlightedText
import com.microsoft.notes.ui.feed.filter.FeedSortCache
import com.microsoft.notes.ui.feed.filter.SortingCriterion
import com.microsoft.notes.ui.feed.recyclerview.feeditem.handleActionModeBorder
import com.microsoft.notes.ui.feed.recyclerview.feeditem.handleCheckBoxVisibility
import com.microsoft.notes.ui.feed.recyclerview.feeditem.setCheckBoxBackground
import com.microsoft.notes.ui.feed.recyclerview.feeditem.setContentLayoutBackgroundColor
import com.microsoft.notes.ui.theme.ThemedCardView
import kotlinx.android.synthetic.main.sn_action_mode_checkbox.view.*

open class SamsungNoteFeedItemComponent(context: Context, attrs: AttributeSet) :
    ThemedCardView(context, attrs) {
    companion object {
        private const val LOG_TAG = "SamsungNoteFeedItemComponent"
    }

    var callbacks: Callbacks? = null
    var sourceNote: Note? = null
    private val samsungNoteCardLayout: View by lazy { findViewById<View>(R.id.samsungNoteCardLayout) }
    protected val noteTitle: TextView? by lazy { findViewById<TextView>(R.id.samsungNoteTitle) }
    protected val notePreview: TextView? by lazy { findViewById<TextView>(R.id.samsungNotePreviewBody) }
    private val samsungNoteSourceLabel: TextView? by lazy { findViewById<TextView>(R.id.samsungNoteSourceLabel) }
    private val samsungNoteDateTime: TextView? by lazy { findViewById<TextView>(R.id.samsungNoteDateTime) }
    private var isFeedUIRefreshEnabled: Boolean = false

    private val actionModeCheckBox: AppCompatCheckBox? by lazy {
        checkBox?.let { setCheckBoxBackground(it.background, context, themeOverride != null) }
        checkBox
    }

    var themeOverride: NotesThemeOverride.SamsungNoteCanvasThemeOverride? = null

    open fun bindNote(
        note: Note,
        keywordsToHighlight: List<String>? = null,
        isListLayout: Boolean = false,
        isSelectionEnabled: Boolean,
        isItemSelected: Boolean,
        isFeedUIRefreshEnabled: Boolean
    ) {
        resetTextViews()
        sourceNote = note
        this.isFeedUIRefreshEnabled = isFeedUIRefreshEnabled
        applyTheme(isSelectionEnabled, isItemSelected)
        bindClickHandlers(note)

        if (note.title.isNullOrEmpty()) {
            noteTitle?.visibility = GONE
        } else {
            noteTitle?.visibility = VISIBLE
            noteTitle?.text = SpannableStringBuilder(note.title).getHighlightedText(context, keywordsToHighlight)
        }

        if (isFeedUIRefreshEnabled) {
            samsungNoteDateTime?.text = context.parseMillisToShortDateFormat(
                if (FeedSortCache.fetchPreferredSortSelection(context).first == SortingCriterion.DATE_CREATED) {
                    note.localCreatedAt
                } else {
                    note.documentModifiedAt
                }
            )
            if (note.title.isNullOrEmpty())
                notePreview?.maxLines = 4
            else
                notePreview?.maxLines = 3
            samsungNoteDateTime?.visibility = View.VISIBLE
        } else {
            samsungNoteDateTime?.visibility = View.GONE
        }

        if (isListLayout) {
            samsungNoteSourceLabel?.visibility = View.VISIBLE
        } else {
            samsungNoteSourceLabel?.visibility = View.GONE
        }

        updateAccessibilityLabel(note, isSelectionEnabled, isItemSelected)
        handleActionModeBorder(isSelectionEnabled && isItemSelected, isFeedUIRefreshEnabled, samsungNoteCardLayout, themeOverride != null)
        handleCheckBoxVisibility(
            isSelectionEnabled,
            isSelectionEnabled && isItemSelected,
            actionModeCheckBox,
            this,
            themeOverride != null
        )
    }

    private fun resetTextViews() {
        noteTitle?.text = ""
        notePreview?.text = ""
        samsungNoteDateTime?.text = ""
    }

    protected fun updateAccessibilityLabel(
        note: Note,
        isSelectionEnabled: Boolean,
        isItemSelected: Boolean
    ) {
        val contentDescription = getAccessibilityLabel(note)
        if (isSelectionEnabled && !isItemSelected) {
            samsungNoteCardLayout.contentDescription = context.resources.getString(
                R.string.sn_item_unselected, contentDescription
            )
        } else {
            samsungNoteCardLayout.contentDescription = contentDescription
        }
    }

    private fun getAccessibilityLabel(note: Note): String {
        val title: String = noteTitle?.text.toString()

        val notePreviewText = notePreview?.text
        val feedNotePreviewAnnouncementLimit = context.resources.getInteger(R.integer.feed_note_preview_announcement_limit)
        val previewText: String = when {
            notePreviewText.isNullOrEmpty() -> context.resources.getString(R.string.feed_item_accessibility_default_preview)
            notePreviewText.length > feedNotePreviewAnnouncementLimit -> notePreviewText.substring(0, feedNotePreviewAnnouncementLimit)
            else -> notePreviewText.toString()
        }

        val source = context.getString(R.string.samsung_note_accessibility_label)
        val date: String = context.getString(
            R.string.sn_label_date,
            context.parseMillisToRFC1123String(note.documentModifiedAt)
        )
        val accessibilityLabels =
            listOf(title, previewText, source, date).filter { it.isNotEmpty() }
        return accessibilityLabels.joinToString(", ")
    }

    open fun applyTheme(isSelectionEnabled: Boolean = false, isItemSelected: Boolean = false) {
        val theme = themeOverride
        sourceNote?.let {
            this.isSelected = isItemSelected && isSelectionEnabled
            setCardBackgroundColor(theme)
            if (theme != null) updateTextFontColor(theme) else updateTextFontColor()
        }
    }

    private fun bindClickHandlers(note: Note) {
        setOnClickListener { callbacks?.onNoteItemClicked(note) }
        setOnLongClickListener {
            callbacks?.onNoteItemLongPress(note, it)
            true
        }
    }

    private fun setCardBackgroundColor(themeOverride: NotesThemeOverride.SamsungNoteCanvasThemeOverride?) {
        val cardBg =
            if (themeOverride != null) ContextCompat.getColor(context, themeOverride.cardBg)
            else ContextCompat.getColor(context, R.color.samsung_note_card_bg_for_light)

        val cardBorderColor = if (themeOverride != null) ContextCompat.getColor(context, themeOverride.cardBorderColor)
        else ContextCompat.getColor(context, R.color.samsung_note_border_for_light)

        setContentLayoutBackgroundColor(cardBg, cardBorderColor, samsungNoteCardLayout, isFeedUIRefreshEnabled, true)
    }

    private fun updateTextFontColor() {
        val noteTitleColor = if (isFeedUIRefreshEnabled)
            R.color.feed_item_ui_refresh_text_color_light else R.color.samsung_note_title_color_for_light

        noteTitle?.setTextColor(
            ContextCompat.getColor(
                context,
                noteTitleColor
            )
        )

        val notePreviewColor = if (isFeedUIRefreshEnabled)
            R.color.feed_item_ui_refresh_text_color_light else R.color.samsung_note_details_color_for_light
        notePreview?.setTextColor(
            ContextCompat.getColor(
                context,
                notePreviewColor
            )
        )
        samsungNoteDateTime?.setTextColor(
            ContextCompat.getColor(
                context,
                R.color.note_reference_timestamp_color_light
            )
        )
        samsungNoteSourceLabel?.setTextColor(
            ContextCompat.getColor(
                context,
                R.color.samsung_note_details_color_for_light
            )
        )
        setFocusChangeHandler(R.color.samsung_note_title_color_for_light, R.color.samsung_note_details_color_for_light)
    }

    private fun setFocusChangeHandler(primaryTextColor: Int, seondaryTextColor: Int) {
        setOnFocusChangeListener { view: View, hasFocus: Boolean ->
            if (hasFocus) samsungNoteSourceLabel?.setTextColor(ContextCompat.getColor(context, primaryTextColor))
            else samsungNoteSourceLabel?.setTextColor(ContextCompat.getColor(context, seondaryTextColor))
        }
    }

    private fun updateTextFontColor(themeOverride: NotesThemeOverride.SamsungNoteCanvasThemeOverride) {
        val cardTitleColor = if (isFeedUIRefreshEnabled)
            ContextCompat.getColor(context, R.color.feed_item_ui_refresh_text_color_dark) else ContextCompat.getColor(context, themeOverride.cardTitleColor)
        val notePreviewColor = if (isFeedUIRefreshEnabled)
            ContextCompat.getColor(context, R.color.feed_item_ui_refresh_text_color_dark) else ContextCompat.getColor(context, themeOverride.cardDetailsColor)
        val sourceLabelColor = ContextCompat.getColor(context, themeOverride.cardDetailsColor)
        noteTitle?.setTextColor(cardTitleColor)
        notePreview?.setTextColor(notePreviewColor)
        samsungNoteDateTime?.setTextColor(ContextCompat.getColor(context, NotesLibrary.getInstance().theme.feedTimestampColor))
        samsungNoteSourceLabel?.setTextColor(sourceLabelColor)
        setFocusChangeHandler(themeOverride.cardTitleColor, themeOverride.cardDetailsColor)
    }

    interface Callbacks {
        fun onNoteItemClicked(note: Note) {}
        fun onNoteItemLongPress(note: Note, view: View) {}
    }
}
