package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.richtext.scheme.InkPoint
import com.microsoft.notes.richtext.scheme.Stroke
import com.microsoft.notes.threeWayMerge.Diff
import com.microsoft.notes.threeWayMerge.diff.StrokeDeletion
import com.microsoft.notes.threeWayMerge.diff.StrokeInsertion
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
    fun should_merge_strokes_no_diff() {
        val baseStrokes = listOf(stroke1, stroke2)
        val primaryDiffs = mutableListOf<Diff>()
        val secondaryDiffs = mutableListOf<Diff>()

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(listOf(stroke1, stroke2)))
    }

    @Test
    fun should_insert_in_primary() {
        val baseStrokes = listOf(stroke1, stroke2)
        val primaryDiffs = mutableListOf<Diff>(StrokeInsertion(stroke3.id, stroke3, 1))
        val secondaryDiffs = mutableListOf<Diff>()

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(listOf(stroke1, stroke3, stroke2)))
    }

    @Test
    fun should_insert_in_secondary() {
        val baseStrokes = listOf(stroke1, stroke2)
        val primaryDiffs = mutableListOf<Diff>()
        val secondaryDiffs = mutableListOf<Diff>(StrokeInsertion(stroke3.id, stroke3, 2))

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(listOf(stroke1, stroke2, stroke3)))
    }

    @Test
    fun should_insert_different_stroke_in_both() {
        val baseStrokes = listOf(stroke1, stroke2)
        val primaryDiffs = mutableListOf<Diff>(StrokeInsertion(stroke3.id, stroke3, 0))
        val secondaryDiffs = mutableListOf<Diff>(StrokeInsertion(stroke4.id, stroke4, 2))

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(listOf(stroke3, stroke1, stroke2, stroke4)))
    }

    @Test
    fun should_insert_same_stroke_in_both() {
        val baseStrokes = listOf(stroke1, stroke2)
        val primaryDiffs = mutableListOf<Diff>(StrokeInsertion(stroke3.id, stroke3, 0))
        val secondaryDiffs = mutableListOf<Diff>(StrokeInsertion(stroke3.id, stroke3, 0))

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(listOf(stroke3, stroke1, stroke2)))
    }

    @Test
    fun should_remove_in_primary() {
        val baseStrokes = listOf(stroke1, stroke2)
        val primaryDiffs = mutableListOf<Diff>(StrokeDeletion(stroke1.id))
        val secondaryDiffs = mutableListOf<Diff>()

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(listOf(stroke2)))
    }

    @Test
    fun should_remove_in_secondary() {
        val baseStrokes = listOf(stroke1, stroke2)
        val primaryDiffs = mutableListOf<Diff>()
        val secondaryDiffs = mutableListOf<Diff>(StrokeDeletion(stroke2.id))

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(listOf(stroke1)))
    }

    @Test
    fun should_remove_different_strokes_in_both() {
        val baseStrokes = listOf(stroke1, stroke2)
        val primaryDiffs = mutableListOf<Diff>(StrokeDeletion(stroke1.id))
        val secondaryDiffs = mutableListOf<Diff>(StrokeDeletion(stroke2.id))

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(emptyList()))
    }

    @Test
    fun should_remove_same_strokes_in_both() {
        val baseStrokes = listOf(stroke1, stroke2)
        val primaryDiffs = mutableListOf<Diff>(StrokeDeletion(stroke1.id))
        val secondaryDiffs = mutableListOf<Diff>(StrokeDeletion(stroke1.id))

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(listOf(stroke2)))
    }

    @Test
    fun should_insert_in_primary_remove_in_secondary() {
        val baseStrokes = listOf(stroke1, stroke2)
        val primaryDiffs = mutableListOf<Diff>(StrokeInsertion(stroke3.id, stroke3, 1))
        val secondaryDiffs = mutableListOf<Diff>(StrokeDeletion(stroke1.id))

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(listOf(stroke3, stroke2)))
    }

    @Test
    fun should_remove_in_primary_insert_in_secondary() {
        val baseStrokes = listOf(stroke1, stroke2)
        val primaryDiffs = mutableListOf<Diff>(StrokeDeletion(stroke2.id))
        val secondaryDiffs = mutableListOf<Diff>(StrokeInsertion(stroke3.id, stroke3, 2))

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(listOf(stroke1, stroke3)))
    }

    @Test
    fun should_both_insert_and_remove() {
        val baseStrokes = listOf(stroke1, stroke2)
        val primaryDiffs = mutableListOf<Diff>(
            StrokeDeletion(stroke2.id),
            StrokeInsertion(stroke3.id, stroke3, 1)
        )
        val secondaryDiffs = mutableListOf<Diff>(
            StrokeDeletion(stroke1.id),
            StrokeInsertion(stroke4.id, stroke4, 0)
        )

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(listOf(stroke3, stroke4)))
    }

    @Test
    fun should_both_insert_multiple() {
        val baseStrokes = emptyList<Stroke>()
        val primaryDiffs = mutableListOf<Diff>(
            StrokeInsertion(stroke1.id, stroke1, 0),
            StrokeInsertion(stroke2.id, stroke2, 1)
        )
        val secondaryDiffs = mutableListOf<Diff>(
            StrokeInsertion(stroke3.id, stroke3, 0),
            StrokeInsertion(stroke4.id, stroke4, 1)
        )

        val merged = merge(baseStrokes, primaryDiffs, secondaryDiffs)

        assertThat(merged, iz(listOf(stroke1, stroke2, stroke3, stroke4)))
    }
}
