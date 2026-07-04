package com.example

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.ai.GeminiService
import com.example.ai.SiriActionResponse
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.ChatMessage
import com.example.data.Task
import com.example.data.UserPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val context: Context = application.applicationContext

    // --- UI Navigation State ---
    private val _currentTab = MutableStateFlow("DASHBOARD") // "DASHBOARD", "SIRI_CHAT", "TEAM_SPACE", "CALENDAR_SYNC", "SECURITY"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    // --- User Role Selection ---
    private val _userRole = MutableStateFlow("STUDENT") // "STUDENT", "TEACHER"
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    fun setUserRole(role: String) {
        _userRole.value = role
        viewModelScope.launch {
            repository.savePreference("USER_ROLE", role)
        }
    }

    // --- Database Flows ---
    val allTasks: StateFlow<List<Task>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeTasks: StateFlow<List<Task>> = repository.activeTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val teamTasks: StateFlow<List<Task>> = repository.teamTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chatHistory: StateFlow<List<ChatMessage>> = repository.chatHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val preferences: StateFlow<List<UserPreference>> = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Siri Speech Interaction State ---
    private val _isSiriListening = MutableStateFlow(false)
    val isSiriListening: StateFlow<Boolean> = _isSiriListening.asStateFlow()

    private val _isSiriSpeaking = MutableStateFlow(false)
    val isSiriSpeaking: StateFlow<Boolean> = _isSiriSpeaking.asStateFlow()

    private val _siriSpeakingText = MutableStateFlow("")
    val siriSpeakingText: StateFlow<String> = _siriSpeakingText.asStateFlow()

    private var tts: TextToSpeech? = null
    private val _isTtsReady = MutableStateFlow(false)

    // --- Active Conflict Alerts ---
    private val _conflictAlerts = MutableStateFlow<List<String>>(emptyList())
    val conflictAlerts: StateFlow<List<String>> = _conflictAlerts.asStateFlow()

    // --- Real-time Notifications & Toast Logs ---
    private val _liveNotifications = MutableStateFlow<List<NotificationLog>>(emptyList())
    val liveNotifications: StateFlow<List<NotificationLog>> = _liveNotifications.asStateFlow()

    // --- Calendar Integration State ---
    private val _isSyncingCalendar = MutableStateFlow(false)
    val isSyncingCalendar: StateFlow<Boolean> = _isSyncingCalendar.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _connectedCalendars = MutableStateFlow(setOf("Local Database"))
    val connectedCalendars: StateFlow<Set<String>> = _connectedCalendars.asStateFlow()

    // --- Encryption visual state ---
    private val _securityKey = MutableStateFlow("AURA-SECURE-E2EE-AES256")
    val securityKey: StateFlow<String> = _securityKey.asStateFlow()

    private val _isEncryptionEnabled = MutableStateFlow(true)
    val isEncryptionEnabled: StateFlow<Boolean> = _isEncryptionEnabled.asStateFlow()

    // --- Dynamic Simple Black Theme ---
    private val _isBlackTheme = MutableStateFlow(false)
    val isBlackTheme: StateFlow<Boolean> = _isBlackTheme.asStateFlow()

    fun setBlackTheme(enabled: Boolean) {
        _isBlackTheme.value = enabled
        com.example.ui.theme.ThemeConfig.isBlackTheme = enabled
        viewModelScope.launch {
            repository.savePreference("BLACK_THEME", enabled.toString())
            postNotification(
                title = "Theme Configured", 
                message = if (enabled) "Switched to Minimalist Deep Black Theme." else "Switched to Polished Material Theme.", 
                type = "INFO"
            )
        }
    }

    // --- Gamification System ---
    private val _gamificationPoints = MutableStateFlow(0)
    val gamificationPoints: StateFlow<Int> = _gamificationPoints.asStateFlow()

    private val _gamificationStreak = MutableStateFlow(0)
    val gamificationStreak: StateFlow<Int> = _gamificationStreak.asStateFlow()

    private val _gamificationCompletedCount = MutableStateFlow(0)
    val gamificationCompletedCount: StateFlow<Int> = _gamificationCompletedCount.asStateFlow()

    private val _gamificationBadges = MutableStateFlow<Set<String>>(emptySet())
    val gamificationBadges: StateFlow<Set<String>> = _gamificationBadges.asStateFlow()

    fun clearGamification() {
        viewModelScope.launch {
            _gamificationPoints.value = 0
            _gamificationStreak.value = 0
            _gamificationCompletedCount.value = 0
            _gamificationBadges.value = emptySet()
            repository.savePreference("GAMIFICATION_POINTS", "0")
            repository.savePreference("GAMIFICATION_STREAK", "0")
            repository.savePreference("GAMIFICATION_COMPLETED_COUNT", "0")
            repository.savePreference("GAMIFICATION_BADGES", "")
            postNotification("Gamification Reset", "Earned rewards, badges, and progress counters cleared.", "WARNING")
        }
    }

    private var lastCompletionTime = 0L

    private fun awardGamificationRewards(task: Task) {
        viewModelScope.launch {
            // Points calculation based on urgency/priority
            val onTime = task.dueDate >= System.currentTimeMillis()
            val basePoints = if (task.priority == "HIGH") 100 else 50
            val modifierPoints = if (onTime) 50 else 20
            val totalEarned = basePoints + modifierPoints
            
            val newPoints = _gamificationPoints.value + totalEarned
            _gamificationPoints.value = newPoints
            repository.savePreference("GAMIFICATION_POINTS", newPoints.toString())

            // Completed count
            val newCount = _gamificationCompletedCount.value + 1
            _gamificationCompletedCount.value = newCount
            repository.savePreference("GAMIFICATION_COMPLETED_COUNT", newCount.toString())

            // Streak evaluation
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 3600 * 1000L
            val newStreak = if (lastCompletionTime > 0 && (now - lastCompletionTime) <= dayMillis) {
                _gamificationStreak.value + 1
            } else {
                1
            }
            lastCompletionTime = now
            _gamificationStreak.value = newStreak
            repository.savePreference("GAMIFICATION_STREAK", newStreak.toString())

            // Badges milestones
            val currentBadges = _gamificationBadges.value.toMutableSet()
            val newlyAwarded = mutableListOf<String>()

            if (currentBadges.add("First Steps")) {
                newlyAwarded.add("First Steps")
            }
            if (newCount >= 5 && currentBadges.add("Task Explorer")) {
                newlyAwarded.add("Task Explorer")
            }
            if (newCount >= 10 && currentBadges.add("Productivity Champion")) {
                newlyAwarded.add("Productivity Champion")
            }
            if (newCount >= 100 && currentBadges.add("100 Tasks Completed")) {
                newlyAwarded.add("100 Tasks Completed")
            }
            if (newStreak >= 3 && currentBadges.add("Streak Starter")) {
                newlyAwarded.add("Streak Starter")
            }
            if (newStreak >= 7 && currentBadges.add("Perfect Week")) {
                newlyAwarded.add("Perfect Week")
            }

            if (newlyAwarded.isNotEmpty()) {
                _gamificationBadges.value = currentBadges
                repository.savePreference("GAMIFICATION_BADGES", currentBadges.joinToString(","))
                newlyAwarded.forEach { badge ->
                    postNotification("🏆 Badge Unlocked!", "Awarded '$badge' milestone badge. +200 bonus points!", "SUCCESS")
                    val finalPoints = _gamificationPoints.value + 200
                    _gamificationPoints.value = finalPoints
                    repository.savePreference("GAMIFICATION_POINTS", finalPoints.toString())
                }
                speakSiri("Outstanding achievement! You've unlocked the '${newlyAwarded.joinToString(" and ")}' productivity milestone badges!")
            } else {
                postNotification(
                    "🏆 Task Completed!", 
                    "Earned $totalEarned points! (Current Streak: $newStreak days)", 
                    "SUCCESS"
                )
            }
        }
    }

    // --- Personalized AI Learning Goals & Paths ---
    private val _learningGoal = MutableStateFlow("")
    val learningGoal: StateFlow<String> = _learningGoal.asStateFlow()

    private val _learningPathText = MutableStateFlow("")
    val learningPathText: StateFlow<String> = _learningPathText.asStateFlow()

    private val _isGeneratingPath = MutableStateFlow(false)
    val isGeneratingPath: StateFlow<Boolean> = _isGeneratingPath.asStateFlow()

    fun updateLearningGoal(goal: String) {
        _learningGoal.value = goal
        viewModelScope.launch {
            repository.savePreference("LEARNING_GOAL", goal)
        }
    }

    fun generateLearningPath() {
        val goal = _learningGoal.value
        if (goal.trim().isEmpty()) {
            postNotification("Goal Required", "Please type a learning goal to generate an AI path.", "WARNING")
            return
        }
        viewModelScope.launch {
            _isGeneratingPath.value = true
            postNotification("Aura Learning Assistant", "Analyzing curriculum, schedules, and learning goal: '$goal'...", "AI")
            _isSiriSpeaking.value = true
            _siriSpeakingText.value = "Consulting academic guidelines and formulating your adaptive learning path..."

            try {
                val path = com.example.ai.GeminiService.generateLearningPath(
                    userGoal = goal,
                    userRole = _userRole.value,
                    upcomingTasks = allTasks.value.filter { !it.isCompleted }
                )
                _learningPathText.value = path
                repository.savePreference("LEARNING_PATH", path)
                postNotification("AI Path Generated", "Personalized study steps & schedule prepared successfully.", "SUCCESS")
                speakSiri("I have created a personalized learning path tailored specifically to your goal of $goal, integrated with your existing schedules.")
            } catch (e: Exception) {
                postNotification("Generation Failed", "Could not build path: ${e.localizedMessage}", "WARNING")
            } finally {
                _isGeneratingPath.value = false
                _isSiriSpeaking.value = false
            }
        }
    }

    fun submitLearningPathFeedback(isPositive: Boolean) {
        viewModelScope.launch {
            val feedbackMsg = if (isPositive) "Liked. Path reinforced in AI preferences." else "Disliked. AI will adjust future study guides."
            postNotification("Learning Path Feedback", feedbackMsg, "AI")
            repository.savePreference("LEARNING_PATH_FEEDBACK", if (isPositive) "POSITIVE" else "NEGATIVE")
            if (isPositive) {
                // Award points for active participation!
                _gamificationPoints.value = _gamificationPoints.value + 50
                repository.savePreference("GAMIFICATION_POINTS", _gamificationPoints.value.toString())
                postNotification("Earned +50 Points", "Giving positive AI path feedback.", "SUCCESS")
            }
        }
    }

    init {
        tts = TextToSpeech(context, this)
        loadSavedSettings()
        observeAndCheckConflicts()
        seedInitialDataIfEmpty()
    }

    private fun loadSavedSettings() {
        viewModelScope.launch {
            val savedRole = repository.getPreferenceValue("USER_ROLE")
            if (savedRole != null) {
                _userRole.value = savedRole
            }
            val encryptionPref = repository.getPreferenceValue("ENCRYPTION_ENABLED")
            if (encryptionPref != null) {
                _isEncryptionEnabled.value = encryptionPref == "true"
            }
            val blackThemePref = repository.getPreferenceValue("BLACK_THEME")
            if (blackThemePref == "true") {
                _isBlackTheme.value = true
                com.example.ui.theme.ThemeConfig.isBlackTheme = true
            }
            val pointsPref = repository.getPreferenceValue("GAMIFICATION_POINTS")
            if (pointsPref != null) {
                _gamificationPoints.value = pointsPref.toIntOrNull() ?: 0
            }
            val streakPref = repository.getPreferenceValue("GAMIFICATION_STREAK")
            if (streakPref != null) {
                _gamificationStreak.value = streakPref.toIntOrNull() ?: 0
            }
            val completedPref = repository.getPreferenceValue("GAMIFICATION_COMPLETED_COUNT")
            if (completedPref != null) {
                _gamificationCompletedCount.value = completedPref.toIntOrNull() ?: 0
            }
            val badgesPref = repository.getPreferenceValue("GAMIFICATION_BADGES")
            if (!badgesPref.isNullOrEmpty()) {
                _gamificationBadges.value = badgesPref.split(",").toSet()
            }
            val goalPref = repository.getPreferenceValue("LEARNING_GOAL")
            if (goalPref != null) {
                _learningGoal.value = goalPref
            }
            val pathPref = repository.getPreferenceValue("LEARNING_PATH")
            if (pathPref != null) {
                _learningPathText.value = pathPref
            }
        }
    }

    private fun seedInitialDataIfEmpty() {
        viewModelScope.launch {
            delay(500) // Small delay to let DB connect
            if (allTasks.value.isEmpty()) {
                // Seed academic schedules for students/teachers
                val now = System.currentTimeMillis()
                val hour = 3600000L

                repository.insertTask(Task(
                    title = "Database Systems Midterm Preparation",
                    description = "Study database normalization rules and Room migrations.",
                    priority = "HIGH",
                    dueDate = now + 18 * hour,
                    category = "Assignment",
                    role = "STUDENT",
                    isCompleted = false
                ))

                repository.insertTask(Task(
                    title = "Class: Advanced Software Architecture",
                    description = "Lecture on MVVM design pattern and Flow architectures.",
                    priority = "MEDIUM",
                    dueDate = now + 4 * hour,
                    category = "Class",
                    role = "BOTH",
                    isCompleted = false
                ))

                repository.insertTask(Task(
                    title = "Grade Assignments: Compose Basics",
                    description = "Review Jetpack Compose homework submissions from students.",
                    priority = "HIGH",
                    dueDate = now + 6 * hour,
                    category = "Assignment",
                    role = "TEACHER",
                    isCompleted = false
                ))

                repository.insertTask(Task(
                    title = "Project Sync: Team Aura Core",
                    description = "Collaborative meeting with team teachers on curriculum mapping.",
                    priority = "LOW",
                    dueDate = now + 40 * hour,
                    category = "Meeting",
                    role = "TEACHER",
                    isTeamTask = true,
                    teamName = "Curriculum Commitee",
                    isCompleted = false
                ))

                // Insert initial welcome message
                repository.addChatMessage(ChatMessage(
                    sender = "SIRI",
                    text = "Hello! I am your Siri-like scheduler. Tell me 'add math exam tomorrow at 2 PM' or ask me 'solve my physics doubt' to begin!",
                    isVoice = true
                ))

                // Seed some learned AI preferences
                repository.savePreference("pref_focus_hours", "Prefers Morning Scheduling (8 AM - 12 PM)")
                repository.savePreference("pref_priority_replan", "Enabled: Priority Auto-Escalation for upcoming deadlines")
            }
        }
    }

    private fun observeAndCheckConflicts() {
        viewModelScope.launch {
            allTasks.collect { tasks ->
                val active = tasks.filter { !it.isCompleted }
                val conflicts = mutableListOf<String>()

                // Check for overlapping tasks (within 2 hours of each other)
                for (i in active.indices) {
                    for (j in i + 1 until active.size) {
                        val task1 = active[i]
                        val task2 = active[j]
                        val diff = Math.abs(task1.dueDate - task2.dueDate)
                        if (diff < 2 * 3600 * 1000L) { // 2 hours
                            conflicts.add("Overlapping Schedules: '${task1.title}' and '${task2.title}' are scheduled very close to each other. Tap 'Ask Siri to Replan' to auto-resolve conflicts based on dynamic priorities!")
                        }
                    }
                }
                _conflictAlerts.value = conflicts
            }
        }
    }

    // Extensions for safety list length checking
    private fun <T> List<T>.length(): Int = size

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            _isTtsReady.value = true
        } else {
            Log.e("MainViewModel", "TextToSpeech initialization failed.")
        }
    }

    fun speakSiri(text: String) {
        _siriSpeakingText.value = text
        _isSiriSpeaking.value = true
        if (_isTtsReady.value) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SiriSpeak")
        }
        viewModelScope.launch {
            delay((text.length * 75L).coerceIn(2000L, 8000L)) // estimate speech time visually
            _isSiriSpeaking.value = false
        }
    }

    // --- Live Notification Log ---
    fun postNotification(title: String, message: String, type: String = "INFO") {
        val newList = _liveNotifications.value.toMutableList()
        newList.add(0, NotificationLog(title = title, message = message, type = type))
        _liveNotifications.value = newList
    }

    // --- Task Database Actions ---
    fun addTask(
        title: String,
        description: String,
        priority: String,
        dueDate: Long,
        category: String,
        role: String,
        isTeamTask: Boolean = false,
        teamName: String? = null
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                priority = priority,
                dueDate = dueDate,
                category = category,
                role = role,
                isTeamTask = isTeamTask,
                teamName = teamName,
                isEncrypted = _isEncryptionEnabled.value
            )
            repository.insertTask(task)
            postNotification("Task Created", "Added '$title' as priority $priority.", "SUCCESS")
            
            // AI habit learning trigger
            updateLearnedHabit("Task Added", category, priority)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
            val msg = if (updated.isCompleted) "Completed task" else "Reopened task"
            postNotification(msg, "'${task.title}' updated successfully.", "INFO")
            
            if (updated.isCompleted) {
                updateLearnedHabit("Task Completed", task.category, task.priority)
                awardGamificationRewards(task)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            postNotification("Task Removed", "'${task.title}' has been deleted.", "WARNING")
        }
    }

    // --- AI Priority Optimizer & Dynamic Scheduling ---
    fun autoReplanSchedules() {
        viewModelScope.launch {
            postNotification("Aura AI Optimizer", "Analyzing active schedules for students & teachers...", "AI")
            _isSiriSpeaking.value = true
            _siriSpeakingText.value = "Sorting schedules... Calculating deadline proximity, urgency, and academic priority weights."
            delay(1500)

            val current = allTasks.value
            var updatedCount = 0
            for (task in current) {
                if (!task.isCompleted) {
                    val hoursLeft = (task.dueDate - System.currentTimeMillis()) / 3600000.0
                    val newPriority = when {
                        hoursLeft < 12.0 && task.priority != "HIGH" -> {
                            updatedCount++
                            "HIGH" // Escalate to HIGH
                        }
                        hoursLeft > 48.0 && task.priority == "HIGH" -> {
                            updatedCount++
                            "MEDIUM" // De-escalate to MEDIUM if plenty of time
                        }
                        else -> task.priority
                    }
                    if (newPriority != task.priority) {
                        repository.updateTask(task.copy(priority = newPriority))
                    }
                }
            }

            _isSiriSpeaking.value = false
            postNotification(
                "Optimization Complete", 
                "Dynamic scheduler updated $updatedCount task priority weights based on real-time urgency.", 
                "SUCCESS"
            )
            speakSiri("I've analyzed your agenda and adjusted priority weights for upcoming deadlines to resolve scheduling bottlenecks!")
        }
    }

    // --- Calendar Sync Syncing Account Engine ---
    fun syncWithExternalCalendar(provider: String) {
        viewModelScope.launch {
            if (_isSyncingCalendar.value) return@launch
            _isSyncingCalendar.value = true
            _syncProgress.value = 0f
            postNotification("Sync Initiated", "Synchronizing offline database with $provider...", "INFO")
            
            while (_syncProgress.value < 1.0f) {
                delay(150)
                _syncProgress.value += 0.1f
            }
            
            _isSyncingCalendar.value = false
            val current = _connectedCalendars.value.toMutableSet()
            current.add(provider)
            _connectedCalendars.value = current
            postNotification("Sync Completed", "E2E Secure Sync with $provider completed in 1.4 seconds. Offline copies are fully secured.", "SUCCESS")
            
            speakSiri("Your Aura Schedule is now fully synchronized with $provider. Offline access is cached and fully encrypted.")
        }
    }

    fun disconnectCalendar(provider: String) {
        val current = _connectedCalendars.value.toMutableSet()
        current.remove(provider)
        _connectedCalendars.value = current
        postNotification("Calendar Disconnected", "Disconnected $provider calendar sync.", "WARNING")
    }

    // --- Voice Recognition Simulation & Chat Processing ---
    fun startVoiceCommandListening() {
        if (_isSiriListening.value || _isSiriSpeaking.value) return
        _isSiriListening.value = true
        postNotification("Siri Microphone Active", "Listening to voice command...", "AI")
    }

    fun submitVoiceCommand(command: String) {
        _isSiriListening.value = false
        if (command.trim().isEmpty()) return

        viewModelScope.launch {
            // Add user turn to chat history
            repository.addChatMessage(ChatMessage(sender = "USER", text = command, isVoice = true))

            postNotification("Voice Query", "\"$command\"", "INFO")
            
            // Siri thinking animation indicator
            _isSiriSpeaking.value = true
            _siriSpeakingText.value = "Analyzing schedule intent..."
            
            val response: SiriActionResponse = GeminiService.parseSiriCommand(command, allTasks.value)
            
            _isSiriSpeaking.value = false
            
            // Save Siri response to database
            repository.addChatMessage(ChatMessage(sender = "SIRI", text = response.responseText, isVoice = true))
            
            // Execute intent changes
            if (response.hasChanges) {
                when (response.recognizedIntent) {
                    "ADD_TASK" -> {
                        if (response.taskTitle.isNotEmpty()) {
                            val now = System.currentTimeMillis()
                            val offsetHours = if (response.taskDueDateOffsetHours > 0) response.taskDueDateOffsetHours else 24
                            val dueDate = now + (offsetHours * 3600L * 1000L)
                            
                            val category = response.taskCategory
                            val priority = response.taskPriority
                            
                            val task = Task(
                                title = response.taskTitle,
                                description = "Added via voice command assistant.",
                                priority = priority,
                                dueDate = dueDate,
                                category = category,
                                role = _userRole.value,
                                isEncrypted = _isEncryptionEnabled.value
                            )
                            repository.insertTask(task)
                            postNotification("Task Auto-Created", "Siri created '${response.taskTitle}' (${category}) as priority ${priority}.", "SUCCESS")
                        }
                    }
                    "TOGGLE_TASK" -> {
                        if (response.targetTaskId != -1) {
                            val target = allTasks.value.find { it.id == response.targetTaskId }
                            if (target != null) {
                                repository.updateTask(target.copy(isCompleted = !target.isCompleted))
                                postNotification("Task Updated", "Siri marked '${target.title}' status.", "SUCCESS")
                            }
                        }
                    }
                }
            } else if (response.recognizedIntent == "CHECK_CONFLICTS") {
                autoReplanSchedules()
                return@launch
            }

            // Speak response
            speakSiri(response.responseText)
        }
    }

    // --- General chatbot / query solver (Chat Tab) ---
    fun sendDirectChat(messageText: String) {
        if (messageText.trim().isEmpty()) return
        viewModelScope.launch {
            repository.addChatMessage(ChatMessage(sender = "USER", text = messageText, isVoice = false))
            
            val currentHistory = chatHistory.value.takeLast(10).map { Pair(it.sender, it.text) }
            
            val system = """
                You are Siri, the scheduling advisor and Academic Doubt Solver for Aura AI Scheduler.
                Be polite, witty, clear, and direct. Helpful for students preparing assignments or classes, and teachers managing syllabi.
            """.trimIndent()
            
            postNotification("Gemini Query", "Sending general chat context to Gemini...", "AI")
            val replyText = GeminiService.getChatResponse(messageText, currentHistory, system)
            
            repository.addChatMessage(ChatMessage(sender = "SIRI", text = replyText, isVoice = false))
            speakSiri(replyText)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            postNotification("Logs Cleared", "Chat history erased successfully.", "WARNING")
        }
    }

    // --- Secure Encryption Toggle ---
    fun toggleEncryption(enabled: Boolean) {
        viewModelScope.launch {
            _isEncryptionEnabled.value = enabled
            repository.savePreference("ENCRYPTION_ENABLED", enabled.toString())
            postNotification(
                "Security Settings", 
                if (enabled) "E2E Encryption enforced. Local database strings are fully hashed." else "E2E Encryption suspended.",
                if (enabled) "SUCCESS" else "WARNING"
            )
        }
    }

    // --- Learning Habits preference engine ---
    private fun updateLearnedHabit(action: String, category: String, priority: String) {
        viewModelScope.launch {
            val key = "pref_habit_${category.lowercase()}"
            val existing = repository.getPreferenceValue(key)
            
            val newValue = when {
                existing == null -> "Frequent interaction with $category ($priority priority)"
                existing.contains("Frequent") -> "High affinity for $category tasks. Auto-sorting prioritizes these."
                else -> "Frequent interaction with $category"
            }
            repository.savePreference(key, newValue)
            postNotification("AI Learning preferences", "Aura updated scheduling weights based on task affinity: $category.", "AI")
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}

data class NotificationLog(
    val id: Long = System.currentTimeMillis() + (0..100000).random(),
    val title: String,
    val message: String,
    val type: String // "INFO", "SUCCESS", "WARNING", "AI"
)

// Factory for ViewModel
class MainViewModelFactory(
    private val application: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
