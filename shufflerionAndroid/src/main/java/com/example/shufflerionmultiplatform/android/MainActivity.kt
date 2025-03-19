package com.example.shufflerionmultiplatform.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import com.example.shufflerionmultiplatform.AndroidLogger
import com.example.shufflerionmultiplatform.DebugLogger
import com.example.shufflerionmultiplatform.Navigation
import com.example.shufflerionmultiplatform.SpotifyApi
import com.example.shufflerionmultiplatform.SpotifyAppRemoteAndroid
import com.example.shufflerionmultiplatform.SpotifyAuthAndroid
import com.newrelic.agent.android.FeatureFlag
import com.newrelic.agent.android.NewRelic
import com.newrelic.agent.android.logging.AgentLog
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {

    private lateinit var spotifyAuth: SpotifyAuthAndroid
    private lateinit var spotifyApi: SpotifyApi
    private lateinit var spotifyAppRemote: SpotifyAppRemoteAndroid
    private lateinit var wakeLock: PowerManager.WakeLock


    override fun onCreate(savedInstanceState: Bundle?) {
        NewRelic.withApplicationToken("AA47eb0821059576bbdbaf433a087389974bce058d-NRMA")
            .withLogLevel(AgentLog.AUDIT)
            .start(this.application);
        NewRelic.enableFeature(FeatureFlag.LogReporting)

        super.onCreate(savedInstanceState)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::WakeLockTag")
        wakeLock.acquire()



        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val logger = if (BuildConfig.DEBUG) {
            DebugLogger()
        } else {
            AndroidLogger()
        }

        spotifyAuth = SpotifyAuthAndroid(this, logger)
        spotifyApi = SpotifyApi(httpClient, logger)
        spotifyAppRemote = SpotifyAppRemoteAndroid(this, logger)

        val spotifyAuthLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                logger.log("Resultado recibido: resultCode = ${result.resultCode}")

                if (result.resultCode == Activity.RESULT_OK) {
                    spotifyAuth.handleActivityResult(
                        spotifyAuth.requestCode,
                        result.resultCode,
                        result.data
                    )
                } else {
                    logger.logError("Error en el resultado de la autorización")
                }
            }

        spotifyAuth.registerLauncher(spotifyAuthLauncher)

        setContent {
            Navigation(
                logger = logger,
                spotifyAuth = spotifyAuth,
                spotifyApi = spotifyApi,
                spotifyAppRemote = spotifyAppRemote
            )
        }

    }

    override fun onResume() {
        super.onResume()
        if (!wakeLock.isHeld) {
            wakeLock.acquire(10 * 60 * 1000L) // 10 minutos
        }
    }

    override fun onPause() {
        super.onPause()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    @SuppressLint("Wakelock")
    override fun onDestroy() {
        super.onDestroy()
        spotifyAppRemote.disconnect()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}


