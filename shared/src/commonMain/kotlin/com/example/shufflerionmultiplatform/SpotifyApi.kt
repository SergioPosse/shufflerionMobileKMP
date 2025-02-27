package com.example.shufflerionmultiplatform

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import org.json.JSONObject

data class Song(
    val title: String,
    val artist: String,
    val url: String,
    val image: String
)

class SpotifyApi(private val httpClient: HttpClient) {

    private var accessToken: String? = null
    private var accessToken2: String? = null


    fun setAccessToken(token: String) {
        println("set access token: $token")
        accessToken = token
    }

    fun setAccessToken2(token: String) {
        println("set access token2: $token")
        accessToken2 = token
    }

    suspend fun saveSession(sessionId: String, hostEmail: String, accessToken: String, refreshToken: String): Boolean {
        return try {
            val domain = "https://shufflerionserver.onrender.com"
            val createSessionUrl = "/session/create"
            println("domain: $domain")
            println("URL: $createSessionUrl")

            val response = httpClient.post("$domain$createSessionUrl") {
                contentType(ContentType.Application.Json)
                setBody(
                    """{
                    "id": "$sessionId",
                    "host": {
                        "email": "$hostEmail",
                        "tokens": {
                            "accessToken": "$accessToken",
                            "refreshToken": "no"
                        }
                    }
                }"""
                )
            }
            println("response $response")
            if (response.status == HttpStatusCode.Created) {
                println("Sesión guardada correctamente en el backend.")
                true
            } else {
                println("Error al guardar la sesión: ${response.status}")
                false
            }
        } catch (e: Exception) {
            println("Excepción al guardar la sesión: ${e.message}")
            false
        }
    }

    suspend fun getDeviceId(): String? {
        if (accessToken == null) return null

        val response: HttpResponse = httpClient.get("https://api.spotify.com/v1/me/player/devices") {
            header("Authorization", "Bearer $accessToken")
        }

        return if (response.status == HttpStatusCode.OK) {
            val jsonResponse = Json.parseToJsonElement(response.bodyAsText())
            println("device jsonresponse: $jsonResponse")

            val devices = jsonResponse.jsonObject["devices"]?.jsonArray
            println("device devices: $devices")

            devices?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
        } else {
            null
        }
    }
    fun playSong(deviceId: String, trackUri: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val res = httpClient.put("https://api.spotify.com/v1/me/player/play?device_id=$deviceId") {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody("""{"uris":["$trackUri"]}""")
            }
            println("result playsong: $res")
        }
    }


    suspend fun getRandomSongs(): List<Song>? {
        return try {
            val domain = "https://shufflerionserver.onrender.com"
            val createSessionUrl = "/songs/random"
            println("URL: $domain$createSessionUrl")

            // Realiza la solicitud y obtiene el cuerpo como texto
            println("token1 before request: $accessToken")
            println("token2 before request: $accessToken2")
            val responseText = httpClient.post("$domain$createSessionUrl") {
                contentType(ContentType.Application.Json)
                setBody(
                    """{
                "access_token1": "$accessToken",
                "access_token2": "$accessToken2"
            }"""
                )
            }.bodyAsText() // Lee el cuerpo como texto

            // Trabaja con la respuesta JSON manualmente
            val songsJsonArray = JSONArray(responseText)

// Convierte la respuesta JSON a una lista de canciones
            val songs = mutableListOf<Song>()
            for (i in 0 until songsJsonArray.length()) {
                val songJson = songsJsonArray.getJSONObject(i)
                val title = songJson.getString("Title") // Nota: Respeta las mayúsculas de la respuesta
                val artist = songJson.getString("Artist")
                val url = songJson.getString("Url")
                val image = songJson.getString("Image")

                songs.add(Song(title, artist, url, image))
            }

            println("Canciones recibidas: $songs")
            songs
        } catch (e: Exception) {
            println("Excepción al obtener canciones aleatorias: ${e.message}")
            null
        }
    }
    suspend fun getCurrentPlayback(): String? {
        if (accessToken == null) return null

        val response: HttpResponse = httpClient.get("https://api.spotify.com/v1/me/player/currently-playing") {
            header("Authorization", "Bearer $accessToken")
        }

        return if (response.status == HttpStatusCode.OK) {
            val jsonResponse = Json.parseToJsonElement(response.bodyAsText())
            val trackUri = jsonResponse.jsonObject["item"]?.jsonObject?.get("uri")?.jsonPrimitive?.content
            trackUri
        } else {
            null
        }
    }
}
