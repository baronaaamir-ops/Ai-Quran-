package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuranStudyData(
    val surahName: String = "",
    val ayahRange: String = "",
    val arabicText: String = "",
    val urduTranslation: String = "",
    val englishTranslation: String = "",
    val wordByWordMeaning: List<WordByWord> = emptyList(),
    val simpleUrduExplanation: String = "",
    val detailedTafsir: String = "",
    val historicalBackground: String = "",
    val mainLessons: List<String> = emptyList(),
    val dailyLifeApplications: List<String> = emptyList(),
    val relatedQuranVerses: List<String> = emptyList(),
    val relatedHadiths: List<String> = emptyList(),
    val duasConnected: List<ConnectedDua> = emptyList(),
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val voiceFriendlySummary: String = "",
    val childFriendlyExplanation: String = "",
    val advancedScholarExplanation: String = "",
    val learningProgressSuggestions: List<String> = emptyList(),
    val memorizationTips: List<String> = emptyList(),
    val reflectionExercises: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WordByWord(
    val word: String = "",
    val urdu: String = "",
    val english: String = ""
)

@JsonClass(generateAdapter = true)
data class ConnectedDua(
    val arabicDua: String = "",
    val translation: String = "",
    val benefit: String = ""
)

@JsonClass(generateAdapter = true)
data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val explanation: String = ""
)
