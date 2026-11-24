package ngo.xnet.aiope.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ngo.xnet.aiope.feature.chat.data.ChatRepository
import ngo.xnet.aiope.feature.chat.db.ConversationEntity
import ngo.xnet.aiope.feature.chat.db.MessageEntity
import ngo.xnet.aiope.feature.chat.engine.*
import java.util.UUID

class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var messagesJob: Job? = null

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            repository.getConversations().collect { convs ->
                _uiState.update { it.copy(conversations = convs) }
                if (convs.isEmpty()) {
                    repository.ensureDefaultConversation(_uiState.value.currentConversationId)
                }
            }
        }
        observeMessages(_uiState.value.currentConversationId)
    }

    fun selectConversation(conversationId: String) {
        if (conversationId == _uiState.value.currentConversationId) return
        activeJob?.cancel()
        _uiState.update { it.copy(currentConversationId = conversationId, isGenerating = false, statusText = null) }
        observeMessages(conversationId)
    }

    fun createNewConversation(title: String = "Nova Conversa") {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val newConv = ConversationEntity(
                id = newId,
                title = title,
                agentName = "Agente Principal",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.ensureDefaultConversation(newId)
            selectConversation(newId)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            val conv = repository.getConversation(conversationId)
            if (conv != null) {
                // Seleciona outra conversa antes de excluir se for a ativa
                if (_uiState.value.currentConversationId == conversationId) {
                    val remaining = _uiState.value.conversations.filter { it.id != conversationId }
                    if (remaining.isNotEmpty()) {
                        selectConversation(remaining.first().id)
                    } else {
                        createNewConversation()
                    }
                }
            }
        }
    }

    private fun observeMessages(conversationId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessages(conversationId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    fun updateProviderSettings(provider: String, apiKey: String, baseUrl: String, model: String) {
        _uiState.update {
            it.copy(
                activeProvider = provider,
                activeBaseUrl = baseUrl,
                activeModel = model
            )
        }
        viewModelScope.launch {
            val configJson = """{"apiKey":"$apiKey","baseUrl":"$baseUrl","model":"$model"}"""
            repository.saveSecureProvider(provider, configJson)
        }
    }

    fun sendMessage(content: String) {
        val state = _uiState.value
        if (content.isBlank() || state.isGenerating) return

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            val convId = state.currentConversationId
            repository.saveMessage(convId, "user", content)

            _uiState.update { it.copy(isGenerating = true, statusText = "Pensando...") }

            val agentMsgId = UUID.randomUUID().toString()
            var currentText = ""

            val llmConfig = LlmConfig(
                providerId = state.activeProvider,
                apiKey = "stored",
                baseUrl = state.activeBaseUrl,
                model = state.activeModel
            )
            val reactEngine = ReActAgentEngine(LlmEngine(llmConfig))

            try {
                reactEngine.executeAutonomousTask(
                    initialPrompt = content,
                    history = emptyList()
                ).collect { step ->
                    when (step) {
                        is AgentStep.Thinking -> _uiState.update { it.copy(statusText = step.thought) }
                        is AgentStep.ToolCallStarted -> _uiState.update { it.copy(statusText = "Executando: ${step.toolName}...") }
                        is AgentStep.ToolCallCompleted -> _uiState.update { it.copy(statusText = "Concluído: ${step.toolName}") }
                        is AgentStep.ContentDelta -> {
                            currentText += step.text
                            repository.updateMessage(
                                MessageEntity(
                                    id = agentMsgId,
                                    conversationId = convId,
                                    role = "assistant",
                                    content = currentText,
                                    timestamp = System.currentTimeMillis(),
                                    modelUsed = state.activeModel
                                )
                            )
                        }
                        is AgentStep.Finished -> {
                            _uiState.update { it.copy(isGenerating = false, statusText = null) }
                        }
                        is AgentStep.Error -> {
                            _uiState.update { it.copy(statusText = step.message, isGenerating = false) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusText = "Erro: ${e.message}", isGenerating = false) }
            } finally {
                _uiState.update { it.copy(isGenerating = false, statusText = null) }
            }
        }
    }

    fun stopGeneration() {
        activeJob?.cancel()
        activeJob = null
        _uiState.update { it.copy(isGenerating = false, statusText = "Geração cancelada.") }
    }
}
