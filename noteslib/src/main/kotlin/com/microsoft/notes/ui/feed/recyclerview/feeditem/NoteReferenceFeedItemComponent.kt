package com.microsoft.notes.ui.feed.recyclerview.feeditem

import android.content.Context
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.extensions.NoteRefColor
import com.microsoft.notes.models.extensions.getColorDark
import com.microsoft.notes.models.extensions.getColorLight
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.NotesThemeOverride
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.styled.gallery.AspectRatioImageView
import com.microsoft.notes.richtext.editor.styled.loadRoundedImageFromUri
import com.microsoft.notes.richtext.editor.styled.parseMillisToRFC1123String
import com.microsoft.notes.richtext.editor.styled.parseMillisToShortDateFormat
import com.microsoft.notes.store.Promise
import com.microsoft.notes.ui.extensions.getHighlightedText
import com.microsoft.notes.ui.extensions.hide
import com.microsoft.notes.ui.extensions.show
import com.microsoft.notes.ui.feed.filter.FeedSortCache
import com.microsoft.notes.ui.feed.filter.SortingCriterion
import com.microsoft.notes.ui.feed.recyclerview.feeditem.notereference.getPreviewRichTextAsSpanned
import com.microsoft.notes.ui.theme.ThemedCardView
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.HostTelemetryKeys
import kotlinx.android.synthetic.main.sn_action_mode_checkbox.view.*
import kotlinx.android.synthetic.main.sn_note_item_layout_note_reference_ui_refresh.view.*

open class NoteReferenceFeedItemComponent(context: Context, attrs: AttributeSet) : ThemedCardView(context, attrs) {
    companion object {
        private const val LOG_TAG = "NoteReferenceFeedItem"
        private const val ICON_TAG = "<i class="
        private const val LIST_TAG = "<li>"
    }

    private val defaultPreviewText by lazy { context.resources.getString(R.string.sn_note_reference_default_preview) }
    private val defaultPreviewTextColor by lazy { context.resources.getColor(R.color.secondary_text_color_light) }

    var callbacks: Callbacks? = null
    var sourceNote: NoteReference? = null
    val noteContentLayout: View by lazy { findViewById<View>(R.id.noteContentLayout) }
    private val noteTitle: TextView? by lazy { findViewById<TextView>(R.id.noteTitle) }
    private val notePreview: TextView? by lazy { findViewById<TextView>(R.id.notePreview) }
    private val textContentLayout: ConstraintLayout? by lazy { findViewById<ConstraintLayout>(R.id.text_content_layout) }
    private val noteHeader: TextView? by lazy { findViewById<TextView>(R.id.noteHeader) }
    protected val noteSource: View? by lazy { findViewById<View>(R.id.noteSource) }
    private val noteSourceText: TextView? by lazy { findViewById<TextView>(R.id.sourceId) }
    protected val noteSourceIcon: ImageView? by lazy { findViewById<ImageView>(R.id.sourceIcon) }
    private val noteDateTime: TextView? by lazy { findViewById<TextView>(R.id.noteDateTime) }
    private val imageView: AspectRatioImageView? by lazy { findViewById<AspectRatioImageView>(R.id.noteReferencePreviewImage) }

    private val actionModeCheckBox: AppCompatCheckBox? by lazy {
        checkBox?.let { setCheckBoxBackground(it.background, context, themeOverride != null) }
        checkBox
    }
    private var isFeedUIRefreshEnabled: Boolean = false
    private val feedCardImprovementsEnabled: Boolean = NotesLibrary.getInstance().experimentFeatureFlags.feedCardImprovementsEnabled
    private val pinnedNotesEnabled: Boolean = NotesLibrary.getInstance().experimentFeatureFlags.pinnedNotesEnabled
    private val pageImagePreviewsEnabled: Boolean = NotesLibrary.getInstance().experimentFeatureFlags.pageImagePreviewsEnabled
    private val density = context.resources.displayMetrics.density

    var themeOverride: NotesThemeOverride.NoteRefCanvasThemeOverride? = null
    open fun bindNote(
        note: NoteReference,
        keywordsToHighlight: List<String>? = null,
        isListLayout: Boolean = false,
        showSource: Boolean = false,
        isSelectionEnabled: Boolean,
        isItemSelected: Boolean,
        isFeedUIRefreshEnabled: Boolean
    ) {
        resetTextViews()
        this.isFeedUIRefreshEnabled = isFeedUIRefreshEnabled
        sourceNote = note
        applyTheme(isSelectionEnabled, isItemSelected)
        bindClickHandlers(note)

        if (showSource) {
            setSourceNameAndIcon(note)
        } else {
            noteSource?.visibility = View.GONE
        }

        noteSourceText?.visibility = if (!isFeedUIRefreshEnabled || isListLayout)
            View.VISIBLE else View.GONE

        setNoteDateTime(note)
        if (isFeedUIRefreshEnabled && !feedCardImprovementsEnabled) {
            noteDateTime?.text = context.parseMillisToShortDateFormat(
                if (FeedSortCache.fetchPreferredSortSelection(context).first == SortingCriterion.DATE_CREATED) {
                    note.createdAt
                } else {
                    note.lastModifiedAt
                }
            )
            noteDateTime?.visibility = View.VISIBLE
        } else {
            noteDateTime?.visibility = View.GONE
        }

        updateAccessibilityLabel(note, showSource, isSelectionEnabled, isItemSelected)
        if (feedCardImprovementsEnabled) {
            noteContentLayout.setPadding(context.resources.getDimension(R.dimen.feed_action_mode_item_padding).toInt(), context.resources.getDimension(R.dimen.feed_action_mode_item_padding).toInt(), context.resources.getDimension(R.dimen.feed_action_mode_item_padding).toInt(), context.resources.getDimension(R.dimen.feed_action_mode_item_padding).toInt())
            (actionModeCheckBox?.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
        }
        if (pinnedNotesEnabled) {
            setPinnedNoteVisibility(note.isPinned)
        }
        if (pinnedNotesEnabled && !feedCardImprovementsEnabled) {
            notePinMark?.setPadding(context.resources.getDimension(R.dimen.feed_pinned_note_top_padding).toInt(), 0, 0, context.resources.getDimension(R.dimen.feed_pinned_note_bottom_padding).toInt())
        }
        handleActionModeBorder(isSelectionEnabled && isItemSelected, isFeedUIRefreshEnabled, noteContentLayout, themeOverride != null)
        handleCheckBoxVisibility(
            isSelectionEnabled,
            isSelectionEnabled && isItemSelected,
            actionModeCheckBox,
            this,
            themeOverride != null
        )
        if (pageImagePreviewsEnabled) {
            setNotePreviewImage(note)
        }
        setPreviewTextAndTitleVisibility(note, keywordsToHighlight)
    }

    private fun setNoteDateTime(note: NoteReference) {
        if (isFeedUIRefreshEnabled && !feedCardImprovementsEnabled) {
            noteDateTime?.text = context.parseMillisToShortDateFormat(
                if (FeedSortCache.fetchPreferredSortSelection(context).first == SortingCriterion.DATE_CREATED) {
                    note.createdAt
                } else {
                    note.lastModifiedAt
                }
            )
            noteDateTime?.visibility = View.VISIBLE
        } else {
            noteDateTime?.visibility = View.GONE
        }
    }

    private fun setPinnedNoteVisibility(isPinned: Boolean) {
        val pinnedNoteIcon: ImageView = findViewById<ImageView>(R.id.pinnedNoteIcon)
        val pinnedNoteText: TextView = findViewById<TextView>(R.id.pinnedNoteText)
        if (isPinned) {
            notePinMark.visibility = View.VISIBLE
            pinnedNoteIcon.setColorFilter(if (themeOverride != null) ContextCompat.getColor(context, R.color.pinned_note_text_color_dark) else ContextCompat.getColor(context, R.color.sn_metadata_color_charcoal))
            pinnedNoteText.setTextColor(if (themeOverride != null) ContextCompat.getColor(context, R.color.pinned_note_text_color_dark) else ContextCompat.getColor(context, R.color.sn_metadata_color_charcoal))
        } else {
            notePinMark.visibility = View.GONE
        }
    }

    private fun resetTextViews() {
        noteTitle?.text = ""
        notePreview?.text = ""
        noteDateTime?.text = ""
    }

    private fun setNotePreviewImage(note: NoteReference) {
        if (note.media?.size != 0 && !note.media?.get(0)?.localImageUrl.isNullOrEmpty()) {
            imageView?.show()
            imageView?.loadRoundedImageFromUri(note.media?.get(0)?.localImageUrl)
        } else {
            imageView.hide()
            if (note.media?.size != 0) {
                NotesLibrary.getInstance().downloadMediaFromCache(note)
            }
        }
    }

    private fun setNotePreviewProperties(note: NoteReference) {
        val lineSpacingExtraPx = context.resources.getInteger(R.integer.feed_card_note_preview_line_spacing)
        notePreview?.setLineSpacing(lineSpacingExtraPx.toFloat(), 1.0f)
        notePreview?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
        if (!note.title.isNullOrEmpty() || note.isPinned)
            notePreview?.setPadding(0, context.resources.getDimension(R.dimen.feed_card_preview_padding).toInt(), 0, 0)
        else
            notePreview?.setPadding(0, 0, 0, 0)
        notePreview?.maxLines = if (note.previewRichText != null && note.previewRichText.trim().isNotEmpty() &&
            (note.previewRichText.contains(ICON_TAG) || note.previewRichText.contains(LIST_TAG))
        ) context.resources.getInteger(R.integer.feed_card_note_preview_list_max_lines)
        else context.resources.getInteger(R.integer.feed_card_note_preview_text_max_lines)
    }

    private fun setNoteTitleProperties(isNotePinned: Boolean) {
        noteTitle?.maxLines = context.resources.getInteger(R.integer.feed_card_note_title_max_lines)
        noteTitle?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
        if (isNotePinned)
            noteTitle?.setPadding(0, 4, 0, 0)
        else
            noteTitle?.setPadding(0, 0, 0, 0)
    }

    private fun shouldShowNoPreviewInCard(note: NoteReference): Boolean {
        return feedCardImprovementsEnabled && (
            note.media?.size == 0 ||
                note.media?.get(0)?.localImageUrl.isNullOrEmpty() ||
                !pageImagePreviewsEnabled
            )
    }

    // TODO: Make the method short and remove suppress annotation
    @Suppress("LongMethod")
    private fun setPreviewTextAndTitleVisibility(note: NoteReference, keywordsToHighlight: List<String>?) {
        textContentLayout?.visibility = View.VISIBLE
        if (note.title?.trim().isNullOrBlank() && note.previewText.trim().isEmpty()) {
            if (isFeedUIRefreshEnabled) {
                noteTitle?.hide()
                if (shouldShowNoPreviewInCard(note)) {
                    notePreview?.show()
                    imageView.hide()
                    notePreview?.text = context.resources.getString(R.string.sn_note_reference_empty_card_preview)
                    notePreview?.setTextColor(if (themeOverride != null) ContextCompat.getColor(context, R.color.sn_metadata_color_charcoal) else ContextCompat.getColor(context, R.color.secondary_text_color_light))
                    setNotePreviewProperties(note)
                } else if (feedCardImprovementsEnabled && note.media?.size != 0) {
                    textContentLayout?.visibility = View.GONE
                    notePreview?.hide()
                } else {
                    notePreview?.hide()
                }
            } else if (hidePreviewTextIfTitlePresent()) {
                noteTitle?.text = context.resources.getString(R.string.sn_note_reference_default_title)
                noteTitle?.show()
                notePreview?.hide()
            } else {
                notePreview?.text = defaultPreviewText
                notePreview?.show()
                noteTitle?.hide()
            }
        } else if (note.title?.trim().isNullOrBlank() && note.previewText.trim().isNotEmpty()) {
            setPreviewText(note, notePreview, keywordsToHighlight)
            if (hidePreviewTextIfTitlePresent() || isFeedUIRefreshEnabled) {
                notePreview?.ellipsize = TextUtils.TruncateAt.END
                notePreview?.maxLines = 4
                if (feedCardImprovementsEnabled)
                    setNotePreviewProperties(note)
            }
            noteTitle?.hide()
            notePreview?.show()
        } else if (!note.title?.trim().isNullOrBlank() && note.previewText.trim().isEmpty()) {
            noteTitle?.text = SpannableStringBuilder(note.title).getHighlightedText(context, keywordsToHighlight)
            if (hidePreviewTextIfTitlePresent() || isFeedUIRefreshEnabled) {
                noteTitle?.ellipsize = TextUtils.TruncateAt.END
                if (feedCardImprovementsEnabled)
                    setNoteTitleProperties(note.isPinned)
                noteTitle?.maxLines = 4
            }
            notePreview?.hide()
            noteTitle?.show()
        } else {
            noteTitle?.text = SpannableStringBuilder(note.title).getHighlightedText(context, keywordsToHighlight)
            setPreviewText(note, notePreview, keywordsToHighlight)
            if (hidePreviewTextIfTitlePresent()) {
                noteTitle?.maxLines = 4
                noteTitle?.ellipsize = TextUtils.TruncateAt.END
                notePreview?.hide()
            } else if (isFeedUIRefreshEnabled) {
                if (feedCardImprovementsEnabled) {
                    setNotePreviewProperties(note)
                    setNoteTitleProperties(note.isPinned)
                } else {
                    notePreview?.maxLines = 3
                    noteTitle?.maxLines = 1
                }
                notePreview?.ellipsize = TextUtils.TruncateAt.END
                noteTitle?.ellipsize = TextUtils.TruncateAt.END
                notePreview?.show()
            } else {
                notePreview?.show()
            }
            noteTitle?.show()
        }
    }

    private fun hidePreviewTextIfTitlePresent(): Boolean =
        NotesLibrary.getInstance().experimentFeatureFlags.hidePreviewTextFromNoteRefEnabled

    private fun feedEnrichedPagePreviewsEnabled(): Boolean =
        NotesLibrary.getInstance().experimentFeatureFlags.feedEnrichedPagePreviewsEnabled

    private fun updateAccessibilityLabel(note: NoteReference, showSource: Boolean, isSelectionEnabled: Boolean, isItemSelected: Boolean) {
        val contentDescription = getAccessibilityLabel(note, showSource)
        if (isSelectionEnabled && !isItemSelected) {
            noteContentLayout.contentDescription = context.resources.getString(
                R.string.sn_item_unselected, contentDescription
            )
        } else {
            noteContentLayout.contentDescription = contentDescription
        }
    }

    private fun setPreviewText(note: NoteReference, notePreview: TextView?, keywordsToHighlight: List<String>?) {
        notePreview?.text =
            if (NotesLibrary.getInstance().experimentFeatureFlags.feedRichTextPagePreviewsEnabled &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                !note.previewRichText.isNullOrEmpty()
            ) {
                try {
                    getPreviewRichTextAsSpanned(note.previewRichText.trim(), context, notePreview)
                } catch (e: java.lang.Exception) {
                    NotesLibrary.getInstance().notesLogger?.recordTelemetry(
                        EventMarkers.NoteReferenceRichPreviewFailed,
                        Pair(HostTelemetryKeys.ERROR_MESSAGE, e.javaClass.name)
                    )

                    SpannableStringBuilder(note.previewText.trim()).getHighlightedText(
                        context,
                        keywordsToHighlight
                    )
                }
            } else {
                SpannableStringBuilder(note.previewText.trim()).getHighlightedText(
                    context,
                    keywordsToHighlight
                )
            }
    }

    // TODO should preview be added here?
    private fun getAccessibilityLabel(note: NoteReference, showSource: Boolean): String {
        val type: String = context.getString(R.string.note_reference_note)
        val isPinned = if (pinnedNotesEnabled && note.isPinned) context.resources.getString(R.string.feed_card_pinned_label) else ""
        val title: String = if (!feedCardImprovementsEnabled && noteTitle?.text.isNullOrEmpty() && notePreview?.text.isNullOrBlank()) context.resources.getString(R.string.sn_note_reference_default_title)
        else noteTitle?.text.toString()

        val notePreviewText = notePreview?.text
        val feedNotePreviewAnnouncementLimit = context.resources.getInteger(R.integer.feed_note_preview_announcement_limit)
        val previewText: String = when {
            notePreviewText.isNullOrBlank() -> if (feedCardImprovementsEnabled) "" else context.resources.getString(R.string.feed_item_accessibility_default_preview)
            notePreviewText.length > feedNotePreviewAnnouncementLimit -> notePreviewText.substring(0, feedNotePreviewAnnouncementLimit)
            else -> notePreviewText.toString()
        }

        val source: String = if (!feedCardImprovementsEnabled && showSource) getSourceName(note) else ""
        val date: String = if (!feedCardImprovementsEnabled) context.getString(
            R.string.sn_label_date,
            context.parseMillisToRFC1123String(note.lastModifiedAt)
        ) else ""
        val accessibilityLabels = listOf(type, isPinned, title, previewText, source, date).filter { it.isNotEmpty() }
        return accessibilityLabels.joinToString(", ")
    }

    open fun applyTheme(isSelectionEnabled: Boolean = false, isItemSelected: Boolean = false) {
        val theme = themeOverride
        sourceNote?.let {
            this.isSelected = isItemSelected && isSelectionEnabled
            setContentLayoutBackgroundColor(theme)
            if (theme != null) updateTextFontColor(theme) else updateTextFontColor()
            // TODO will have to change the section icon color here for light and dark modes
        }
    }

    private fun bindClickHandlers(note: NoteReference) {
        setOnClickListener { callbacks?.onNoteItemClicked(note) }
        setOnLongClickListener {
            callbacks?.onNoteItemLongPress(note, it)
            true
        }
    }

    // TODO: update below to handle section color values from OneNote
    fun setSourceNameAndIcon(note: NoteReference) {
        if (!isFeedUIRefreshEnabled) {
            noteSourceText?.text = getSourceName(note)
        } else {
            if (feedCardImprovementsEnabled)
                noteHeader?.visibility = View.GONE
            else
                noteHeader?.text = getSourceName(note)
        }
        setSourceIcon()
        noteSourceText?.visibility = View.VISIBLE
        if (feedCardImprovementsEnabled)
            noteSource?.visibility = View.GONE
        else
            noteSource?.visibility = View.VISIBLE
    }

    protected open fun setSourceIcon() {
        noteSourceIcon?.setImageResource(R.drawable.sn_ic_section)
        noteSourceIcon?.setColorFilter(ContextCompat.getColor(context, NotesLibrary.getInstance().theme.primaryAppColor))
    }

    protected open fun getSourceName(note: NoteReference): String =
        resources.getString(R.string.sourceText, note.rootContainerName, note.containerName)

    private fun getBodyColor(themeOverride: NotesThemeOverride.NoteRefCanvasThemeOverride?): Int {
        val bodyColor = if (feedEnrichedPagePreviewsEnabled()) {
            val color = sourceNote?.color ?: NoteRefColor.getDefault()
            if (themeOverride != null) {
                color.getColorDark()
            } else {
                color.getColorLight()
            }
        } else {
            themeOverride?.bodyColor ?: R.color.note_reference_note_color_white
        }
        return ContextCompat.getColor(context, bodyColor)
    }

    private fun setContentLayoutBackgroundColor(themeOverride: NotesThemeOverride.NoteRefCanvasThemeOverride?) {
        val cardBodyColor = getBodyColor(themeOverride)

        val cardBorderColor = if (themeOverride != null) ContextCompat.getColor(context, themeOverride.noteBorderColor)
        else ContextCompat.getColor(context, R.color.note_border_color_light)

        setContentLayoutBackgroundColor(cardBodyColor, cardBorderColor, noteContentLayout, isFeedUIRefreshEnabled, false)
    }

    private fun setFocusChangeHandler(primaryTextColor: Int, secondaryTextColor: Int) {
        setOnFocusChangeListener { _, hasFocus: Boolean ->
            if (hasFocus) noteHeader?.setTextColor(ContextCompat.getColor(context, primaryTextColor))
            else noteHeader?.setTextColor(ContextCompat.getColor(context, secondaryTextColor))
            if (hasFocus) noteSourceText?.setTextColor(ContextCompat.getColor(context, primaryTextColor))
            else noteSourceText?.setTextColor(ContextCompat.getColor(context, secondaryTextColor))
        }
    }

    private fun updateTextFontColor() {
        val textColorValue = if (isFeedUIRefreshEnabled) ContextCompat.getColor(context, R.color.feed_item_ui_refresh_text_color_light)
        else ContextCompat.getColor(context, R.color.primary_text_color_light)

        noteTitle?.setTextColor(textColorValue)

        notePreview?.setTextColor(textColorValue)

        noteDateTime?.setTextColor(ContextCompat.getColor(context, R.color.note_reference_timestamp_color_light))

        noteHeader?.setTextColor(ContextCompat.getColor(context, R.color.secondary_text_color_light))
        noteSourceText?.setTextColor(ContextCompat.getColor(context, R.color.secondary_text_color_light))
        setFocusChangeHandler(R.color.primary_text_color_light, R.color.secondary_text_color_light)
    }

    private fun updateTextFontColor(themeOverride: NotesThemeOverride.NoteRefCanvasThemeOverride) {
        val textColorValue = ContextCompat.getColor(context, themeOverride.textAndInkColor)
        val secondaryTextColor = ContextCompat.getColor(context, themeOverride.secondaryTextColor)
        noteTitle?.setTextColor(textColorValue)

        notePreview?.setTextColor(textColorValue)

        noteDateTime?.setTextColor(ContextCompat.getColor(context, NotesLibrary.getInstance().theme.feedTimestampColor))

        noteHeader?.setTextColor(secondaryTextColor)
        noteSourceText?.setTextColor(secondaryTextColor)
        setFocusChangeHandler(themeOverride.textAndInkColor, themeOverride.secondaryTextColor)
    }

    interface Callbacks {
        fun onNoteItemClicked(note: NoteReference) {}
        fun onNoteItemLongPress(note: NoteReference, view: View) {}
        fun shareNoteItem(note: NoteReference) {}
        fun deleteNoteItem(note: NoteReference) {}
        fun organiseNoteItem(note: NoteReference) {}
        fun deleteNoteItems(notes: List<NoteReference>) {}
        fun organizeNoteItems(notes: List<NoteReference>) {}
        fun canPerformActionOnNote(note: NoteReference): Promise<Boolean>? = null
        fun pinNoteShortcutToHomeScreen(note: NoteReference) {}
    }
}
