package com.example.shufflerionmultiplatform

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class SpotifyAuthAndroid(private val activity: Activity) : SpotifyAuth {

    private val clientId = "335ea7b32dd24009bd0529ba85f0f8cc"
    private val redirectUri = "shufflerionApp://callback"
    private val requestCode = 1337
    private var onTokenReceived: ((String) -> Unit)? = null

    override fun requestAccessToken(onTokenReceived: (String) -> Unit) {
        this.onTokenReceived = onTokenReceived
        val authBuilder = AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.TOKEN, redirectUri)
        authBuilder.setScopes(arrayOf("streaming", "user-modify-playback-state", "user-read-playback-state"))
        val request = authBuilder.build()
        AuthorizationClient.openLoginActivity(activity, requestCode, request)
    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Any?) {
        if (requestCode == this.requestCode) {
            val intent = data as? Intent
            val response = AuthorizationClient.getResponse(resultCode, intent)
            Log.e("SpotifyAuthResponse", "response: $response")
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    onTokenReceived?.invoke(response.accessToken)
                }
                AuthorizationResponse.Type.ERROR -> {
                    Log.e("SpotifyAuth", "Error: ${response.error}")
                }
                else -> {
                    Log.d("SpotifyAuth", "Respuesta no manejada: ${response.type}")
                }
            }
        }
    }
}

