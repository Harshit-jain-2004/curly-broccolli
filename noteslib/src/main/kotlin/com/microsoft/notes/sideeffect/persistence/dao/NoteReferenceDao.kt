package com.microsoft.notes.sideeffect.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.microsoft.notes.sideeffect.persistence.NoteReference

@Dao
interface NoteReferenceDao {

    @Insert
    fun insert(vararg notes: NoteReference)

    @Insert
    fun insert(notes: List<NoteReference>)

    @Update
    fun update(vararg notes: NoteReference)

    @Update
    fun update(notes: List<NoteReference>)

    @Delete
    fun delete(vararg notes: NoteReference)

    @Delete
    fun delete(notes: List<NoteReference>)

    @Query("DELETE FROM NoteReference WHERE id = :localId")
    fun deleteById(localId: String)

    @Query("SELECT * FROM NoteReference")
    fun getAll(): List<NoteReference>

    @Query("DELETE FROM NoteReference")
    fun deleteAll(): Int

    @Query("UPDATE NoteReference SET isDeleted = :isDeleted, isLocalOnlyPage = :isLocalOnlyPage WHERE id = :localId")
    fun markAsDeleted(localId: String, isDeleted: Boolean, isLocalOnlyPage: Boolean)

    @Query("UPDATE NoteReference SET media = :media WHERE id = :localId")
    fun updateMedia(localId: String, media: String)

    @Query("UPDATE NoteReference SET isPinned = :isPinned, pinnedAt = :pinnedAt WHERE id = :localId")
    fun setPinnedProperties(localId: String, isPinned: Boolean, pinnedAt: Long?)
}
