package com.microsoft.notes.sync.models

import com.microsoft.notes.sync.JSON
import com.microsoft.notes.sync.JSON.JObject
import com.microsoft.notes.sync.JSON.JString
import com.microsoft.notes.sync.remoteRichTextNote
import com.microsoft.notes.sync.remoteRichTextNoteJSON
import okio.Buffer
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class DeltaSyncPayloadTest {
    @Test
    fun should_be_convertible_from_JSON_as_deleted() {
        val json = JObject(
            hashMapOf(
                "reason" to JString("deleted"),
                "id" to JString("123")
            )
        )
        val actual = DeltaSyncPayload.fromJSON(json)
        assertThat<DeltaSyncPayload>(actual, iz(DeltaSyncPayload.Deleted(id = "123")))
    }

    @Test
    fun should_be_convertible_from_JSON_as_non_deleted() {
        val id = "123"
        val buf = Buffer()
        val json = JSON.read(buf.write(remoteRichTextNoteJSON(id).toString().toByteArray())).unwrap() ?: return fail("Could not parse json")
        val actual = DeltaSyncPayload.fromJSON(json)
        assertThat<DeltaSyncPayload>(
            actual,
            iz(
                DeltaSyncPayload.NonDeleted(
                    note = remoteRichTextNote(
                        id
                    )
                )
            )
        )
    }
}
