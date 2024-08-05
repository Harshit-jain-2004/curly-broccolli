package com.microsoft.notes.richtext.editor

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.text.Editable
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.ArrowKeyMovementMethod
import android.text.method.MovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TtsSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.text.util.Linkify
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BaseTarget
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.transition.Transition
import com.microsoft.notes.models.Note
import com.microsoft.notes.models.extensions.justMedia
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.extensions.increaseRevision
import com.microsoft.notes.richtext.editor.extensions.scaleToWidth
import com.microsoft.notes.richtext.editor.operations.changeFormatting
import com.microsoft.notes.richtext.editor.operations.changeParagraphStyle
import com.microsoft.notes.richtext.editor.operations.removeEmptySpans
import com.microsoft.notes.richtext.editor.operations.replaceRange
import com.microsoft.notes.richtext.editor.operations.updateMediaWithAltText
import com.microsoft.notes.richtext.editor.styled.NoteStyledView
import com.microsoft.notes.richtext.editor.utils.NotesEditTextUndoRedoManager
import com.microsoft.notes.richtext.editor.utils.NotesEditTextUndoRedoStackChangeCallback
import com.microsoft.notes.richtext.render.ImageSpanWithMedia
import com.microsoft.notes.richtext.render.NotesBulletSpan
import com.microsoft.notes.richtext.render.PendingImageSpan
import com.microsoft.notes.richtext.render.PlaceholderImageSpan
import com.microsoft.notes.richtext.render.blocksSelected
import com.microsoft.notes.richtext.render.toGlobalRange
import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Range
import com.microsoft.notes.richtext.scheme.asParagraph
import com.microsoft.notes.richtext.scheme.isEmpty
import com.microsoft.notes.richtext.scheme.isParagraph
import com.microsoft.notes.threeWayMerge.canThreeWayMerge
import com.microsoft.notes.threeWayMerge.noteWithNewType
import com.microsoft.notes.threeWayMerge.threeWayMerge
import com.microsoft.notes.ui.extensions.asTextSpannable
import com.microsoft.notes.utils.logging.EventMarkers
import com.microsoft.notes.utils.logging.HostTelemetryKeys
import com.microsoft.notes.utils.logging.NoteContentActionType
import com.microsoft.notes.utils.logging.NotesSDKTelemetryKeys
import java.lang.ref.WeakReference

interface NotesEditTextCallback {
    fun updateDocument(document: Document, uiRevision: Long)
    fun updateRange(range: Range)
    fun openMediaInFullScreen(mediaLocalUrl: String, mediaMimeType: String)
    fun recordTelemetryEvent(eventMarker: EventMarkers, vararg keyValuePairs: Pair<String, String>)
    fun recordNoteContentUpdated()
    fun getCustomInputConnectionForNotesEditText(editorInfo: EditorInfo): InputConnection?
}

interface RibbonUpdateCallback {
    fun updateFormatState(
        isBoldEnabled: Boolean,
        isItalicsEnabled: Boolean,
        isUnderlineEnabled: Boolean,
        isStrikeThroughEnabled: Boolean,
        isBulletedListEnabled: Boolean
    )
    fun setEditMode(isEditMode: Boolean): Boolean
    fun isInEditMode(): Boolean
}

interface EditTextFocusCallback {
    fun focusBeforeEditText()
    fun focusAfterEditText()
}

@Suppress("TooManyFunctions", "LargeClass")
class NotesEditText(context: Context, attrs: AttributeSet?) :
    AppCompatEditText(context, attrs),
    NotesEditTextUndoRedoStackChangeCallback,
    InlineMediaContextMenuCallbacks {

    /*
     * Default implementation of ArrowKeyMovementMethod set cursor postion to 0th index on initialize() inside
     * setText() call. We require to retain selection after calling setText, hence another setSelection call is
     * triggered. This leads to two setSelection calls and flicker. This custom method initializes setSelection
     * directly on required index and save one setSelection call. This resolves flicker.
     */
    private class CustomArrowKeyMovementMethod(var cursorIndex: Int) : ArrowKeyMovementMethod() {
        override fun initialize(widget: TextView?, text: Spannable?) {
            Log.e("text>>>",text.toString())
            if (cursorIndex <= text?.length ?: 0) {
                Selection.setSelection(text, cursorIndex)
            }
        }
    }

    companion object {
        const val UNINITIALIZED_REV_VAL = -1L
    }

    private var notesEditTextCallback: WeakReference<NotesEditTextCallback>? = null
    private var editorState: EditorState = EditorState()
    private var ignoreTextChangeCount = 0
    private var currentViewWidth = 0
    private var previousDocuments = hashSetOf<List<Block>>()
    private var bitmapCache = mutableMapOf<InlineMedia, Bitmap>()
    private var pendingSpanBuilder: SpannableStringBuilder? = null
    private var imm: InputMethodManager? = null
    private val notesEditTextAccessibilityHelper = NotesEditTextAccessibilityHelper(this, context)
    private val inlineMediaContextMenuManager = InlineMediaContextMenuManager(this, context)
    private val notesEditTextUndoRedoManager = NotesEditTextUndoRedoManager().apply { stackChangeCallback = this@NotesEditText }
    private var scrollingView: NestedScrollView? = null
    private var revision: Long = UNINITIALIZED_REV_VAL
    private lateinit var movementMethod: CustomArrowKeyMovementMethod
    private var ic: InputConnection? = null
    var ribbonCallback: RibbonUpdateCallback? = null
    var focusCallback: EditTextFocusCallback? = null
    var onUndoChanged: ((isEnabled: Boolean) -> Unit)? = null
    var onRedoChanged: ((isEnabled: Boolean) -> Unit)? = null

    var inkColor: Int? = null
        set(value) {
            if (!editorState.document.isRenderedInkDocument) {
                return
            }
            // only repaint if value changed
            if (field != value) {
                field = value
                reloadDocument(keepSelection = true, showSoftInput = false)
            }
        }
    var hasMedia: Boolean = false

    private data class SearchQueryOccurrences(
        var query: String,
        var selectedIndex: Int,
        var matches: List<Int>
    )

    private var searchOccurrences: SearchQueryOccurrences = SearchQueryOccurrences("", -1, emptyList())

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
            if (notesEditTextAccessibilityHelper.isAccessibilityEnabled()) {
                notesEditTextAccessibilityHelper.handleClick()
                return true
            }

            getClickedUrlSpan(motionEvent)?.let {
                onUrlSpanClick(it)
                return true
            }

            val offset = getOffsetForPosition(motionEvent.x, motionEvent.y)

            if (offset > 0) {
                val imageBefore = text?.getSpans(offset - 1, offset, ImageSpanWithMedia::class.java)
                if (imageBefore != null && imageBefore.isNotEmpty() &&
                    getBoundsFromOffset(offset - 1).contains(motionEvent.x, motionEvent.y)
                ) {
                    val media = imageBefore.first().media
                    media.localUrl?.let {
                        showMediaInFullscreen(it, media.mimeType)
                        return true
                    }
                }
            }

            val safeText = text
            if (safeText != null && offset < safeText.length) {
                val imageAfter = safeText.getSpans(offset, offset + 1, ImageSpanWithMedia::class.java)
                if (imageAfter.isNotEmpty() && getBoundsFromOffset(offset).contains(motionEvent.x, motionEvent.y)) {
                    val media = imageAfter.first().media
                    media.localUrl?.let {
                        showMediaInFullscreen(it, media.mimeType)
                        return true
                    }
                }
            }
            return false
        }

        override fun onLongPress(e: MotionEvent) {
            if (notesEditTextAccessibilityHelper.isAccessibilityEnabled()) {
                notesEditTextAccessibilityHelper.handleLongPress()
            }

            if (!editorState.readOnlyMode) {
                ribbonCallback?.setEditMode(isEditMode = true)
            }
        }
    }

    private val gestureDetector: GestureDetectorCompat = GestureDetectorCompat(context, gestureListener)

    constructor(context: Context) : this(context, null)

    init {
        ViewCompat.setAccessibilityDelegate(this, notesEditTextAccessibilityHelper)
        setupTextWatcher()

        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (currentViewWidth != width) {
                    currentViewWidth = width
                    setDocument()
                    viewTreeObserver.removeOnGlobalLayoutListener(this)

                    // Add the initial state to undo stack
                    notesEditTextUndoRedoManager.initialize(editorState)
                }
            }
        })

        imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        this.customSelectionActionModeCallback = inlineMediaContextMenuManager.actionModeCallback
        isSaveEnabled = false
    }

    /**
     * When active note changes, we clear the cached data of the old note
     */
    fun onBeforeActiveNoteChanged() {
        editorState = EditorState()
        ignoreTextChangeCount = 0
        previousDocuments = hashSetOf()
        bitmapCache = mutableMapOf()
        pendingSpanBuilder = null
        revision = UNINITIALIZED_REV_VAL
        isEnabled = true
        notesEditTextUndoRedoManager.clear()
        setDocument()
    }

    fun addLinks() {
        text?.let { editable ->
            Linkify.addLinks(editable, Linkify.ALL)
            executeForSpans(URLSpan::class.java) {
                val ttsSpan = TtsSpan.TextBuilder().setText(
                    context.getString(
                        R.string.sn_notes_link_label,
                        editable.subSequence(editable.getSpanStart(it), editable.getSpanEnd(it))
                    )
                ).build()
                editable.setSpan(
                    ttsSpan, editable.getSpanStart(it), editable.getSpanEnd(it),
                    Spannable.SPAN_COMPOSING
                )
            }
        }
    }

    fun removeLinks() {
        executeForSpans(URLSpan::class.java) {
            text?.removeSpan(it)
        }
        executeForSpans(TtsSpan::class.java) {
            if (it.args.getString(TtsSpan.ARG_TEXT)?.startsWith(
                    context.getString(R.string.sn_notes_link_label, "")
                ) ?: false
            ) {
                text?.removeSpan(it)
            }
        }
    }

    /*
     * Layout provides getOffsetForHorizontal for exactly same functionality but it has some issues.
     * It returns nearest valid offset from touch area which may not be in touch area. For URL clicking this leads
     * to invalid clicks when user clicks on an empty space in the same line after URL. This method establishes
     * first whether touch is inside line bounds and then uses getOffsetForHorizontal to find exact offset.
     */
    private fun getValidOffsetFromCoordinates(event: MotionEvent): Int {
        var touchX = event.x
        var touchY = event.y

        // Ignore padding.
        touchX -= totalPaddingLeft
        touchY -= totalPaddingTop

        // Account for scrollable text.
        touchX += scrollX
        touchY += scrollY

        val touchedLine = layout.getLineForVertical(touchY.toInt())
        val touchAreaRectF = RectF(
            layout.getLineLeft(touchedLine), layout.getLineTop(touchedLine).toFloat(),
            layout.getLineWidth(touchedLine) + layout.getLineLeft(touchedLine),
            layout.getLineBottom(touchedLine).toFloat()
        )

        return if (touchAreaRectF.contains(touchX, touchY)) {
            layout.getOffsetForHorizontal(touchedLine, touchX)
        } else {
            -1
        }
    }

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection? {
        ic = notesEditTextCallback?.get()?.getCustomInputConnectionForNotesEditText(editorInfo)
            ?: super.onCreateInputConnection(editorInfo)
        return ic
    }

    fun insertTextToCurrentNote(text: String) {
        this.ic?.commitText(text, 1)
    }

    private fun getClickedUrlSpan(motionEvent: MotionEvent?): URLSpan? =
        motionEvent?.let {
            val offset = getValidOffsetFromCoordinates(motionEvent)
            if (offset >= 0) {
                val spans = text?.getSpans(offset, offset, URLSpan::class.java)
                if (spans != null && spans.isNotEmpty()) spans[0] else null
            } else {
                null
            }
        }

    private fun getURLType(urlSpan: URLSpan): String = Uri.parse(urlSpan.url)?.scheme ?: "unknown"

    private fun onUrlSpanClick(urlSpan: URLSpan) {
        urlSpan.onClick(this)
        notesEditTextCallback?.get()?.recordTelemetryEvent(
            EventMarkers.InContentHyperlinkClicked,
            Pair(HostTelemetryKeys.HYPER_LINK_TYPE, getURLType(urlSpan))
        )
    }

    fun setNotesEditTextViewCallback(notesEditTextCallback: NotesEditTextCallback) {
        this.notesEditTextCallback = WeakReference(notesEditTextCallback)
    }

    fun setDocument(
        document: Document? = null,
        loadImagesDelayed: Boolean = false,
        keepSelection: Boolean = true,
        showSoftInput: Boolean = false
    ) {
        if (document != null) {
            editorState = reduceEditorState(document, currentEditorState = editorState)
            isEnabled = !editorState.readOnlyMode
            if (editorState.readOnlyMode) {
                ribbonCallback?.setEditMode(isEditMode = !editorState.readOnlyMode)
            }
        }

        when (loadImagesDelayed) {
            true -> {
                setText(getSpannableForNoteDetails(), BufferType.SPANNABLE, keepSelection, showSoftInput)
                Handler(context.mainLooper).post { loadPendingImages() }
            }
            false -> {
                reloadDocument(keepSelection, showSoftInput)
            }
        }

        sendRibbonUpdate()
        validate(validatorEnabled = false)

        notesEditTextAccessibilityHelper.invalidateVirtualTreeOnUpdate()
    }

    private fun getSpannableForNoteDetails(): SpannableStringBuilder {
        if (editorState.document.isSamsungNoteDocument) {
            return editorState.spannedText as SpannableStringBuilder
        }
        val spanBuilder = getCurrentSpans(context)
        val spans = spanBuilder.getSpans(0, spanBuilder.length, StyleSpan::class.java).toSet() +
            spanBuilder.getSpans(0, spanBuilder.length, UnderlineSpan::class.java) +
            spanBuilder.getSpans(0, spanBuilder.length, StrikethroughSpan::class.java) +
            spanBuilder.getSpans(0, spanBuilder.length, ImageSpan::class.java) +
            spanBuilder.getSpans(0, spanBuilder.length, NotesBulletSpan::class.java)
        editorState = editorState.trackSpans(spans)
        return spanBuilder
    }

    private fun removeExistingSpans() {
        // Remove all existing spans unless they are applied for composing text (currently editing word)
        removeCharacterSpans(type = StyleSpan::class.java)
        removeCharacterSpans(type = UnderlineSpan::class.java)
        removeCharacterSpans(type = StrikethroughSpan::class.java)
        text?.let {
            val existingBulletSpans = it.getSpans(0, it.length, NotesBulletSpan::class.java)
            for (existingBulletSpan in existingBulletSpans) {
                it.removeSpan(existingBulletSpan)
            }
        }
    }

    private fun <T> removeCharacterSpans(type: Class<T>) {
        text?.let {
            val existingSpans = it.getSpans(0, it.length, type)
            for (existingSpan in existingSpans) {
                if ((it.getSpanFlags(existingSpan) and Spannable.SPAN_COMPOSING) == 0) {
                    it.removeSpan(existingSpan)
                }
            }
        }
    }

    private fun getCurrentSpans(context: Context) = editorState.document.asTextSpannable(context)

    private fun applyFormattingSpans(spanBuilder: SpannableStringBuilder) {
        for (
            span in spanBuilder.getSpans(0, spanBuilder.length, StyleSpan::class.java).toSet() +
                spanBuilder.getSpans(0, spanBuilder.length, UnderlineSpan::class.java) +
                spanBuilder.getSpans(0, spanBuilder.length, StrikethroughSpan::class.java) +
                spanBuilder.getSpans(0, spanBuilder.length, NotesBulletSpan::class.java)
        ) {
            val start = spanBuilder.getSpanStart(span)
            val end = spanBuilder.getSpanEnd(span)
            text?.setSpan(
                span, start, end,
                if (span is NotesBulletSpan) Spannable.SPAN_INCLUSIVE_EXCLUSIVE else Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun forceApplyFormattingSpans(spans: SpannableStringBuilder? = null) {
        removeExistingSpans()
        applyFormattingSpans(spans ?: getCurrentSpans(context))
    }

    private fun loadPendingImages(spanBuilder: SpannableStringBuilder? = null) {
        populatePlaceholderImageSpans(spanBuilder)
        val builder = spanBuilder ?: text as SpannableStringBuilder
        val placeholderImages = builder.getSpans(0, builder.length, PlaceholderImageSpan::class.java)
        placeholderImages.forEach {
            val bitmap = bitmapCache[it.media]
            if (bitmap != null) {
                imageLoaded(it, bitmap)
            } else if (it.media.localUrl != null) {
                val opts = RequestOptions()
                    .fitCenter()
                Glide.with(context)
                    .asBitmap()
                    .apply(opts)
                    .load(it.media.localUrl)
                    .into(PlaceholderImageSpanTarget(it))
            }
        }
    }

    private fun setupTextWatcher() {
        this.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(editable: Editable) {
                if (editorState.needsRender) {
                    if (editorState.renderFlags has RenderFlags.DELAY_RENDER) {
                        delayedRender()
                    } else {
                        renderIfNecessary()
                    }
                }
                post { forceApplyFormattingSpans() }
            }

            private fun delayedRender() {
                post { renderIfNecessary() }
            }

            private fun renderIfNecessary() {
                if (!editorState.needsRender) {
                    return
                }

                val shouldSetSelection = editorState.renderFlags has RenderFlags.SET_SELECTION
                val newSelection = when {
                    shouldSetSelection -> editorState.document.range.toGlobalRange(editorState.document)
                    else -> null
                }

                editorState = editorState.renderCompleted()
                setDocument(keepSelection = !shouldSetSelection)
                if (newSelection != null) {
                    setSelection(newSelection.startOffset, newSelection.endOffset)
                }
            }

            override fun beforeTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
                if (ignoreTextChangeCount == 0 && isTextChangeAfterLastEop(text, start, count)) {
                    editorState = editorState.forceRender()
                }
            }

            private fun isTextChangeAfterLastEop(text: CharSequence, start: Int, count: Int): Boolean =
                start > text.length - 1 || start + count > text.length - 1

            override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
                if (ignoreTextChangeCount == 0) {
                    handleTextChange(text, start, before, count)
                }
            }
        })
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (super.onKeyUp(keyCode, event)) {
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_DEL) {
            with(editorState) {
                if (isDeletingBulletAtBeginning()) {
                    setParagraphFormatting(ParagraphFormattingProperty.BULLETS, false)
                    return true
                }
            }
        }

        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            with(editorState) {
                val prevBlock = with(document) { if (range.startBlock > 0) blocks[range.startBlock - 1] else null }
                if (prevBlock != null && prevBlock.isEmptyBulletParagraph()) {
                    setParagraphFormatting(prevBlock.asParagraph(), ParagraphFormattingProperty.BULLETS, false)
                    setParagraphFormatting(ParagraphFormattingProperty.BULLETS, false)
                    return true
                }
            }
        }
        return false
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        if (notesEditTextAccessibilityHelper.isAccessibilityEnabled()) {
            val eventHandled: Boolean = notesEditTextAccessibilityHelper.handleHoverEvent(event)
            if (!eventHandled) {
                sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
            }
            return eventHandled
        }
        return super.dispatchHoverEvent(event)
    }

    private fun EditorState.isDeletingBulletAtBeginning(): Boolean =
        with(document) {
            range.isCollapsed &&
                range.startBlock == 0 &&
                range.startOffset == 0 &&
                blocks[0].isParagraph() &&
                blocks[0].asParagraph().style.unorderedList
        }

    private fun Block.isEmptyBulletParagraph(): Boolean =
        isParagraph() && asParagraph().style.unorderedList && isEmpty()

    fun showMediaInFullscreen(mediaLocalUrl: String, mediaMimeType: String) {
        if (!editorState.document.isRenderedInkDocument) {
            notesEditTextCallback?.get()?.openMediaInFullScreen(mediaLocalUrl, mediaMimeType)
        }
    }

    fun toggleBold() {
        setFormatting(FormattingProperty.BOLD, !editorState.getFormatting().bold)
    }

    fun toggleItalic() {
        setFormatting(FormattingProperty.ITALIC, !editorState.getFormatting().italic)
    }

    fun toggleUnderline() {
        setFormatting(FormattingProperty.UNDERLINE, !editorState.getFormatting().underline)
    }

    fun toggleStrikethrough() {
        setFormatting(FormattingProperty.STRIKETHROUGH, !editorState.getFormatting().strikethrough)
    }

    fun undo() {
        notesEditTextUndoRedoManager.undo()?.let { restoreEditorState(it) }
    }

    fun redo() {
        notesEditTextUndoRedoManager.redo()?.let { restoreEditorState(it) }
    }

    private fun restoreEditorState(oldEditorState: EditorState) {
        editorState = oldEditorState
        setDocument()
        onDocumentUpdated()
    }

    private fun setFormatting(formattingProperty: FormattingProperty, value: Boolean) {
        editorState = editorState.changeFormatting(formattingProperty, value)
        forceApplyFormattingSpans(getSpannableForNoteDetails())
        sendRibbonUpdate()
        onDocumentUpdated()
        notesEditTextUndoRedoManager.addUndoState(editorState)
    }

    fun toggleUnorderedList() {
        setParagraphFormatting(
            ParagraphFormattingProperty.BULLETS,
            !editorState.getParagraphFormatting().unorderedList
        )
    }

    private fun setParagraphFormatting(paragraph: Paragraph, property: ParagraphFormattingProperty, value: Boolean) {
        editorState = editorState.changeParagraphStyle(paragraph, property, value)
        setDocument()
        onDocumentUpdated()
        notesEditTextUndoRedoManager.addUndoState(editorState)
    }

    private fun setParagraphFormatting(property: ParagraphFormattingProperty, value: Boolean) {
        editorState = editorState.changeParagraphStyle(property, value)
        setDocument()
        onDocumentUpdated()
        notesEditTextUndoRedoManager.addUndoState(editorState)
    }

    private fun handleTextChange(text: CharSequence, start: Int, before: Int, count: Int) {
        val range = editorState.document.blocksSelected(start, start + before)
        val newText = text.substring(start, start + count)

        recordUpdateTextTelemetry(editorState.document, newText)

        editorState = editorState.updateRange(range).replaceRange(newText)
        validate(validatorEnabled = false)
        onDocumentUpdated()

        val isSignificantChange = NotesEditTextUndoRedoManager.isSignificantTextChange(newText, count)
        notesEditTextUndoRedoManager.addUndoState(editorState, isSignificantChange = isSignificantChange)
    }

    private fun recordUpdateTextTelemetry(oldDocument: Document, newText: String) {
        notesEditTextCallback?.get()?.recordNoteContentUpdated()
        if (oldDocument.isEmpty() && newText.isNotEmpty() && !hasMedia) {
            notesEditTextCallback?.get()?.recordTelemetryEvent(
                EventMarkers.NoteContentActionTaken,
                Pair(NotesSDKTelemetryKeys.NoteProperty.ACTION, NoteContentActionType.TEXT_ADDED_TO_EMPTY_NOTE)
            )
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val noteBodyEditText = findViewById<TextView>(R.id.noteBodyEditText)
            noteBodyEditText.highlightColor = ContextCompat.getColor(context, R.color.transparent_color)
        }
        super.onSelectionChanged(selStart, selEnd)

        if (clampSelectionToValidRange(selStart, selEnd)) {
            return
        }
        if (editorState != null) { // This CAN happen, because the android lifecycle this function is used
            // BEFORE the view is Instantiated ¯\_(ツ)_/¯
            val range = editorState.document.blocksSelected(selStart, selEnd)
            if (range != editorState.document.range) {
                editorState = editorState.updateRange(range)
                onRangeChanged()
            }
        }
        sendRibbonUpdate()
    }

    fun removeEmptySpans() {
        if (editorState != null) {
            editorState = editorState.removeEmptySpans()
        }
    }

    private fun sendRibbonUpdate() {
        editorState?.let {
            val currentSelectionIsBulleted = it.getParagraphFormatting().unorderedList
            with(it.getFormatting()) {
                ribbonCallback?.updateFormatState(bold, italic, underline, strikethrough, currentSelectionIsBulleted)
            }
        }
    }

    private fun clampSelectionToValidRange(selStart: Int, selEnd: Int): Boolean {
        val safeText = text
        if (safeText != null) {
            val length = safeText.length
            if (isSelectionAfterLastEop(length, selStart, selEnd)) {
                setSelection(minOf(selStart, length - 1), minOf(selEnd, length - 1))
                return true
            }
        }
        return false
    }

    private fun isSelectionAfterLastEop(length: Int, start: Int, end: Int): Boolean =
        length > 0 && (start >= length || end >= length)

    private fun setText(text: CharSequence, type: BufferType, keepSelection: Boolean, showSoftInput: Boolean) {
        val selEndOffsetFromEnd = length() - selectionEnd
        val selEnd = if (selEndOffsetFromEnd > text.length) 0 else text.length - selEndOffsetFromEnd
        val selStart = Math.min(selectionStart, selEnd)

        movementMethod.cursorIndex = if (keepSelection) selEnd else 0

        setText(text, type)
        if (keepSelection) {
            // setText removes the current selection
            setSelection(selStart, selEnd)
        }

        if (showSoftInput) {
            imm?.showSoftInput(this, 0)
        }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        Log.e("setText>>>",text.toString())
        ignoreTextChangeCount++
        super.setText(text, type)

        // When we will not be able to determine mode of canvas we will not show links.
        val editMode = ribbonCallback?.isInEditMode() ?: true
        if (!editMode) {
            addLinks()
        }
        ignoreTextChangeCount--
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        NoteStyledView.isEdit=true
        gestureDetector.onTouchEvent(event)

        // Below code is adapted from TextView.java :: onTouchEvent and shows the VKB after any gesture on the view
        val action = event?.actionMasked
        val touchIsFinished = action == MotionEvent.ACTION_UP
        val readOnlyMode = editorState?.readOnlyMode

        if (!readOnlyMode && (movementMethod != null || onCheckIsTextEditor()) && isEnabled &&
            text is Spannable && layout != null
        ) {
            if (touchIsFinished && (isTextEditable() || isTextSelectable)) {
                if (getClickedUrlSpan(event) == null && isTextEditable()) {
                    ribbonCallback?.setEditMode(isEditMode = true)
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun dispatchKeyEventPreIme(event: KeyEvent?): Boolean {
        val editMode = ribbonCallback?.isInEditMode()
        val readOnlyMode = editorState?.readOnlyMode ?: false

        if ((event?.keyCode == KeyEvent.KEYCODE_BACK || event?.keyCode == KeyEvent.KEYCODE_ESCAPE) && event?.action == KeyEvent.ACTION_UP) {
            if (editMode != null && editMode) {
                ribbonCallback?.setEditMode(isEditMode = false)
                imm?.hideSoftInputFromWindow(windowToken, 0)
                /* back key in edit mode handled
                 * as we have switched to view mode
                 * and hid the vkb/voice keyboard */
                return true
            }
        } else if (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
            if (!readOnlyMode && editMode != null && !editMode) {
                clickOnView(this)
                setSelection(text?.length ?: 0)
                val result = ribbonCallback?.setEditMode(isEditMode = true)

                return if (result != null && result) {
                    true
                } else {
                    super.dispatchKeyEventPreIme(event)
                }
            }
        }

        // Ignore typing keys in view mode
        if (isEditModeFalse() && (event != null && !isNavigationKey(event.keyCode))) {
            return true
        }
        return super.dispatchKeyEventPreIme(event)
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        // Ignore keys in view mode
        if (isEditModeFalse() && !isNavigationKey(keyCode)) {
            return true
        }

        return super.onKeyPreIme(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Ignore keys in view mode
        if (isEditModeFalse() && !isNavigationKey(keyCode)) {
            return true
        } else if (isEditModeFalse()) {
            if (handleArrowKeyNavigation(event)) return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun handleArrowKeyNavigation(event: KeyEvent?): Boolean {
        return event?.let { keyEvent ->
            focusCallback?.let {
                when {
                    isUpKey(keyEvent.keyCode) -> {
                        it.focusBeforeEditText()
                        true
                    }
                    isDownKey(keyEvent.keyCode) -> {
                        it.focusAfterEditText()
                        true
                    }
                    else -> false
                }
            } ?: false
        } ?: false
    }

    private fun isEditModeFalse(): Boolean {
        val editMode = ribbonCallback?.isInEditMode() ?: false
        return !editMode
    }

    private fun isNavigationKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> true
            else -> false
        }
    }

    private fun isUpKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> true
            else -> false
        }
    }

    private fun isDownKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> true
            else -> false
        }
    }

    private fun isTextEditable(): Boolean = text is Editable && onCheckIsTextEditor() && isEnabled

    private fun clickOnView(view: View?) {
        if (view == null) return
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val motionEvent = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis() + 50,
            MotionEvent.ACTION_UP,
            (location[0] + view.width) / 2.0f,
            (location[1] + view.height) / 2.0f,
            0
        )
        view.dispatchTouchEvent(motionEvent)
    }

    fun getBoundsFromOffset(offset: Int): RectF {
        text?.let {
            if (offset !in 0..it.length) {
                throw IllegalArgumentException("Offset $offset out of bounds (length ${it.length})")
            }
        }

        with(layout) {
            val line = getLineForOffset(offset)
            return RectF(
                getPrimaryHorizontal(offset) + compoundPaddingLeft,
                getLineTop(line).toFloat() + extendedPaddingTop,
                getPrimaryHorizontal(offset + 1) + compoundPaddingLeft,
                getLineBottom(line).toFloat() + extendedPaddingTop
            )
        }
    }

    fun getScrollView(): NestedScrollView? = scrollingView

    fun setScrollView(scrollView: NestedScrollView) {
        if (scrollingView != scrollView) {
            scrollingView = scrollView
        }
    }

    private fun validate(validatorEnabled: Boolean = true) {

        fun setBackgroundColorWithValidatorResult(success: Boolean) {
            when (success) {
                true -> setBackgroundColor(android.graphics.Color.TRANSPARENT)
                false -> {
                    val validatorFailColor = ContextCompat.getColor(context, R.color.sn_validator)
                    setBackgroundColor(validatorFailColor)
                }
            }
        }

        if (validatorEnabled) {
            val success = validate(text as SpannableStringBuilder, editorState)
            setBackgroundColorWithValidatorResult(success)
        }
    }

    private fun onDocumentUpdated() {
        Log.e("textEditText>>>",NoteStyledView.noteBodyEditText?.text.toString())
        if(NoteStyledView.sourceNote!=null)
        {
            saveTextTranscript(NoteStyledView.sourceNote!!,NoteStyledView.noteBodyEditText?.text.toString())

        }
        previousDocuments.add(editorState.document.blocks)
        notesEditTextCallback?.get()?.updateDocument(editorState.document, revision.increaseRevision())
    }
    private fun saveTextTranscript(note : Note,text: String){
        val sharedPreferences = context.getSharedPreferences("MyNotesSharedPref", MODE_PRIVATE)
        val myEdit = sharedPreferences.edit()
        myEdit.putString("textTranscript_${note.localId}", text);
        myEdit.commit();
    }

    private fun onRangeChanged() {
        previousDocuments.add(editorState.document.blocks)
        removeEmptySpans()
        notesEditTextCallback?.get()?.updateRange(editorState.document.range)
    }

    fun setNoteContent(updatedNote: Note) {
        if (updatedNote.isInkNote || updatedNote.document.isSamsungNoteDocument) {
            setDocumentWithDelayedImages(updatedNote.document)
            return
        }

        if (revision == UNINITIALIZED_REV_VAL) {
            // Initialize editor revision version to store revision on opening note because we are not resetting it
            // in store.
            revision = updatedNote.uiRevision
        }
        if (previousDocuments.isEmpty()) {
            setDocumentWithDelayedImages(updatedNote.document)
            return
        }

        if (updatedNote.uiRevision != revision) {
            val baseNote = updatedNote.uiShadow ?: return
            val currentNote = baseNote.copy(document = editorState.document)
            val nextNote = baseNote.copy(document = updatedNote.document)
            val mergedNote = if (canThreeWayMerge(
                    baseNote, currentNote,
                    nextNote
                )
            ) {
                threeWayMerge(baseNote, currentNote, nextNote)
            } else {
                noteWithNewType(baseNote, currentNote, nextNote)
            }
            setDocumentWithDelayedImages(mergedNote.document)
        } else {
            setDocumentWithDelayedImages(updatedNote.document)
        }
    }

    private fun setDocumentWithDelayedImages(document: Document) {
        if (document.isInkDocument || document.isSamsungNoteDocument) {
            setDocument(document)
            return
        }

        val updatedBlocks = document.blocks.minus(editorState.document.blocks).toMutableList()

        if (updatedBlocks.isEmpty()) {
            if (document.range != editorState.document.range) {
                editorState = editorState.updateRange(document.range)
            }
            previousDocuments.clear()
            previousDocuments.add(document.blocks)
            return
        }

        removeUnusefulMediaUpdates(editorState.document, updatedBlocks)

        if (updatedBlocks.isEmpty()) {
            previousDocuments.clear()
            previousDocuments.add(document.blocks)
            return
        }

        if (!previousDocuments.contains(document.blocks)) {
            setDocument(document)
        }
    }

    private fun removeUnusefulMediaUpdates(editorDocument: Document, updatedBlocks: MutableList<Block>) {
        val mediaUpdates = updatedBlocks.filterIsInstance<InlineMedia>().toMutableList()
        if (!mediaUpdates.isEmpty()) {
            val mediaFromEditorDocument = editorDocument.justMedia()
            updatedBlocks.removeAll(
                mediaUpdates.filter { mediaUpdate ->
                    mediaFromEditorDocument.find {
                        it.localId == mediaUpdate.localId &&
                            (it.localUrl != null || mediaUpdate.localUrl == null)
                    } != null
                }
            )
        }
    }

    /**
     * Replace the zero size Pending Image spans with non-zero size placeholder
     * image spans so that Placeholder image is visible while image is loading
     */
    private fun populatePlaceholderImageSpans(spanBuilder: SpannableStringBuilder? = null) {
        var placeHolderBitmap = BitmapFactory.decodeResource(
            resources,
            R.drawable.sn_notes_canvas_image_placeholder
        )
        placeHolderBitmap = placeHolderBitmap.scaleToWidth(
            requiredWidth = currentViewWidth - paddingLeft - paddingRight
        )
        val bitmapDrawable = BitmapDrawable(resources, placeHolderBitmap)
        bitmapDrawable.setBounds(0, 0, placeHolderBitmap.width, placeHolderBitmap.height)

        val builder = spanBuilder ?: text as SpannableStringBuilder
        val pendingImageSpans = builder.getSpans(0, builder.length, PendingImageSpan::class.java)
        pendingImageSpans.forEach {
            val start = builder.getSpanStart(it)
            val end = builder.getSpanEnd(it)
            val placeHolderImageSpan = PlaceholderImageSpan(it.media, bitmapDrawable)
            if (start >= 0 && end >= 0) {
                builder.setSpan(placeHolderImageSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                editorState = editorState.trackSpans(editorState.trackedSpans + placeHolderImageSpan - it)
            }
            builder.removeSpan(it)
        }
    }

    private fun imageLoaded(placeholderImageSpan: PlaceholderImageSpan, bitmap: Bitmap) {
        val finalBitmap = if (editorState.document.isRenderedInkDocument) tintBitmap(bitmap) else bitmap

        val currentBitmap = finalBitmap.scaleToWidth(requiredWidth = currentViewWidth - paddingLeft - paddingRight)
        bitmapCache[placeholderImageSpan.media] = currentBitmap
        val bitmapDrawable = BitmapDrawable(resources, currentBitmap)
        bitmapDrawable.setBounds(0, 0, currentBitmap.width, currentBitmap.height)

        val imageSpan = ImageSpanWithMedia(placeholderImageSpan.media, bitmapDrawable)
        val builder = pendingSpanBuilder ?: SpannableStringBuilder(text)
        val start = builder.getSpanStart(placeholderImageSpan)
        val end = builder.getSpanEnd(placeholderImageSpan)

        if (start >= 0 && end >= 0) {
            builder.setSpan(imageSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            editorState = editorState.trackSpans(editorState.trackedSpans + imageSpan - placeholderImageSpan)
        }
        builder.removeSpan(placeholderImageSpan)

        when {
            pendingSpanBuilder != null -> pendingSpanBuilder = builder
            else -> setText(builder, BufferType.SPANNABLE, true, false)
        }
    }

    private fun tintBitmap(bitmap: Bitmap): Bitmap =
        inkColor?.let {
            val paint = Paint()
            paint.colorFilter = PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN)
            val bitmapResult = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmapResult)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            bitmapResult
        } ?: bitmap

    private fun reloadDocument(keepSelection: Boolean, showSoftInput: Boolean) {
        val spannableForNoteDetails = getSpannableForNoteDetails()
        pendingSpanBuilder = spannableForNoteDetails
        loadPendingImages(spannableForNoteDetails)
        setText(spannableForNoteDetails, BufferType.SPANNABLE, keepSelection, showSoftInput)
        pendingSpanBuilder = null
    }

    private fun <T> executeForSpans(spanType: Class<T>, block: (T) -> Unit) {
        text?.let {
            val spans = it.getSpans(0, it.length, spanType)
            spans.forEach { block(it) }
        }
    }

    inner class PlaceholderImageSpanTarget(private val placeholderImageSpan: PlaceholderImageSpan) :
        BaseTarget<Bitmap>() {

        override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
            imageLoaded(placeholderImageSpan, bitmap)
        }

        override fun getSize(cb: SizeReadyCallback) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            val maxWidth = width - paddingLeft - paddingRight
            if (placeholderImageSpan.media.localUrl != null) {
                val uri = Uri.parse(placeholderImageSpan.media.localUrl)
                BitmapFactory.decodeFile(uri.path, options)
                val aspectRatio = options.outHeight.toFloat() / options.outWidth.toFloat()
                cb.onSizeReady(maxWidth, (aspectRatio * maxWidth).toInt())
            } else {
                BitmapFactory.decodeResource(resources, R.drawable.sn_notes_canvas_image_placeholder, options)
                val aspectRatio = options.outHeight.toFloat() / options.outWidth.toFloat()
                cb.onSizeReady(maxWidth, (aspectRatio * maxWidth).toInt())
            }
        }
        override fun removeCallback(cb: SizeReadyCallback) {
        }
    }

    override fun getDefaultMovementMethod(): MovementMethod {
        movementMethod = CustomArrowKeyMovementMethod(cursorIndex = 0)
        return movementMethod
    }


    override fun updateMediaWithAltText(localId: String, altText: String) {
        editorState = editorState.updateMediaWithAltText(localId, altText)
        setDocument(showSoftInput = true)
        onDocumentUpdated()
        notesEditTextUndoRedoManager.addUndoState(editorState)
    }

    override fun isSelectionOnlyImage(): Boolean {
        val selectionStart = this.selectionStart
        val selectionEnd = this.selectionEnd

        if (selectionEnd - selectionStart == 1) {
            val images = getMediaListInCurrentSelection()
            if (images != null && images.isNotEmpty() && images.size == 1) {
                return true
            }
        }
        return false
    }

    override fun getMediaListInCurrentSelection(): List<ImageSpanWithMedia>? {
        val safeText = text
        return when {
            safeText != null -> (
                safeText.getSpans(
                    this.selectionStart,
                    this.selectionEnd,
                    ImageSpanWithMedia::class.java
                )
                ).asList()
            else -> null
        }
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (action == AccessibilityNodeInfoCompat.ACTION_CLICK) {
            ribbonCallback?.setEditMode(true)
        }
        return super.performAccessibilityAction(action, arguments)
    }

    override fun onUndoChanged(enabled: Boolean) {
        onUndoChanged?.invoke(enabled)
    }

    override fun onRedoChanged(enabled: Boolean) {
        onRedoChanged?.invoke(enabled)
    }

    fun canPerformUndo(): Boolean = notesEditTextUndoRedoManager.canPerformUndo()

    fun canPerformRedo(): Boolean = notesEditTextUndoRedoManager.canPerformRedo()

    fun searchInNote(query: String) {
        val content = text.toString()
        searchOccurrences.query = query
        searchOccurrences.matches = findAllOccurrences(content, query)
        if (searchOccurrences.matches.isNotEmpty()) {
            highlightOccurrences()
        } else {
            clearHighlights()
            Toast.makeText(context, R.string.sn_search_no_matches_found, Toast.LENGTH_SHORT).show()
        }
    }

    // Navigate to the next occurrence
    fun navigateSearchedOccurrence(previous: Boolean = false) {
        val matches = searchOccurrences.matches
        if (matches.isNotEmpty()) {
            val direction = if (previous) -1 else 1
            searchOccurrences.selectedIndex = (searchOccurrences.selectedIndex + direction + matches.size) % matches.size
            highlightOccurrences()
        }
    }

    // Function to find all occurrences of a query string in the content string
    private fun findAllOccurrences(content: String, query: String): List<Int> {
        if (query.isEmpty()) return emptyList()

        val occurrences = mutableListOf<Int>()

        try {
            var index = content.indexOf(query, 0)
            while (index >= 0) {
                occurrences.add(index)
                index = content.indexOf(query, index + query.length)
            }
        } finally {
            return occurrences
        }
    }

    private fun highlightOccurrences() {
        if (text is Spannable) {
            clearHighlights()
            val spannable = SpannableString(text)
            val queryLength = searchOccurrences.query.length
            val lightColor = ContextCompat.getColor(context, android.R.color.darker_gray)
            val darkColor = ContextCompat.getColor(context, android.R.color.background_dark)

            for ((index, start) in searchOccurrences.matches.withIndex()) {
                val color = if (index == searchOccurrences.selectedIndex) darkColor else lightColor
                spannable.setSpan(BackgroundColorSpan(color), start, start + queryLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            setText(spannable)
        }
    }

    private fun clearHighlights() {
        if (text is Spannable) {
            val spannable = SpannableString(text)
            val spans = spannable.getSpans(0, spannable.length, BackgroundColorSpan::class.java)
            spans.forEach { spannable.removeSpan(it) }
            setText(spannable)
        }
    }
}
