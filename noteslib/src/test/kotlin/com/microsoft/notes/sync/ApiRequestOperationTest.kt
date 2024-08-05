package com.microsoft.notes.sync

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidDeleteNote
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidUpdateNote
import com.microsoft.notes.sync.ApiRequestOperation.InvalidApiRequestOperation.InvalidUploadMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.CreateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DeleteNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.DownloadMedia
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.GetNoteForMerge
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.Sync
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UpdateNote
import com.microsoft.notes.sync.ApiRequestOperation.ValidApiRequestOperation.UploadMedia
import com.microsoft.notes.sync.models.Token
import com.microsoft.notes.sync.models.localOnly.Note
import com.microsoft.notes.sync.models.localOnly.RemoteData
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

val jsonParser: Gson by lazy { Gson() }

class ApiRequestOperationTest {
    val remoteData = RemoteData(
        id = "remoteId", changeKey = "somechangekey",
        lastServerVersion = remoteRichTextNote("remoteId"), createdAt = "2018-01-31T16:45:05.0000000Z",
        lastModifiedAt = "2018-01-31T16:50:31.0000000Z"
    )

    val note = Note(
        id = "id", remoteData = remoteData, document = richTextDocument(),
        color = Note.Color.BLUE, createdByApp = "Notes Android",
        documentModifiedAt = "2018-02-01T16:45:05.0000000Z",
        // todo implement
        media = listOf(),
        metadata = testRemoteNoteMetadata()
    )

    @Test
    fun syncRequestOperation_serialization_is_reversible() {
        val syncRequestOperation = Sync(deltaToken = null)

        val serializedSyncRequestOperation = jsonParser.toJson(syncRequestOperation)
        val deserializedMap = deserializeIntoMap(serializedSyncRequestOperation)
        val deserializedSyncRequestOperation = ApiRequestOperation.fromMap(
            deserializedMap
        ) as Sync
        assertThat(deserializedSyncRequestOperation.deltaToken, iz(syncRequestOperation.deltaToken))

        val syncRequestOperationWithDeltaToken = Sync(deltaToken = Token.Delta("abcd"))
        val serializedSyncRequestOperationWithDeltaToken = jsonParser.toJson(syncRequestOperationWithDeltaToken)
        val deserializedMapWithDeltaToken = deserializeIntoMap(serializedSyncRequestOperationWithDeltaToken)
        val deserializedSyncRequestOperationWithDeltaToken = ApiRequestOperation.fromMap(
            deserializedMapWithDeltaToken
        ) as Sync
        assertThat(
            deserializedSyncRequestOperationWithDeltaToken.deltaToken,
            iz(syncRequestOperationWithDeltaToken.deltaToken)
        )
    }

    @Test
    fun createNote_serialization_is_reversible() {
        val createNoteOperation = CreateNote(note = note)

        val serializedCreateNote = jsonParser.toJson(createNoteOperation)
        val deserializedMap = jsonParser.fromJson<Map<String, Any>>(
            serializedCreateNote,
            object : TypeToken<Map<String, Any>>() {}.type
        )
        val deserializedCreateNote = ApiRequestOperation.fromMap(
            deserializedMap
        ) as CreateNote
        assertThat(deserializedCreateNote.note, iz(createNoteOperation.note))
    }

    @Test
    fun updateNote_serialization_is_reversible() {
        val updateNoteOperation = UpdateNote(note = note, uiBaseRevision = 20)
        val serializedUpdateNote = jsonParser.toJson(updateNoteOperation)
        val deserializedMap = deserializeIntoMap(serializedUpdateNote)
        val deserializedUpdateNote = ApiRequestOperation.fromMap(
            deserializedMap
        ) as UpdateNote
        assertThat(deserializedUpdateNote.note, iz(updateNoteOperation.note))
        assertThat(deserializedUpdateNote.uiBaseRevision, iz(updateNoteOperation.uiBaseRevision))
    }

    @Test
    fun deleteNote_serialization_is_reversible() {
        val operation = DeleteNote(localId = "localId", remoteId = "remoteId")
        val serializedOperation = jsonParser.toJson(operation)
        val deserializedMap = deserializeIntoMap(serializedOperation)
        val deserializedOperation = ApiRequestOperation.fromMap(
            deserializedMap
        ) as DeleteNote
        assertThat(deserializedOperation.remoteId, iz(operation.remoteId))
        assertThat(deserializedOperation.localId, iz(operation.localId))
    }

    @Test
    fun getNoteForMerge_serialization_is_reversible() {
        val operation = GetNoteForMerge(note = note, uiBaseRevision = 1)
        val serializedOperation = jsonParser.toJson(operation)
        val deserializedMap = deserializeIntoMap(serializedOperation)
        val deserializedOperation = ApiRequestOperation.fromMap(
            deserializedMap
        ) as GetNoteForMerge
        assertThat(deserializedOperation.note, iz(operation.note))
        assertThat(deserializedOperation.uiBaseRevision, iz(operation.uiBaseRevision))
    }

    @Test
    fun uploadMedia_serialization_is_reversible() {
        val uploadMediaOperation = UploadMedia(
            note = note, mimeType = "image/png",
            mediaLocalId = "media_block_id", localUrl = "file://data/user/0/xyz.png"
        )
        val serializedUploadMediaWithLocalUrl = jsonParser.toJson(uploadMediaOperation)
        val deserializedMap = deserializeIntoMap(serializedUploadMediaWithLocalUrl)
        val deserializedUploadMediaWithLocalURL = ApiRequestOperation.fromMap(
            deserializedMap
        ) as UploadMedia
        assertThat(deserializedUploadMediaWithLocalURL.note, iz(uploadMediaOperation.note))
        assertThat(deserializedUploadMediaWithLocalURL.mimeType, iz(uploadMediaOperation.mimeType))
        assertThat(deserializedUploadMediaWithLocalURL.mediaLocalId, iz(uploadMediaOperation.mediaLocalId))
        assertThat(deserializedUploadMediaWithLocalURL.localUrl, iz(uploadMediaOperation.localUrl))
    }

    @Test
    fun downloadMedia_serialization_is_reversible() {
        val operation = DownloadMedia(
            note = note, mediaRemoteId = "mediaId",
            mimeType = "image/png"
        )
        val serializedOperation = jsonParser.toJson(operation)
        val deserializedMap = deserializeIntoMap(serializedOperation)
        val deserializedOperation = ApiRequestOperation.fromMap(
            deserializedMap
        ) as DownloadMedia
        assertThat(deserializedOperation.note, iz(operation.note))
        assertThat(deserializedOperation.mediaRemoteId, iz(operation.mediaRemoteId))
        assertThat(deserializedOperation.mimeType, iz(operation.mimeType))
    }

    @Test
    fun invalidUpdateNote_serialization_is_reversible() {
        val operation = InvalidUpdateNote(note = note, uiBaseRevision = 20)
        val serializedUpdateNote = jsonParser.toJson(operation)
        val deserializedMap = deserializeIntoMap(serializedUpdateNote)
        val deserializedUpdateNote = ApiRequestOperation.fromMap(
            deserializedMap
        ) as InvalidUpdateNote
        assertThat(deserializedUpdateNote.note, iz(operation.note))
        assertThat(deserializedUpdateNote.uiBaseRevision, iz(operation.uiBaseRevision))
    }

    @Test
    fun invalidDeleteNote_serialization_is_reversible() {
        val operation = InvalidDeleteNote(localId = "localId")
        val serializedOperation = jsonParser.toJson(operation)
        val deserializedMap = deserializeIntoMap(serializedOperation)
        val deserializedOperation = ApiRequestOperation.fromMap(
            deserializedMap
        ) as InvalidDeleteNote
        assertThat(deserializedOperation.localId, iz(operation.localId))
    }

    @Test
    fun invalidUploadMedia_serialization_is_reversible() {
        val invalidUploadMediaOperation = InvalidUploadMedia(
            note = note, mimeType = "image/png",
            mediaLocalId = "media_local_id", localUrl = "file://data/user/0/xyz.png"
        )
        val serializedUploadMediaWithLocalUrl = jsonParser.toJson(invalidUploadMediaOperation)
        val deserializedMap = deserializeIntoMap(serializedUploadMediaWithLocalUrl)
        val deserializedUploadMediaWithLocalURL = ApiRequestOperation.fromMap(
            deserializedMap
        ) as InvalidUploadMedia
        assertThat(deserializedUploadMediaWithLocalURL.note, iz(invalidUploadMediaOperation.note))
        assertThat(deserializedUploadMediaWithLocalURL.mimeType, iz(invalidUploadMediaOperation.mimeType))
        assertThat(deserializedUploadMediaWithLocalURL.mediaLocalId, iz(invalidUploadMediaOperation.mediaLocalId))
        assertThat(deserializedUploadMediaWithLocalURL.localUrl, iz(invalidUploadMediaOperation.localUrl))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun should_migrate_from_1_to_current_correctly() {
        val original = deserializeIntoMap(serializedUpdateNoteV1)
        val migrated = ApiRequestOperation.migrate(
            original, 1,
            ApiRequestOperation.SCHEMA_VERSION
        )
        assertThat(migrated, iz(CoreMatchers.instanceOf(Map::class.java)))

        val updateNoteOperation = ApiRequestOperation.fromMap(
            migrated as Map<String, Any>
        )
        assertThat<ApiRequestOperation>(updateNoteOperation, iz(not(nullValue())))
    }

    private fun deserializeIntoMap(serializedOperation: String): Map<String, Any> {
        return jsonParser.fromJson<Map<String, Any>>(
            serializedOperation,
            object : TypeToken<Map<String, Any>>() {}.type
        )
    }

    // V1 didn't have note relative media source URL
    private val serializedUpdateNoteV1 = """
        {
            "note": {
                "color": "YELLOW",
                "createdByApp": "OneNote",
                "document": {
                    "blocks": [
                        {
                            "blockStyles": {
                                "rightToLeft": false,
                                "unorderedList": false
                            },
                            "content": [
                                {
                                    "inlineStyles": [],
                                    "string": "Hello",
                                    "type": "RichText"
                                }
                            ],
                            "id": "AF868DA3-78FC-4C7D-8472-F4165C9DF077",
                            "type": "Paragraph"
                        },
                        {
                            "id": "CD9B49E2-25FA-47FD-A529-98D912CC13D4",
                            "mimeType": "image/jpeg",
                            "source": "/media/cid-CD9B49E2-25FA-47FD-A529-98D912CC13D4",
                            "type": "Media"
                        },
                        {
                            "blockStyles": {
                                "rightToLeft": false,
                                "unorderedList": true
                            },
                            "content": [
                                {
                                    "inlineStyles": [],
                                    "string": "Thanks again for u",
                                    "type": "RichText"
                                }
                            ],
                            "id": "F42E776B-2ED8-453B-A212-2D37B11E2615",
                            "type": "Paragraph"
                        },
                        {
                            "blockStyles": {
                                "rightToLeft": false,
                                "unorderedList": true
                            },
                            "content": [
                                {
                                    "inlineStyles": [
                                        "Bold"
                                    ],
                                    "string": "I will be",
                                    "type": "RichText"
                                },
                                {
                                    "inlineStyles": [
                                        "Bold",
                                        "Italic"
                                    ],
                                    "string": " there in town and",
                                    "type": "RichText"
                                }
                            ],
                            "id": "localId_028f161f25934a61b3553d79dfc11cb4",
                            "type": "Paragraph"
                        },
                        {
                            "blockStyles": {
                                "rightToLeft": false,
                                "unorderedList": true
                            },
                            "content": [
                                {
                                    "inlineStyles": [
                                        "Bold",
                                        "Italic",
                                        "Underlined"
                                    ],
                                    "string": "I can do a room",
                                    "type": "RichText"
                                }
                            ],
                            "id": "localId_bdc9022df7e7427ea83599584bf52344",
                            "type": "Paragraph"
                        },
                        {
                            "blockStyles": {
                                "rightToLeft": false,
                                "unorderedList": true
                            },
                            "content": [
                                {
                                    "inlineStyles": [
                                        "Bold",
                                        "Italic",
                                        "Underlined"
                                    ],
                                    "string": "I will",
                                    "type": "RichText"
                                },
                                {
                                    "inlineStyles": [
                                        "Bold",
                                        "Italic",
                                        "Underlined",
                                        "Strikethrough"
                                    ],
                                    "string": " be at your place by cell",
                                    "type": "RichText"
                                },
                                {
                                    "inlineStyles": [
                                        "Strikethrough"
                                    ],
                                    "string": " number so I will be at your",
                                    "type": "RichText"
                                }
                            ],
                            "id": "localId_8925b00787be4e3b9162f78f28016b01",
                            "type": "Paragraph"
                        },
                        {
                            "blockStyles": {
                                "rightToLeft": false,
                                "unorderedList": true
                            },
                            "content": [
                                {
                                    "inlineStyles": [
                                        "Underlined"
                                    ],
                                    "string": "I will see you tomorrow and b",
                                    "type": "RichText"
                                }
                            ],
                            "id": "localId_78d2c9b97d904e17af4ec129e6439714",
                            "type": "Paragraph"
                        },
                        {
                            "blockStyles": {
                                "rightToLeft": false,
                                "unorderedList": true
                            },
                            "content": [
                                {
                                    "inlineStyles": [
                                        "Underlined"
                                    ],
                                    "string": "Hello",
                                    "type": "RichText"
                                }
                            ],
                            "id": "localId_a03e01d704b64f4e888208da105fae54",
                            "type": "Paragraph"
                        }
                    ],
                    "type": "RICH_TEXT"
                },
                "id": "localId_aadb85617fc64844a912281720d7edd4",
                "remoteData": {
                    "changeKey": "CQAAABYAAAA7WcMd3DtiTK92aJG+eG+XAAEtXjmZ",
                    "createdAt": "2018-05-17T17:16:15.0000000Z",
                    "id": "AAkALgAAAAAAHYQDEapmEc2byACqAC-EWg0AO1nDHdw7YkyvdmiRvnhvlwABJ7WFdgAA",
                    "lastModifiedAt": "2018-05-24T15:10:56.0000000Z"
                }
            },
            "type": "UpdateNote",
            "uiBaseRevision": 6
        }
    """
}
