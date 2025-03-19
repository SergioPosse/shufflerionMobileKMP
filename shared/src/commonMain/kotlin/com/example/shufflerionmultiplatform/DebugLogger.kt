package com.example.shufflerionmultiplatform

class DebugLogger : Logger {
    override fun log(message: String) {
        println("Debug info: $message")
    }

    override fun logError(error: String) {
        println("Debug error: $error")
    }

    override fun logWarning(warning: String) {
        println("Debug warning: $warning")
    }
}
