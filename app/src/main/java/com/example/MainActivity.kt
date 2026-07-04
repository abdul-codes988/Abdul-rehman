package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.ChatMessage
import com.example.data.Task
import com.example.data.UserPreference
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SQLite Offline Room Database
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "aura_scheduler_db"
        ).fallbackToDestructiveMigration().build()

        val repository = AppRepository(
            database.taskDao(),
            database.preferenceDao(),
            database.chatMessageDao()
        )

        val factory = MainViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavBar(viewModel) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeepDarkBlue)
                            .padding(innerPadding)
                    ) {
                        MainScreenContent(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreenContent(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()

    AnimatedVisibility(
        visible = currentTab == "DASHBOARD",
        enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
        exit = fadeOut()
    ) {
        DashboardScreen(viewModel)
    }

    AnimatedVisibility(
        visible = currentTab == "SIRI_CHAT",
        enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
        exit = fadeOut()
    ) {
        SiriChatScreen(viewModel)
    }

    AnimatedVisibility(
        visible = currentTab == "TEAM_SPACE",
        enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
        exit = fadeOut()
    ) {
        TeamCollabScreen(viewModel)
    }

    AnimatedVisibility(
        visible = currentTab == "CALENDAR_SYNC",
        enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
        exit = fadeOut()
    ) {
        CalendarSyncScreen(viewModel)
    }

    AnimatedVisibility(
        visible = currentTab == "SECURITY",
        enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
        exit = fadeOut()
    ) {
        SecurityScreen(viewModel)
    }
}

// --- Dashboard Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val tasks by viewModel.allTasks.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val conflictAlerts by viewModel.conflictAlerts.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isBlackTheme by viewModel.isBlackTheme.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // App Bar / Header mimicking Professional Polish theme
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // User Avatar Circle with initials
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFD0BCFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JD",
                        color = Color(0xFF381E72),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Lumina AI",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.25).sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF16A34A), CircleShape)
                        )
                        Text(
                            text = "Secured & Synced",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            IconButton(
                onClick = { viewModel.selectTab("SECURITY") },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // Hero Header Vibe
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("welcome_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isBlackTheme) {
                                    Modifier
                                } else {
                                    Modifier.drawBehind {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(SiriCyan.copy(alpha = 0.08f), Color.Transparent),
                                                center = Offset(size.width * 0.8f, size.height * 0.2f),
                                                radius = size.width * 0.5f
                                            )
                                        )
                                    }
                                }
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "AURA SCHEDULER",
                                    color = SiriCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                )
                                // Role Switch Badge
                                Surface(
                                    color = if (userRole == "STUDENT") SiriIndigo else SiriPurple,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable {
                                        viewModel.setUserRole(if (userRole == "STUDENT") "TEACHER" else "STUDENT")
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (userRole == "STUDENT") Icons.Filled.School else Icons.Filled.Book,
                                            contentDescription = "Role Mode",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (userRole == "STUDENT") "STUDENT" else "TEACHER",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (userRole == "STUDENT") "Maximize Your Study Productivity" else "Syllabus & Grading Assistant",
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Offline-first dynamic calendar scheduler with iPhone Siri-style proactive voice agents.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // Progress Metrics
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val total = tasks.size
                            val completed = tasks.filter { it.isCompleted }.size
                            val pending = total - completed

                            MetricItem("Pending", pending.toString(), SiriYellow)
                            MetricItem("Completed", completed.toString(), SiriGreen)
                            MetricItem("Conflicts", conflictAlerts.size.toString(), SiriRed)
                        }
                    }
                }
            }
        }

        // 🎨 Themes & Custom Appearance Section
        item {
            val isBlackTheme by viewModel.isBlackTheme.collectAsState()
            Card(
                modifier = Modifier.fillMaxWidth().testTag("theme_switch_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(if (isBlackTheme) Color(0xFF1C1C1E) else SiriPurple.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBlackTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = "Theme Icon",
                                tint = if (isBlackTheme) Color.White else SiriPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Simple Black Theme",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "A clean, high-contrast black and grey color palette",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isBlackTheme,
                        onCheckedChange = { viewModel.setBlackTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isBlackTheme) Color.White else Color.Black,
                            checkedTrackColor = if (isBlackTheme) Color(0xFF3A3A3C) else SiriPurple.copy(alpha = 0.5f),
                            uncheckedThumbColor = if (isBlackTheme) Color(0xFF8E8E93) else Color.LightGray,
                            uncheckedTrackColor = if (isBlackTheme) Color(0xFF1C1C1E) else Color.Gray.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // 🏆 Gamification & Progress Section
        item {
            val points by viewModel.gamificationPoints.collectAsState()
            val streak by viewModel.gamificationStreak.collectAsState()
            val completedCount by viewModel.gamificationCompletedCount.collectAsState()
            val badges by viewModel.gamificationBadges.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth().testTag("gamification_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SiriYellow.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = "Gamification Icon",
                                    tint = SiriYellow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "🏆 LEVEL & PROGRESS",
                                    color = SiriYellow,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Study Academy Milestones",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        IconButton(onClick = { viewModel.clearGamification() }) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = "Reset stats", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Score & Streak Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Points Column
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SiriCyan.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, SiriCyan.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Points", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                                Text("$points", color = SiriCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        
                        // Streak Column
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SiriRed.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, SiriRed.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Streak", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Fire", tint = SiriRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$streak d", color = SiriRed, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        // Completion Column
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SiriGreen.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, SiriGreen.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Tasks", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                                Text("$completedCount", color = SiriGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Badges Section
                    Text(
                        text = "UNLOCKED BADGES",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (badges.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TextSecondary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No badges unlocked yet. Complete tasks to earn milestones!",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(badges.toList()) { badgeName ->
                                val (badgeIcon, badgeColor) = when (badgeName) {
                                    "First Steps" -> Pair(Icons.Filled.DirectionsRun, SiriCyan)
                                    "Task Explorer" -> Pair(Icons.Filled.Map, SiriPurple)
                                    "Productivity Champion" -> Pair(Icons.Filled.WorkspacePremium, SiriIndigo)
                                    "Streak Starter" -> Pair(Icons.Filled.Bolt, SiriYellow)
                                    "Perfect Week" -> Pair(Icons.Filled.Star, SiriGreen)
                                    else -> Pair(Icons.Filled.Badge, SiriPurple)
                                }
                                Surface(
                                    color = badgeColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(badgeIcon, contentDescription = badgeName, tint = badgeColor, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(badgeName, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🎓 Personalized AI Learning Goals & Paths
        item {
            val goal by viewModel.learningGoal.collectAsState()
            val pathText by viewModel.learningPathText.collectAsState()
            val isGenerating by viewModel.isGeneratingPath.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth().testTag("learning_path_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SiriPurple.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = "Education Icon",
                                tint = SiriPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "🎓 ADAPTIVE SYLLABUS ADVISOR",
                                color = SiriPurple,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Personalized Learning Paths",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Set a custom learning goal. Aura AI will integrate recommendations, study materials, and optimal deep-work hours with your local assignments schedule.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = goal,
                        onValueChange = { viewModel.updateLearningGoal(it) },
                        placeholder = { Text("e.g., Master Jetpack Compose components", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = SiriPurple,
                            unfocusedBorderColor = BorderSlate
                        ),
                        trailingIcon = {
                            if (goal.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateLearningGoal("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generateLearningPath() },
                        enabled = !isGenerating && goal.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SiriPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Assembling Learning Syllabus...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate/Update AI Study Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (pathText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = BorderSlate)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "YOUR CUSTOM STUDY GUIDE",
                            color = SiriCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TextSecondary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = pathText,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Was this guide helpful?",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { viewModel.submitLearningPathFeedback(true) },
                                    modifier = Modifier.background(SiriGreen.copy(alpha = 0.1f), CircleShape).size(36.dp)
                                ) {
                                    Icon(Icons.Filled.ThumbUp, contentDescription = "Like", tint = SiriGreen, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.submitLearningPathFeedback(false) },
                                    modifier = Modifier.background(SiriRed.copy(alpha = 0.1f), CircleShape).size(36.dp)
                                ) {
                                    Icon(Icons.Filled.ThumbDown, contentDescription = "Dislike", tint = SiriRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active scheduling conflicts & Proactive AI Alert Card
        if (conflictAlerts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WarningLightBg),
                    border = BorderStroke(1.dp, SiriRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(WarningDarkText.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Conflict Alert",
                                    tint = WarningDarkText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Conflict Detected by AI Agent",
                                color = WarningDarkText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = conflictAlerts.firstOrNull() ?: "",
                            color = WarningDarkText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.autoReplanSchedules() },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningDarkText),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Replan Icon",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Resolve Conflicts & Auto-Replan Priorities",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // Proactive Helper Card matching Professional Polish M3 styling
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF21005D).copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = "AI Agent Icon",
                                        tint = Color(0xFF21005D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PROACTIVE ADVICE",
                                    color = Color(0xFF21005D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                            Surface(
                                color = Color(0xFF21005D),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "AI ACTIVE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "\"Good morning, Professor. I've detected a conflict between your 2 PM lecture and the Dept meeting. Shall I re-prioritize your grading task to 4 PM?\"",
                            color = Color(0xFF21005D),
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.autoReplanSchedules() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21005D)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_replan_button")
                            ) {
                                Text("Yes, Adjust", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { /* Ignore */ },
                                border = BorderStroke(1.dp, Color(0xFF21005D)),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF21005D)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ignore", color = Color(0xFF21005D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Daily Motivation Reminder Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SiriPurple.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, SiriPurple.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = "Motivational Reminder",
                        tint = SiriPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Daily Siri Reminder:",
                            color = SiriPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "\"Every step you take today secures your path to academic mastery tomorrow. Keep pushing!\"",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }

        // Quick Filter Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MY SCHEDULES & ASSIGNMENTS",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Button(
                    onClick = { showAddTaskDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SiriCyan),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("add_task_form_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", tint = DeepDarkBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task", color = DeepDarkBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Render Task List
        val activeList = activeTasks.filter { it.role == "BOTH" || it.role == userRole }
        if (activeList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.EventAvailable,
                            contentDescription = "No tasks",
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No upcoming items in schedule",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "All clear! Tell Siri to add some tasks for you.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(activeList) { task ->
                TaskCard(task = task, onCompleteToggle = {
                    viewModel.toggleTaskCompletion(task)
                }, onDelete = {
                    viewModel.deleteTask(task)
                })
            }
        }

        // Secure Sync Status Alert Logs
        item {
            ToastList(viewModel)
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            userRole = userRole,
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, desc, prio, cat, dueOffsetHours, isTeam, teamName ->
                val due = System.currentTimeMillis() + (dueOffsetHours * 3600L * 1000L)
                viewModel.addTask(
                    title = title,
                    description = desc,
                    priority = prio,
                    dueDate = due,
                    category = cat,
                    role = userRole,
                    isTeamTask = isTeam,
                    teamName = teamName
                )
                showAddTaskDialog = false
            }
        )
    }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .background(BorderSlate.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun TaskCard(task: Task, onCompleteToggle: () -> Unit, onDelete: () -> Unit) {
    val prioColor = when (task.priority) {
        "HIGH" -> SiriRed
        "MEDIUM" -> SiriYellow
        else -> SiriGreen
    }

    val catIcon = when (task.category) {
        "Assignment" -> Icons.Filled.School
        "Class" -> Icons.Filled.Book
        "Exam" -> Icons.Filled.NotificationImportant
        "Meeting" -> Icons.Filled.People
        else -> Icons.Filled.Event
    }

    val dateFormatted = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(task.dueDate))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
        border = BorderStroke(1.dp, BorderSlate),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for task completion
            IconButton(
                onClick = onCompleteToggle,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("task_complete_check_${task.id}")
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = "Complete Toggle",
                    tint = if (task.isCompleted) SiriGreen else SiriCyan
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category icon
                    Icon(
                        imageVector = catIcon,
                        contentDescription = task.category,
                        tint = SiriCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = task.category,
                        color = SiriCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Priority tag
                    Box(
                        modifier = Modifier
                            .background(prioColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority,
                            color = prioColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (task.isTeamTask) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(SiriPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TEAM",
                                color = SiriPurple,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Encryption Lock Badge
                    if (task.isEncrypted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "E2E Encrypted",
                            tint = SiriGreen,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = task.description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = "Due date icon",
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateFormatted,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Task",
                    tint = SiriRed.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// --- Live Toast Log / Terminal Visualizer for Syncing ---
@Composable
fun ToastList(viewModel: MainViewModel) {
    val logs by viewModel.liveNotifications.collectAsState()

    if (logs.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepDarkBlue),
            border = BorderStroke(1.dp, BorderSlate),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "LIVE SYSTEM EVENT STREAM",
                    color = SiriCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    logs.take(3).forEach { log ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val color = when (log.type) {
                                "SUCCESS" -> SiriGreen
                                "WARNING" -> SiriRed
                                "AI" -> SiriPurple
                                else -> SiriCyan
                            }
                            Text(
                                text = "[${log.type}]",
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = log.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = log.message, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        HorizontalDivider(color = BorderSlate.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

// --- Siri AI Voice Chat Room Screen ---
@Composable
fun SiriChatScreen(viewModel: MainViewModel) {
    val history by viewModel.chatHistory.collectAsState()
    val isListening by viewModel.isSiriListening.collectAsState()
    val isSpeaking by viewModel.isSiriSpeaking.collectAsState()
    val speakingText by viewModel.siriSpeakingText.collectAsState()

    var typedText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Siri Voice Orb Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Interactive Siri Orb
                SiriOrb(isListening = isListening, isSpeaking = isSpeaking, onClick = {
                    if (!isListening) {
                        viewModel.startVoiceCommandListening()
                        // Simulate voice recognition in 2 seconds
                        coroutineScope.launch {
                            delay(2000)
                            val samples = listOf(
                                "add grading syllabus tomorrow at 4 PM priority HIGH",
                                "remind me to grade exam tomorrow",
                                "replan everything",
                                "solve conflicts",
                                "How do teachers design adaptive student scheduling priorities?"
                            )
                            viewModel.submitVoiceCommand(samples.random())
                        }
                    }
                })

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        isListening -> "Listening... Speak your command."
                        isSpeaking -> "Siri is talking..."
                        else -> "Tap the Orb to Simulate Voice Command"
                    },
                    color = if (isListening) SiriCyan else if (isSpeaking) SiriPurple else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (isSpeaking && speakingText.isNotEmpty()) {
                    Text(
                        text = speakingText,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Help Prompt hints
                Text(
                    text = "Try: \"add math class tomorrow\" or \"replan everything\"",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }

        // Chat Bubble Transcripts
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
            border = BorderStroke(1.dp, BorderSlate)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SIRI DIALOG TRANSCRIPT",
                        color = SiriCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.clearChat() }) {
                        Text("Clear Logs", color = SiriRed, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    history.forEach { msg ->
                        ChatBubble(message = msg)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Text Typing Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = typedText,
                onValueChange = { typedText = it },
                placeholder = { Text("Ask Siri or Gemini doubts...", color = TextSecondary) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("siri_text_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFECE6F0),
                    unfocusedContainerColor = Color(0xFFECE6F0),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedIndicatorColor = SiriCyan,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp), // Premium rounded-full pill look
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (typedText.isNotEmpty()) {
                        viewModel.sendDirectChat(typedText)
                        typedText = ""
                    }
                },
                modifier = Modifier
                    .background(SiriCyan, CircleShape)
                    .size(48.dp)
                    .testTag("submit_chat_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "USER"
    val align = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) SiriIndigo else Color(0xFFECE6F0)
    val textColors = if (isUser) Color.White else TextPrimary

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Box(
            modifier = Modifier
                .background(
                    color = bg,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) SiriIndigo.copy(alpha = 0.5f) else BorderSlate,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
                .widthIn(max = 260.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isUser) Icons.Filled.Person else Icons.Filled.AutoAwesome,
                        contentDescription = "Sender Icon",
                        tint = if (isUser) SiriCyan else SiriPurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isUser) "YOU" else "SIRI ASSISTANT",
                        color = if (isUser) SiriCyan else SiriPurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    color = textColors,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun SiriOrb(isListening: Boolean, isSpeaking: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val orbScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else if (isSpeaking) 1.15f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 600 else 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(orbScale)
            .clip(CircleShape)
            .then(
                if (ThemeConfig.isBlackTheme) {
                    Modifier.background(Color(0xFF1C1C1E))
                } else {
                    Modifier.background(
                        Brush.radialGradient(
                            colors = if (isListening) {
                                listOf(SiriCyan, SiriPurple, Color.Transparent)
                            } else if (isSpeaking) {
                                listOf(SiriPurple, SiriIndigo, Color.Transparent)
                            } else {
                                listOf(SiriIndigo.copy(alpha = 0.8f), BorderSlate, Color.Transparent)
                            }
                        )
                    )
                }
            )
            .then(
                if (ThemeConfig.isBlackTheme) {
                    Modifier
                } else {
                    Modifier.border(2.dp, SiriCyan.copy(alpha = if (isListening) 1f else 0.4f), CircleShape)
                }
            )
            .clickable(onClick = onClick)
            .testTag("siri_voice_orb"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isListening) Icons.Filled.Mic else Icons.Filled.AutoAwesome,
            contentDescription = "Siri Voice Controller",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

// --- Project Collaboration & Team Space Screen ---
@Composable
fun TeamCollabScreen(viewModel: MainViewModel) {
    val activeTasks by viewModel.allTasks.collectAsState()
    var showAddTeamTask by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf("All Projects") }

    val projects = listOf("All Projects", "Science Expo 2026", "Curriculum Committee", "Senior CS Capstone")
    val members = listOf(
        Triple("Alice V. (Student)", "Active", SiriGreen),
        Triple("Bob K. (Student)", "In Meeting", SiriYellow),
        Triple("Prof. John Doe", "Grading", SiriPurple),
        Triple("Sarah L. (Admin)", "Active", SiriGreen)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Core workspace info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SiriIndigo.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, SiriIndigo.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "TEAM WORKSPACE & COLLABORATION",
                        color = SiriPurple,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Classroom Shared Cockpit",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Connect teacher syllabus goals directly with student assignments. Deadlines are automatically coordinated and updated on team members' devices.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showAddTeamTask = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SiriPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.GroupAdd, contentDescription = "Group", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Post Shared Assignment Deadline", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Shared Projects selection
        item {
            Column {
                Text(
                    text = "ACTIVE TEAM PROJECTS",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(projects) { proj ->
                        val isSel = selectedProject == proj
                        Surface(
                            color = if (isSel) SiriPurple else CardSlateBlue,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSel) SiriPurple else BorderSlate),
                            modifier = Modifier.clickable { selectedProject = proj }
                        ) {
                            Text(
                                text = proj,
                                color = if (isSel) Color.White else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Team Members list card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PROJECT TEAM MEMBERS",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        members.forEach { (name, status, color) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, BorderSlate),
                                colors = CardDefaults.cardColors(containerColor = TextSecondary.copy(alpha = 0.02f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(color.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name.first().toString(),
                                            color = color,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = name.substringBefore(" "),
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(5.dp).background(color, CircleShape))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(status, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Collective Calendar Agenda Title
        item {
            Text(
                text = "COLLECTIVE DEADLINES & SCHEDULES",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        // Filter shared tasks list
        val sharedList = activeTasks.filter { task ->
            task.isTeamTask && (selectedProject == "All Projects" || task.teamName == selectedProject)
        }

        if (sharedList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.EventBusy, contentDescription = "No team", tint = TextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No shared deadlines under '$selectedProject'",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            items(sharedList) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
                    border = BorderStroke(1.dp, BorderSlate),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(SiriPurple.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = task.teamName ?: "SHARED PROJECT",
                                        color = SiriPurple,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = task.category,
                                    color = SiriCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(SiriGreen, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Synced", color = SiriGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = task.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = task.description, color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Assigned to: Project Team",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                            Text(
                                text = "Due: " + SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(task.dueDate)),
                                color = SiriCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Live Real-Time Collaboration Audit log
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LIVE SYNC & ACTIVITY LOG",
                        color = SiriCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        Pair("Alice V. completed assignment step", "12 mins ago"),
                        Pair("Prof. John Doe updated curriculum guidelines", "1 hr ago"),
                        Pair("Sarah L. synchronized calendar database", "3 hrs ago")
                    ).forEach { (activity, time) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Done", tint = SiriGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(activity, color = TextPrimary, fontSize = 11.sp)
                            }
                            Text(time, color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddTeamTask) {
        AddTaskDialog(
            userRole = "BOTH",
            isTeamFlow = true,
            onDismiss = { showAddTeamTask = false },
            onSave = { title, desc, prio, cat, dueOffsetHours, isTeam, teamName ->
                val due = System.currentTimeMillis() + (dueOffsetHours * 3600L * 1000L)
                viewModel.addTask(
                    title = title,
                    description = desc,
                    priority = prio,
                    dueDate = due,
                    category = cat,
                    role = "BOTH",
                    isTeamTask = true,
                    teamName = teamName ?: "Science Expo 2026"
                )
                viewModel.postNotification("Team Calendar Synced", "Broadcasting assignment notification alerts to all users.", "SUCCESS")
                showAddTeamTask = false
            }
        )
    }
}

// --- External Calendar Integration & Account Screen ---
@Composable
fun CalendarSyncScreen(viewModel: MainViewModel) {
    val isSyncing by viewModel.isSyncingCalendar.collectAsState()
    val progress by viewModel.syncProgress.collectAsState()
    val connected by viewModel.connectedCalendars.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
            border = BorderStroke(1.dp, BorderSlate),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CALENDAR ACCOUNTS INTEGRATIONS",
                    color = SiriCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lightning-Fast Synchronization",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Aura securely links external student and teacher calendar events, providing offline access with fast local cached copies.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                if (isSyncing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column {
                        Text(text = "Syncing in Progress... ${(progress * 100).toInt()}%", color = SiriCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            color = SiriCyan,
                            trackColor = BorderSlate,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Text(
            text = "AVAILABLE PLUGINS",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        val calendarOptions = listOf(
            Triple("Google Calendar", "Student Assignments Sync Engine", Icons.Filled.Language),
            Triple("Outlook Calendar", "Teacher Institutional Syllabi", Icons.Filled.Mail),
            Triple("Apple Calendar", "Local Mobile Handheld Synchronizer", Icons.Filled.MobileFriendly)
        )

        calendarOptions.forEach { calendar ->
            val isConn = connected.contains(calendar.first)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
                border = BorderStroke(1.dp, if (isConn) SiriCyan.copy(alpha = 0.5f) else BorderSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(calendar.third, contentDescription = "Cal icon", tint = if (isConn) SiriCyan else TextSecondary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = calendar.first, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = calendar.second, color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (isConn) {
                                viewModel.disconnectCalendar(calendar.first)
                            } else {
                                viewModel.syncWithExternalCalendar(calendar.first)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isConn) SiriRed else SiriCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("sync_btn_${calendar.first.lowercase().replace(" ", "_")}")
                    ) {
                        Text(text = if (isConn) "Disconnect" else "Sync", color = DeepDarkBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- Security & Preferences Screen ---
@Composable
fun SecurityScreen(viewModel: MainViewModel) {
    val isEncrypted by viewModel.isEncryptionEnabled.collectAsState()
    val securityKey by viewModel.securityKey.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    val isBlackTheme by viewModel.isBlackTheme.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Simple Black Theme Switcher Option in settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_theme_switch_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(if (isBlackTheme) Color(0xFF1C1C1E) else SiriPurple.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBlackTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = "Theme Icon",
                                tint = if (isBlackTheme) Color.White else SiriPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Simple Black Theme",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "A clean, high-contrast black and grey color palette",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isBlackTheme,
                        onCheckedChange = { viewModel.setBlackTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isBlackTheme) Color.White else Color.Black,
                            checkedTrackColor = if (isBlackTheme) Color(0xFF3A3A3C) else SiriPurple.copy(alpha = 0.5f),
                            uncheckedThumbColor = if (isBlackTheme) Color(0xFF8E8E93) else Color.LightGray,
                            uncheckedTrackColor = if (isBlackTheme) Color(0xFF1C1C1E) else Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("settings_theme_switch")
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
                border = BorderStroke(1.dp, BorderSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = "Secure lock", tint = SiriGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "END-TO-END DATA ENCRYPTION",
                            color = SiriGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Zero-Knowledge Local Cryptography",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aura secures all personal schedules, class titles, and notes locally. Encryption strings are parsed with AES-256 before storage.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enforce AES Encryption", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Status: " + if (isEncrypted) "Active Secure" else "Disabled", color = if (isEncrypted) SiriGreen else SiriRed, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isEncrypted,
                            onCheckedChange = { viewModel.toggleEncryption(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = SiriGreen, checkedTrackColor = SiriGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("encryption_switch")
                        )
                    }

                    if (isEncrypted) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ACTIVE SYMMETRIC AES KEY CONTAINER",
                            color = SiriGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepDarkBlue, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = securityKey,
                                color = SiriGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
                border = BorderStroke(1.dp, BorderSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.OfflinePin, contentDescription = "Offline Access icon", tint = SiriCyan, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Certified Offline Local Access", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Task lists, chat logs, and settings are fully cached locally in Room SQL for absolute server downtime insurance.", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "LEARNED AI USER PREFERENCES LOG",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // List Learned Preferences
        val activePrefs = prefs.filter { it.key.startsWith("pref_") }
        if (activePrefs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No AI preferences compiled yet. Complete some tasks!", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(activePrefs) { preference ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSlateBlue),
                    border = BorderStroke(1.dp, BorderSlate),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI habit", tint = SiriPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = preference.key.replace("pref_", "").replace("_", " ").uppercase(),
                                color = SiriPurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(text = preference.value, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// --- Shared Custom Dialog: Create / Edit Task ---
@Composable
fun AddTaskDialog(
    userRole: String,
    isTeamFlow: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int, Boolean, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var category by remember { mutableStateOf("Assignment") }
    var dueOffsetHours by remember { mutableStateOf(24) }
    var teamName by remember { mutableStateOf("") }

    val priorities = listOf("HIGH", "MEDIUM", "LOW")
    val categories = listOf("Assignment", "Class", "Exam", "Meeting", "Personal")
    val times = listOf(
        Pair("Today (+2 hrs)", 2),
        Pair("Tonight (+6 hrs)", 6),
        Pair("Tomorrow (+24 hrs)", 24),
        Pair("Next Week (+168 hrs)", 168)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isTeamFlow) "Post Team Shared Assignment" else "Create Task Schedule",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task / Class Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SiriCyan,
                        unfocusedBorderColor = BorderSlate
                    )
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SiriCyan,
                        unfocusedBorderColor = BorderSlate
                    )
                )

                if (isTeamFlow) {
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Team / Class Group Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SiriPurple,
                            unfocusedBorderColor = BorderSlate
                        )
                    )
                }

                // Category Selector
                Text("Category:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SiriCyan.copy(alpha = 0.3f),
                                selectedLabelColor = SiriCyan
                            )
                        )
                    }
                }

                // Priority Selector
                Text("Priority Level:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    priorities.forEach { p ->
                        Button(
                            onClick = { priority = p },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (priority == p) {
                                    when (p) {
                                        "HIGH" -> SiriRed
                                        "MEDIUM" -> SiriYellow
                                        else -> SiriGreen
                                    }
                                } else BorderSlate
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = p, color = DeepDarkBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Due date selector (Offset)
                Text("Due/Timing Deadline:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    times.forEach { time ->
                        Button(
                            onClick = { dueOffsetHours = time.second },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (dueOffsetHours == time.second) SiriCyan else BorderSlate
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = time.first.substringBefore(" "), color = DeepDarkBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        onSave(title, desc, priority, category, dueOffsetHours, isTeamFlow, teamName.ifEmpty { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SiriCyan),
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Save", color = DeepDarkBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SiriRed)
            }
        },
        containerColor = CardSlateBlue
    )
}

// --- Styled Custom Bottom Navigation Bar ---
@Composable
fun BottomNavBar(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()

    val tabs = listOf(
        Triple("DASHBOARD", Icons.Filled.Home, "Home"),
        Triple("SIRI_CHAT", Icons.Filled.Mic, "Siri Chat"),
        Triple("TEAM_SPACE", Icons.Filled.Group, "Collab"),
        Triple("CALENDAR_SYNC", Icons.Filled.Sync, "Sync"),
        Triple("SECURITY", Icons.Filled.Security, "Secure")
    )

    NavigationBar(
        containerColor = CardSlateBlue,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier.height(72.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = currentTab == tab.first
            NavigationBarItem(
                selected = isSelected,
                onClick = { viewModel.selectTab(tab.first) },
                icon = {
                    Icon(
                        imageVector = tab.second,
                        contentDescription = tab.third,
                        tint = if (isSelected) SiriCyan else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.third,
                        color = if (isSelected) SiriCyan else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = SiriCyan.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("nav_btn_${tab.third.lowercase()}")
            )
        }
    }
}
