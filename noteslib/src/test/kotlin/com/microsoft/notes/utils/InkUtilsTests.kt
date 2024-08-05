package com.microsoft.notes.utils

import com.microsoft.notes.utils.utils.calculateScaleFactor
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.number.OrderingComparison.lessThanOrEqualTo
import org.junit.Test

class InkUtilsTests {

    @Test
    fun shouldReturnAValueNotMoreThanDefaultScaleFactor() {

        // Min of Scale X and Scale Y is Less than Default Scale Factor
        val scaleFactor1 = calculateScaleFactor(2.0f, 3.0f, 4, 6, 4.0f)
        assertThat(scaleFactor1, lessThanOrEqualTo(4.0f))

        // Min of Scale X and Scale Y is Equal to Default Scale Factor
        val scaleFactor2 = calculateScaleFactor(3.0f, 3.0f, 60, 15, 5.0f)
        assertThat(scaleFactor2, lessThanOrEqualTo(5.0f))

        // Min of Scale X and Scale Y is Greater than Default Scale Factor
        val scaleFactor3 = calculateScaleFactor(2.0f, 2.0f, 10, 10, 4.0f)
        assertThat(scaleFactor3, lessThanOrEqualTo(4.0f))
    }
}
