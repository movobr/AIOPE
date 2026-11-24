package ngo.xnet.aiope.feature.chat.fileserver

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class FileServerService : Service() {

    private var serverSocket: ServerSocket? = null
    private val port = 8088
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        startServer()
    }

    private fun startServer() {
        serviceScope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.i("FileServerService", "HTTP File Server started on port $port")
                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                Log.e("FileServerService", "File server error", e)
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            val input = BufferedReader(InputStreamReader(s.getInputStream()))
            val output = BufferedOutputStream(s.getOutputStream())
            val line = input.readLine() ?: return
            val parts = line.split(" ")
            if (parts.size >= 2 && parts[0] == "GET") {
                val path = parts[1]
                val response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\nAIOPE File Server: $path"
                output.write(response.toByteArray())
                output.flush()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        serverSocket?.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
