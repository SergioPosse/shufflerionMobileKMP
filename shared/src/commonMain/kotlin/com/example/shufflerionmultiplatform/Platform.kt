package com.example.shufflerionmultiplatform

import androidx.compose.runtime.Composable

interface Platform {
    val name: String
}

interface ClipboardManager {
    fun copyToClipboard(text: String)
}

@Composable
expect fun getClipboardManager(): ClipboardManager

expect fun getPlatform(): Platform