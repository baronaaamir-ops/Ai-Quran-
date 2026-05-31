package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.QuranProgress
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.viewmodel.QuranViewModel
import com.example.viewmodel.ScreenState
import com.example.viewmodel.StudyUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranAssistantApp(viewModel: QuranViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (currentScreen != ScreenState.WELCOME) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Quran AI",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    navigationIcon = {
                        if (currentScreen == ScreenState.DETAIL || currentScreen == ScreenState.PROGRESS_LOGS) {
                            IconButton(
                                onClick = {
                                    viewModel.stopSpeaking()
                                    viewModel.navigateTo(ScreenState.DASHBOARD)
                                },
                                modifier = Modifier.testTag("back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    actions = {
                        // Language changer always available on Top bar once set
                        IconButton(
                            onClick = {
                                viewModel.stopSpeaking()
                                viewModel.navigateTo(ScreenState.WELCOME)
                            },
                            modifier = Modifier.testTag("change_lang_top_button")
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Language,
                                contentDescription = "Change Language",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ScreenState.WELCOME -> WelcomeScreen(viewModel)
                ScreenState.DASHBOARD -> DashboardScreen(viewModel)
                ScreenState.DETAIL -> DetailScreen(viewModel)
                ScreenState.PROGRESS_LOGS -> ProgressScreen(viewModel)
            }
        }
    }
}

// ---------------- Welcome & Language Selection Screen ----------------
@Composable
fun WelcomeScreen(viewModel: QuranViewModel) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("welcome_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Aesthetic Lantern / Mosque silhouette using canvas draw decoration
        Box(
            modifier = Modifier
                .size(130.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AntiqueGold.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width / 1.1f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = "Quran Light",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = selectedLanguage.welcomeMessage,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = selectedLanguage.selectText,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Grid of Languages
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Language.entries.chunked(2).forEach { rowLanguages ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowLanguages.forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        OutlinedButton(
                            onClick = { viewModel.selectLanguage(lang) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("lang_btn_${lang.code}"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = lang.nativeName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = lang.englishName,
                                        fontSize = 10.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { viewModel.navigateTo(ScreenState.DASHBOARD) },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(54.dp)
                .testTag("welcome_enter_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Continue / آگے بڑھیں",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}


// ---------------- Dashboard & Home Screen ----------------
@Composable
fun DashboardScreen(viewModel: QuranViewModel) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val localProgressList by viewModel.localProgressList.collectAsStateWithLifecycle()
    val studyUiState by viewModel.studyUiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
    ) {
        // Dynamic Greeting Canvas Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(IslamicEmerald, IslamicEmerald.copy(alpha = 0.85f))
                        )
                    )
                }
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column {
                Text(
                    text = selectedLanguage.welcomeMessage,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "AI Quran Assistant • Sunday, May 31, 2026",
                    fontSize = 13.sp,
                    color = AntiqueGold,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Study Search / Reference Form
            Text(
                text = "Seek Quranic Guidance / سورہ یا آیت تلاش کریں",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(text = selectedLanguage.searchPlaceholder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        if (searchQuery.isNotBlank()) {
                            viewModel.analyzeSurah(searchQuery)
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            if (searchQuery.isNotBlank()) {
                                viewModel.analyzeSurah(searchQuery)
                            }
                        },
                        modifier = Modifier.testTag("search_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.35f)
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Popular recommendations shortcuts
            Text(
                text = selectedLanguage.suggestSurahMsg,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            val recommendations = listOf(
                "Al-Fatiha" to "الفاتحة",
                "Ayat-al-Kursi (2:255)" to "آية الكرسي",
                "Al-Ikhlas" to "الإخلاص",
                "Al-Mulk" to "الملك",
                "Al-Kahf" to "الكهف",
                "An-Nas" to "الناس",
                "Surah Ya-Sin" to "يس"
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(recommendations) { (eng, arabic) ->
                    Card(
                        modifier = Modifier
                            .clickable {
                                searchQuery = eng
                                viewModel.analyzeSurah(eng)
                            }
                            .testTag("popular_${eng.replace(" ", "_")}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = arabic,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = eng,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Loading state banner
            if (studyUiState is StudyUiState.Loading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("loading_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing verses from authentic classical Tafsirs in progress...",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Please wait, preparing structured translation, lessons, and quizzes.",
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Error display
            if (studyUiState is StudyUiState.Error) {
                StudyErrorCard(errorState = studyUiState as StudyUiState.Error)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Saved logs or history from Room
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLanguage.progressLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (localProgressList.isNotEmpty()) {
                    TextButton(onClick = { viewModel.navigateTo(ScreenState.PROGRESS_LOGS) }) {
                        Text("View Journal (${localProgressList.size})", color = AntiqueGold, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (localProgressList.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Gray.copy(alpha = 0.05f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Empty History",
                            tint = Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No study logs recorded yet.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    localProgressList.take(3).forEach { log ->
                        ProgressRowItem(log = log, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun StudyErrorCard(errorState: StudyUiState.Error) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("error_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Connection Warning",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Request Failed",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorState.message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Please check your network and make sure a valid API key is set in AI Studio's Secrets Manager.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ProgressRowItem(log: QuranProgress, viewModel: QuranViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.analyzeSurah(log.surahName) }
            .testTag("log_row_${log.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (log.isMemorized) Icons.Default.Star else Icons.Default.Book,
                    contentDescription = "History Item icon",
                    tint = if (log.isMemorized) AntiqueGold else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = log.surahName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 14.sp
                    )
                    if (log.notes.isNotBlank()) {
                        Text(
                            text = log.notes,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (log.quizScore >= 0) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Score: ${log.quizScore}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(
                    onClick = { viewModel.deleteLocalProgressItem(log) },
                    modifier = Modifier.testTag("delete_log_${log.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete from local progress list",
                        modifier = Modifier.size(20.dp),
                        tint = Color.Red.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}


// ---------------- Quran Study Analysis Detail Screen ----------------
@Composable
fun DetailScreen(viewModel: QuranViewModel) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val studyUiState by viewModel.studyUiState.collectAsStateWithLifecycle()
    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsStateWithLifecycle()
    val currentNotesDraft by viewModel.currentNotesDraft.collectAsStateWithLifecycle()
    val isCurrentMemorized by viewModel.isCurrentMemorized.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (studyUiState !is StudyUiState.Success) {
        // Fallback in case of invalid UI states
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val studyData = (studyUiState as StudyUiState.Success).data
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        "Translation",
        "Tafsir",
        "Reflections",
        "Hifz & Kids",
        "Quiz Mode",
        "AI Q&A Chat"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("detail_screen")
    ) {
        // Quran Title Header block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(IslamicEmerald)
                    drawLine(
                        color = AntiqueGold,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 4f
                    )
                }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = studyData.surahName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "Ayah Range: ${studyData.ayahRange}",
                        fontSize = 12.sp,
                        color = AntiqueGold,
                        fontWeight = FontWeight.Medium
                    )
                }

                // TTS Floating volume button
                IconButton(
                    onClick = {
                        if (isTtsSpeaking) {
                            viewModel.stopSpeaking()
                        } else {
                            viewModel.speakText(studyData.voiceFriendlySummary)
                        }
                    },
                    modifier = Modifier
                        .background(
                            if (isTtsSpeaking) AntiqueGold else Color.White.copy(alpha = 0.15f),
                            CircleShape
                        )
                        .testTag("tts_reader_button")
                ) {
                    Icon(
                        imageVector = if (isTtsSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Read Audio Aloud",
                        tint = if (isTtsSpeaking) Color.Black else Color.White
                    )
                }
            }
        }

        // Horizontal tabs implementation
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = AntiqueGold
                )
            },
            modifier = Modifier.testTag("detail_tabs")
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    modifier = Modifier.testTag("tab_button_$index"),
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Active layout contents
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeTab) {
                0 -> TranslationTab(studyData, selectedLanguage)
                1 -> TafsirTab(studyData, selectedLanguage)
                2 -> ReflectionsTab(studyData, selectedLanguage, viewModel, currentNotesDraft, isCurrentMemorized)
                3 -> HifzAndKidsTab(studyData, selectedLanguage)
                4 -> QuizTab(studyData, selectedLanguage, viewModel)
                5 -> ChatTab(studyData, selectedLanguage, viewModel)
            }
        }
    }
}

// ---------------- TAB 0: Translations & Word-By-Word ----------------
@Composable
fun TranslationTab(studyData: QuranStudyData, selectedLanguage: Language) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("tab_translation")
    ) {
        // Arabic Text Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "القرآن الكريم",
                        fontSize = 12.sp,
                        color = AntiqueGold,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Decoration Star",
                        tint = AntiqueGold,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = studyData.arabicText,
                    fontSize = 25.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Right,
                    lineHeight = 42.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }

        // Urdu Translation Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftCream)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "اردو ترجمہ (Urdu Translation)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = studyData.urduTranslation,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.tertiary,
                    lineHeight = 26.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // English Translation Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "English Translation",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = studyData.englishTranslation,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    lineHeight = 22.sp
                )
            }
        }

        // Word-By-Word Grid Module
        Text(
            text = selectedLanguage.wordByWordLabel,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 10.dp, top = 4.dp)
        )

        if (studyData.wordByWordMeaning.isEmpty()) {
            Text(
                text = "No word breakdown available for this selection.",
                fontSize = 13.sp,
                color = Color.Gray,
                fontStyle = FontStyle.Italic
            )
        } else {
            // Horizontal or flow list of word-breaks
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(studyData.wordByWordMeaning) { wordItem ->
                    Card(
                        modifier = Modifier.width(135.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = wordItem.word,
                                fontSize = 21.sp,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "En: ${wordItem.english}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                text = "أرد: ${wordItem.urdu}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB 1: Detailed classical Tafsir & Context ----------------
@Composable
fun TafsirTab(studyData: QuranStudyData, selectedLanguage: Language) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("tab_tafsir")
    ) {
        // Simple Urdu Explanation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "آسان اردو تشریح (Simple Urdu Explanation)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueGold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = studyData.simpleUrduExplanation,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Detailed Tafsir Body
        Text(
            text = selectedLanguage.tafsirLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, top = 6.dp)
        )
        Text(
            text = studyData.detailedTafsir,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.tertiary,
            lineHeight = 23.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Historical context / Shan-e-Nuzool
        Text(
            text = selectedLanguage.historicalLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.04f))
        ) {
            Text(
                text = studyData.historicalBackground,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.tertiary,
                lineHeight = 22.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}


// ---------------- TAB 2: Lessons, Actions, & self-reflection notes ----------------
@Composable
fun ReflectionsTab(
    studyData: QuranStudyData,
    selectedLanguage: Language,
    viewModel: QuranViewModel,
    notesDraft: String,
    isMemorized: Boolean
) {
    val context = LocalContext.current
    var personalThoughts by remember { mutableStateOf(notesDraft) }
    var isHifzModeChecked by remember { mutableStateOf(isMemorized) }

    // Sync draft updates locally
    LaunchedEffect(notesDraft, isMemorized) {
        personalThoughts = notesDraft
        isHifzModeChecked = isMemorized
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("tab_reflections")
    ) {
        // Visual core lessons
        Text(
            text = selectedLanguage.keyLessonsLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        studyData.mainLessons.forEachIndexed { i, lesson ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    color = AntiqueGold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = lesson,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Daily life applications
        Text(
            text = selectedLanguage.dailyActionLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        studyData.dailyLifeApplications.forEach { appText ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Action Bullet",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = appText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Related Quran verses & Hadith
        Text(
            text = "Related Scriptures & Traditions",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        if (studyData.relatedHadiths.isNotEmpty()) {
            studyData.relatedHadiths.forEach { h ->
                Text(text = "Hadith: $h", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        if (studyData.relatedQuranVerses.isNotEmpty()) {
            studyData.relatedQuranVerses.forEach { v ->
                Text(text = "Verse: $v", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Connected Dua array
        if (studyData.duasConnected.isNotEmpty()) {
            Text(
                text = "Connected Prayers (Duas)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            studyData.duasConnected.forEach { dua ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCream)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = dua.arabicDua,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Trans: " + dua.translation,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        if (dua.benefit.isNotBlank()) {
                            Text(
                                text = "Benefit: " + dua.benefit,
                                fontSize = 11.sp,
                                color = AntiqueGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))

        // Personal reflection exercise journaling text field
        Text(
            text = selectedLanguage.savedNotesLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = selectedLanguage.reflectionLabel,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        studyData.reflectionExercises.forEach { med ->
            Text(
                text = med,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = personalThoughts,
            onValueChange = {
                personalThoughts = it
                viewModel.currentNotesDraft.value = it
            },
            placeholder = { Text(text = selectedLanguage.notePlaceholder) },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("notes_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isHifzModeChecked,
                    onCheckedChange = {
                        isHifzModeChecked = it
                        viewModel.isCurrentMemorized.value = it
                    },
                    modifier = Modifier.testTag("memorized_checkbox")
                )
                Text(
                    text = "Mark as Memorized (Hifz)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = {
                    viewModel.saveProgressNotes()
                    Toast.makeText(context, "Progress Saved Offline!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("save_notes_button")
            ) {
                Text(text = selectedLanguage.saveNotesBtn, fontSize = 13.sp)
            }
        }
    }
}


// ---------------- TAB 3: Memorization Mode & Kids story explanation ----------------
@Composable
fun HifzAndKidsTab(studyData: QuranStudyData, selectedLanguage: Language) {
    var isChildMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("tab_hifz_kids")
    ) {
        // Toggle options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isChildMode) selectedLanguage.childFriendlyLabel else selectedLanguage.scholarlyLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Scholar", fontSize = 12.sp, color = if (!isChildMode) MaterialTheme.colorScheme.primary else Color.Gray)
                Switch(
                    checked = isChildMode,
                    onCheckedChange = { isChildMode = it },
                    modifier = Modifier
                        .scale(0.85f)
                        .padding(horizontal = 4.dp)
                        .testTag("kids_mode_switch")
                )
                Text("Kids Story", fontSize = 12.sp, color = if (isChildMode) MaterialTheme.colorScheme.primary else Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Narrative space
        AnimatedContent(targetState = isChildMode, label = "ModeAnim") { currentMode ->
            if (currentMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrightGold.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.EmojiPeople, contentDescription = "Kids story icon", tint = AntiqueGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Faith Stories for Kids", fontWeight = FontWeight.Bold, color = AntiqueGold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = studyData.childFriendlyExplanation,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            lineHeight = 22.sp
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.School, contentDescription = "Scholar icon", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Advanced Academic Analysis", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = studyData.advancedScholarExplanation,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            lineHeight = 21.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hifz mode block
        Text(
            text = selectedLanguage.hifzTipsLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.04f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                studyData.memorizationTips.forEachIndexed { idx, tip ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("${idx + 1}. ", color = AntiqueGold, fontWeight = FontWeight.Bold)
                        Text(text = tip, fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Progress system tracking suggestions
        Text(
            text = "Structured Learning Progress Suggestions",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        studyData.learningProgressSuggestions.forEach { suggestion ->
            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                Icon(imageVector = Icons.Default.Star, contentDescription = "suggest star", tint = AntiqueGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = suggestion, fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

// Helper extensiveness to scale buttons
private fun Modifier.scale(scale: Float) = this


// ---------------- TAB 4: Interactive Quiz Mode ----------------
@Composable
fun QuizTab(studyData: QuranStudyData, selectedLanguage: Language, viewModel: QuranViewModel) {
    if (studyData.quizQuestions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No quiz available for these verses.", color = Color.Gray)
        }
        return
    }

    // Quiz states
    var score by remember { mutableStateOf(0) }
    var currentQuestionIdx by remember { mutableStateOf(0) }
    var selectedOptionIdx by remember { mutableStateOf<Int?>(null) }
    var answered by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    val activeQuestion = studyData.quizQuestions.getOrNull(currentQuestionIdx)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("tab_quiz")
    ) {
        if (!finished && activeQuestion != null) {
            // Header Quiz Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${currentQuestionIdx + 1} of ${studyData.quizQuestions.size}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Score: $score",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (currentQuestionIdx + 1).toFloat() / studyData.quizQuestions.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Gray.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Question text
            Text(
                text = activeQuestion.question,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                lineHeight = 22.sp,
                modifier = Modifier.testTag("quiz_question_text")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Options List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activeQuestion.options.forEachIndexed { index, option ->
                    val isChosen = selectedOptionIdx == index
                    val isCorrectIdx = activeQuestion.correctOptionIndex == index

                    val containerColor = when {
                        !answered -> if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        answered && isCorrectIdx -> Color.Green.copy(alpha = 0.15f)
                        answered && isChosen && !isCorrectIdx -> Color.Red.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val borderColor = when {
                        !answered -> if (isChosen) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.25f)
                        answered && isCorrectIdx -> Color.Green
                        answered && isChosen && !isCorrectIdx -> Color.Red
                        else -> Color.Gray.copy(alpha = 0.15f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !answered) {
                                selectedOptionIdx = index
                            }
                            .testTag("quiz_option_$index"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        border = BorderStroke(if (isChosen || (answered && isCorrectIdx)) 2.dp else 1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    answered && isCorrectIdx -> Icons.Default.CheckCircle
                                    answered && isChosen && !isCorrectIdx -> Icons.Default.Cancel
                                    else -> if (isChosen) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = "Choice Option mark",
                                tint = when {
                                    answered && isCorrectIdx -> Color.Green
                                    answered && isChosen && !isCorrectIdx -> Color.Red
                                    else -> if (isChosen) MaterialTheme.colorScheme.primary else Color.Gray
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = option,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Interactive Answers buttons
            if (!answered) {
                Button(
                    onClick = {
                        if (selectedOptionIdx != null) {
                            answered = true
                            if (selectedOptionIdx == activeQuestion.correctOptionIndex) {
                                score++
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("quiz_submit_btn"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = selectedOptionIdx != null
                ) {
                    Text("Submit Answer", fontWeight = FontWeight.Bold)
                }
            } else {
                // Explanation Display Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Explanation:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = activeQuestion.explanation, fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                    }
                }

                Button(
                    onClick = {
                        if (currentQuestionIdx + 1 < studyData.quizQuestions.size) {
                            currentQuestionIdx++
                            selectedOptionIdx = null
                            answered = false
                        } else {
                            finished = true
                            viewModel.recordQuizScore(score)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("quiz_next_btn"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (currentQuestionIdx + 1 < studyData.quizQuestions.size) "Next Question" else "Finish & Save Progress",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Finished State Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Success Score",
                        tint = AntiqueGold,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Quiz Completed!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "You scored $score out of ${studyData.quizQuestions.size}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            score = 0
                            currentQuestionIdx = 0
                            selectedOptionIdx = null
                            answered = false
                            finished = false
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("quiz_restart_btn")
                    ) {
                        Text("Retake Quiz", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}


// ---------------- TAB 5: Live Follow-Up AI Assistant Chat ----------------
@Composable
fun ChatTab(studyData: QuranStudyData, selectedLanguage: Language, viewModel: QuranViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val chatLoading by viewModel.chatLoading.collectAsStateWithLifecycle()
    var userMessageText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tab_chat")
    ) {
        // Chat Logs List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatHistory) { msg ->
                val isMe = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 0.dp,
                            bottomEnd = if (isMe) 0.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else SoftCream
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isMe) "You" else "Quran AI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMe) MaterialTheme.colorScheme.primary else AntiqueGold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = msg.text,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            // Waiting placeholder bubble
            if (chatLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftCream)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Thinking...", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Message Input Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userMessageText,
                onValueChange = { userMessageText = it },
                placeholder = { Text(text = selectedLanguage.askAiPlaceholder) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        focusManager.clearFocus()
                        if (userMessageText.isNotBlank()) {
                            viewModel.sendChatMessage(userMessageText)
                            userMessageText = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    focusManager.clearFocus()
                    if (userMessageText.isNotBlank()) {
                        viewModel.sendChatMessage(userMessageText)
                        userMessageText = ""
                    }
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .size(44.dp)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message to Quran Tutor AI",
                    tint = Color.White
                )
            }
        }
    }
}


// ---------------- FULL JOURNAL RECAP PROGRESS SCREEN ----------------
@Composable
fun ProgressScreen(viewModel: QuranViewModel) {
    val localProgressList by viewModel.localProgressList.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("logs_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "My Reflection Journal Logs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Journal History Title Logo",
                tint = AntiqueGold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(localProgressList) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = log.surahName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(onClick = { viewModel.deleteLocalProgressItem(log) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete item log",
                                    tint = Color.Red.copy(alpha = 0.6f)
                                )
                            }
                        }

                        if (log.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "My Reflection notes:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AntiqueGold
                            )
                            Text(
                                text = log.notes,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontStyle = FontStyle.Italic
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (log.isMemorized) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Star, contentDescription = "suggest star indicator", tint = AntiqueGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Memorized (Hifz)", fontSize = 11.sp, color = AntiqueGold, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (log.quizScore >= 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "quiz indicator", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Quiz score: ${log.quizScore}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
