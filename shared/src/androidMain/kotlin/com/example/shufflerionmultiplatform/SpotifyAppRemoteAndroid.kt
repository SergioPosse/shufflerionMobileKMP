package com.example.shufflerionmultiplatform

import android.content.Context
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState

class SpotifyAppRemoteAndroid(private val context: Context, loggerParam: Logger) : SpotifyAppRemoteInterface {

    private val clientId = "335ea7b32dd24009bd0529ba85f0f8cc"
    private val redirectUri = "shufflerionApp://callback"
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var logger: Logger = loggerParam

    override fun connect(onConnected: () -> Unit, onError: (Throwable) -> Unit) {
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                logger.log("Conectado a Spotify")
                onConnected()
            }

            override fun onFailure(throwable: Throwable) {
                logger.logError("Error al conectar: ${throwable.message}")
                onError(throwable)
            }
        })
    }

    override fun disconnect() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
            logger.log("Desconectado de Spotify")
        }
    }

    override fun subscribeToPlayerState(onStateChanged: (playerState: PlayerState) -> Unit) {
        spotifyAppRemote?.playerApi?.subscribeToPlayerState()
            ?.setEventCallback { playerState: PlayerState ->
                onStateChanged(playerState)
            }
    }
}
