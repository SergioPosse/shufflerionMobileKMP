package com.example.shufflerionmultiplatform

import com.spotify.protocol.types.PlayerState

interface SpotifyAppRemoteInterface {

    fun connect(onConnected: () -> Unit, onError: (Throwable) -> Unit)
    fun disconnect()
    fun subscribeToPlayerState(onStateChanged: (playerState: PlayerState) -> Unit)
}
