import android.text.Spannable
import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.editor.utils.NotesEditTextUndoRedoManager
import com.microsoft.notes.richtext.editor.utils.NotesEditTextUndoRedoStackChangeCallback
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class NotesEditTextUndoRedoManagerTest {
    private lateinit var undoRedoManager: NotesEditTextUndoRedoManager
    private lateinit var callback: NotesEditTextUndoRedoStackChangeCallback

    private val state1 = EditorState(
        Document(
            listOf(
                Paragraph(
                    content = Content(
                        "hello world",
                        listOf(Span(SpanStyle.BOLD, 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                    )
                )
            )
        )
    )

    private val state2 = EditorState(
        Document(
            listOf(
                Paragraph(
                    content = Content(
                        "hello world",
                        listOf(Span(SpanStyle.BOLD, 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                    )
                ),
                Paragraph(
                    content = Content(
                        "foo bar baz",
                        emptyList()
                    )
                )
            )
        )
    )

    private val state3 = EditorState(
        Document(
            listOf(
                Paragraph(
                    content = Content(
                        "hello world",
                        listOf(Span(SpanStyle.BOLD, 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                    )
                ),
                Paragraph(
                    content = Content(
                        "foo bar baz",
                        emptyList()
                    )
                ),
                Paragraph(
                    content = Content(
                        "lorem ipsum",
                        emptyList()
                    )
                )
            )
        )
    )

    @Before
    fun setUp() {
        undoRedoManager = NotesEditTextUndoRedoManager()
        callback = mock(NotesEditTextUndoRedoStackChangeCallback::class.java)
        undoRedoManager.stackChangeCallback = callback
        undoRedoManager.initialize(state1)
    }

    @Test
    fun testAddToUndoStack() {
        undoRedoManager.addUndoState(state2)
        assertTrue(undoRedoManager.canPerformUndo())
        assertFalse(undoRedoManager.canPerformRedo())

        undoRedoManager.addUndoState(state3, isSignificantChange = true)
        assertTrue(undoRedoManager.canPerformUndo())
        assertFalse(undoRedoManager.canPerformRedo())
        verify(callback).onUndoChanged(true)
    }

    @Test
    fun testUndo() {
        undoRedoManager.addUndoState(state2, isSignificantChange = true)
        undoRedoManager.addUndoState(state3, isSignificantChange = true)

        var undoState1 = undoRedoManager.undo()
        assertEquals(state2, undoState1)

        val undoState2 = undoRedoManager.undo()
        assertEquals(state1, undoState2)

        assertFalse(undoRedoManager.canPerformUndo())
        verify(callback, times(2)).onUndoChanged(anyBoolean())
        verify(callback, times(1)).onRedoChanged(anyBoolean())
    }

    @Test
    fun testRedo() {
        undoRedoManager.addUndoState(state2, isSignificantChange = true)
        undoRedoManager.addUndoState(state3, isSignificantChange = true)

        undoRedoManager.undo()
        undoRedoManager.undo()

        val redoState1 = undoRedoManager.redo()
        assertEquals(state2, redoState1)

        val redoState2 = undoRedoManager.redo()
        assertEquals(state3, redoState2)

        assertFalse(undoRedoManager.canPerformRedo())
        verify(callback, times(3)).onUndoChanged(anyBoolean())
        verify(callback, times(2)).onRedoChanged(anyBoolean())
    }

    @Test
    fun testStackInteraction() {
        undoRedoManager.addUndoState(state2, isSignificantChange = true)
        undoRedoManager.addUndoState(state3, isSignificantChange = true)

        assertEquals(state2, undoRedoManager.undo())
        assertEquals(state3, undoRedoManager.redo())
    }

    @Test
    fun testCallbackInvocation() {
        undoRedoManager.addUndoState(state2, isSignificantChange = true)

        verify(callback).onUndoChanged(true)

        undoRedoManager.undo()

        verify(callback, times(2)).onUndoChanged(anyBoolean())
        verify(callback, times(1)).onRedoChanged(anyBoolean())
    }

    @Test
    fun testClearRedoStack() {
        undoRedoManager.addUndoState(state2)
        undoRedoManager.undo()
        undoRedoManager.addUndoState(state3)

        assertFalse(undoRedoManager.canPerformRedo())
    }
}
