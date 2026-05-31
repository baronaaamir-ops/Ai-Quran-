package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.QuranProgress
import com.example.data.model.*
import com.example.data.remote.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

sealed interface StudyUiState {
    object Idle : StudyUiState
    object Loading : StudyUiState
    data class Success(val data: QuranStudyData) : StudyUiState
    data class Error(val message: String) : StudyUiState
}

enum class ScreenState {
    WELCOME,
    DASHBOARD,
    DETAIL,
    PROGRESS_LOGS
}

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val progressDao = db.quranProgressDao()

    // 1. Language States
    private val _selectedLanguage = MutableStateFlow(Language.ENGLISH)
    val selectedLanguage: StateFlow<Language> = _selectedLanguage.asStateFlow()

    // 2. Navigation States
    private val _currentScreen = MutableStateFlow(ScreenState.WELCOME)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    // 3. Main Study UI State
    private val _studyUiState = MutableStateFlow<StudyUiState>(StudyUiState.Idle)
    val studyUiState: StateFlow<StudyUiState> = _studyUiState.asStateFlow()

    // 4. Progress database list
    private val _localProgressList = MutableStateFlow<List<QuranProgress>>(emptyList())
    val localProgressList: StateFlow<List<QuranProgress>> = _localProgressList.asStateFlow()

    // 5. Follow-up Chat state for current verse
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    // 6. Text to Speech TTS
    private var tts: TextToSpeech? = null
    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()

    // Current notes local draft
    val currentNotesDraft = MutableStateFlow("")

    // For Hifz / completed database toggling
    val isCurrentMemorized = MutableStateFlow(false)

    init {
        // Observe Room Progress Logs
        viewModelScope.launch {
            progressDao.getAllProgress().collect { list ->
                _localProgressList.value = list
            }
        }

        // Setup TTS
        try {
            tts = TextToSpeech(application) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // Default to selected language locale
                    updateTtsLocale()
                } else {
                    Log.e("QuranViewModel", "TTS Initialization failed")
                }
            }
        } catch (e: Exception) {
            Log.e("QuranViewModel", "Exception initializing TextToSpeech", e)
        }
    }

    fun selectLanguage(language: Language) {
        _selectedLanguage.value = language
        updateTtsLocale()
    }

    private fun updateTtsLocale() {
        val currentLang = _selectedLanguage.value
        val locale = when (currentLang) {
            Language.ENGLISH -> Locale.ENGLISH
            Language.URDU, Language.ARABIC -> Locale("ur", "PK") // Best approximate/Arabic voice support
            Language.HINDI -> Locale("hi", "IN")
            Language.TURKISH -> Locale("tr", "TR")
            Language.FRENCH -> Locale.FRENCH
            Language.SPANISH -> Locale("es", "ES")
            Language.INDONESIAN -> Locale("id", "ID")
        }
        tts?.language = locale
    }

    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
        if (screen == ScreenState.WELCOME) {
            stopSpeaking()
        }
    }

    // TTS Control
    fun speakText(text: String) {
        stopSpeaking()
        if (text.isNotBlank()) {
            _isTtsSpeaking.value = true
            try {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "QuranTtsSystem")
                // Periodically check if speaking has finished
                viewModelScope.launch {
                    try {
                        while (tts?.isSpeaking == true) {
                            kotlinx.coroutines.delay(500)
                        }
                    } catch (e: Exception) {
                        Log.e("QuranViewModel", "Error checking active speak state", e)
                    }
                    _isTtsSpeaking.value = false
                }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error initiating TTS speak", e)
                _isTtsSpeaking.value = false
            }
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("QuranViewModel", "Error stopping TTS playback", e)
        }
        _isTtsSpeaking.value = false
    }

    // API Call to analyze Surah or Ayah
    fun analyzeSurah(query: String) {
        if (query.isBlank()) return

        _studyUiState.value = StudyUiState.Loading
        _chatHistory.value = emptyList() // clear chat
        stopSpeaking()

        viewModelScope.launch {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
                _studyUiState.value = StudyUiState.Error("Gemini API key is not configured in Secrets list.")
                return@launch
            }

            try {
                val currentLang = _selectedLanguage.value
                val systemMessage = """
                    You are an Advanced AI Quran Assistant.
                    Your role is to provide authentic, deeply respectful, and educational Quranic guidance based only on verified Quranic text and trusted classical Islamic sources.
                    
                    The user's currently selected Language is ${currentLang.englishName}.
                    Hence: All explanations, historical contexts, tafsir, list of lessons, related hadiths explanation, daily life applications, hifz tips, voice-friendly summaries, quiz questions, learning progress advice, and self-reflection exercises MUST be generated beautifully in "${currentLang.englishName}" language.
                    Exception: Quranic verses must remain in original Arabic along with their translations in ${currentLang.englishName}.
                    The uUrdu explanation & Urdu translation MUST also be generated because they are explicit checkboxes requested by the product team.
                    
                    Respond strictly with valid JSON. Do not include any prefix or codeblocks wrapping the JSON. The JSON output must strictly parse against the standard Kotlin model properties.
                """.trimIndent()

                val promptText = """
                    Analyze Surah or Ayah: "$query".
                    Provide authentic, deeply educational materials. All explanations and quizzes must be in the "${currentLang.englishName}" language.
                    
                    You MUST return exactly a formatted JSON object. 
                    Ensure it contains these exact keys and has NO leading/trailing Markdown code blocks (e.g. do NOT wrap with ```json ... ```):
                    {
                      "surahName": "...",
                      "ayahRange": "...",
                      "arabicText": "...",
                      "urduTranslation": "Official complete Urdu translation of the verses requested",
                      "englishTranslation": "Official complete English translation of the verses requested",
                      "wordByWordMeaning": [
                        { "word": "Arabic word", "english": "English equivalent", "urdu": "Urdu equivalent" }
                      ],
                      "simpleUrduExplanation": "A short, simple explanation of these verses written beautifully in Urdu script",
                      "detailedTafsir": "Comprehensive Tafsir in ${currentLang.englishName} language based on trusted classic sources",
                      "historicalBackground": "Shan-e-Nuzool context in ${currentLang.englishName}",
                      "mainLessons": ["Lesson 1 in ${currentLang.englishName}", "Lesson 2 in ${currentLang.englishName}"],
                      "dailyLifeApplications": ["Practical action point 1", "Practical action point 2"],
                      "relatedQuranVerses": ["Reference (e.g. Al-Baqarah 2:2) followed by a short note in ${currentLang.englishName}"],
                      "relatedHadiths": ["Hadith reference (e.g. Bukhari 352) and short description in ${currentLang.englishName}"],
                      "duasConnected": [
                        { "arabicDua": "Dua in Arabic text", "translation": "Translation in ${currentLang.englishName}", "benefit": "The virtues of this dua in ${currentLang.englishName}" }
                      ],
                      "quizQuestions": [
                        { "question": "Question text in ${currentLang.englishName}", "options": ["Option 1", "Option 2", "Option 3", "Option 4"], "correctOptionIndex": 0, "explanation": "Why this is correct in ${currentLang.englishName}" }
                      ],
                      "voiceFriendlySummary": "A smooth, majestic audio reading summary in ${currentLang.englishName} specifically optimized for TTS audio text-to-speech engine",
                      "childFriendlyExplanation": "A simple story-based narrative explaining these verses for kids in ${currentLang.englishName}",
                      "advancedScholarExplanation": "Academic and linguist analysis (root words, rulings) in ${currentLang.englishName}",
                      "learningProgressSuggestions": ["Suggested milestone tracker 1", "Suggested milestone tracker 2"],
                      "memorizationTips": ["Step-by-step recitation guide for Hifz in ${currentLang.englishName}"],
                      "reflectionExercises": ["A deep meditative prompt or question to ask oneself in ${currentLang.englishName}"]
                    }
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemMessage))),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(key, request)
                }

                val jsonResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (jsonResponse != null) {
                    // Normalize JSON line to avoid markdown markers if any got returned despite the prompt
                    val cleanedJson = jsonResponse.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    Log.d("QuranViewModel", "Cleaned JSON: $cleanedJson")

                    val adapter = RetrofitClient.moshiParser.adapter(QuranStudyData::class.java)
                    val studyData = withContext(Dispatchers.Default) {
                        adapter.fromJson(cleanedJson)
                    }

                    if (studyData != null) {
                        _studyUiState.value = StudyUiState.Success(studyData)
                        _currentScreen.value = ScreenState.DETAIL

                        // Check if we already have progress logs or notes saved for this Surah in Room
                        val localProgress = progressDao.getProgressForSurah(studyData.surahName)
                        if (localProgress != null) {
                            currentNotesDraft.value = localProgress.notes
                            isCurrentMemorized.value = localProgress.isMemorized
                        } else {
                            currentNotesDraft.value = ""
                            isCurrentMemorized.value = false
                        }

                        // Add initial placeholder message to follow-up chat
                        _chatHistory.value = listOf(
                            ChatMessage(
                                sender = "ai",
                                text = when (currentLang) {
                                    Language.URDU -> "عرب اور قرآنی تصورات کے متعلق مجھ سے کوئی بھی سوال پوچھیں۔"
                                    Language.ARABIC -> "اسألني أي سؤال إضافي حول هذه الآيات الكريمة ومعانيها."
                                    else -> "Ask me any custom follow-up questions about this Surah/Ayah!"
                                }
                            )
                        )
                    } else {
                        _studyUiState.value = StudyUiState.Error("Parsing failure: JSON returned did not match QuranStudyData schema.")
                    }
                } else {
                    _studyUiState.value = StudyUiState.Error("API returned empty candidates.")
                }

            } catch (e: Exception) {
                _studyUiState.value = StudyUiState.Error("Error: ${e.message ?: "Unknown Connection Error"}")
                Log.e("QuranViewModel", "Exception analyzing Surah", e)
            }
        }
    }

    // Follow-up Q&A Assistant Session
    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        val state = _studyUiState.value
        if (state !is StudyUiState.Success) return

        // Add user message to state
        val updatedChat = _chatHistory.value.toMutableList()
        updatedChat.add(ChatMessage(sender = "user", text = userText))
        _chatHistory.value = updatedChat

        _chatLoading.value = true

        viewModelScope.launch {
            val key = BuildConfig.GEMINI_API_KEY
            try {
                val currentLang = _selectedLanguage.value
                val studyContext = """
                    Surah: ${state.data.surahName} (${state.data.ayahRange})
                    Arabic verses: ${state.data.arabicText}
                    Translation: ${state.data.englishTranslation}
                    Tafsir: ${state.data.detailedTafsir}
                """.trimIndent()

                val systemPrompt = """
                    You are the Advanced AI Quran Assistant.
                    You are replying to a follow-up question regarding the studied surah contextualized below:
                    $studyContext
                    
                    The user's currently selected language is ${currentLang.englishName}.
                    Respond beautifully, accurately, and respectfully entirely in "${currentLang.englishName}" language.
                    Quranic references must rely on trusted authentic traditions.
                """.trimIndent()

                // Compile previous messages as prompt context
                val conversationSummary = updatedChat.takeLast(10).joinToString("\n") { "${it.sender}: ${it.text}" }
                val promptText = """
                    Conversation History:
                    $conversationSummary
                    
                    Follow-up question: "$userText"
                    Provide a detailed, authentic and clear response in ${currentLang.englishName}. Include related verses or authentic hadiths if necessary. Remember to stay neutral between recognized schools of thought.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                    generationConfig = GenerationConfig(temperature = 0.5f)
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(key, request)
                }

                val aiResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                _chatLoading.value = false

                if (!aiResponse.isNullOrBlank()) {
                    val finalChat = _chatHistory.value.toMutableList()
                    finalChat.add(ChatMessage(sender = "ai", text = aiResponse))
                    _chatHistory.value = finalChat
                } else {
                    val finalChat = _chatHistory.value.toMutableList()
                    finalChat.add(ChatMessage(sender = "ai", text = "Sorry, I couldn't process that query. Please try again!"))
                    _chatHistory.value = finalChat
                }

            } catch (e: Exception) {
                _chatLoading.value = false
                val finalChat = _chatHistory.value.toMutableList()
                finalChat.add(ChatMessage(sender = "ai", text = "Connection error: ${e.message}"))
                _chatHistory.value = finalChat
            }
        }
    }

    // Save notes/memorization status locally into Room Database
    fun saveProgressNotes() {
        val uiStateVal = _studyUiState.value
        if (uiStateVal !is StudyUiState.Success) return

        val surah = uiStateVal.data.surahName
        val ayahRef = uiStateVal.data.ayahRange
        val notesText = currentNotesDraft.value
        val memValue = isCurrentMemorized.value
        val langCode = _selectedLanguage.value.code

        viewModelScope.launch {
            val existing = progressDao.getProgressForSurah(surah)
            if (existing != null) {
                val updated = existing.copy(
                    notes = notesText,
                    isMemorized = memValue,
                    timestamp = System.currentTimeMillis()
                )
                progressDao.updateProgress(updated)
            } else {
                val newProgress = QuranProgress(
                    surahName = surah,
                    ayahReference = ayahRef,
                    languageCode = langCode,
                    notes = notesText,
                    isMemorized = memValue
                )
                progressDao.insertProgress(newProgress)
            }
        }
    }

    // Record quiz score
    fun recordQuizScore(score: Int) {
        val uiStateVal = _studyUiState.value
        if (uiStateVal !is StudyUiState.Success) return

        val surah = uiStateVal.data.surahName
        val ayahRef = uiStateVal.data.ayahRange
        val langCode = _selectedLanguage.value.code

        viewModelScope.launch {
            val existing = progressDao.getProgressForSurah(surah)
            if (existing != null) {
                val updated = existing.copy(
                    quizScore = score,
                    timestamp = System.currentTimeMillis()
                )
                progressDao.updateProgress(updated)
            } else {
                val newProgress = QuranProgress(
                    surahName = surah,
                    ayahReference = ayahRef,
                    languageCode = langCode,
                    quizScore = score
                )
                progressDao.insertProgress(newProgress)
            }
        }
    }

    // Delete a local log entry
    fun deleteLocalProgressItem(item: QuranProgress) {
        viewModelScope.launch {
            progressDao.deleteProgress(item)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("QuranViewModel", "Error shutting down TTS service", e)
        }
    }
}
