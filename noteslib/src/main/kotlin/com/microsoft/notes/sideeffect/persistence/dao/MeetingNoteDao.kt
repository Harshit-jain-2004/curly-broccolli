package com.microsoft.notes.sideeffect.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.microsoft.notes.sideeffect.persistence.MeetingNote

@Dao
interface MeetingNoteDao {

    @Insert
    fun insert(vararg notes: MeetingNote)

    @Insert
    fun insert(notes: List<MeetingNote>)

    @Update
    fun update(vararg notes: MeetingNote)

    @Update
    fun update(notes: List<MeetingNote>)

    @Delete
    fun delete(vararg notes: MeetingNote)

    @Delete
    fun delete(notes: List<MeetingNote>)

    @Query("DELETE FROM MeetingNote WHERE localId = :localId")
    fun deleteById(localId: String)

    @Query("DELETE FROM MeetingNote")
    fun deleteAll(): Int

    @Query("SELECT * FROM MeetingNote")
    fun getAll(): List<MeetingNote>
}
