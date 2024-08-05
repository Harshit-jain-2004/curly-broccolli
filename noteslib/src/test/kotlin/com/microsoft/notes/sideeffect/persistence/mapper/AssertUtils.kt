package com.microsoft.notes.sideeffect.persistence.mapper

import com.google.gson.Gson
import com.microsoft.notes.sideeffect.persistence.extensions.fromDocumentJson
import com.microsoft.notes.sideeffect.persistence.extensions.fromMediaJson
import com.microsoft.notes.sideeffect.persistence.extensions.toJson
import org.junit.Assert.assertThat
import com.microsoft.notes.models.Media as StoreMedia
import com.microsoft.notes.models.Note as StoreNote
import com.microsoft.notes.models.RemoteData as StoreRemoteData
import com.microsoft.notes.richtext.scheme.Document as StoreDocument
import com.microsoft.notes.sideeffect.persistence.Note as PersistenceNote
import org.hamcrest.CoreMatchers.`is` as iz

object AssertUtils {

    fun assert_Note(persistenceNote: PersistenceNote, storeNote: StoreNote) {
        with(persistenceNote) {
            assertThat(id, iz(storeNote.localId))
            assertThat(isDeleted, iz(storeNote.isDeleted))
            assertThat(color, iz(storeNote.color.value))
            assertThat(localCreatedAt, iz(storeNote.localCreatedAt))
            assertThat(documentModifiedAt, iz(storeNote.documentModifiedAt))
            assert_Document(
                document,
                storeNote.document
            )
            assert_Media(media, storeNote.media)
            assert_RemoteData(
                remoteData,
                storeNote.remoteData
            )
        }
    }

    fun assert_Media(persistenceMedia: String, storeMedia: List<StoreMedia>) {
        assertThat(persistenceMedia, iz(storeMedia.toJson()))
        val mediaList = persistenceMedia.fromMediaJson()
        assertThat(mediaList, iz(storeMedia))
    }

    fun assert_Document(persistenceDocument: String, storeDocument: StoreDocument) {
        assertThat(persistenceDocument, iz(storeDocument.toJson()))
        val documentObj = persistenceDocument.fromDocumentJson()
        assertThat(documentObj, iz(storeDocument))
    }

    fun assert_RemoteData(persistenceRemoteData: String?, remoteData: StoreRemoteData?) {
        assertThat(persistenceRemoteData, iz(remoteData?.toJson()))
        val remoteDataObj = Gson().fromJson(persistenceRemoteData, StoreRemoteData::class.java)
        assertThat(remoteDataObj, iz(remoteData))
    }
}
