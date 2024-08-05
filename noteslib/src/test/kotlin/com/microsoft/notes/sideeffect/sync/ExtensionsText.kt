package com.microsoft.notes.sideeffect.sync

import org.junit.Assert.assertThat
import org.junit.Test
import com.microsoft.notes.models.Media as StoreMedia
import com.microsoft.notes.models.Note as StoreNote
import org.hamcrest.CoreMatchers.`is` as iz

class ExtensionsText {

    fun media(
        id: String,
        remoteId: String? = null,
        localUrl: String? = null
    ): StoreMedia {
        return StoreMedia(
            localId = id,
            remoteId = remoteId,
            localUrl = localUrl,
            mimeType = "image/png",
            altText = null,
            imageDimensions = null,
            lastModified = System.currentTimeMillis()
        )
    }

    @Test
    fun should_process_Media_that_need_download_in_List_of_Store_Note() {
        var mediaProcessed = 0
        val enqueueDownloadMockFn = { _: StoreNote, _: String, _: String -> mediaProcessed++; Unit }

        val media1 = media(id = "media1", localUrl = null, remoteId = "remote1")
        val media2 = media(id = "media2", localUrl = null, remoteId = "remote2")
        val note1 = StoreNote(localId = "note1", media = listOf(media1, media2))

        val media3 = media(id = "media3", localUrl = null, remoteId = "remote3")
        val media4 = media(id = "media4", localUrl = "local4", remoteId = null)
        val note2 = StoreNote(localId = "note1", media = listOf(media3, media4))

        listOf(note1, note2).processMediaNeedingDownload(enqueueDownload = enqueueDownloadMockFn)
        assertThat(mediaProcessed, iz(3))
    }

    @Test
    fun should_get_Media_that_need_download() {
        val media1 = media(id = "media1", localUrl = null, remoteId = "remote1")
        val media2 = media(id = "media2", localUrl = "local2", remoteId = "remote2")
        val media3 = media(id = "media3", localUrl = null, remoteId = "remote3")
        val media4 = media(id = "media4", localUrl = "local4", remoteId = null)

        val mediaThatNeedDownload = StoreNote(
            localId = "note2",
            media = listOf(media1, media2, media3, media4)
        ).mediaNeedingDownload()
        assertThat(mediaThatNeedDownload.count(), iz(2))
    }
}
