package server

import handler.TimeHandler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import config.host
import config.port

class MCPServer(
    private val timeHandler: TimeHandler = TimeHandler()
) {
    fun start() {
        println("[MCP] Starting server on $host:$port...")
        ServerSocket(port).use { server ->
            while (true) {
                val client = server.accept()
                println("[MCP] Connection from ${client.inetAddress.hostAddress}")

                val input = BufferedReader(InputStreamReader(client.getInputStream()))
                val output = PrintWriter(client.getOutputStream(), true)

                var line: String? = null
                while (client.isConnected && input.readLine().also { line = it } != null) {
                    println("[MCP] Received: $line")
                    val response = timeHandler.handle(line!!)
                    output.println(response)
                }

                println("[MCP] Connection closed.")
                client.close()
            }
        }
    }
}
