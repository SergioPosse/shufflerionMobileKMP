package com.example.shufflerionmultiplatform

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import kotlinx.coroutines.*

class PlayerServiceAndroid : Service(), PlayerService {

    private val spotifyApi = ServiceDependencies.spotifyApi
    private val logger = ServiceDependencies.logger
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var wakeLock: PowerManager.WakeLock

    companion object {
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SpotifyService::WakeLock")
        wakeLock.acquire()
        logger.log("playerservice - Servicio creado")
        startForeground(NOTIFICATION_ID, createNotification("Starting..."))
//        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.log("player service Service en ejecución")
        val contentText = "Player Service is running"
        startForeground(NOTIFICATION_ID, createNotification(contentText))
        return START_STICKY
    }

    override fun test() {

            val handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                override fun run() {
                    println("Service foreground...")
                    handler.postDelayed(this, 3000)
                }
            }
            handler.post(runnable)
    }

    override suspend fun playSong(deviceId: String, songUrl: String, songTitle: String): Boolean {
            val response = spotifyApi.playSong(deviceId, songUrl, songTitle)
            if (response) {
                startForeground(NOTIFICATION_ID, createNotification("Now playing: $songTitle"))
            } else {
                startForeground(NOTIFICATION_ID, createNotification("Failed to play song"))
            }
             return response


    }

        override suspend fun getDeviceIdService(): String? {

                val response = spotifyApi.getDeviceId()
                if (response == null) {
                    logger.log("Failed to get random songs")
                    return null
                }
                return response

    }


    override suspend fun getRandomSongs(): List<Song>? {

            val response = spotifyApi.getRandomSongs()
            if (response == null) {
                logger.log("Failed to get random songs")
                return null
            }
            return response

    }

    override suspend fun getPlayerState(): String? {

            val response = spotifyApi.getPlayerState()
            if (response == null) {
                logger.log("Failed to get random songs")
                return null
            }
           return response

    }

//    override suspend fun refreshAccessToken(): String? {
//            val response = spotifyApi.refreshAccessToken()
//            if (response == null) {
//                logger.log("Failed to get random songs")
//                return null
//            }
//            return response
//
//    }

    private fun createNotification(contentText: String): Notification {
        val channelId = "PlaybackServiceChannel"
        val channel = NotificationChannel(
            channelId, "Music Playback",
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

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinderAndroid(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
