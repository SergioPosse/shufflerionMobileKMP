package com.example.shufflerionmultiplatform

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable

interface Platform {
    val name: String
}

interface ClipboardManager {
    fun copyToClipboard(text: String, logger: Logger)
}



@Composable
expect fun getClipboardManager(): ClipboardManager

expect fun getPlatform(): Platform