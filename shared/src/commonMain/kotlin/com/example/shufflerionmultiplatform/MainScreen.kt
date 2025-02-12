package com.example.shufflerionmultiplatform

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.material.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun mainScreen(spotifyAuth: SpotifyAuth, spotifyApi: SpotifyApi) {
    var hostEmail by remember { mutableStateOf("") }
    var guestEmail by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Host email:")
        OutlinedTextField(
            value = hostEmail,
            onValueChange = { hostEmail = it },
            label = { Text("Enter Host Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = {
            spotifyAuth.requestAccessToken { receivedToken ->
                println("Token recibido: $receivedToken")
                sessionId = generateSessionId()
                spotifyApi.setAccessToken(receivedToken)
                token = receivedToken

                // Llamada a tu backend para guardar la sesión
                CoroutineScope(Dispatchers.IO).launch {
                    spotifyApi.saveSession(
                        sessionId = sessionId,
                        hostEmail = hostEmail,
                        accessToken = receivedToken,
                        refreshToken = "somedummytoken" // Aquí pon el refresh token real si lo tienes
                    )
                }
            }
        }) {
            Text("Get Spotify permission")
        }

        Text("Guest email:")
        OutlinedTextField(
            value = guestEmail,
            onValueChange = { guestEmail = it },
            label = { Text("Enter Guest Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                spotifyApi.saveSession(hostEmail, guestEmail, token, sessionId)
            }
        }) {
            Text("Share the link")
        }

//        Button(onClick = {
//            CoroutineScope(Dispatchers.IO).launch {
//                spotifyApi.saveSessionData(emailHost, emailGuest, token, sessionId)
//            }
//        }) {
//            Text("guardar sesión")
//        }
        Button(onClick = {
            val trackUri = "spotify:track:2s99JIa7LENyy9vmtBCrwR"
            deviceId?.let { spotifyApi.playSong(it, trackUri) }
        }) {
            Text("Play test song")
        }

        Button(onClick = {
            println("click continuar")
        }) {
            Text("Continuar al reproductor")
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

    LaunchedEffect(sessionId) {
        if (sessionId.isNotEmpty() && token.isNotEmpty()) {
            spotifyApi.saveSession(hostEmail, guestEmail, token, sessionId)
        }
    }
}

fun generateSessionId(): String {
    return List(16) { Random.nextInt(0, 16).toString(16) }.joinToString("")
}
