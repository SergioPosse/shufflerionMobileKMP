package com.example.shufflerionmultiplatform

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.activity.result.ActivityResultLauncher
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class SpotifyAuthAndroid : Service(), SpotifyAuth {

    private val clientId = "335ea7b32dd24009bd0529ba85f0f8cc"
    private val redirectUri = "shufflerionApp://callback"
    val requestCode = 1337
    private var onTokenReceived: ((String) -> Unit)? = null
    private var launcher: ActivityResultLauncher<Intent>? = null
    private var logger: Logger = ServiceDependencies.logger
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var activityRef: WeakReference<Activity>? = null
    private lateinit var wakeLock: PowerManager.WakeLock


    companion object {
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SpotifyService::WakeLock")
        wakeLock.acquire()
        logger.log("SpotifyAuthAndroid - Servicio creado")
        startForeground(1, createNotification("Starting..."))
//        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.log("SpotifyAuthAndroid Service en ejecución")
        val contentText = "SpotifyAuthAndroid Service is running"
        startForeground(NOTIFICATION_ID, createNotification(contentText))
        return START_STICKY
    }

    fun registerActivity(activity: Activity) {

            this@SpotifyAuthAndroid.activityRef = WeakReference(activity)
            logger.log("Activity registrada: $activity")

    }

    override fun requestAccessToken(activity: Activity, onTokenReceived: (String) -> Unit) {

            this@SpotifyAuthAndroid.onTokenReceived = onTokenReceived
            val authBuilder = AuthorizationRequest.Builder(
                clientId,
                AuthorizationResponse.Type.CODE,
                redirectUri
            )
            authBuilder.setScopes(
                arrayOf(
                    "user-library-read",
                    "streaming",
                    "user-read-playback-state",
                    "user-modify-playback-state",
                    "user-read-currently-playing",
                    "user-read-private",
                    "user-read-playback-position",
                    "user-read-email"
                )
            )
            val request = authBuilder.build()
            logger.log("Lanzando actividad de autorización")

            val loginIntent = AuthorizationClient.createLoginActivityIntent(activity, request)
            launcher?.launch(loginIntent) ?: logger.logError("Launcher no configurado")

    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Any?) {
            logger.log("handleActivityResult llamado con requestCode: $requestCode, resultCode: $resultCode")
            if (requestCode == this@SpotifyAuthAndroid.requestCode) {
                val intent = data as? Intent
                if (intent == null) {
                    logger.log("Data no es un Intent válido")
                    return
                }
                logger.log("data: $data")
                val response = AuthorizationClient.getResponse(resultCode, intent)
                logger.log("response auth client: $response")
                when (response.type) {
                    AuthorizationResponse.Type.CODE -> {
                        logger.log("CODE recibido: ${response.code}")
                        onTokenReceived?.invoke(response.code)
                    }

                    AuthorizationResponse.Type.ERROR -> {
                        logger.logError("Error: ${response.error}")
                    }
                    AuthorizationResponse.Type.EMPTY -> {
                        logger.logError("Respuesta vacía, reintentando autenticación...")
                    }

                    else -> {
                        logger.logError("Respuesta no manejada: ${response.type}")
                    }
                }
            }

    }


    fun requestAuthIntent(callback: (Intent?) -> Unit) {
            logger.log("-----------------------------requestAuthIntent---------------------------------------------")

            val activity = activityRef?.get()
            if (activity != null) {
                logger.logError("si hay activity")
                val authBuilder = AuthorizationRequest.Builder(
                    clientId,
                    AuthorizationResponse.Type.CODE,
                    redirectUri
                )
                authBuilder.setScopes(
                    arrayOf(
                        "user-library-read",
                        "streaming",
                        "user-read-playback-state",
                        "user-modify-playback-state",
                        "user-read-currently-playing",
                        "user-read-private",
                        "user-read-playback-position",
                        "user-read-email"
                    )
                )
                val request = authBuilder.build()
                val loginIntent = AuthorizationClient.createLoginActivityIntent(activity, request)
                callback(loginIntent)
            } else {
                logger.logError("No hay Activity registrada para autenticación")
                callback(null)
            }

    }

    fun registerLauncher(launcher: ActivityResultLauncher<Intent>) {
        serviceScope.launch {

            logger.log("intento register launcher")
            this@SpotifyAuthAndroid.launcher = launcher
            logger.log("Launcher configurado")
        }
    }

    private fun createNotification(contentText: String): Notification {
        val channelId = "SpotifyAuthAndroid"
        val channel = NotificationChannel(
            channelId, "SpotifyAuthAndroid",
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
//        wakeLock.release()
        serviceScope.cancel()
//        activityRef?.clear()
        logger.log("SpotifyAuthAndroid - Servicio destruido")
    }

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinderAndroid(this)
    }
}
