package com.microsoft.notes.richtext.editor.styled

import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class DataVersionTest {
    @Test
    fun `test compare Version is lesser`() {
        assertThat(Version("1.0.0").compare(Version("2.0.0")), iz(-1))
        assertThat(Version("1.1.0").compare(Version("1.2.0")), iz(-1))
        assertThat(Version("1.1.1").compare(Version("1.1.2")), iz(-1))
        assertThat(Version("1.1").compare(Version("1.1.2")), iz(-1))
    }

    @Test
    fun `test compare Version is greater`() {
        assertThat(Version("2.0.0").compare(Version("1.0.0")), iz(1))
        assertThat(Version("1.2.0").compare(Version("1.1.0")), iz(1))
        assertThat(Version("1.1.2").compare(Version("1.1.1")), iz(1))
        assertThat(Version("1.1.2").compare(Version("1.1")), iz(1))
    }

    @Test
    fun `test compare Version is same`() {
        assertThat(Version("2.0.0").compare(Version("2.0.0")), iz(0))
        assertThat(Version("1.2.0").compare(Version("1.2.0")), iz(0))
        assertThat(Version("1.1.2").compare(Version("1.1.2")), iz(0))
        assertThat(Version("1.1").compare(Version("1.1")), iz(0))
    }

    @Test
    fun `test empty Version is lesser`() {
        assertThat(Version("").compare(Version("2.0.0")), iz(-1))
    }
}
