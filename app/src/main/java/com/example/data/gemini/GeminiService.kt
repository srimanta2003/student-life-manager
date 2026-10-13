package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class GeminiModelMode(val modelName: String, val displayName: String, val description: String) {
    GENERAL_FLASH(
        modelName = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        description = "Balanced & grounded with Google Search"
    ),
    HIGH_THINKING(
        modelName = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro (High Thinking)",
        description = "Deep reasoning & complex math/coding"
    ),
    LOW_LATENCY_LITE(
        modelName = "gemini-3.1-flash-lite",
        displayName = "Gemini 3.1 Flash Lite",
        description = "Ultra-fast response for quick questions"
    )
}

data class GeminiResponse(
    val text: String,
    val modelUsed: String,
    val sources: List<String> = emptyList(),
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

object GeminiService {

    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_INSTRUCTION =
        "You are 'EduMate', the intelligent academic tutor and college mentor for Student Life Manager. " +
        "You assist college and university students with course concepts, exam revision strategies, study task breakdowns, " +
        "schedule prioritization, and academic advice. Format responses with clean structure, markdown bullets, " +
        "and actionable recommendations. Be friendly, encouraging, and academically precise."

    suspend fun sendMessage(
        history: List<ChatMessage>,
        userMessage: String,
        mode: GeminiModelMode,
        enableSearchGrounding: Boolean = false,
        enableMapsGrounding: Boolean = false
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResponse(
                text = "⚠️ Gemini API key is not configured in Secrets. Please add your GEMINI_API_KEY in the AI Studio Secrets panel.",
                modelUsed = mode.modelName,
                isSuccess = false,
                errorMessage = "Missing API key"
            )
        }

        try {
            val rootJson = JSONObject()

            // System instruction
            val systemInstructionJson = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", SYSTEM_INSTRUCTION) })
                }
                put("parts", partsArray)
            }
            rootJson.put("systemInstruction", systemInstructionJson)

            // Conversation history contents
            val contentsArray = JSONArray()
            // Append past user/AI messages (limiting last 12 turns for token safety)
            val recentHistory = history.filter { it.sender != ChatSender.SYSTEM }.takeLast(12)
            for (msg in recentHistory) {
                val role = if (msg.sender == ChatSender.USER) "user" else "model"
                val partObj = JSONObject().apply { put("text", msg.text) }
                val contentObj = JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply { put(partObj) })
                }
                contentsArray.put(contentObj)
            }

            // Append current user message
            val currentTurn = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userMessage) })
                })
            }
            contentsArray.put(currentTurn)
            rootJson.put("contents", contentsArray)

            // Generation config
            val generationConfig = JSONObject()
            if (mode == GeminiModelMode.HIGH_THINKING) {
                // High thinking for complex queries on gemini-3.1-pro-preview
                val thinkingConfig = JSONObject().apply {
                    put("thinkingLevel", "HIGH")
                }
                generationConfig.put("thinkingConfig", thinkingConfig)
            }
            rootJson.put("generationConfig", generationConfig)

            // Tools (Search & Maps grounding)
            val toolsArray = JSONArray()
            if (enableSearchGrounding || mode == GeminiModelMode.GENERAL_FLASH) {
                toolsArray.put(JSONObject().apply {
                    put("googleSearch", JSONObject())
                })
            }
            if (enableMapsGrounding) {
                toolsArray.put(JSONObject().apply {
                    put("googleMaps", JSONObject())
                })
            }
            if (toolsArray.length() > 0) {
                rootJson.put("tools", toolsArray)
            }

            val requestUrl = "$BASE_URL${mode.modelName}:generateContent?key=$apiKey"
            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API failed with HTTP ${response.code}: $bodyStr")
                    return@withContext GeminiResponse(
                        text = "Gemini request failed: HTTP ${response.code}",
                        modelUsed = mode.modelName,
                        isSuccess = false,
                        errorMessage = bodyStr
                    )
                }

                val responseJson = JSONObject(bodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext GeminiResponse(
                        text = "No response generated by model.",
                        modelUsed = mode.modelName,
                        isSuccess = false
                    )
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")

                val textBuilder = StringBuilder()
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text")
                        if (text.isNotEmpty()) {
                            textBuilder.append(text)
                        }
                    }
                }

                // Extract grounding sources if present
                val sourcesList = mutableListOf<String>()
                val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
                if (groundingMetadata != null) {
                    val searchChunks = groundingMetadata.optJSONArray("groundingChunks")
                    if (searchChunks != null) {
                        for (i in 0 until searchChunks.length()) {
                            val chunk = searchChunks.optJSONObject(i)
                            val web = chunk?.optJSONObject("web")
                            val title = web?.optString("title")
                            val uri = web?.optString("uri")
                            if (!title.isNullOrBlank() && !uri.isNullOrBlank()) {
                                sourcesList.add("$title: $uri")
                            }
                        }
                    }
                }

                val outputText = textBuilder.toString().ifBlank { "I received your query but was unable to generate text." }
                GeminiResponse(
                    text = outputText,
                    modelUsed = mode.modelName,
                    sources = sourcesList,
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Gemini call", e)
            GeminiResponse(
                text = "Connection error: ${e.localizedMessage ?: e.message}",
                modelUsed = mode.modelName,
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }
}
