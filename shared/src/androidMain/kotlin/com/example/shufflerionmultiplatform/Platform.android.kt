package com.example.shufflerionmultiplatform

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import android.content.ClipboardManager as AndroidClipboardManager
import androidx.compose.ui.platform.LocalContext
import android.net.Uri

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

class AndroidClipboardManager(private val context: Context): ClipboardManager {
    override fun copyToClipboard(text: String, logger: Logger) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? AndroidClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("Copied Link", text)
            clipboard.setPrimaryClip(clip)
            logger.log("Enlace copiado al portapapeles: $text")
        } else {
            logger.logError("Error: No se pudo acceder al ClipboardManager")
        }
    }
}

//actual fun openSpotifyAuthorization(activity: Activity, activityResultLauncher: ActivityResultLauncher<Intent>) {
//    val clientId = "335ea7b32dd24009bd0529ba85f0f8cc"
//    val redirectUri = "shufflerionApp://callback"
//
//    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.spotify.com/authorize?client_id=$clientId&response_type=token&redirect_uri=$redirectUri"))
//    activityResultLauncher.launch(intent)
//}

@Composable
actual fun getClipboardManager(): ClipboardManager {
    val context = LocalContext.current
    return AndroidClipboardManager(context)
}

actual fun getPlatform(): Platform = AndroidPlatform()