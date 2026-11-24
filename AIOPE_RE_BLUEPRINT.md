# AIOPE (ngo.xnet.aiope) — Relatório Completo de Engenharia Reversa e Arquitetura de Clone

> **Alvo Analisado**: `C:\Users\Carllos\Downloads\AIOPE\base.apk`  
> **SHA-256**: `b8f99a9dbd0bd9382aa50c251d2212da808e50970c5c758bccc3a5e5f239b4cc`  
> **Versão**: 4.6.0 (Version Code 44)  
> **Target SDK**: 34 (Android 14) | **Min SDK**: 26 (Android 8.0)  
> **Framework Principal**: Jetpack Compose + Room SQLite + WorkManager + Native Termux/PRoot

---

## 1. Visão Geral do Aplicativo

O **AIOPE** (All-In-One Personal Environment / Autonomous AI Agent) é uma plataforma avançada de **Agente de Inteligência Artificial autônomo rodando localmente no Android**.

Ele combina:
1. **Chat com Múltiplos Provedores de LLM**: OpenAI, Google Gemini, DeepSeek, Mistral, Together AI, OpenRouter e endpoints Customizados (Ollama/vLLM locais).
2. **Ambiente Linux Embutido (Termux + PRoot)**: Utiliza `libtermux.so`, `libproot.so`, `libproot-xed.so`, `libbsdtar.so` e `libtalloc.so` para executar comandos de terminal `bash` e ferramentas CLI completas direto no Android sem necessidade de root.
3. **Model Context Protocol (MCP)**: Suporte completo ao protocolo MCP da Anthropic para estender dinamicamente as ferramentas do agente via JSON-RPC.
4. **Gerenciamento de Servidores Remotos (SSH)**: Cliente SSH embutido (`net.schmizz.sshj` + `BouncyCastle`) com instalador automatizado de daemon (`assets/aiope-remote-installer.sh`) para orquestrar servidores Linux externos via `ssh_start`, `ssh_exec`, `ssh_exit`.
5. **Servidor HTTP Local de Arquivos**: `FileServerService` rodando em segundo plano para transferência e upload/download de artefatos.
6. **Agentes Autônomos e Agendamento (Cron/Intervalos)**: Tabela de tarefas agendadas (`scheduled_tasks_new`), histórico de execuções (`task_runs`), memória de longo prazo (`memories`) e workers em segundo plano (`AgentRunWorker`, `AgentRescheduleWorker`, `AgentAlarmReceiver`) mantidos vivos por `AiopeForegroundService` com `PARTIAL_WAKE_LOCK`.
7. **Motor RAG e Vetores Local**: Base SQLite com tabelas `documents`, `chunks` e `embeddings` (vetores binários BLOB) para busca semântica em arquivos do usuário.
8. **Renderização Rica**: Renderizador Markdown com suporte a LaTeX (`ru.noties.jlatexmath`) e mapas vetoriais nativos com `MapLibre Native Android SDK` (`libmaplibre.so`).

---

## 2. Esquema Completo do Banco de Dados (Room SQLite)

### Banco Principal (`ChatDatabase`):
- **`conversations`**:
  - `id` (TEXT PRIMARY KEY)
  - `title` (TEXT)
  - `agentName` (TEXT)
  - `createdAt` (INTEGER)
  - `updatedAt` (INTEGER)
- **`messages`**:
  - `id` (TEXT PRIMARY KEY)
  - `conversationId` (TEXT)
  - `role` (TEXT: `user`, `assistant`, `system`, `tool`)
  - `content` (TEXT)
  - `imagePaths` (TEXT)
  - `timestamp` (INTEGER)
  - `inputTokens` (INTEGER)
  - `outputTokens` (INTEGER)
  - `latencyMs` (INTEGER)
  - `modelUsed` (TEXT)
- **`agents`**:
  - `id` (TEXT PRIMARY KEY)
  - `name` (TEXT)
  - `prompt` (TEXT - Prompt de Sistema do Agente)
  - `model` (TEXT)
  - `tools` (TEXT - Ferramentas habilitadas)
  - `maxContext` (INTEGER DEFAULT 32000)
  - `temperature` (REAL DEFAULT 0.7)
  - `topP` (REAL DEFAULT 0.9)
  - `topK` (INTEGER DEFAULT 0)
  - `builtin` (INTEGER DEFAULT 0)
  - `createdAt` (INTEGER)
- **`agent_tasks`**:
  - `id` (TEXT PRIMARY KEY)
  - `agentId`, `agentName`, `prompt`, `status`, `result`, `toolCalls`, `startedAt`, `finishedAt`, `conversationId`, `scheduledTaskId`
- **`scheduled_tasks_new`**:
  - `id` (TEXT PRIMARY KEY)
  - `agentId`, `agentName`, `prompt`, `tools`
  - `scheduleType` (`once`, `interval`, `daily`, `weekly`)
  - `intervalValue`, `intervalUnit` (`min`, `hour`, `day`)
  - `timeHour`, `timeMinute`, `daysOfWeek`, `dayOfMonth`
  - `maxRuns`, `runsCompleted`, `status`, `reportMode` (`notification`, `chat`)
  - `conversationId`, `enabled`, `lastRun`, `nextRun`, `createdAt`
- **`task_runs`**:
  - `id` (INTEGER PRIMARY KEY AUTOINCREMENT)
  - `scheduledTaskId`, `runNumber`, `timestamp`, `prompt`, `output`, `status`
- **`memories`**:
  - `key` (TEXT PRIMARY KEY), `content`, `category`, `createdAt`, `updatedAt`
- **`providers`**:
  - `id` (TEXT PRIMARY KEY), `json` (Configuração com API key, base URL, modelos), `isActive`, `updatedAt`
- **`tool_toggles`**:
  - `toolId` (TEXT PRIMARY KEY), `enabled` (INTEGER)
- **`mcp_servers`**:
  - `id` (TEXT PRIMARY KEY), `json` (Configuração do servidor MCP)
- **`model_cache`**:
  - `builtinId` (TEXT PRIMARY KEY), `json`, `cachedAt`
- **`settings_kv`**:
  - `key` (TEXT PRIMARY KEY), `value` (TEXT)

### Banco de Servidores Remotos (`RemoteDatabase`):
- **`remote_servers`**:
  - `id` (TEXT PRIMARY KEY)
  - `name`, `host`, `port`, `user`, `bootstrapPort`
  - `privateKey`, `publicKey`, `password`
  - `osType`, `status`, `lastSeen`, `osInfo`, `daemonVersion`, `createdAt`

---

## 3. Ferramentas Nativas do Agente (Tool Registry)

1. **`bash`**: Executa comandos shell locais em ambiente Linux isolado (Termux C-runtime + PRoot).
2. **`read_file` / `write_file` / `list_directory`**: Acesso direto aos arquivos com escopo de storage.
3. **`fetch_url` / `web_search`**: Requisições HTTP e busca web.
4. **`ssh_start` / `ssh_exec` / `ssh_exit`**: Controle de instâncias remotas SSH via daemon `aiope-remote`.
5. **Integrações Android**: Disparo de alarmes (`SET_ALARM`), leitura de localização (`ACCESS_FINE_LOCATION`), envio de SMS, acesso a contatos e notificações.
6. **`mcp_servers`**: Conexão com qualquer servidor MCP local ou remoto.

---

## 4. Estrutura do Clone Criado

O projeto clone foi estruturado em:
`C:\Users\Carllos\.gemini\antigravity\scratch\AIOPE_Clone\`

```
AIOPE_Clone/
├── build.gradle.kts
├── settings.gradle.kts
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/ngo/xnet/aiope/
│       │   ├── AiopeApp.kt
│       │   ├── AiopeForegroundService.kt
│       │   ├── BootReceiver.kt
│       │   ├── MainActivity.kt
│       │   ├── feature/chat/db/
│       │   │   ├── Entities.kt
│       │   │   ├── ChatDao.kt
│       │   │   └── ChatDatabase.kt
│       │   ├── feature/chat/engine/
│       │   │   ├── AgentAlarmReceiver.kt
│       │   │   ├── AgentRescheduleWorker.kt
│       │   │   ├── AgentRunWorker.kt
│       │   │   ├── LlmEngine.kt
│       │   │   └── ToolRegistry.kt
│       │   ├── feature/chat/fileserver/
│       │   │   └── FileServerService.kt
│       │   ├── feature/remote/db/
│       │   │   └── RemoteDatabase.kt
│       │   └── ui/
│       │       ├── Theme.kt
│       │       └── ChatScreen.kt
│       └── res/
│           ├── values/strings.xml
│           └── xml/
│               ├── file_paths.xml
│               └── network_security_config.xml
└── AIOPE_RE_BLUEPRINT.md
```
