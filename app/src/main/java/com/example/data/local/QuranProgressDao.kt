package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranProgressDao {
    @Query("SELECT * FROM quran_progress ORDER BY timestamp DESC")
    fun getAllProgress(): Flow<List<QuranProgress>>

    @Query("SELECT * FROM quran_progress WHERE surahName = :surahName LIMIT 1")
    suspend fun getProgressForSurah(surahName: String): QuranProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: QuranProgress)

    @Update
    suspend fun updateProgress(progress: QuranProgress)

    @Delete
    suspend fun deleteProgress(progress: QuranProgress)

    @Query("DELETE FROM quran_progress")
    suspend fun clearAll()
}
