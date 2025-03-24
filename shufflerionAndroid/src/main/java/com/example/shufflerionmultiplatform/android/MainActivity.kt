package com.example.shufflerionmultiplatform.android

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import com.example.shufflerionmultiplatform.*
import com.newrelic.agent.android.FeatureFlag
import com.newrelic.agent.android.NewRelic
import com.newrelic.agent.android.logging.AgentLog
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


class MainActivity : ComponentActivity() {


    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var playerIntent: Intent
    private lateinit var spotifyAppRemoteIntent: Intent
    private lateinit var spotifyAuthIntent: Intent
    private var spotifyAuthService: SpotifyAuthAndroid? = null
    private var playerService: PlayerServiceAndroid? = null
    private var spotifyAppRemoteService: SpotifyAppRemoteInterface? = null
    private lateinit var spotifyAppRemoteConnection: ServiceConnection
    private lateinit var playerServiceConnection: ServiceConnection
    private lateinit var spotifyAuthConnection: ServiceConnection
    private var spotifyAuthLauncher: ActivityResultLauncher<Intent>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        NewRelic.withApplicationToken("AA47eb0821059576bbdbaf433a087389974bce058d-NRMA")
            .withLogLevel(AgentLog.AUDIT)
            .start(this.application)
        NewRelic.enableFeature(FeatureFlag.LogReporting)
        NewRelic.enableFeature(FeatureFlag.BackgroundReporting)

        super.onCreate(savedInstanceState)
        spotifyAuthLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    spotifyAuthService?.handleActivityResult(
                        spotifyAuthService!!.requestCode,
                        result.resultCode,
                        result.data
                    )
                } else {
                    ServiceDependencies.logger.logError("launcher Error en el resultado de la autorización")
                }
            }
        initializeServices()
        initializeServiceConnections()
    }

    private fun initializeServices() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::WakeLockTag")
        wakeLock.acquire()

        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
//        ServiceDependencies.logger = DebugLogger()
        ServiceDependencies.logger = AndroidLogger()
        ServiceDependencies.spotifyApi = SpotifyApi(httpClient, ServiceDependencies.logger)
    }

    private fun initializeServiceConnections() {
        spotifyAppRemoteConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as? LocalBinderAndroid<*>
                spotifyAppRemoteService = binder?.getService() as? SpotifyAppRemoteInterface
                if (spotifyAppRemoteService != null) {
                    runOnUiThread { initializeUI() }
                    ServiceDependencies.logger.log("spotifyAppRemoteService está listo para usarse")
                } else {
                    ServiceDependencies.logger.logError("spotifyAppRemoteService sigue siendo null después de la conexión")
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                spotifyAppRemoteService = null
                ServiceDependencies.logger.log("SpotifyAppRemoteAndroid desconectado")
            }
        }

        spotifyAppRemoteIntent = Intent(this, SpotifyAppRemoteAndroid::class.java)
        startService(spotifyAppRemoteIntent)
        bindService(spotifyAppRemoteIntent, spotifyAppRemoteConnection, Context.BIND_AUTO_CREATE)

        playerServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as? LocalBinderAndroid<*>
                playerService = binder?.getService() as? PlayerServiceAndroid
                if (playerService != null) {
                    runOnUiThread { initializeUI() }
                    ServiceDependencies.logger.log("playerService está listo para usarse")
                } else {
                    ServiceDependencies.logger.logError("playerService sigue siendo null después de la conexión")
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerService = null
                ServiceDependencies.logger.log("playerService desconectado")
            }
        }

        playerIntent = Intent(this, PlayerServiceAndroid::class.java)
        startService(playerIntent)
        bindService(playerIntent, playerServiceConnection, Context.BIND_AUTO_CREATE)

        spotifyAuthConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as? LocalBinderAndroid<*>
                spotifyAuthService = binder?.getService() as? SpotifyAuthAndroid

                if (spotifyAuthService != null) {
                    runOnUiThread {
                        initializeUI()
                    }

                    spotifyAuthService?.requestAuthIntent { loginIntent ->
                        if (loginIntent != null) {
                            startActivity(loginIntent)
                            if (spotifyAuthLauncher != null) {
                                spotifyAuthService?.registerLauncher(spotifyAuthLauncher!!)
                            } else {
                                ServiceDependencies.logger.log("no hay launcher en activity")
                            }
                        } else {
                            ServiceDependencies.logger.logError("Error: loginIntent es null")
                        }
                    }
                } else {
                    ServiceDependencies.logger.logError("spotifyAuthService sigue siendo null después de la conexión")
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                spotifyAuthService = null
                ServiceDependencies.logger.log("El servicio SpotifyAuthAndroid se ha desconectado")
            }
        }

        spotifyAuthIntent = Intent(this, SpotifyAuthAndroid::class.java)
        startService(spotifyAuthIntent)
        bindService(spotifyAuthIntent, spotifyAuthConnection, Context.BIND_AUTO_CREATE)
    }

    private fun initializeUI() {
        if (spotifyAuthService == null || spotifyAppRemoteService == null || playerService == null) {
            ServiceDependencies.logger.logError("Error: uno de los servicios es null")
            return
        }

        spotifyAuthService?.registerActivity(this@MainActivity)
        setContent {
            Navigation(
                logger = ServiceDependencies.logger,
                spotifyAuth = spotifyAuthService!!,
                playerService = playerService!!,
                spotifyApi = ServiceDependencies.spotifyApi,
                spotifyAppRemote = spotifyAppRemoteService!!
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (!wakeLock.isHeld) {
            wakeLock.acquire(10 * 60 * 1000L)
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
        unbindService(playerServiceConnection)
        stopService(playerIntent)
        unbindService(spotifyAppRemoteConnection)
        stopService(spotifyAppRemoteIntent)
        unbindService(spotifyAuthConnection)
        stopService(spotifyAuthIntent)

        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}
