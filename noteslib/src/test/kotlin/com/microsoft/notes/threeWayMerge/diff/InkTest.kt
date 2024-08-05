package com.microsoft.notes.threeWayMerge.diff

import com.microsoft.notes.models.Note
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InkPoint
import com.microsoft.notes.richtext.scheme.Stroke
import com.microsoft.notes.threeWayMerge.Diff
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class InkTest {
    private val stroke1 = Stroke(
        id = "stroke1ID",
        points = listOf(
            InkPoint(1.0, 0.0, 0.5),
            InkPoint(1.0, 1.0, 0.5)
        )
    )
    private val stroke2 = Stroke(
        id = "stroke2ID",
        points = listOf(
            InkPoint(2.0, 0.0, 0.5),
            InkPoint(2.0, 2.0, 0.5)
        )
    )
    private val stroke3 = Stroke(
        id = "stroke3ID",
        points = listOf(
            InkPoint(3.0, 0.0, 0.5),
            InkPoint(3.0, 3.0, 0.5)
        )
    )
    private val stroke4 = Stroke(
        id = "stroke4ID",
        points = listOf(
            InkPoint(4.0, 0.0, 0.5),
            InkPoint(4.0, 4.0, 0.5)
        )
    )
    private val stroke5 = Stroke(
        id = "stroke5ID",
        points = listOf(
            InkPoint(5.0, 0.0, 0.5),
            InkPoint(5.0, 5.0, 0.5)
        )
    )

    @Test
    fun should_diff_be_empty() {
        val baseStrokes = listOf(stroke1, stroke2)
        val targetStrokes = listOf(stroke1, stroke2)

        val diff = inkDiffHelper(baseStrokes, targetStrokes)

        assertThat(diff, iz(emptyList()))
    }

    @Test
    fun should_add_stroke_at_end() {
        val baseStrokes = listOf(stroke1)
        val targetStrokes = listOf(stroke1, stroke2)

        val diff = inkDiffHelper(baseStrokes, targetStrokes)
        val expectedDiffs: List<Diff> = listOf(StrokeInsertion(stroke2.id, stroke2, 1))

        assertThat(diff, iz(expectedDiffs))
    }

    @Test
    fun should_add_stroke_at_beginning() {
        val baseStrokes = listOf(stroke2)
        val targetStrokes = listOf(stroke1, stroke2)

        val diff = inkDiffHelper(baseStrokes, targetStrokes)
        val expectedDiffs: List<Diff> = listOf(StrokeInsertion(stroke1.id, stroke1, 0))

        assertThat(diff, iz(expectedDiffs))
    }

    @Test
    fun should_add_stroke_from_empty() {
        val baseStrokes = emptyList<Stroke>()
        val targetStrokes = listOf(stroke1)

        val diff = inkDiffHelper(baseStrokes, targetStrokes)
        val expectedDiffs: List<Diff> = listOf(StrokeInsertion(stroke1.id, stroke1, 0))

        assertThat(diff, iz(expectedDiffs))
    }

    @Test
    fun should_add_multiple_strokes() {
        val baseStrokes = listOf(stroke2)
        val targetStrokes = listOf(stroke1, stroke2, stroke3, stroke4)

        val diff = inkDiffHelper(baseStrokes, targetStrokes)
        val expectedDiffs: List<Diff> = listOf(
            StrokeInsertion(stroke1.id, stroke1, 0),
            StrokeInsertion(stroke3.id, stroke3, 2),
            StrokeInsertion(stroke4.id, stroke4, 3)
        )

        assertThat(diff, iz(expectedDiffs))
    }

    @Test
    fun should_remove_stroke_at_end() {
        val baseStrokes = listOf(stroke1, stroke2)
        val targetStrokes = listOf(stroke1)

        val diff = inkDiffHelper(baseStrokes, targetStrokes)
        val expectedDiffs: List<Diff> = listOf(StrokeDeletion(stroke2.id))

        assertThat(diff, iz(expectedDiffs))
    }

    @Test
    fun should_remove_stroke_at_beginning() {
        val baseStrokes = listOf(stroke1, stroke2)
        val targetStrokes = listOf(stroke2)

        val diff = inkDiffHelper(baseStrokes, targetStrokes)
        val expectedDiffs: List<Diff> = listOf(StrokeDeletion(stroke1.id))

        assertThat(diff, iz(expectedDiffs))
    }

    @Test
    fun should_remove_stroke_to_empty() {
        val baseStrokes = listOf(stroke1)
        val targetStrokes = emptyList<Stroke>()

        val diff = inkDiffHelper(baseStrokes, targetStrokes)
        val expectedDiffs: List<Diff> = listOf(StrokeDeletion(stroke1.id))

        assertThat(diff, iz(expectedDiffs))
    }

    @Test
    fun should_remove_multiple_strokes() {
        val baseStrokes = listOf(stroke1, stroke2, stroke3, stroke4)
        val targetStrokes = listOf(stroke3)

        val diff = inkDiffHelper(baseStrokes, targetStrokes)
        val expectedDiffs: List<Diff> = listOf(
            StrokeDeletion(stroke1.id), StrokeDeletion(stroke2.id), StrokeDeletion(stroke4.id)
        )

        assertThat(diff, iz(expectedDiffs))
    }

    @Test
    fun should_replace_stroke() {
        val baseStrokes = listOf(stroke1, stroke2, stroke3)
        val targetStrokes = listOf(stroke1, stroke4, stroke3)

        val diff = inkDiffHelper(baseStrokes, targetStrokes)
        val expectedDiffs: List<Diff> = listOf(
            StrokeDeletion(stroke2.id), StrokeInsertion(stroke4.id, stroke4, 1)
        )

        assertThat(diff, iz(expectedDiffs))
    }

    @Test
    fun should_add_and_delete_multiple_strokes() {
        val baseStrokes = listOf(stroke1, stroke2, stroke3)
        val targetStrokes = listOf(stroke3, stroke4, stroke5)

        val diff = inkDiffHelper(baseStrokes, targetStrokes)
        val expectedDiffs: List<Diff> = listOf(
            StrokeDeletion(stroke1.id),
            StrokeDeletion(stroke2.id),
            StrokeInsertion(stroke4.id, stroke4, 1),
            StrokeInsertion(stroke5.id, stroke5, 2)
        )

        assertThat(diff, iz(expectedDiffs))
    }

    private fun inkDiffHelper(baseStrokes: List<Stroke>, targetStrokes: List<Stroke>): List<Diff> {
        return inkDiff(
            Note(document = Document(strokes = baseStrokes)),
            Note(document = Document(strokes = targetStrokes))
        )
    }
}
