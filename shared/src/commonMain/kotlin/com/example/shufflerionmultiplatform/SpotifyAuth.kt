package com.example.shufflerionmultiplatform

interface SpotifyAuth {
    fun requestAccessToken(onTokenReceived: (String) -> Unit)
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Any?)
}
