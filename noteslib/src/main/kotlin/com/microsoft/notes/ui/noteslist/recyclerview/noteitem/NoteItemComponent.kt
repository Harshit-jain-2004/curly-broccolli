package com.microsoft.notes.ui.noteslist.recyclerview.noteitem

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.Note
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.NotesThemeOverride
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.styled.parseMillisToShortDateFormat
import com.microsoft.notes.richtext.editor.styled.toCardColorContextColor
import com.microsoft.notes.richtext.editor.styled.toIconColorContextColor
import com.microsoft.notes.richtext.editor.styled.toMetadataColor
import com.microsoft.notes.richtext.editor.styled.toTopBarContextColor
import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.NoteContext
import com.microsoft.notes.richtext.scheme.asParagraph
import com.microsoft.notes.richtext.scheme.isParagraph
import com.microsoft.notes.ui.extensions.getAccessibilityLabel
import com.microsoft.notes.ui.extensions.setContrastColor
import com.microsoft.notes.ui.extensions.setDateTimeContrastColor
import com.microsoft.notes.ui.extensions.setDateTimeContrastColorForFeedUIRefresh
import com.microsoft.notes.ui.feed.NoteContextComponent
import com.microsoft.notes.ui.feed.filter.FeedSortCache
import com.microsoft.notes.ui.feed.filter.SortingCriterion
import com.microsoft.notes.ui.feed.recyclerview.feeditem.handleActionModeBorder
import com.microsoft.notes.ui.feed.recyclerview.feeditem.handleCheckBoxVisibility
import com.microsoft.notes.ui.feed.recyclerview.feeditem.setCheckBoxBackground
import com.microsoft.notes.ui.feed.recyclerview.feeditem.setContentLayoutBackgroundColor
import com.microsoft.notes.ui.note.reminder.ReminderLabelComponent
import com.microsoft.notes.ui.theme.ThemedCardView
import kotlinx.android.synthetic.main.sn_action_mode_checkbox.view.*
import kotlinx.android.synthetic.main.sticky_note_top_bar.view.*
import kotlin.math.min

open class NoteItemComponent(context: Context, attrs: AttributeSet) : ThemedCardView(context, attrs) {
    var sourceNote: Note? = null
    var callbacks: Callbacks? = null
    val noteContentLayout: View by lazy { findViewById<View>(R.id.noteContentLayout) }
    val noteBody: TextView? by lazy { findViewById<TextView>(R.id.noteBody) }
    val recordingIcon: CardView? by lazy { findViewById<CardView>(R.id.recordingIcon) }
    private val noteImage: ImageView? by lazy { findViewById<ImageView>(R.id.noteImage1) }
    val noteDateTime: TextView? by lazy { findViewById<TextView>(R.id.noteDateTime) }
    val reminderLabel: ReminderLabelComponent? by lazy { findViewById<ReminderLabelComponent>(R.id.reminderLabel) }
    var themeOverride: NotesThemeOverride.StickyNoteCanvasThemeOverride? = null
    private val actionModeCheckBox: AppCompatCheckBox? by lazy {
        checkBox?.let { setCheckBoxBackground(it.background, context, themeOverride != null) }
        checkBox
    }
    private val noteSource: View? by lazy { findViewById<View>(R.id.noteSource) }
    private val noteSourceText: TextView? by lazy { findViewById<TextView>(R.id.sourceId) }
    private val noteSourceIcon: ImageView? by lazy { findViewById<ImageView>(R.id.sourceIcon) }
    var isFeedUiRefreshEnabled = false

    open fun bindNote(
        note: Note,
        keywordsToHighlight: List<String>? = null,
        isSelectionEnabled: Boolean = false,
        isItemSelected: Boolean = false,
        showDateTime: Boolean = false,
        showSource: Boolean = false,
        showSourceText: Boolean = false,
        isFeedUiRefreshEnabled: Boolean = false
    ) {
        resetTextViews()
        sourceNote = note
        this.isFeedUiRefreshEnabled = isFeedUiRefreshEnabled
        bindClickHandlers(note)
        applyTheme(isSelectionEnabled, isItemSelected)
        if (showDateTime) {
            noteDateTime?.text = context.parseMillisToShortDateFormat(
                if (FeedSortCache.fetchPreferredSortSelection(context).first == SortingCriterion.DATE_CREATED) {
                    note.localCreatedAt
                } else {
                    note.documentModifiedAt
                }
            )
            noteDateTime?.visibility = View.VISIBLE
        } else {
            noteDateTime?.visibility = View.GONE
        }

        if (showSource) {
            noteSourceIcon?.setColorFilter(note.color.toIconColorContextColor(context, themeOverride))
            noteSourceText?.visibility = if (showSourceText) View.VISIBLE else View.GONE
            noteSource?.visibility = View.VISIBLE
        } else {
            noteSource?.visibility = View.GONE
        }

        if (NotesLibrary.getInstance().experimentFeatureFlags.stickyNotesCardImprovementsEnabled) {
            sourceNote?.document?.blocks?.let { setPreviewTextMaxLines(it) }
        }

        setPinnedLayout(note.isPinned)
        reminderLabel?.setReminderLayout(note.metadata.reminder)
        note.metadata.context?.let { setContextLayout(it) }
        setTopMarginOfNoteBody(showSource, showDateTime, note.isPinned)

        updateAccessibilityLabel(note, isSelectionEnabled, isItemSelected)

        handleActionModeBorder(isSelectionEnabled && isItemSelected, isFeedUiRefreshEnabled, noteContentLayout, themeOverride != null)
        handleCheckBoxVisibility(
            isSelectionEnabled,
            isSelectionEnabled && isItemSelected,
            actionModeCheckBox,
            this,
            themeOverride != null
        )
    }
    private fun setTopMarginOfNoteBody(showSource: Boolean, showDateTime: Boolean, isPinned: Boolean) {
        val pinnedNoteCardTopMargin = context.resources.getDimensionPixelSize(R.dimen.sticky_note_body_top_margin)
        val verticalNotesListCardTopMargin = context.resources.getDimensionPixelSize(R.dimen.sn_feed_card_top_margin)
        if (NotesLibrary.getInstance().experimentFeatureFlags.pinnedNotesEnabled && isPinned) {
            (noteImage?.layoutParams as MarginLayoutParams?)?.topMargin = pinnedNoteCardTopMargin
            (noteBody?.layoutParams as MarginLayoutParams?)?.topMargin = pinnedNoteCardTopMargin
        } else {
            (noteBody?.layoutParams as MarginLayoutParams?)?.topMargin = 0
            if (!showSource && showDateTime) {
                (noteBody?.layoutParams as MarginLayoutParams?)?.topMargin = verticalNotesListCardTopMargin
                (noteDateTime?.layoutParams as MarginLayoutParams?)?.topMargin = verticalNotesListCardTopMargin
            }
        }
    }

    private fun resetTextViews() {
        noteBody?.text = ""
        noteDateTime?.text = ""
    }

    open fun onRemovingFromParent() {
    }

    private fun setPreviewTextMaxLines(blocks: List<Block>) {
        /* As of now we only support block type either inlineMedia or Paragraph for a note
           hence currently checking for first block only to avoid setting this for ink note */
        if (blocks.isEmpty() || !blocks[0].isParagraph()) return
        noteBody?.maxLines = MINIMUM_TEXT_PREVIEW_LINES
        for (i in 0 until min(blocks.size, 6 /* check if list is present in first 6 paragraph blocks, then only we increase the max lines in preview text */)) {
            if (blocks[i].isParagraph() && blocks[i].asParagraph().style.unorderedList) {
                noteBody?.maxLines = MAXIMUM_TEXT_PREVIEW_LINES
                break
            }
        }
    }

    private fun updateAccessibilityLabel(note: Note, isSelectionEnabled: Boolean, isItemSelected: Boolean) {
        val contentDescription = note.getAccessibilityLabel(context)
        if (isSelectionEnabled && !isItemSelected) {
            noteContentLayout.contentDescription = context.resources.getString(
                R.string.sn_item_unselected, contentDescription
            )
        } else {
            noteContentLayout.contentDescription = contentDescription
        }
    }

    private fun bindClickHandlers(note: Note) {
        setOnClickListener { callbacks?.onNoteItemClicked(note) }
        setOnLongClickListener {
            callbacks?.onNoteItemLongPress(note, it)
            true
        }
    }

    open fun applyTheme(isSelectionEnabled: Boolean = false, isItemSelected: Boolean = false) {
        val theme = themeOverride
        sourceNote?.let {
            this.isSelected = isItemSelected && isSelectionEnabled
            setContentLayoutBackgroundColor(it.color, theme)
            if (theme != null) updateTextFontColor(theme) else updateTextFontColor(it.color)
        }
    }

    private fun setContentLayoutBackgroundColor(
        noteColor: Color,
        themeOverride: NotesThemeOverride.StickyNoteCanvasThemeOverride?
    ) {
        val cardBodyColor = noteColor.toCardColorContextColor(context, themeOverride)

        val cardTopBarColor = noteColor.toTopBarContextColor(context, themeOverride)

        val cardBorderColor = when {
            (themeOverride != null) -> ContextCompat.getColor(context, themeOverride.noteBorderColor)
            isFeedUiRefreshEnabled -> cardTopBarColor
            else -> ContextCompat.getColor(context, R.color.sn_note_border_color_light)
        }

        setContentLayoutBackgroundColor(cardBodyColor, cardBorderColor, noteContentLayout, isFeedUiRefreshEnabled, false)
        setCardTopBarColor(cardTopBarColor, cardBorderColor)
    }

    private fun setCardTopBarColor(cardTopBarColor: Int, cardBorderColor: Int) {

        val layerDrawable = ContextCompat.getDrawable(
            context,
            R.drawable.sn_note_card_top_bg
        ) as LayerDrawable

        (
            layerDrawable.findDrawableByLayerId(
                R.id.card_top_color
            ) as GradientDrawable
            ).also {
            it.setColor(cardTopBarColor)
        }

        (
            layerDrawable.findDrawableByLayerId(
                R.id.card_top_border
            ) as GradientDrawable
            ).also {
            it.setStroke(resources.getDimensionPixelSize(R.dimen.feed_card_border_stroke_width), cardBorderColor)
        }

        sn_top_bar?.background = layerDrawable
    }

    private fun updateTextFontColor(color: Color) {
        noteBody?.setContrastColor(color)
        if (isFeedUiRefreshEnabled) {
            noteDateTime?.setDateTimeContrastColorForFeedUIRefresh(color)
        } else {
            noteDateTime?.setDateTimeContrastColor(color)
        }
        noteSourceText?.setTextColor(color.toMetadataColor(context))
    }

    private fun updateTextFontColor(themeOverride: NotesThemeOverride.StickyNoteCanvasThemeOverride) {
        val textColorValue = ContextCompat.getColor(context, themeOverride.textAndInkColor)
        noteBody?.setTextColor(textColorValue)
        val timeStampColor = when (isFeedUiRefreshEnabled) {
            true -> ContextCompat.getColor(context, themeOverride.metadataColor)
            false -> textColorValue
        }
        noteDateTime?.setTextColor(timeStampColor)
        noteSourceText?.setTextColor(ContextCompat.getColor(context, themeOverride.metadataColor))
    }

    private fun setContextLayout(noteContext: NoteContext) {
        val contextComponent: NoteContextComponent? = findViewById(R.id.noteContext)
        contextComponent?.showNoteContext(noteContext = noteContext, themeOverride = themeOverride)
    }

    private fun setPinnedLayout(isPinned: Boolean) {
        val pinnedNoteIcon: ImageView? = findViewById(R.id.pinnedNoteIcon)
        val pinnedNoteText: TextView? = findViewById(R.id.pinnedNoteText)

        if (NotesLibrary.getInstance().experimentFeatureFlags.pinnedNotesEnabled && isPinned) {
            pinnedNoteIcon?.visibility = View.VISIBLE
            pinnedNoteText?.visibility = View.VISIBLE
            pinnedNoteIcon?.setColorFilter(if (themeOverride != null) ContextCompat.getColor(context, R.color.pinned_note_text_color_dark) else ContextCompat.getColor(context, R.color.sn_metadata_color_charcoal))
            pinnedNoteText?.setTextColor(if (themeOverride != null) ContextCompat.getColor(context, R.color.pinned_note_text_color_dark) else ContextCompat.getColor(context, R.color.sn_metadata_color_charcoal))
        } else {
            pinnedNoteIcon?.visibility = View.GONE
            pinnedNoteText?.visibility = View.GONE
        }
    }

    open fun prepareSharedElements(markSharedElement: (View, String) -> Unit) {
        markSharedElement(this, "card")
        markSharedElement(noteContentLayout, "linearLayout")
    }

    open fun clearTransitionNames() {
        ViewCompat.setTransitionName(this, "")
        ViewCompat.setTransitionName(noteContentLayout, "")
    }

    fun setRootTransitionName(name: String) {
        clearTransitionNames()
        ViewCompat.setTransitionName(this, name)
    }

    open class Callbacks {
        open fun onNoteItemClicked(note: Note) {}
        open fun onNoteItemLongPress(note: Note, view: View) {}
    }

    companion object {
        private const val MAXIMUM_TEXT_PREVIEW_LINES = 10
        private const val MINIMUM_TEXT_PREVIEW_LINES = 5
    }
}
