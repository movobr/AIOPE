package ngo.xnet.aiope

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ngo.xnet.aiope.feature.chat.data.ChatRepository
import ngo.xnet.aiope.feature.chat.ui.ChatViewModel
import ngo.xnet.aiope.feature.chat.voice.SpeechAssistant
import ngo.xnet.aiope.ui.AiopeTheme
import ngo.xnet.aiope.ui.ChatScreen
import ngo.xnet.aiope.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private var speechAssistant: SpeechAssistant? = null

    private val viewModel: ChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = (application as AiopeApp).chatDatabase
                val repository = ChatRepository(db.chatDao())
                return ChatViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, AiopeForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        speechAssistant = SpeechAssistant(this) { spokenText ->
            viewModel.sendMessage(spokenText)
        }

        setContent {
            AiopeTheme {
                val uiState by viewModel.uiState.collectAsState()
                var currentScreen by remember { mutableStateOf("chat") }

                if (currentScreen == "settings") {
                    SettingsScreen(
                        currentProvider = uiState.activeProvider,
                        currentBaseUrl = uiState.activeBaseUrl,
                        currentModel = uiState.activeModel,
                        onSaveSettings = { provider, apiKey, baseUrl, model ->
                            viewModel.updateProviderSettings(provider, apiKey, baseUrl, model)
                        },
                        onBack = { currentScreen = "chat" }
                    )
                } else {
                    ChatScreen(
                        state = uiState,
                        onSendMessage = { viewModel.sendMessage(it) },
                        onStopGeneration = { viewModel.stopGeneration() },
                        onVoiceInputClick = { speechAssistant?.startListening() },
                        onSelectConversation = { convId -> viewModel.selectConversation(convId) },
                        onNewConversation = { viewModel.createNewConversation() },
                        onOpenSettings = { currentScreen = "settings" }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechAssistant?.destroy()
    }
}
