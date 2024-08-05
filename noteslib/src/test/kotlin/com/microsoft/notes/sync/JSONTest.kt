package com.microsoft.notes.sync

import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class JSONTest {

    @Before
    fun setup() {
        JSON.isGsonEnabled = true
    }

    @Test
    fun `should escape characters test 1`() {
        val TEST = "this \" has some \" characters that need \\escaping \\"
        val EXPECTED = "\"this \\\" has some \\\" characters that need \\\\escaping \\\\\""

        val jsonString = JSON.JString(TEST)
        val result = jsonString.toString()
        assertThat(result, iz(EXPECTED))
    }

    @Test
    fun `should escape characters test 2`() {
        val TEST = "this \"text\" has a new line \n"
        val EXPECTED = "\"this \\\"text\\\" has a new line \\n\""

        val jsonString = JSON.JString(TEST)
        val result = jsonString.toString()
        assertThat(result, iz(EXPECTED))
    }

    @Test
    fun `should escape characters test 3`() {
        val TEST = "this \"text\" has a tab \\. that was a tab"
        val EXPECTED = "\"this \\\"text\\\" has a tab \\\\. that was a tab\""

        val jsonString = JSON.JString(TEST)
        val result = jsonString.toString()
        assertThat(result, iz(EXPECTED))
    }

    @Test
    fun `should escape characters test 4`() {
        val TEST = "Hello, how are you \t this is a tab \n this is a new line"
        val EXPECTED = "\"Hello, how are you \\t this is a tab \\n this is a new line\""

        val jsonString = JSON.JString(TEST)
        val result = jsonString.toString()
        assertThat(result, iz(EXPECTED))
    }
}
