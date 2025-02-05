package com.example.shufflerionmultiplatform

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SpotifyApi(private val httpClient: HttpClient) {

    private var accessToken: String? = null

    fun setAccessToken(token: String) {
        accessToken = token
    }

    suspend fun getDeviceId(): String? {
        if (accessToken == null) return null

        val response: HttpResponse = httpClient.get("https://api.spotify.com/v1/me/player/devices") {
            header("Authorization", "Bearer $accessToken")
        }

        return if (response.status == HttpStatusCode.OK) {
            val jsonResponse = Json.parseToJsonElement(response.bodyAsText())
            val devices = jsonResponse.jsonObject["devices"]?.jsonArray
            devices?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
        } else {
            null
        }
    }

    fun playSong(deviceId: String, trackUri: String) {
        CoroutineScope(Dispatchers.IO).launch {
            httpClient.put("https://api.spotify.com/v1/me/player/play?device_id=$deviceId") {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody("""{"uris":["$trackUri"]}""")
            }
        }
    }
}
