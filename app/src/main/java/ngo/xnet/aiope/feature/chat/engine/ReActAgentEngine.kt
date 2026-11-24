package ngo.xnet.aiope.feature.chat.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject

sealed class AgentStep {
    data class Thinking(val thought: String) : AgentStep()
    data class ToolCallStarted(val toolName: String, val argsJson: String) : AgentStep()
    data class ToolCallCompleted(val toolName: String, val output: String) : AgentStep()
    data class ContentDelta(val text: String) : AgentStep()
    data class Finished(val finalAnswer: String) : AgentStep()
    data class Error(val message: String) : AgentStep()
}

class ReActAgentEngine(
    private val llmEngine: LlmEngine,
    private val maxIterations: Int = 5
) {

    fun executeAutonomousTask(
        initialPrompt: String,
        history: List<ChatMessage>,
        systemPrompt: String = "Você é o AIOPE, um agente autônomo rodando localmente no Android. Use as ferramentas necessárias para responder ao usuário."
    ): Flow<AgentStep> = flow {
        val currentConversation = history.toMutableList()
        currentConversation.add(ChatMessage(role = "user", content = initialPrompt))

        var iterations = 0
        var isCompleted = false
        val finalContent = StringBuilder()

        try {
            while (!isCompleted && iterations < maxIterations) {
                iterations++
                emit(AgentStep.Thinking("Raciocinando (Etapa $iterations de $maxIterations)..."))

                var stepOutput = ""
                var pendingToolName: String? = null
                var pendingToolArgs: String? = null

                llmEngine.streamChat(
                    messages = currentConversation,
                    systemPrompt = systemPrompt,
                    toolsEnabled = true
                ).collect { delta ->
                    // Checa se o delta contém chamada de função ou texto comum
                    if (delta.contains("\"function\"") || delta.contains("\"name\"")) {
                        try {
                            val json = JSONObject(delta)
                            pendingToolName = json.optString("name")
                            pendingToolArgs = json.optString("arguments")
                        } catch (_: Exception) {}
                    } else {
                        stepOutput += delta
                        emit(AgentStep.ContentDelta(delta))
                    }
                }

                if (pendingToolName != null && pendingToolArgs != null) {
                    emit(AgentStep.ToolCallStarted(pendingToolName!!, pendingToolArgs!!))
                    val toolResult = ToolRegistry.executeTool(pendingToolName!!, pendingToolArgs!!)
                    emit(AgentStep.ToolCallCompleted(pendingToolName!!, toolResult))

                    currentConversation.add(ChatMessage(role = "assistant", content = "Chamando ferramenta $pendingToolName com $pendingToolArgs"))
                    currentConversation.add(ChatMessage(role = "tool", content = toolResult))
                } else {
                    isCompleted = true
                    finalContent.append(stepOutput)
                    emit(AgentStep.Finished(stepOutput))
                }
            }
        } catch (e: CancellationException) {
            emit(AgentStep.Error("Geração interrompida pelo usuário."))
            throw e
        } catch (e: Exception) {
            emit(AgentStep.Error("Erro no ciclo ReAct do agente: ${e.message}"))
        }
    }
}
