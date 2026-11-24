package ngo.xnet.aiope.feature.chat.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class ToolDefinition(
    val name: String,
    val description: String,
    val parametersJson: String
)

object ToolRegistry {

    private val client = OkHttpClient()

    fun getAvailableTools(): List<ToolDefinition> {
        return listOf(
            ToolDefinition(
                name = "bash",
                description = "Executa um comando no ambiente shell local (Termux / PRoot Linux)",
                parametersJson = """{"type":"object","properties":{"command":{"type":"string"}},"required":["command"]}"""
            ),
            ToolDefinition(
                name = "read_file",
                description = "Lê o conteúdo de um arquivo do sistema de arquivos",
                parametersJson = """{"type":"object","properties":{"path":{"type":"string"}},"required":["path"]}"""
            ),
            ToolDefinition(
                name = "write_file",
                description = "Escreve conteúdo em um arquivo local",
                parametersJson = """{"type":"object","properties":{"path":{"type":"string"},"content":{"type":"string"}},"required":["path","content"]}"""
            ),
            ToolDefinition(
                name = "fetch_url",
                description = "Faz uma requisição HTTP GET para uma URL e retorna o conteúdo textual",
                parametersJson = """{"type":"object","properties":{"url":{"type":"string"}},"required":["url"]}"""
            )
        )
    }

    suspend fun executeTool(name: String, argsJson: String): String = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(argsJson)
            when (name) {
                "bash" -> {
                    val cmd = json.getString("command")
                    val process = ProcessBuilder("sh", "-c", cmd)
                        .redirectErrorStream(true)
                        .start()
                    val output = process.inputStream.bufferedReader().readText()
                    process.waitFor()
                    output
                }
                "read_file" -> {
                    val path = json.getString("path")
                    val file = File(path)
                    if (file.exists()) file.readText() else "Erro: Arquivo não encontrado: $path"
                }
                "write_file" -> {
                    val path = json.getString("path")
                    val content = json.getString("content")
                    val file = File(path)
                    file.parentFile?.mkdirs()
                    file.writeText(content)
                    "Sucesso: Arquivo gravado em $path (${content.length} caracteres)"
                }
                "fetch_url" -> {
                    val url = json.getString("url")
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        response.body?.string() ?: "Erro: Resposta vazia"
                    }
                }
                else -> "Erro: Ferramenta desconhecida: $name"
            }
        } catch (e: Exception) {
            "Erro executando ferramenta $name: ${e.message}"
        }
    }
}
