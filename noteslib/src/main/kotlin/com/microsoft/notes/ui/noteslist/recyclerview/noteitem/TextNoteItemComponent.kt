package com.microsoft.notes.ui.noteslist.recyclerview.noteitem

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.render.NotesBulletSpan
import com.microsoft.notes.richtext.scheme.asParagraph
import com.microsoft.notes.richtext.scheme.isParagraph
import com.microsoft.notes.ui.extensions.asPreviewSpannable
import com.microsoft.notes.ui.extensions.highlightKeywords
import com.microsoft.notes.ui.extensions.setPreviewContentAndVisibility
import java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT
import java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
import java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
import java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
import java.util.Locale
import kotlin.math.roundToInt

class TextNoteItemComponent(context: Context, attrs: AttributeSet) : NoteItemComponent(context, attrs) {
    companion object {
        private const val LOG_TAG = "TextNoteItemComponent"
    }

    var keywordsToHighlight: List<String>? = null
    var preDrawListener: ViewTreeObserver.OnPreDrawListener? = null
    var showTime = true

    override fun bindNote(
        note: Note,
        keywordsToHighlight: List<String>?,
        isSelectionEnabled: Boolean,
        isItemSelected: Boolean,
        showDateTime: Boolean,
        showSource: Boolean,
        showSourceText: Boolean,
        isFeedUiRefreshEnabled: Boolean
    ) {
        super.bindNote(
            note, keywordsToHighlight, isSelectionEnabled, isItemSelected, showDateTime,
            showSource, showSourceText, isFeedUiRefreshEnabled
        )
        val documentPreview = note.document.asPreviewSpannable(context)
        this@TextNoteItemComponent.showTime = showDateTime
        if (keywordsToHighlight != null) {
            documentPreview.highlightKeywords(context, keywordsToHighlight, note.color)
        }
        setupOnPreDrawListener()
        noteBody?.setPreviewContentAndVisibility(documentPreview)
    }

    private fun setupOnPreDrawListener() {
        if (preDrawListener != null) {
            // to avoid setting onPreDrawListener multiple times for a single view
            return
        }
        preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (noteBody?.layout != null) {
                    viewTreeObserver.removeOnPreDrawListener(this)
                    preDrawListener = null
                }
                sourceNote?.also {
                    adjustLayout(it, this@TextNoteItemComponent.keywordsToHighlight)
                }
                return true
            }
        }
        // TODO: we may adjust the timing to perform the calculation
        // the listener will be invoked each time when the draw operation is dispatched on the window
        // no matter this view is invalidated or not
        viewTreeObserver.addOnPreDrawListener(preDrawListener)
    }

    private fun removeOnPreDrawListener() {
        preDrawListener?.also {
            viewTreeObserver.removeOnPreDrawListener(it)
            preDrawListener = null
        }
    }

    override fun onRemovingFromParent() {
        super.onRemovingFromParent()
        // right before this view is about to be removed from the parent view
        // we may need to remove the OnPreDrawListener we set before
        // to avoid listener leakage
        removeOnPreDrawListener()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // set the onPreDraw listener when the view is attached to the window
        setupOnPreDrawListener()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // we don't need to do the onPreDraw calculation when this view is detached from the window
        // because this view would never be drawn until it is attached
        removeOnPreDrawListener()
    }

    private fun adjustLayout(note: Note, keywordsToHighlight: List<String>?) {
        val deviceRightToLeft = isDeviceRightToLeft()
        val textRightToLeft = isFirstLineRightToLeft(note)

        val documentPreview = note.document.asPreviewSpannable(context)
        if (keywordsToHighlight != null) {
            documentPreview.highlightKeywords(context, keywordsToHighlight, note.color)
        }
        if (showTime) {
            if (deviceRightToLeft != textRightToLeft) {
                // this adds a new line at the start without the formatting
                noteBody?.text = TextUtils.concat("\n", documentPreview)
            } else {
                splitFirstLine(note, documentPreview)
            }
        }
    }

    private fun splitFirstLine(note: Note, documentPreview: SpannableStringBuilder) {
        val start = noteBody?.layout?.getLineStart(0)
        val end = noteBody?.layout?.getLineEnd(0)
        if (start == null || end == null) {
            // could not get the line
            return
        }

        var firstLine: String = noteBody?.text?.subSequence(start, end)?.toString() ?: ""
        if (firstLine.isEmpty()) {
            // first line is empty, so splitting not needed
            return
        }

        // multiplying wanted width by 1.1 to give it a bit of extra padding
        var textWidth = noteBody?.paint?.measureText(firstLine)?.toInt() ?: 0
        var wantedWidth = (noteBody?.width ?: 0) - ((noteDateTime?.width ?: 0) * 1.1).roundToInt()

        val firstBlock = note.document.blocks.elementAtOrNull(0)
        if (firstBlock != null && firstBlock.isParagraph() && firstBlock.asParagraph().isBulleted()) {
            wantedWidth -= NotesBulletSpan.DEFAULT_LEADING_MARGIN
        }

        if (textWidth <= wantedWidth) {
            // line is already shorter than wanted width
            return
        }

        // find the characters to split out to ensure the first line first into wanted width
        // TODO https://github.com/microsoft-notes/notes-android-sdk/issues/858
        var splitCharacters = ""
        // Some Unicode characters (e.g., emojis) may take up more than one chars.
        // To avoid unexpected character breaks, we need to search by code points,
        // instead of chars to break the first line
        var codePointCount = firstLine.codePointCount(0, firstLine.length)
        while (codePointCount > 0 && textWidth > wantedWidth && firstLine.isNotEmpty()) {
            val characterToShift = firstLine.substring(firstLine.offsetByCodePoints(0, codePointCount - 1))
            splitCharacters = characterToShift + splitCharacters
            firstLine = firstLine.substring(0, firstLine.length - characterToShift.length)
            codePointCount--

            textWidth = noteBody?.paint?.measureText(firstLine)?.toInt() ?: 0
        }

        if (splitCharacters.trim().isEmpty()) {
            // split line only contains spaces
            return
        }

        // check whether the split happens in the middle of the word
        val wordSplit = !firstLine.endsWith(" ") && !splitCharacters.startsWith(" ")
        if (wordSplit) {
            // only make adjustments if there is more than one word in the first line,
            // otherwise it is a block of text, so split at character level rather than word level
            // TODO https://github.com/microsoft-notes/notes-android-sdk/issues/857
            if (firstLine.contains(" ")) {
                val charactersToShift = firstLine.substring(firstLine.lastIndexOf(" "))
                firstLine = firstLine.removeSuffix(charactersToShift)
            }
        }

        // if the split happens before space, next line will have an indent of one space
        // split after space character instead

        if (documentPreview.length > firstLine.length) {
            val splittingBeforeSpace = documentPreview[firstLine.length] == ' '
            noteBody?.text = documentPreview.insert(
                if (splittingBeforeSpace) firstLine.length + 1 else firstLine.length,
                "\n"
            )
        } else {
            Log.i(LOG_TAG, "The text note is smaller than text on UI, ignoring the split")
        }
    }

    private fun isDeviceRightToLeft(): Boolean =
        TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL

    private fun isFirstLineRightToLeft(note: Note): Boolean {
        val document = note.document

        // ensure note has at least one paragraph, should be always the case
        val paragraph = document.blocks.firstOrNull()?.asParagraph() ?: return false

        // check paragraph flag
        if (paragraph.isRightToLeft()) {
            return true
        }

        // if paragraph is not right to left, do a text check, as model is wrong in some cases
        val paragraphText = paragraph.content.text

        // empty paragraph - assume left to right, no action needed
        if (paragraphText.isEmpty()) {
            return false
        }

        // if first char is right to left, format text based on this
        val firstChar = paragraphText[0]
        return when (Character.getDirectionality(firstChar)) {
            DIRECTIONALITY_RIGHT_TO_LEFT, DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING, DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE -> true
            else -> false
        }
    }

    override fun prepareSharedElements(markSharedElement: (View, String) -> Unit) {
        super.prepareSharedElements(markSharedElement)
        markSharedElement(noteBody as View, "body")
    }

    override fun clearTransitionNames() {
        super.clearTransitionNames()
        ViewCompat.setTransitionName(noteBody as View, "")
    }
}
