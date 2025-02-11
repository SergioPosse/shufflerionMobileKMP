package com.example.shufflerionmultiplatform.android

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.shufflerionmultiplatform.SpotifyApi
import com.example.shufflerionmultiplatform.mainScreen
import com.example.shufflerionmultiplatform.SpotifyAuthAndroid
import io.ktor.client.HttpClient

class MainActivity : ComponentActivity() {

    private lateinit var spotifyAuth: SpotifyAuthAndroid
    private lateinit var spotifyApi: SpotifyApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spotifyAuth = SpotifyAuthAndroid(this)
        spotifyApi = SpotifyApi(HttpClient())

        val spotifyAuthLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            println("Resultado recibido: resultCode = ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                spotifyAuth.handleActivityResult(spotifyAuth.requestCode, result.resultCode, result.data)
            } else {
                println("Error en el resultado de la autorización")
            }        }

        spotifyAuth.registerLauncher(spotifyAuthLauncher)

        setContent {
            mainScreen(spotifyAuth, spotifyApi)
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        mainScreen(SpotifyAuthAndroid(this), SpotifyApi(HttpClient()))
    }
}