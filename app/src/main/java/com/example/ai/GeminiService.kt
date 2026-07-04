package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * Sends a chat prompt to Gemini and gets a response.
     */
    suspend fun getChatResponse(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        systemInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is missing or invalid. Please configure GEMINI_API_KEY in the Secrets panel in AI Studio."
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val contentsArray = JSONArray()

            // Add history
            for (turn in history) {
                val role = if (turn.first == "USER") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", turn.second)
                        })
                    })
                })
            }

            // Add current prompt
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", prompt)
                    })
                })
            })

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                if (systemInstruction != null) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemInstruction)
                            })
                        })
                    })
                }
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call failed: Status ${response.code}, Body: $bodyStr")
                    return@withContext "I'm having trouble connecting to the network right now. (Status ${response.code})"
                }

                if (bodyStr.isNullOrEmpty()) {
                    return@withContext "Received an empty response from the assistant."
                }

                val jsonResponse = JSONObject(bodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No response text found.")
                    }
                }
                "I was unable to process that. Please try again."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getChatResponse", e)
            "Error: ${e.localizedMessage ?: "Unknown connection error"}"
        }
    }

    /**
     * Uses Gemini to parse a natural language "Siri-like" command and return structured actions.
     */
    suspend fun parseSiriCommand(
        command: String,
        currentTasks: List<Task>
    ): SiriActionResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext SiriActionResponse(
                recognizedIntent = "ANSWER_DOUBT",
                responseText = "I cannot perform calendar updates without a valid API Key. Please add the GEMINI_API_KEY in the AI Studio secrets panel.",
                hasChanges = false
            )
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            
            // Format tasks list for context
            val tasksContext = currentTasks.joinToString("\n") { task ->
                "- ID: ${task.id}, Title: '${task.title}', Priority: ${task.priority}, Category: ${task.category}, Due: ${task.dueDate}, Completed: ${task.isCompleted}"
            }

            val systemInstruction = """
                You are Siri, an intelligent offline-first task scheduler and voice assistant for Aura AI Scheduler.
                Analyze the user's voice command in the context of their existing task list:
                
                Existing Tasks:
                $tasksContext
                
                Determine the user's intent. You MUST respond with a valid JSON object matching this schema:
                {
                  "recognizedIntent": "ADD_TASK" | "RESCHEDULE" | "GET_MOTIVATION" | "ANSWER_DOUBT" | "CHECK_CONFLICTS" | "TOGGLE_TASK",
                  "responseText": "Siri's conversational voice spoken reply (brief, professional, warm, Siri-like, e.g. 'I've added your Assignment to your schedule!')",
                  "taskTitle": "Title of task to add/update if applicable",
                  "taskPriority": "HIGH" | "MEDIUM" | "LOW" (defaults to MEDIUM),
                  "taskCategory": "Assignment" | "Class" | "Exam" | "Meeting" | "Personal" (defaults to Personal),
                  "taskDueDateOffsetHours": offset in hours from now (e.g., 24 for tomorrow, 2 for later today) or -1 if unchanged,
                  "targetTaskId": ID of the task being updated/completed if applicable or -1
                }
                
                Guidelines:
                - If they say "mark task X as done" or "complete X", intent is TOGGLE_TASK, targetTaskId is X.
                - If they ask general homework, math, physics, or academic questions, intent is ANSWER_DOUBT, and responseText contains the friendly Siri explanation.
                - If they say "remind me to grade exam tomorrow" or "add a class on chemistry", intent is ADD_TASK.
                - If they say "replan everything" or "solve conflicts", intent is CHECK_CONFLICTS.
                - Ensure the returned response is ONLY the JSON object, with no markdown code blocks.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Voice Command: \"$command\"")
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("responseFormat", JSONObject().apply {
                        put("type", "OBJECT")
                        put("responseMimeType", "application/json")
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (!response.isSuccessful || bodyStr.isNullOrEmpty()) {
                    return@withContext SiriActionResponse(
                        recognizedIntent = "ANSWER_DOUBT",
                        responseText = "I'm having a little trouble processing that. Can you repeat? (Status ${response.code})",
                        hasChanges = false
                    )
                }

                val jsonResponse = JSONObject(bodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val rawText = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val actionJson = JSONObject(rawText)
                    val intent = actionJson.optString("recognizedIntent", "ANSWER_DOUBT")
                    val replyText = actionJson.optString("responseText", "Done.")
                    val title = actionJson.optString("taskTitle", "")
                    val priority = actionJson.optString("taskPriority", "MEDIUM")
                    val category = actionJson.optString("taskCategory", "Personal")
                    val offset = actionJson.optInt("taskDueDateOffsetHours", -1)
                    val targetId = actionJson.optInt("targetTaskId", -1)

                    return@withContext SiriActionResponse(
                        recognizedIntent = intent,
                        responseText = replyText,
                        taskTitle = title,
                        taskPriority = priority,
                        taskCategory = category,
                        taskDueDateOffsetHours = offset,
                        targetTaskId = targetId,
                        hasChanges = intent == "ADD_TASK" || intent == "RESCHEDULE" || intent == "TOGGLE_TASK"
                    )
                }
                SiriActionResponse(
                    recognizedIntent = "ANSWER_DOUBT",
                    responseText = "I'm sorry, I couldn't understand that command.",
                    hasChanges = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Siri voice command", e)
            SiriActionResponse(
                recognizedIntent = "ANSWER_DOUBT",
                responseText = "I encountered an error: ${e.localizedMessage}. Would you like to try again?",
                hasChanges = false
            )
        }
    }

    /**
     * Uses Gemini to generate a personalized learning path/study guide.
     */
    suspend fun generateLearningPath(
        userGoal: String,
        userRole: String,
        upcomingTasks: List<Task>
    ): String = withContext(Dispatchers.IO) {
        val tasksContext = upcomingTasks.joinToString("\n") { task ->
            "- ${task.title} (Category: ${task.category}, Priority: ${task.priority})"
        }
        val prompt = """
            User Goal: "$userGoal"
            User Role: $userRole
            Upcoming Tasks & Schedule Context:
            $tasksContext
            
            Based on these details, please create a beautiful, highly personalized learning path.
            Provide:
            1. 📚 RECOMMENDED STUDY MATERIALS: Specific books, courses, or core topics to study first.
            2. 🕒 OPTIMAL FOCUS HOURS: Recommend times for quiet focus sessions based on their role and task priorities.
            3. 🛠️ SKILL DEVELOPMENT: Targeted skills to focus on with practical resources.
            
            Keep your advice structured with bullet points and clear emojis. Make it encouraging and highly actionable. Limit to 200 words.
        """.trimIndent()

        getChatResponse(prompt = prompt, systemInstruction = "You are an expert academic advisor. Provide direct, highly structured, actionable study schedules and skill development learning paths.")
    }
}

data class SiriActionResponse(
    val recognizedIntent: String,
    val responseText: String,
    val taskTitle: String = "",
    val taskPriority: String = "MEDIUM",
    val taskCategory: String = "Personal",
    val taskDueDateOffsetHours: Int = -1,
    val targetTaskId: Int = -1,
    val hasChanges: Boolean = false
)
