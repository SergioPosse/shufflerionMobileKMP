package com.example.shufflerionmultiplatform

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.material.*
import androidx.compose.foundation.layout.*
import kotlin.random.Random

@Composable
fun mainScreen(spotifyAuth: SpotifyAuth, spotifyApi: SpotifyApi) {
    var emailHost = ""
    var emailGuest = ""
    var token by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Email del Host:")

        Button(onClick = {
            spotifyAuth.requestAccessToken { receivedToken ->
                println("Token recibido: $receivedToken")
                val sessionId = generateSessionId()
                spotifyApi.setAccessToken(receivedToken) // Establece el token en la API de Spotify
                token = receivedToken // Guarda el token en el estado
          }
        }) {
            Text("Obtener Access Token")
        }

        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                spotifyApi.saveSessionData(emailHost, emailGuest, token, sessionId)
            }
        }) {
            Text("Guardar sesión")
        }
    }

    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            val id = spotifyApi.getDeviceId()
            if (id != null) {
                deviceId = id
                println("Device ID: $deviceId")
//                val trackUri = "spotify:track:2s99JIa7LENyy9vmtBCrwR"
//                deviceId?.let { spotifyApi.playSong(it, trackUri) }
            } else {
                println("No se pudo obtener el Device ID")
            }
        }
    }

    Button(onClick = {
        val trackUri = "spotify:track:2s99JIa7LENyy9vmtBCrwR"
        deviceId?.let { spotifyApi.playSong(it, trackUri) }
    }) {
        Text("Play song")
    }

    LaunchedEffect(sessionId) {
        if (sessionId.isNotEmpty() && token.isNotEmpty()) {
            spotifyApi.saveSessionData(emailHost, emailGuest, token, sessionId)
        }
    }
}

fun generateSessionId(): String {
    return List(16) { Random.nextInt(0, 16).toString(16) }.joinToString("")
}
