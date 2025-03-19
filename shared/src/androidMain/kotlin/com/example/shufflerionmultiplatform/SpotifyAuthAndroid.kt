package com.example.shufflerionmultiplatform

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse


class SpotifyAuthAndroid(private val activity: Activity, loggerParam: Logger) : SpotifyAuth {

    private val clientId = "335ea7b32dd24009bd0529ba85f0f8cc"
    private val redirectUri = "shufflerionApp://callback"
    val requestCode = 1337
    private var onTokenReceived: ((String) -> Unit)? = null
    private var launcher: ActivityResultLauncher<Intent>? = null
    private var logger: Logger = loggerParam

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
        logger.log("Lanzando actividad de autorización: $onTokenReceived")
        val loginIntent = AuthorizationClient.createLoginActivityIntent(activity, request)
        launcher?.launch(loginIntent) ?: logger.logError("Launcher no configurado")
    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Any?) {
        logger.log("handleActivityResult llamado con requestCode: $requestCode, resultCode: $resultCode")
        if (requestCode == this.requestCode) {
            val intent = data as? Intent
            if (intent == null) {
                logger.log("Data no es un Intent válido")
                return
            }
            val response = AuthorizationClient.getResponse(resultCode, intent)
            logger.log("response auth client: $response")
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    logger.log("Token recibido: ${response.accessToken}")
                    onTokenReceived?.invoke(response.accessToken)
                }

                AuthorizationResponse.Type.ERROR -> {
                    logger.logError("Error: ${response.error}")
                }

                else -> {
                    logger.logError("Respuesta no manejada: ${response.type}")
                }
            }
        }
    }

    fun registerLauncher(launcher: ActivityResultLauncher<Intent>) {
        this.launcher = launcher
        logger.log("launcher configurado: $launcher")
    }
}

