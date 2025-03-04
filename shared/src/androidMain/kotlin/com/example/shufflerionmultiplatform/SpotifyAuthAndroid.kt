package com.example.shufflerionmultiplatform

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse


class SpotifyAuthAndroid(private val activity: Activity) : SpotifyAuth {

    private val clientId = "335ea7b32dd24009bd0529ba85f0f8cc"
    private val redirectUri = "shufflerionApp://callback"
    val requestCode = 1337
    private var onTokenReceived: ((String) -> Unit)? = null
    private var launcher: ActivityResultLauncher<Intent>? = null


    override fun requestAccessToken(onTokenReceived: (String) -> Unit) {
        this.onTokenReceived = onTokenReceived
        val authBuilder =
            AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.TOKEN, redirectUri)
        authBuilder.setScopes(
            arrayOf(
                "user-library-read",
                "streaming",
                "user-read-playback-state",
                "user-modify-playback-state",
                "user-read-currently-playing",
                "user-read-private",
                "user-read-playback-position",
                "user-read-email"
            )
        )
        val request = authBuilder.build()
        println("Lanzando actividad de autorización: $onTokenReceived")
        val loginIntent = AuthorizationClient.createLoginActivityIntent(activity, request)
        launcher?.launch(loginIntent) ?: println("Launcher no configurado")
    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Any?) {
        println("handleActivityResult llamado con requestCode: $requestCode, resultCode: $resultCode")
        if (requestCode == this.requestCode) {
            val intent = data as? Intent
            if (intent == null) {
                println("Data no es un Intent válido")
                return
            }
            val response = AuthorizationClient.getResponse(resultCode, intent)
            println("response auth client: $response")
            println("refreshToken: ${response}")
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    println("Token recibido: ${response.accessToken}")
                    onTokenReceived?.invoke(response.accessToken)
                }

                AuthorizationResponse.Type.ERROR -> {
                    println("Error: ${response.error}")
                }

                else -> {
                    println("Respuesta no manejada: ${response.type}")
                }
            }
        }
    }

    fun registerLauncher(launcher: ActivityResultLauncher<Intent>) {
        this.launcher = launcher
        println("launcher configurado: $launcher")
    }
}

