package com.microsoft.notes.sync.models

import com.microsoft.notes.sync.JSON
import com.microsoft.notes.sync.JSON.JArray
import com.microsoft.notes.sync.JSON.JNull
import com.microsoft.notes.sync.JSON.JObject
import com.microsoft.notes.sync.JSON.JString
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class SyncResponseTest {
    @Test
    fun should_be_convertible_from_JSON_with_skip_token() {
        val json = JObject(
            hashMapOf(
                "skipToken" to JString("some-skip"),
                "value" to JArray(
                    listOf(
                        JString("Hello")
                    )
                )
            )
        )

        val actual = SyncResponse.fromJSON(json, valueParser = ::dummyValueParser, tokenParser = (Token)::fromJSON)
        val expected = SyncResponse(Token.Skip("some-skip"), value = listOf("Hello"))
        assertThat<SyncResponse<String>>(actual, iz(expected))
    }

    @Test
    fun should_be_convertible_from_JSON_with_delta_token() {
        val json = JObject(
            hashMapOf(
                "deltaToken" to JString("some-delta"),
                "value" to JArray(
                    listOf(
                        JString("World")
                    )
                )
            )
        )

        val actual = SyncResponse.fromJSON(json, valueParser = ::dummyValueParser, tokenParser = (Token)::fromJSON)
        val expected = SyncResponse(Token.Delta("some-delta"), value = listOf("World"))
        assertThat<SyncResponse<String>>(actual, iz(expected))
    }

    @Test
    fun should_be_convertible_from_JSON_with_skip_token_erroneous_value() {
        val json = JObject(
            hashMapOf(
                "skipToken" to JString("some-skip"),
                "value" to JArray(listOf(JString("Hello"), JNull(), JString("World")))
            )
        )

        val actual = SyncResponse.fromJSON(json, valueParser = ::dummyValueParser, tokenParser = (Token)::fromJSON)
        val expected = SyncResponse(
            Token.Skip("some-skip"),
            value = listOf("Hello", "World")
        )
        assertThat<SyncResponse<String>>(actual, iz(expected))
    }

    @Test
    fun should_be_convertible_from_JSON_with_delta_token_erroneous_value() {
        val json = JObject(
            hashMapOf(
                "deltaToken" to JString("some-delta"),
                "value" to JArray(listOf(JString("Hello"), JNull(), JString("World")))
            )
        )

        val actual = SyncResponse.fromJSON(json, valueParser = ::dummyValueParser, tokenParser = (Token)::fromJSON)
        val expected = SyncResponse(
            Token.Delta("some-delta"),
            value = listOf("Hello", "World")
        )
        assertThat<SyncResponse<String>>(actual, iz(expected))
    }

    fun dummyValueParser(str: JSON): String? {
        return (str as? JString)?.string
    }
}
