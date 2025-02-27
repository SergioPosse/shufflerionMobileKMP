package com.example.shufflerionmultiplatform

import android.content.ClipData
import android.content.Context
import androidx.compose.runtime.Composable
import android.content.ClipboardManager as AndroidClipboardManager
import androidx.compose.ui.platform.LocalContext

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

class AndroidClipboardManager(private val context: Context): ClipboardManager {
    override fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? AndroidClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("Copied Link", text)
            clipboard.setPrimaryClip(clip)
            println("Enlace copiado al portapapeles: $text")
        } else {
            println("Error: No se pudo acceder al ClipboardManager")
        }
    }
}

@Composable
actual fun getClipboardManager(): ClipboardManager {
    val context = LocalContext.current
    return AndroidClipboardManager(context)
}

actual fun getPlatform(): Platform = AndroidPlatform()