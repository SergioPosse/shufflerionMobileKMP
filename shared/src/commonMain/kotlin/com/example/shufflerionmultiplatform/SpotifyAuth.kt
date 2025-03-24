package com.example.shufflerionmultiplatform

import android.app.Activity

interface SpotifyAuth {
    fun requestAccessToken(activity: Activity, onTokenReceived: (String) -> Unit)
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Any?)
}
