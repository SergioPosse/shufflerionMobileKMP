package com.example.shufflerionmultiplatform

interface PlayerService {
    suspend fun playSong(deviceId: String, songUrl: String, songTitle: String): Boolean
    suspend fun getRandomSongs(): List<Song>?
    suspend fun getPlayerState(): String?
    suspend fun getDeviceIdService(): String?
    fun test()
}