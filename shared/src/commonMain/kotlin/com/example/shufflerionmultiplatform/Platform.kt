package com.example.shufflerionmultiplatform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform