package com.example.shufflerionmultiplatform

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

@Serializable
data class WebSocketMessage(val action: String, val sessionId: String)

class WebSocketClient(
    private val sessionId: String,
    private val onMessageReceived: (String) -> Unit
) : CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()
    private val client = HttpClient(CIO) { install(WebSockets) }
    private var session: DefaultClientWebSocketSession? = null

    fun connect() {
        launch {
            try {
                client.webSocket("wss://shufflerionserver.onrender.com/session/socket") {
                    session = this
                    println("🔗 WebSocket conectado")

                    // Enviar mensaje de suscripción
                    val subscribeMessage = Json.encodeToString(WebSocketMessage("subscribe", sessionId))
                    send(Frame.Text(subscribeMessage))

                    // Escuchar mensajes entrantes
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val receivedMessage = frame.readText()
                                println("📩 Mensaje recibido: $receivedMessage")
                                onMessageReceived(receivedMessage) // Notificar a la UI
                            }
                            else -> Unit
                        }
                    }
                }
            } catch (e: Exception) {
                println("❌ Error en WebSocket: ${e.message}")
            }
        }
    }

    fun disconnect() {
        launch {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Cierre de WebSocket"))
            client.close()
            println("🔒 WebSocket cerrado")
        }
    }
}