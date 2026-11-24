package ngo.xnet.aiope.feature.chat.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class LlmConfig(
    val providerId: String, // "openai", "gemini", "deepseek", "custom"
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val temperature: Float = 0.7f
)

data class ChatMessage(
    val role: String,
    val content: String
)

class LlmEngine(private val config: LlmConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun streamChat(
        messages: List<ChatMessage>,
        systemPrompt: String = "",
        toolsEnabled: Boolean = true
    ): Flow<String> = flow {
        val url = if (config.baseUrl.endsWith("/")) "${config.baseUrl}chat/completions" else "${config.baseUrl}/chat/completions"

        val bodyJson = JSONObject().apply {
            put("model", config.model)
            put("stream", true)
            put("temperature", config.temperature)

            val msgsArray = JSONArray()
            if (systemPrompt.isNotBlank()) {
                msgsArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
            }
            for (msg in messages) {
                msgsArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
            put("messages", msgsArray)

            if (toolsEnabled) {
                val toolsArray = JSONArray()
                for (tool in ToolRegistry.getAvailableTools()) {
                    toolsArray.put(JSONObject().apply {
                        put("type", "function")
                        put("function", JSONObject().apply {
                            put("name", tool.name)
                            put("description", tool.description)
                            put("parameters", JSONObject(tool.parametersJson))
                        })
                    })
                }
                put("tools", toolsArray)
            }
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit("Erro na API LLM: HTTP ${response.code} - ${response.message}")
                return@flow
            }
            val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: break
                if (currentLine.startsWith("data: ")) {
                    val data = currentLine.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val json = JSONObject(data)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            val text = delta?.optString("content", "") ?: ""
                            if (text.isNotEmpty()) {
                                emit(text)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
