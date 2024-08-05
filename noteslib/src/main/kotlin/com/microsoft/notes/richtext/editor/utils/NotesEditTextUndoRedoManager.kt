package com.microsoft.notes.richtext.editor.utils

import com.microsoft.notes.richtext.editor.EditorState
import java.util.ArrayDeque

private const val UNDO_STACK_MAX_SIZE = 20
private const val TIME_THRESHOLD_MS = 1000

interface NotesEditTextUndoRedoStackChangeCallback {
    fun onUndoChanged(isEnabled: Boolean)
    fun onRedoChanged(isEnabled: Boolean)
}

class NotesEditTextUndoRedoManager {
    private val undoStack = ArrayDeque<EditorState>(UNDO_STACK_MAX_SIZE)
    private val redoStack = ArrayDeque<EditorState>(UNDO_STACK_MAX_SIZE)

    private var lastStateChangeTime: Long = System.currentTimeMillis()
    private var previousEditorState: EditorState? = null

    var stackChangeCallback: NotesEditTextUndoRedoStackChangeCallback? = null

    fun initialize(editorState: EditorState) {
        clear()
        addUndoState(editorState, isSignificantChange = true, clearRedoStack = true)
    }

    fun addUndoState(
        editorState: EditorState,
        isSignificantChange: Boolean = false,
        clearRedoStack: Boolean = true
    ) {
        val currentTime = System.currentTimeMillis()

        if (isSignificantChange || shouldAddToUndoStack(editorState, currentTime)) {
            previousEditorState?.let {
                undoStack.addLast(it)
                maintainStackSize(undoStack)
                if (undoStack.size == 1) stackChangeCallback?.onUndoChanged(isEnabled = true)
            }
            lastStateChangeTime = currentTime
            previousEditorState = editorState
            if (clearRedoStack && redoStack.size > 0) {
                redoStack.clear()
                stackChangeCallback?.onRedoChanged(isEnabled = false)
            }
        }
    }

    private fun shouldAddToUndoStack(editorState: EditorState, currentTime: Long): Boolean {
        val timeDifference = currentTime - lastStateChangeTime
        return (
            previousEditorState != editorState && timeDifference > TIME_THRESHOLD_MS
            ) || undoStack.isEmpty()
    }

    private fun addRedoState(editorState: EditorState) {
        if (redoStack.peekLast() != editorState) {
            redoStack.addLast(editorState)
            maintainStackSize(redoStack)
            if (redoStack.size == 1) stackChangeCallback?.onRedoChanged(isEnabled = true)
        }
    }

    private fun maintainStackSize(stack: ArrayDeque<EditorState>) {
        if (stack.size >= UNDO_STACK_MAX_SIZE) {
            stack.removeFirst()
        }
    }

    fun undo(): EditorState? {
        return undoStack.pollLast()?.also { undoState ->
            previousEditorState?.let {
                addRedoState(it)
                if (undoStack.isEmpty()) stackChangeCallback?.onUndoChanged(isEnabled = false)
            }
            previousEditorState = undoState
        }
    }

    fun redo(): EditorState? {
        return redoStack.pollLast()?.also { redoState ->
            previousEditorState?.let {
                addUndoState(it, isSignificantChange = true, clearRedoStack = false)
                if (redoStack.isEmpty()) stackChangeCallback?.onRedoChanged(isEnabled = false)
            }
            previousEditorState = redoState
        }
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        previousEditorState = null
    }

    fun canPerformUndo() = !undoStack.isEmpty()
    fun canPerformRedo() = !redoStack.isEmpty()

    companion object {
        fun isSignificantTextChange(newText: String, countDiffence: Int): Boolean {
            // Defines a regex pattern matching common escape sequences and printable special characters at string ends.
            // Updates the undo stack for special characters or text size changes exceeding 1 character during events like cut/paste.
            val specialCharsPattern = Regex("[\\n\\t\\r\\f!\"#\$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~ ]$")
            return specialCharsPattern.containsMatchIn(newText) || countDiffence > 1
        }
    }
}
