package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val taskDao: TaskDao,
    private val preferenceDao: PreferenceDao,
    private val chatMessageDao: ChatMessageDao
) {
    // Tasks flow
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val activeTasks: Flow<List<Task>> = taskDao.getActiveTasks()
    val teamTasks: Flow<List<Task>> = taskDao.getTeamTasks()

    // Task actions
    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    suspend fun deleteTaskById(id: Int) = taskDao.deleteById(id)
    suspend fun clearAllTasks() = taskDao.deleteAll()

    // Chat actions
    val chatHistory: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()
    suspend fun addChatMessage(message: ChatMessage): Long = chatMessageDao.insertMessage(message)
    suspend fun clearChatHistory() = chatMessageDao.deleteAll()

    // Preferences
    val preferences: Flow<List<UserPreference>> = preferenceDao.getAllPreferences()
    suspend fun getPreferenceValue(key: String): String? = preferenceDao.getValueByKey(key)
    suspend fun savePreference(key: String, value: String) {
        preferenceDao.insertPreference(UserPreference(key, value))
    }
    suspend fun removePreference(key: String) {
        preferenceDao.deleteByKey(key)
    }
}
