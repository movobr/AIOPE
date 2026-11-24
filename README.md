# AIOPE — Autonomous AI Agent & Local Linux Environment for Android

<p align="center">
  <b>Ambiente de Agente Autônomo com LLMs, Execução Local Linux (PRoot/Termux) e Gerenciamento SSH para Android</b>
</p>

---

## 🚀 Sobre o Projeto

O **AIOPE** é uma plataforma que transforma o smartphone Android em um assistente de inteligência artificial autônomo, capaz de raciocinar com múltiplos passos (ReAct Loop), orquestrar ferramentas no próprio aparelho e conectar-se a servidores remotos.

Reconstruído e modernizado em **Kotlin** e **Jetpack Compose Material3**, seguindo arquitetura limpa (**MVVM** com Repositório desacoplado) e padrões de segurança com hardware Keystore.

---

## 🛠️ Principais Recursos

- **Múltiplos Provedores de IA**: Suporte direto e presets para Ollama local, OpenAI, Google Gemini, DeepSeek e Groq.
- **Criptografia por Hardware (Android Keystore)**: Chaves de API e credenciais de servidores criptografadas com AES-256-GCM antes de persistir no banco de dados.
- **Motor Autônomo ReAct com Cancelamento**: Ciclo contínuo de pensamento, chamada de ferramenta, leitura de resultados e síntese com botão de parada imediata (Stop).
- **Barra Lateral de Múltiplas Conversas**: Criação, navegação e persistência de múltiplos tópicos de conversa utilizando Room SQLite.
- **Blocos de Código Interativos**: Mensagens com código contam com visualização monoespaçada e botão dedicado para cópia para a área de transferência.
- **Entrada e Saída por Voz**: Reconhecimento de fala (`SpeechRecognizer`) e sintetizador de voz (`TextToSpeech`) nativos em Português (Brasil).
- **Serviço de Segundo Plano Persistente**: `AiopeForegroundService` com `PARTIAL_WAKE_LOCK` e WorkManager para tarefas agendadas autônomas.
- **Cliente e Servidor Integrados**: Gerenciador de servidores Linux remotos via SSH (`SSHJ` + `BouncyCastle`) e servidor HTTP local (`FileServerService`).

---

## 📂 Arquitetura do Código

```
app/src/main/java/ngo/xnet/aiope/
├── AiopeApp.kt                  # Inicialização da aplicação e agendador
├── AiopeForegroundService.kt    # Serviço persistente de segundo plano com WakeLock
├── BootReceiver.kt              # Inicializador após reinicialização do dispositivo
├── MainActivity.kt              # Activity principal desacoplada
├── core/
│   └── security/
│       └── SecurityManager.kt   # Criptografia AES-GCM via AndroidKeyStore
├── feature/chat/
│   ├── data/
│   │   └── ChatRepository.kt    # Repositório central de dados e persistência
│   ├── db/
│   │   ├── ChatDatabase.kt      # Room Database
│   │   ├── ChatDao.kt           # Consultas e inserções
│   │   └── Entities.kt          # Esquemas das 12 tabelas principais
│   ├── engine/
│   │   ├── LlmEngine.kt         # Comunicação com APIs de LLM (SSE Streaming)
│   │   ├── ReActAgentEngine.kt  # Loop de raciocínio autônomo e ferramentas
│   │   ├── ToolRegistry.kt      # Registro de ferramentas nativas
│   │   └── Workers.kt           # WorkManager para tarefas em background
│   ├── fileserver/
│   │   └── FileServerService.kt # Servidor HTTP local de arquivos
│   ├── ui/
│   │   ├── ChatUiState.kt       # Estado imutável da interface
│   │   └── ChatViewModel.kt     # ViewModel com StateFlow
│   └── voice/
│       └── SpeechAssistant.kt   # Integração com STT e TTS nativo
├── feature/remote/
│   └── db/
│       └── RemoteDatabase.kt    # Gerenciador de servidores remotos SSH
└── ui/
    ├── Theme.kt                 # Tema Material3 Dark
    ├── ChatScreen.kt            # Interface de chat com gaveta e blocos de código
    └── SettingsScreen.kt        # Configuração de chaves e provedores de IA
```

---

## 📋 Relatório de Engenharia Reversa

Para detalhes sobre a extração dos esquemas SQLite originais, binários C/C++ do Termux/PRoot e fluxo de decompilação, consulte [AIOPE_RE_BLUEPRINT.md](AIOPE_RE_BLUEPRINT.md).

---

## 📄 Licença

Distribuído sob a licença MIT. Consulte `LICENSE` para mais informações.
