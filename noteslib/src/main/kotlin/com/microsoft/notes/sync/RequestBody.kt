package com.microsoft.notes.sync

import com.microsoft.notes.sync.models.Document
import com.microsoft.notes.sync.models.localOnly.Note

object RequestBody {
    fun forCreateNote(note: Note): JSON =
        JSON.JObject(
            hashMapOf(
                document(note),
                color(note),
                createdWithLocalId(note),
                createdByApp(note.createdByApp)
            )
        )

    fun forUpdateNote(note: Note): JSON? {
        if (note.remoteData == null) return null

        val values = hashMapOf(
            color(note),
            changeKey(note.remoteData.changeKey),
            documentModifiedAt(note.documentModifiedAt)
        )
        if (note.document is Document.RichTextDocument || note.document is Document.InkDocument) {
            values += document(note)
        }

        return JSON.JObject(values)
    }

    fun forUpdateMediaAltText(note: Note, altText: String?): JSON? {
        if (note.remoteData == null) return null

        val values = hashMapOf(
            altText(altText),
            changeKey(note.remoteData.changeKey)
        )

        return JSON.JObject(values)
    }

    private fun changeKey(changeKey: String): Pair<String, JSON.JString> =
        "changeKey" to JSON.JString(changeKey)

    private fun document(note: Note): Pair<String, JSON.JObject> =
        "document" to note.document.toJSON()

    private fun color(note: Note): Pair<String, JSON.JNumber> =
        "color" to JSON.JNumber(note.color.value.toDouble())

    private fun createdWithLocalId(note: Note): Pair<String, JSON.JString> =
        "createdWithLocalId" to JSON.JString(note.id)

    private fun createdByApp(createdByApp: String?): Pair<String, JSON> =
        "createdByApp" to when (createdByApp) {
            null -> JSON.JNull()
            else -> JSON.JString(createdByApp)
        }

    private fun documentModifiedAt(documentModifiedAt: String?): Pair<String, JSON> =
        "documentModifiedAt" to when (documentModifiedAt) {
            null -> JSON.JNull()
            else -> JSON.JString(documentModifiedAt)
        }

    private fun altText(altText: String?): Pair<String, JSON> =
        "altText" to when (altText) {
            null -> JSON.JNull()
            else -> JSON.JString(altText)
        }
}
