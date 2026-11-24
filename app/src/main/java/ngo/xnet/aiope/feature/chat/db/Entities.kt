package ngo.xnet.aiope.feature.chat.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val agentName: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val imagePaths: String = "",
    val timestamp: Long,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val latencyMs: Int = 0,
    val modelUsed: String = ""
)

@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val prompt: String,
    val model: String = "",
    val tools: String = "", // Comma-separated or JSON list of enabled tools
    val maxContext: Int = 32000,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 0,
    val builtin: Int = 0,
    val createdAt: Long
)

@Entity(tableName = "agent_tasks")
data class AgentTaskEntity(
    @PrimaryKey val id: String,
    val agentId: String,
    val agentName: String,
    val prompt: String,
    val status: String = "queued", // "queued", "running", "finished", "failed"
    val result: String = "",
    val toolCalls: String = "",
    val startedAt: Long,
    val finishedAt: Long? = null,
    val conversationId: String? = null,
    val scheduledTaskId: String? = null
)

@Entity(tableName = "scheduled_tasks_new")
data class ScheduledTaskEntity(
    @PrimaryKey val id: String,
    val agentId: String,
    val agentName: String,
    val prompt: String,
    val tools: String = "",
    val scheduleType: String = "once", // "once", "interval", "daily", "weekly"
    val intervalValue: Int = 0,
    val intervalUnit: String = "min", // "min", "hour", "day"
    val timeHour: Int = 0,
    val timeMinute: Int = 0,
    val daysOfWeek: String = "",
    val dayOfMonth: Int = 1,
    val maxRuns: Int = 0,
    val runsCompleted: Int = 0,
    val status: String = "scheduled", // "scheduled", "running", "paused", "completed"
    val reportMode: String = "notification", // "notification", "chat"
    val conversationId: String? = null,
    val enabled: Int = 1,
    val lastRun: Long? = null,
    val nextRun: Long? = null,
    val createdAt: Long
)

@Entity(tableName = "task_runs")
data class TaskRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduledTaskId: String,
    val runNumber: Int,
    val timestamp: Long,
    val prompt: String = "",
    val output: String = "",
    val status: String = "finished"
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val key: String,
    val content: String,
    val category: String = "general",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String, // "openai", "gemini", "deepseek", "custom", etc.
    val json: String, // JSON containing apiKey, baseUrl, defaultModel, headers
    val isActive: Int = 0,
    val updatedAt: Long
)

@Entity(tableName = "tool_toggles")
data class ToolToggleEntity(
    @PrimaryKey val toolId: String,
    val enabled: Int
)

@Entity(tableName = "mcp_servers")
data class McpServerEntity(
    @PrimaryKey val id: String,
    val json: String // JSON config with command, args, env, sseUrl, headers
)

@Entity(tableName = "model_cache")
data class ModelCacheEntity(
    @PrimaryKey val builtinId: String,
    val json: String,
    val cachedAt: Long
)

@Entity(tableName = "settings_kv")
data class SettingsKvEntity(
    @PrimaryKey val key: String,
    val value: String
)
