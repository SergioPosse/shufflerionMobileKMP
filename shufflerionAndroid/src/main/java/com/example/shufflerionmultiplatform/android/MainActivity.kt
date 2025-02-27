package com.example.shufflerionmultiplatform.android

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.shufflerionmultiplatform.MainContent
import com.example.shufflerionmultiplatform.Navigation
import com.example.shufflerionmultiplatform.SpotifyApi
import com.example.shufflerionmultiplatform.SpotifyAppRemoteAndroid
import com.example.shufflerionmultiplatform.SpotifyAuth
import com.example.shufflerionmultiplatform.SpotifyAuthAndroid
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.serialization.json.Json

class FakeSpotifyAuth : SpotifyAuth {
    override fun requestAccessToken(onTokenReceived:     (String) -> Unit){}
    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Any?){}
}

class FakeLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
}


class MainActivity : ComponentActivity() {

    private lateinit var spotifyAuth: SpotifyAuthAndroid
    private lateinit var spotifyApi: SpotifyApi
    private lateinit var spotifyAppRemote: SpotifyAppRemoteAndroid


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spotifyAuth = SpotifyAuthAndroid(this)
        spotifyApi = SpotifyApi(HttpClient())
        spotifyAppRemote = SpotifyAppRemoteAndroid(this)

        val spotifyAuthLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                println("Resultado recibido: resultCode = ${result.resultCode}")
                if (result.resultCode == Activity.RESULT_OK) {
                    spotifyAuth.handleActivityResult(
                        spotifyAuth.requestCode,
                        result.resultCode,
                        result.data
                    )
                } else {
                    println("Error en el resultado de la autorización")
                }
            }

        spotifyAuth.registerLauncher(spotifyAuthLauncher)

        setContent {
                Navigation(spotifyAuth = spotifyAuth, spotifyApi = spotifyApi, spotifyAppRemote = spotifyAppRemote)
            }

    }
    override fun onDestroy() {
        super.onDestroy()
        spotifyAppRemote.disconnect()
    }
}


