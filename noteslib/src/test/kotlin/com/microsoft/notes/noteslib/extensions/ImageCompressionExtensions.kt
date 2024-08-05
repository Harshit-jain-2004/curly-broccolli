package com.microsoft.notes.noteslib.extensions

import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ImageCompressionExtensions {

    @Test
    fun `should not scale down perfectly sized image`() {
        val width = 1000
        val sampleSize = calculateSampleSize(width, width)
        assertThat(sampleSize, iz(1))
    }

    @Test
    fun `should not scale down small image`() {
        val width = 500
        val sampleSize = calculateSampleSize(width, width)
        assertThat(sampleSize, iz(1))
    }

    @Test
    fun `should scale down double size image`() {
        val width = 2000
        val sampleSize = calculateSampleSize(width, width)
        assertThat(sampleSize, iz(2))
    }

    @Test
    fun `should scale down large image to the closest size`() {
        // largest correct value: 9000 / 8 = 1125
        val width = 9000
        val sampleSize = calculateSampleSize(width, width)
        assertThat(sampleSize, iz(8))
    }

    @Test
    fun `should scale down another large image to the closest size`() {
        // largest correct value:
        // 7500 / 8 = ~937
        // 7500 / 4 = 1875
        // scaling down by 8 would be too much
        val width = 7500
        val sampleSize = calculateSampleSize(width, width)
        assertThat(sampleSize, iz(4))
    }
}
