package ngo.xnet.aiope.feature.chat.ui

import ngo.xnet.aiope.feature.chat.db.ConversationEntity
import ngo.xnet.aiope.feature.chat.db.MessageEntity

data class ChatUiState(
    val currentConversationId: String = "default_conversation",
    val conversationTitle: String = "Conversa Geral",
    val conversations: List<ConversationEntity> = emptyList(),
    val messages: List<MessageEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val statusText: String? = null,
    val activeProvider: String = "custom",
    val activeBaseUrl: String = "http://127.0.0.1:11434/v1",
    val activeModel: String = "llama3.2:1b"
)
