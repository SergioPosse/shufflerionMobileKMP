package com.example.shufflerionmultiplatform.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.shufflerionmultiplatform.MainScreen
import com.example.shufflerionmultiplatform.SpotifyAuthAndroid

class MainActivity : ComponentActivity() {

    private lateinit var spotifyAuth: SpotifyAuthAndroid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        spotifyAuth = SpotifyAuthAndroid(this)
        MainScreen(spotifyAuth)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        spotifyAuth.handleActivityResult(requestCode, resultCode, data)
    }
}
