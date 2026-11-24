package ngo.xnet.aiope.feature.chat.data

import kotlinx.coroutines.flow.Flow
import ngo.xnet.aiope.core.security.SecurityManager
import ngo.xnet.aiope.feature.chat.db.*
import java.util.UUID

class ChatRepository(private val chatDao: ChatDao) {

    fun getConversations(): Flow<List<ConversationEntity>> = chatDao.getAllConversations()

    suspend fun getConversation(id: String): ConversationEntity? = chatDao.getConversation(id)

    suspend fun ensureDefaultConversation(id: String = "default_conversation"): ConversationEntity {
        val existing = chatDao.getConversation(id)
        if (existing != null) return existing

        val defaultConv = ConversationEntity(
            id = id,
            title = "Conversa Geral",
            agentName = "Agente Principal",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertConversation(defaultConv)
        return defaultConv
    }

    fun getMessages(conversationId: String): Flow<List<MessageEntity>> = chatDao.getMessages(conversationId)

    suspend fun saveMessage(
        conversationId: String,
        role: String,
        content: String,
        modelUsed: String = ""
    ): MessageEntity {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = role,
            content = content,
            timestamp = System.currentTimeMillis(),
            modelUsed = modelUsed
        )
        chatDao.insertMessage(message)
        return message
    }

    suspend fun updateMessage(message: MessageEntity) {
        chatDao.insertMessage(message)
    }

    suspend fun saveSecureProvider(id: String, rawConfigJson: String) {
        val encryptedJson = SecurityManager.encrypt(rawConfigJson)
        val provider = ProviderEntity(
            id = id,
            json = encryptedJson,
            isActive = 1,
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertProvider(provider)
    }

    suspend fun getActiveDecryptedProvider(): String? {
        val provider = chatDao.getActiveProvider() ?: return null
        return SecurityManager.decrypt(provider.json)
    }
}
