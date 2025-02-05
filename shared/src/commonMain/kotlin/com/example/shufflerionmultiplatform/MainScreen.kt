package com.example.shufflerionmultiplatform

import com.example.shufflerionmultiplatform.SpotifyAuth

fun MainScreen(spotifyAuth: SpotifyAuth) {
    spotifyAuth.requestAccessToken { token ->
        println("Access token recibido: $token")
    }
}