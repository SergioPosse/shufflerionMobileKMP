package com.example.shufflerionmultiplatform

interface Logger {
    fun log(message: String)
    fun logError(error: String)
    fun logWarning(warning: String)
}