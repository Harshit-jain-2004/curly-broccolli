package com.microsoft.notes.sideeffect.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.microsoft.notes.sideeffect.persistence.Note

@Suppress("TooManyFunctions")
@Dao
interface NoteDao {

    @Insert
    fun insert(note: Note)

    @Insert
    fun insert(note: List<Note>)

    @Delete
    fun delete(notes: List<Note>)

    @Update
    fun update(notes: List<Note>)

    @Query("UPDATE Note SET color = :color WHERE id = :noteId")
    fun updateColor(noteId: String, color: Int)

    @Query("UPDATE Note SET reminder = :reminder WHERE id = :noteId")
    fun updateReminder(noteId: String, reminder: String)

    @Query("UPDATE Note SET isDeleted = :isDeleted WHERE id = :noteId")
    fun markNoteAsDeleted(noteId: String, isDeleted: Boolean)

    @Query("UPDATE Note SET documentModifiedAt = :documentModifiedAt WHERE id = :noteId")
    fun updateDocumentModifiedAt(noteId: String, documentModifiedAt: Long)

    @Query("UPDATE Note SET document = :document WHERE id = :noteId")
    fun updateDocument(noteId: String, document: String)

    @Query("UPDATE Note SET media = :media WHERE id = :noteId")
    fun updateMedia(noteId: String, media: String)

    @Query("UPDATE Note SET remoteData = :remoteData WHERE id = :noteId")
    fun updateRemoteData(noteId: String, remoteData: String)

    @Query("SELECT * FROM Note")
    fun getAll(): List<Note>

    @Query(
        "SELECT * FROM Note " +
            "ORDER BY documentModifiedAt DESC " +
            "LIMIT :limit " +
            "OFFSET :offset"
    )
    fun getNotes(limit: Int, offset: Int): List<Note>

    @Query("SELECT * FROM Note WHERE id = :noteId")
    fun getNoteById(noteId: String): Note?

    @Query(
        "SELECT * FROM Note " +
            "ORDER BY documentModifiedAt DESC " +
            "LIMIT :limit"
    )
    fun getFirstOrderByDocumentModifiedAt(limit: Int): List<Note>

    // multiple notes may have the same documentModifiedAt given how sync works and the fidelity of the timestamp
    // this query is designed to handle this case by overlapping with the previous page's documentModifiedAt
    // while also excluding any ids previously seen
    @Query(
        "SELECT * FROM Note " +
            "WHERE documentModifiedAt<=:documentModifiedAt " +
            "AND id NOT IN (:excludeIds) " +
            "ORDER BY documentModifiedAt DESC " +
            "LIMIT :limit"
    )
    fun getNextOrderByDocumentModifiedAt(limit: Int, documentModifiedAt: Long, excludeIds: List<String>): List<Note>

    @Query("DELETE FROM Note")
    fun deleteAll(): Int

    @Query("DELETE FROM Note WHERE id = :noteId")
    fun deleteNoteById(noteId: String)

    @Query("DELETE FROM Note WHERE createdByApp = :createdByApp")
    fun deleteNoteByAppCreated(createdByApp: String)

    @Query("DELETE FROM Note WHERE id in (SELECT id FROM Note ORDER BY documentModifiedAt DESC LIMIT :limit OFFSET :offset)")
    fun deleteNotes(limit: Int, offset: Int)

    @Query("UPDATE Note SET isPinned = :isPinned, pinnedAt = :pinnedAt WHERE id = :localId")
    fun setPinnedProperties(localId: String, isPinned: Boolean, pinnedAt: Long?)
}
