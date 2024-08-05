package com.microsoft.notes.sync

import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class CorrelationVectorTest {
    @Test
    fun should_generate_correlation_vector() {
        val correlationVector = CorrelationVector(
            encodeBase64 = { _ -> "1234567890123456789012==" }
        )

        val first = correlationVector.incrementAndGet()
        assertThat(first, iz("1234567890123456789012.1"))

        val correlationVectorBase = first.subSequence(0, first.indexOf("."))
        assertThat(correlationVectorBase.length, iz(22))

        val second = correlationVector.incrementAndGet()
        assertThat(second, iz("1234567890123456789012.2"))
    }
}
