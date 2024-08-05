package com.microsoft.notes.sideeffect.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.microsoft.notes.sideeffect.persistence.Preference

@Dao
interface PreferencesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(pair: Preference)

    @Query("SELECT value FROM Preference WHERE id = :id")
    fun get(id: String): String?

    @Query("DELETE FROM Preference WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM Preference")
    fun deleteAll(): Int
}
