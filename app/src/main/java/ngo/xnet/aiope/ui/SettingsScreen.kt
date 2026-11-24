package ngo.xnet.aiope.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentProvider: String,
    currentBaseUrl: String,
    currentModel: String,
    onSaveSettings: (provider: String, apiKey: String, baseUrl: String, model: String) -> Unit,
    onBack: () -> Unit
) {
    val providers = listOf("custom", "openai", "gemini", "deepseek", "groq")
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var apiKey by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var baseUrl by remember { mutableStateOf(currentBaseUrl) }
    var modelName by remember { mutableStateOf(currentModel) }
    var savedFeedback by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações de IA") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Provedor de LLM", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                providers.forEach { p ->
                    FilterChip(
                        selected = selectedProvider == p,
                        onClick = {
                            selectedProvider = p
                            when (p) {
                                "custom" -> {
                                    baseUrl = "http://127.0.0.1:11434/v1"
                                    modelName = "llama3.2:1b"
                                }
                                "openai" -> {
                                    baseUrl = "https://api.openai.com/v1"
                                    modelName = "gpt-4o-mini"
                                }
                                "gemini" -> {
                                    baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai"
                                    modelName = "gemini-1.5-flash"
                                }
                                "deepseek" -> {
                                    baseUrl = "https://api.deepseek.com/v1"
                                    modelName = "deepseek-chat"
                                }
                                "groq" -> {
                                    baseUrl = "https://api.groq.com/openai/v1"
                                    modelName = "llama-3.1-8b-instant"
                                }
                            }
                        },
                        label = { Text(p.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL da API") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("Nome do Modelo") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Chave de API (Criptografada no Keystore)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                        Icon(
                            if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Mostrar chave"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onSaveSettings(selectedProvider, apiKey, baseUrl, modelName)
                    savedFeedback = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar Configurações")
            }

            if (savedFeedback) {
                Text(
                    "Configurações gravadas e criptografadas com sucesso!",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
