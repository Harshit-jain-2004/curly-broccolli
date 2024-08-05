package com.microsoft.notes.ui.note.ink

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.TOOL_TYPE_ERASER
import android.view.MotionEvent.TOOL_TYPE_STYLUS
import com.microsoft.notes.richtext.editor.NotesEditText.Companion.UNINITIALIZED_REV_VAL
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InkPoint
import com.microsoft.notes.richtext.scheme.Stroke
import com.microsoft.notes.richtext.scheme.distanceTo
import com.microsoft.notes.richtext.scheme.scaleDown
import com.microsoft.notes.utils.logging.EventMarkers
import java.util.UUID
import kotlin.math.max

interface NotesEditInkCallback {
    fun updateInkDocument(document: Document, uiRevision: Long)
    fun recordTelemetryEvent(eventMarker: EventMarkers, vararg keyValuePairs: Pair<String, String>)
    fun recordNoteContentUpdated()
    fun onUndoStackChanged(count: Int)
}

private fun generateLocalId(): String = UUID.randomUUID().toString()

private enum class InkOperationType {
    ADD,
    REMOVE
}

enum class InkState {
    INK,
    ERASE,
    READ
}

private data class InkOperation(val strokes: List<Stroke>, val action: InkOperationType)
private const val UNDO_STACK_MAX_SIZE = 20
private const val STYLUS_PRESSURE_SCALE_FACTOR = 2
private const val STYLUS_MIN_STROKE_PRESSURE = 0.3

class EditInkView(context: Context, attrs: AttributeSet) : InkView(context, attrs) {
    private var currentStrokePoints: MutableList<InkPoint> = mutableListOf()
    private var hasErasedStrokes = false
    private var notesEditInkCallback: NotesEditInkCallback? = null
    private val undoStack: MutableList<InkOperation> = mutableListOf()
    private var currentPointerId: Int = 0

    var revision = UNINITIALIZED_REV_VAL

    var inkState: InkState = InkState.INK
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        // Prevents the scroll view from intercepting the touch event
        // Otherwise ink cannot be drawn vertically
        parent.requestDisallowInterceptTouchEvent(true)

        if (event.actionIndex != 0) {
            return false
        }

        val erasingInk = inkState == InkState.ERASE || event.allErasePointers()

        // Map the x, y scale used by Android to the scale we use in the ink note data structure
        val scaleFactor = getScaleFactorWithDefault()

        return if (erasingInk) {
            handleEraseEvent(event, scaleFactor)
        } else if (inkState == InkState.INK) {
            handleDrawEvent(event, scaleFactor)
        } else
            return false
    }

    private fun handleDrawEvent(event: MotionEvent, scaleFactor: Float): Boolean {
        // https://stackoverflow.com/questions/17384983/in-android-what-is-the-difference-between-getaction-and-getactionmasked-in
        val eventIsValidMotion = event.actionMasked.let {
            it == MotionEvent.ACTION_DOWN || it == MotionEvent.ACTION_MOVE || it == MotionEvent.ACTION_UP
        }

        val rawEventPoints = if (eventIsValidMotion)
            getPointsFromValidMotionEvent(event) else emptyList()
        currentStrokePoints.addAll(rawEventPoints)

        if (event.actionMasked != MotionEvent.ACTION_DOWN && event.actionMasked != MotionEvent.ACTION_MOVE) {
            // TODO do we need this lock? Appears this is the only place we lock on currentPoints
            synchronized(currentStrokePoints) {
                if (currentStrokePoints.isNotEmpty()) {
                    val newStroke = Stroke(generateLocalId(), currentStrokePoints.map { it.scaleDown(scaleFactor) }.toList())
                    addOperationToUndoStack(InkOperation(listOf(newStroke), InkOperationType.ADD))
                    notesEditInkCallback?.onUndoStackChanged(undoStack.size)
                    val updatedDocument = document.copy(strokes = document.strokes + newStroke)
                    notesEditInkCallback?.let {
                        it.updateInkDocument(updatedDocument, revision)

                        if (document.strokes.isEmpty()) {
                            it.recordTelemetryEvent(EventMarkers.InkAddedToEmptyNote)
                        }
                        it.recordNoteContentUpdated()
                    }
                    updateDocument(doc = updatedDocument, invalidatePathCache = false)
                    updatePathCache(newStroke)
                    currentStrokePoints.clear()
                }
            }
        }

        if (eventIsValidMotion) {
            invalidate()
        }

        return eventIsValidMotion
    }

    private fun handleEraseEvent(event: MotionEvent, scaleFactor: Float): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val erasePoint = InkPoint(event.x.toDouble(), event.y.toDouble(), 1.0).scaleDown(scaleFactor)
                val deletedStrokes = document.strokes.filter { dryStroke ->
                    dryStroke.points.any {
                        it.distanceTo(erasePoint) < ERASER_THRESHOLD / scaleFactor
                    }
                }

                if (deletedStrokes.isNotEmpty()) {
                    deletedStrokes.forEach {
                        addOperationToUndoStack(InkOperation(listOf(it), InkOperationType.REMOVE))
                    }
                    notesEditInkCallback?.onUndoStackChanged(undoStack.size)
                    val updatedDocument = document.copy(strokes = document.strokes - deletedStrokes)
                    updateDocument(updatedDocument)
                    hasErasedStrokes = true
                    invalidate()
                }
                return true
            }
            else -> {
                // Commit changes from erasing ink strokes
                if (hasErasedStrokes) {
                    notesEditInkCallback?.let {
                        it.updateInkDocument(document, revision)
                        it.recordNoteContentUpdated()
                    }
                    invalidate()
                }
                hasErasedStrokes = false
                return event.actionMasked == MotionEvent.ACTION_UP
            }
        }
    }

    fun undoLastStroke() {
        if (undoStack.size > 0) {
            val lastInkAction = undoStack.last()
            val newStrokes =
                when (lastInkAction.action) {
                    InkOperationType.ADD -> document.strokes - lastInkAction.strokes
                    InkOperationType.REMOVE -> document.strokes + lastInkAction.strokes
                }
            val updatedDocument = document.copy(strokes = newStrokes)
            notesEditInkCallback?.updateInkDocument(updatedDocument, revision)
            updateDocument(updatedDocument)
            undoStack.removeAt(undoStack.size - 1)
            notesEditInkCallback?.onUndoStackChanged(undoStack.size)
            invalidate()
        }
    }

    fun clearCanvas() {
        val currentStrokes = document.strokes
        if (currentStrokes.isEmpty()) return

        val updatedDocument = document.copy(strokes = emptyList())
        notesEditInkCallback?.updateInkDocument(updatedDocument, revision)
        notesEditInkCallback?.recordNoteContentUpdated()
        updateDocument(updatedDocument)
        addOperationToUndoStack(InkOperation(currentStrokes, InkOperationType.REMOVE))
        notesEditInkCallback?.onUndoStackChanged(undoStack.size)
        invalidate()
    }

    private fun addOperationToUndoStack(inkOperation: InkOperation) {
        if (undoStack.size == UNDO_STACK_MAX_SIZE) {
            undoStack.removeAt(0)
        }
        undoStack.add(inkOperation)
    }

    fun resetUndoStack() = undoStack.clear()

    fun isUndoStackEmpty() = undoStack.isEmpty()

    private fun getPointsFromValidMotionEvent(event: MotionEvent): List<InkPoint> {
        // Since we already check for actionIndex == 0, the assumption that there's only one pointer
        // id can be made. Note the pointer id is not always 0, and can be arbitrary.
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            currentPointerId = event.getPointerId(0)
        }

        return if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            extractInkPointsFromMoveActionEvent(event)
        } else {
            val pressureScale = if (event.getToolType(0) == TOOL_TYPE_STYLUS) STYLUS_PRESSURE_SCALE_FACTOR else 1
            listOf(
                InkPoint(
                    event.x.toDouble(),
                    event.y.toDouble(),
                    max((event.pressure * pressureScale).toDouble(), STYLUS_MIN_STROKE_PRESSURE)
                )
            )
        }
    }

    private fun extractInkPointsFromMoveActionEvent(event: MotionEvent): List<InkPoint> {
        val points = mutableListOf<InkPoint>()
        for (h in 0 until event.historySize) {
            for (p in 0 until event.pointerCount) {
                // Only draw with the first finger/pen that touches the screen in multitouch scenarios
                if (event.getToolType(p) != TOOL_TYPE_ERASER && event.getPointerId(p) == currentPointerId) {
                    val pressureScale = if (event.getToolType(p) == TOOL_TYPE_STYLUS) STYLUS_PRESSURE_SCALE_FACTOR else 1
                    points.add(
                        InkPoint(
                            event.getHistoricalX(p, h).toDouble(),
                            event.getHistoricalY(p, h).toDouble(),
                            max((event.getHistoricalPressure(p, h) * pressureScale).toDouble(), STYLUS_MIN_STROKE_PRESSURE)
                        )
                    )
                }
            }
        }
        return points
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawStrokePoints(currentStrokePoints, canvas, 1.0f)
    }

    fun setNotesEditInkViewCallback(notesEditInkCallback: NotesEditInkCallback) {
        this.notesEditInkCallback = notesEditInkCallback
    }
}

private fun MotionEvent.allErasePointers(): Boolean =
    (0 until this.pointerCount).all { this.getToolType(it) == TOOL_TYPE_ERASER }
