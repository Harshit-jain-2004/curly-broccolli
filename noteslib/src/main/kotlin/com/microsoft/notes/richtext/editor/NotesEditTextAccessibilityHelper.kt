package com.microsoft.notes.richtext.editor

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.render.ImageSpanWithMedia
import java.util.Arrays
import java.util.Comparator

class NotesEditTextAccessibilityHelper(private val notesEditText: NotesEditText, val context: Context) :
    ExploreByTouchHelper(notesEditText) {

    companion object {
        private const val DEFAULT_INVALIDATION_TEXT = ""
    }

    private val accessibilityManager = (
        context.getSystemService(
            Context.ACCESSIBILITY_SERVICE
        ) as AccessibilityManager
        )

    public override fun getVirtualViewAt(x: Float, y: Float): Int {
        val virtualViewSpans = getVirtualViewSpans()
        if (virtualViewSpans != null) {
            (0 until virtualViewSpans.size).forEach { virtualViewId ->
                val rect = getBoundsFromVirtualViewId(virtualViewId)
                if (rect.contains(x.toInt() + notesEditText.scrollX, y.toInt() + notesEditText.scrollY)) {
                    return virtualViewId
                }
            }
        }
        return INVALID_ID
    }

    override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>?) {
        getVirtualViewSpans()?.let {
            (0 until it.size).forEach { virtualSpan -> virtualViewIds?.add(virtualSpan) }
        }
    }

    override fun onPopulateNodeForVirtualView(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
        getVirtualViewSpans()?.let {
            if (virtualViewId < it.size) {
                node.contentDescription = getContentDescriptionForVirtualViewId(virtualViewId)
                node.setBoundsInParent(getBoundingRectForVirtualViewWrtViewPort(virtualViewId))
                node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                node.addAction(AccessibilityNodeInfoCompat.ACTION_LONG_CLICK)
            } else {
                node.text = DEFAULT_INVALIDATION_TEXT
                node.setBoundsInParent(Rect(0, 0, 1, 1))
            }
        }
    }

    override fun onPopulateEventForVirtualView(virtualViewId: Int, event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
            val rect = getBoundsFromVirtualViewId(virtualViewId)
            notesEditText.getScrollView()?.scrollTo(rect.left, rect.top)
        }
    }

    override fun onPerformActionForVirtualView(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
        // Starting with Android O, instead of sending simulated touch events on double tap,
        // a proper ACTION_CLICK is send to the performAction method of the currently selected accessibility node.
        when (action) {
            AccessibilityNodeInfoCompat.ACTION_CLICK -> {
                handleClick()
                return true
            }
            AccessibilityNodeInfoCompat.ACTION_LONG_CLICK -> {
                handleLongPress()
                return true
            }
        }
        return false
    }

    fun handleHoverEvent(event: MotionEvent): Boolean = dispatchHoverEvent(event)

    fun invalidateVirtualTreeOnUpdate() {
        if (isAccessibilityEnabled()) {
            invalidateRoot()
            if (accessibilityFocusedVirtualViewId > HOST_ID) {
                invalidateVirtualView(accessibilityFocusedVirtualViewId, AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE)
            }
        }
    }

    fun handleClick() {
        if (accessibilityFocusedVirtualViewId > HOST_ID) {
            getVirtualViewSpans()
            val media = getVirtualViewSpans()?.get(accessibilityFocusedVirtualViewId)?.media
            media?.localUrl?.let {
                notesEditText.showMediaInFullscreen(it, media.mimeType)
            }
        }
    }

    fun handleLongPress() {
        if (accessibilityFocusedVirtualViewId > HOST_ID) {
            val spans = getVirtualViewSpans()
            spans?.let {
                notesEditText.text?.let { text ->
                    notesEditText.setSelection(
                        text.getSpanStart(spans[accessibilityFocusedVirtualViewId]),
                        text.getSpanEnd(spans[accessibilityFocusedVirtualViewId])
                    )
                }
            }
        }
    }

    fun isAccessibilityEnabled(): Boolean =
        accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled

    private fun getLeftRelativeToRootView(myView: View): Int {
        return if (myView.parent === myView.rootView) {
            myView.left
        } else {
            myView.left + getLeftRelativeToRootView(myView.parent as View)
        }
    }

    private fun getTopRelativeToRootView(myView: View): Int {
        return if (myView.parent === myView.rootView) {
            myView.top
        } else {
            myView.top + getTopRelativeToRootView(myView.parent as View)
        }
    }

    private fun getContentDescriptionForVirtualViewId(virtualViewId: Int): String {
        val virtualViewSpans = getVirtualViewSpans()
        val vViewID = virtualViewSpans?.get(virtualViewId)
        val alttext = vViewID?.media?.altText
        if (alttext.isNullOrEmpty()) {
            return context.getString(R.string.sn_notes_image)
        }
        return context.getString(R.string.sn_notes_read_alt_text, alttext)
    }

    private fun getBoundingRectForVirtualViewWrtViewPort(virtualViewId: Int): Rect {
        val virtualViewRect = getBoundsFromVirtualViewId(virtualViewId)
        notesEditText.getScrollView()?.let { scrollView ->
            var scrollXWrtNotesEditText = scrollView.scrollX
            var scrollYWrtNotesEditText = scrollView.scrollY

            if (notesEditText.left < scrollXWrtNotesEditText)
                scrollXWrtNotesEditText = scrollView.scrollX - (
                    getLeftRelativeToRootView(
                        notesEditText
                    ) - getLeftRelativeToRootView(scrollView)
                    )

            if (notesEditText.top < scrollView.scrollY)
                scrollYWrtNotesEditText = scrollView.scrollY - (
                    getTopRelativeToRootView(
                        notesEditText
                    ) - getTopRelativeToRootView(scrollView)
                    )

            // Viewport bounds wrt NotesEditText and taking scroll into account
            val viewportRect = Rect(
                scrollXWrtNotesEditText, scrollYWrtNotesEditText,
                scrollXWrtNotesEditText + scrollView.width, scrollYWrtNotesEditText + scrollView.height
            )
            // Virtual view bounds wrt NotesEditText

            if (virtualViewRect.left > viewportRect.right || viewportRect.left > virtualViewRect.right ||
                virtualViewRect.top > viewportRect.bottom || viewportRect.top > virtualViewRect.bottom
            )
                return Rect(
                    scrollXWrtNotesEditText, scrollYWrtNotesEditText,
                    scrollXWrtNotesEditText + 1, scrollYWrtNotesEditText + 1
                )

            // Return intersection of viewport bounds and virtual view bounds wrt NotesEditText
            return Rect(
                maxOf(virtualViewRect.left, viewportRect.left),
                maxOf(virtualViewRect.top, viewportRect.top),
                minOf(virtualViewRect.right, viewportRect.right),
                minOf(virtualViewRect.bottom, viewportRect.bottom)
            )
        } ?: return virtualViewRect
    }

    private fun getVirtualViewSpans(): Array<ImageSpanWithMedia>? {
        val text = notesEditText.text
        if (text != null) {
            val virtualViewSpans = text.getSpans(0, text.length, ImageSpanWithMedia::class.java)
            Arrays.sort(
                virtualViewSpans,
                Comparator<ImageSpanWithMedia> { imageSpanWithMedia1, imageSpanWithMedia2 ->
                    return@Comparator text.getSpanStart(imageSpanWithMedia1) - text.getSpanStart(
                        imageSpanWithMedia2
                    )
                }
            )
            return virtualViewSpans
        } else {
            return null
        }
    }

    private fun getBoundsFromVirtualViewId(virtualViewId: Int): Rect {
        val virtualViewSpans = getVirtualViewSpans()
        val text = notesEditText.text
        val rect = Rect()
        if (virtualViewSpans != null && text != null) {
            notesEditText.getBoundsFromOffset(
                text.getSpanStart(virtualViewSpans[virtualViewId])
            ).roundOut(rect)
        }
        return rect
    }
}
