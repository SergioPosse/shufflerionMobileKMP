package com.example.shufflerionmultiplatform

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray

data class Song(
    val title: String,
    val artist: String,
    val url: String,
    val image: String,
    var visible : Boolean,
    var disabled : Boolean
)

@Serializable
data class TrackResponse(
    val available_markets: List<String>
)

class SpotifyApi(private val httpClient: HttpClient) {

    private var accessToken: String? = null
    private var accessToken2: String? = null
    private var refreshToken2: String? = null


    fun setAccessToken(token: String) {
        println("set access token: $token")
        accessToken = token
    }

    fun setAccessToken2(token: String) {
        println("set access token2: $token")
        accessToken2 = token
    }

    fun setRefreshToken2(refreshToken: String) {
        println("set refreshtoken2: $refreshToken")
        refreshToken2 = refreshToken
    }


    suspend fun saveSession(
        sessionId: String,
        hostEmail: String,
        accessToken: String,
        refreshToken: String
    ): Boolean {
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

        val response: HttpResponse =
            httpClient.get("https://api.spotify.com/v1/me/player/devices") {
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

    suspend fun isTrackAvailable(trackUri: String): Boolean {
        return withContext(Dispatchers.IO) {
            val trackId = trackUri.split(":").last()
            val response = httpClient.get("https://api.spotify.com/v1/tracks/$trackId") {
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status != HttpStatusCode.OK) {
                println("Error al obtener la información del track: ${response.status}")
                return@withContext false
            }

            val trackInfo: TrackResponse = response.body()

            println("track info: ${trackInfo.available_markets}")

            return@withContext trackInfo.available_markets.contains("AR")
        }
    }

    suspend fun playSong(deviceId: String, trackUri: String): Boolean {
        return withContext(Dispatchers.IO) {
            var attempts = 0
            while (attempts < 3) {
                val response = httpClient.put("https://api.spotify.com/v1/me/player/play?device_id=$deviceId") {
                    header("Authorization", "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                    setBody("""{"uris":["$trackUri"]}""")
                }

                println("Intento ${attempts + 1} - Response Status: ${response.status}")

                if (response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.OK) {
                    return@withContext true
                }

                attempts++
                delay(2000)
            }

            return@withContext false
        }
    }

    suspend fun getRandomSongs(): List<Song>? {
        return try {
            val domain = "https://shufflerionserver.onrender.com"
            val createSessionUrl = "/songs/random"
            println("URL: $domain$createSessionUrl")

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
            }.bodyAsText()

            val songsJsonArray = JSONArray(responseText)

            val songs = mutableListOf<Song>()
            for (i in 0 until songsJsonArray.length()) {
                val songJson = songsJsonArray.getJSONObject(i)
                val title = songJson.getString("Title")
                val artist = songJson.getString("Artist")
                val url = songJson.getString("Url")
                val image = songJson.getString("Image")

                songs.add(Song(title, artist, url, image, disabled = false, visible = false))
            }

            println("Canciones recibidas: $songs")
            songs
        } catch (e: Exception) {
            println("Excepción al obtener canciones aleatorias: ${e.message}")
            null
        }
    }

    suspend fun getPlayerState(): String? {
        return withContext(Dispatchers.IO) {
            if (accessToken == null) return@withContext null

            val response: HttpResponse = httpClient.get("https://api.spotify.com/v1/me/player") {
                header("Authorization", "Bearer $accessToken")
            }

            return@withContext if (response.status == HttpStatusCode.OK) {
                response.bodyAsText()
            } else {
                println("Error al obtener el estado del player: ${response.status}")
                null
            }
        }
    }

    suspend fun refreshAccessToken(refreshToken: String): String? {
        return try {
            val clientId = "335ea7b32dd24009bd0529ba85f0f8cc"
            val clientSecret = "b482ee9f0aa4408da21b224d59c2d445"
            val credentials = "$clientId:$clientSecret".encodeBase64()

            val response = httpClient.post("https://accounts.spotify.com/api/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                header("Authorization", "Basic $credentials")
                setBody(FormDataContent(Parameters.build {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                }))
            }

            if (response.status == HttpStatusCode.OK) {
                val jsonResponse = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                val newAccessToken = jsonResponse["access_token"]?.jsonPrimitive?.content

                if (newAccessToken != null) {
                    setAccessToken2(newAccessToken)
                    println("Nuevo access token: $newAccessToken")
                }
                return newAccessToken
            } else {
                println("Error al refrescar el token: ${response.status}")
                null
            }
        } catch (e: Exception) {
            println("Excepción al refrescar el token: ${e.message}")
            null
        }
    }
}
