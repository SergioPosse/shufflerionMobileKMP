package com.example.shufflerionmultiplatform

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SpotifyAppRemoteAndroid: Service(), SpotifyAppRemoteInterface {

    private val clientId = "335ea7b32dd24009bd0529ba85f0f8cc"
    private val redirectUri = "shufflerionApp://callback"
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var logger: Logger = ServiceDependencies.logger
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var wakeLock: PowerManager.WakeLock


    companion object {
        const val NOTIFICATION_ID = 1

//        private var instance: SpotifyAppRemoteAndroid? = null
//
//        fun getInstance(): SpotifyAppRemoteAndroid? {
//            return instance
//        }
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SpotifyService::WakeLock")
        wakeLock.acquire()
        logger.log("SpotifyAppRemoteAndroid - Servicio creado")
        startForeground(NOTIFICATION_ID, createNotification("Starting..."))
//        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.log("SpotifyAppRemote Service en ejecución")
        val contentText = "SpotifyAppRemote Service is running"
        startForeground(NOTIFICATION_ID, createNotification(contentText))
        return START_STICKY
    }

    override fun connect(onConnected: () -> Unit, onError: (Throwable) -> Unit) {
            val connectionParams = ConnectionParams.Builder(clientId)
                .setRedirectUri(redirectUri)
                .showAuthView(true)
                .build()
            logger.log("Intentando conectar a Spotify...")
            SpotifyAppRemote.connect(this@SpotifyAppRemoteAndroid, connectionParams, object : Connector.ConnectionListener {
                override fun onConnected(appRemote: SpotifyAppRemote) {
                    spotifyAppRemote = appRemote
                    logger.log("Conectado a Spotify exitosamente")
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

    private fun createNotification(contentText: String): Notification {
        val channelId = "SpotifyAppRemoteAndroidChannel"
        val channel = NotificationChannel(
            channelId, "SpotifyRemote",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("Shufflerion")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_music_note)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
//        instance = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinderAndroid(this)
    }
}

