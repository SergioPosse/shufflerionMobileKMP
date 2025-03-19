package com.example.shufflerionmultiplatform

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.material.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import org.json.JSONObject

@Composable
fun WebSocketScreen(logger: Logger, sessionId: String, spotifyApi: SpotifyApi, onContinue: () -> Unit) {
    var isButtonEnabled by remember { mutableStateOf(false) }
    val webSocketManager = remember {
        WebSocketManager(logger, sessionId, onMessageReceived = { message ->
            logger.log("message socket: $message")
            val accessToken = extractAccessToken(message)
            val refreshToken = extractRefreshToken(message)
            if (!accessToken.isNullOrEmpty()) {
                logger.log("token2: $accessToken")
                spotifyApi.setAccessToken2(accessToken)
                if (refreshToken != null) {
                    spotifyApi.setRefreshToken2(refreshToken)
                }
                isButtonEnabled = true
            }
        })
    }

    LaunchedEffect(sessionId) {
        webSocketManager.connect()
    }

    if (isButtonEnabled) {
        LaunchedEffect(Unit) {
            onContinue()
            webSocketManager.close()
        }
    }
}


@Composable
fun MainContent(
    spotifyAuth: SpotifyAuth,
    spotifyApi: SpotifyApi,
    goToPlayer: () -> Unit,
    clipboardManager: ClipboardManager,
    spotifyAppRemote: SpotifyAppRemoteInterface,
    logger: Logger
) {

    var hostEmail by remember { mutableStateOf("") }
    var guestEmail by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf<String?>(null) }
    var isButtonEnabled by remember { mutableStateOf(true) }
    var canContinue by remember { mutableStateOf(false) }
    val canShareLink by remember { derivedStateOf { sessionId.isNotEmpty() && guestEmail.isNotEmpty() } }
    var isWebSocketActive by remember {
        mutableStateOf(false)
    }
    val refreshInterval = 2500 * 1000L

    fun refreshAccessToken() {
        spotifyAuth.requestAccessToken { receivedToken ->
            logger.log("Token refresh recibido: $receivedToken")
            spotifyApi.setAccessToken(receivedToken)
            token = receivedToken
        }
    }

    fun startTokenRefreshLoop() {
        logger.log("refreshacces token loop started")
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(refreshInterval)
                refreshAccessToken()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Host email:")
        OutlinedTextField(
            value = hostEmail,
            onValueChange = { hostEmail = it },
            label = { Text("Enter Host Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isButtonEnabled
        )

        Button(
            onClick = {
                spotifyAuth.requestAccessToken { receivedToken ->
                    logger.log("Token recibido: $receivedToken")
                    sessionId = generateSessionId()
                    spotifyApi.setAccessToken(receivedToken)
                    token = receivedToken
                    startTokenRefreshLoop()
                }
            }, enabled = isButtonEnabled
        ) {
            Text("Get Spotify permission")
        }

        Text("Guest email:")
        OutlinedTextField(
            value = guestEmail,
            onValueChange = { guestEmail = it },
            label = { Text("Enter Guest Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val link =
                    "https://shufflerionauth.onrender.com?guestEmail=$guestEmail&sessionId=$sessionId"
                clipboardManager.copyToClipboard(link)
                logger.log("click share link $guestEmail with $sessionId")
                isWebSocketActive = true

            },
            enabled = canShareLink
        ) {
            Text("Share the link")
        }

        if (isWebSocketActive) {
            WebSocketScreen(logger, sessionId = sessionId, spotifyApi, onContinue = {
                canContinue = true
            })
        }

        Button(onClick = goToPlayer, enabled = canContinue) {
            Text("Continuar al reproductor")
        }
    }

    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            val id = spotifyApi.getDeviceId()
            if (id != null) {
                deviceId = id
                logger.log("Device ID: $deviceId")
            } else {
                logger.logError("No se pudo obtener el device id")
            }
        }
    }

    LaunchedEffect(sessionId) {
        if (sessionId.isNotEmpty() && token.isNotEmpty()) {
            val success = spotifyApi.saveSession(
                sessionId = sessionId,
                hostEmail = hostEmail,
                accessToken = token,
                refreshToken = "somedummytoken"
            )
            logger.log("success $success")
            if (success) {
                isButtonEnabled = false
            }
        }
    }

    LaunchedEffect(Unit) {
        spotifyAppRemote.connect(
            onConnected = { println("Conectado a Spotify") },
            onError = { println("Error en conexión: ${it.message}")}
        )
    }

    LaunchedEffect(Unit) {
        spotifyAppRemote.subscribeToPlayerState { trackName ->
            logger.log("Reproduciendo: $trackName")
        }
    }
}

fun generateSessionId(): String {
    return List(16) { Random.nextInt(0, 16).toString(16) }.joinToString("")
}

class WebSocketManager(
    private val logger: Logger,
    private val sessionId: String,
    private val onMessageReceived: (String) -> Unit
) {
    private var socketSession: WebSocketSession? = null
    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    suspend fun connect() {
        client.webSocket(
            method = HttpMethod.Get,
            host = "shufflerionserver.onrender.com",
            port = 80,
            path = "/session/socket"
        ) {
            socketSession = this
            logger.log("Conectado al WebSocket")

            val subscribeMessage = """{
                "action": "subscribe",
                "sessionId": "$sessionId"
            }"""
            send(Frame.Text(subscribeMessage))

            incoming.consumeEach { message ->
                if (message is Frame.Text) {
                    onMessageReceived(message.readText())
                }
            }
        }
    }

    suspend fun close() {
        socketSession?.close()
    }
}

fun extractAccessToken(message: String): String? {
    val jsonObject = JSONObject(message)
    val dataObject = jsonObject.optJSONObject("data")
    val guestObject = dataObject?.optJSONObject("Guest")
    val tokensObject = guestObject?.optJSONObject("Tokens")
    return tokensObject?.optString("AccessToken")
}

fun extractRefreshToken(message: String): String? {
    val jsonObject = JSONObject(message)
    val dataObject = jsonObject.optJSONObject("data")
    val guestObject = dataObject?.optJSONObject("Guest")
    val tokensObject = guestObject?.optJSONObject("Tokens")
    return tokensObject?.optString("RefreshToken")
}