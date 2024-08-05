package com.microsoft.notes.noteslib.extensions

import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class StringExtensionsTest {

    @Test
    fun `test removeNonAsciiNonPrintableCharacters`() {
        var nonAsciiString = "Office Android 16.0.14228.20012 (Android 11; Pixel 5\u200B)"
        assertThat(nonAsciiString.removeNonAsciiNonPrintableCharacters(), iz("Office Android 16.0.14228.20012 (Android 11; Pixel 5)"))

        nonAsciiString = "tête-à-tête.pdf"
        assertThat(nonAsciiString.removeNonAsciiNonPrintableCharacters(), iz("tte--tte.pdf"))

        var asciiString = "abc      efg "
        assertThat(asciiString, iz(asciiString))

        asciiString = "abc ,,,?<>```***     efg%%%%% "
        assertThat(asciiString, iz(asciiString))
    }
}
