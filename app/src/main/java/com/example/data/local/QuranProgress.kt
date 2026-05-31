package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quran_progress")
data class QuranProgress(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surahName: String,
    val ayahReference: String = "",
    val languageCode: String,
    val notes: String = "",
    val quizScore: Int = -1,
    val isMemorized: Boolean = false,
    val isCompleted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
