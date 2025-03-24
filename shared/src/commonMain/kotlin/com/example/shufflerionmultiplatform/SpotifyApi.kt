package com.example.shufflerionmultiplatform

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
    var visible: Boolean,
    var disabled: Boolean
)

data class TokenResponse(val accessToken: String, val refreshToken: String)


class SpotifyApi(private val httpClient: HttpClient, loggerParam: Logger) {

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var accessToken2: String? = null
    private var refreshToken2: String? = null
    private var logger: Logger = loggerParam


    private fun setAccessToken(token: String) {
        accessToken = token
    }

    private fun setRefreshToken(receivedRefreshToken: String) {
        refreshToken = receivedRefreshToken
    }

    fun setAccessToken2(token: String) {
        accessToken2 = token
    }

    fun setRefreshToken2(refreshToken: String) {
        refreshToken2 = refreshToken
    }

    suspend fun saveSession(
        sessionId: String,
        hostEmail: String,
        accessToken: String,
    ): Boolean {
        return try {
            val domain = "https://shufflerionserver.onrender.com"
            val createSessionUrl = "/session/create"
            logger.log("domain: $domain")
            logger.log("URL: $createSessionUrl")

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
            logger.log("response $response")
            if (response.status == HttpStatusCode.Created) {
                logger.log("Sesión guardada correctamente en el backend. $sessionId")
                true
            } else {
                logger.logError("Error al guardar la sesión $sessionId: ${response.status}")
                false
            }
        } catch (e: Exception) {
            logger.logError("Excepción al guardar la sesión $sessionId: ${e.message}")
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
            val devices = jsonResponse.jsonObject["devices"]?.jsonArray
            logger.log("device devices: $devices")

            devices?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
        } else {
            null
        }
    }

    suspend fun playSong(deviceId: String, trackUri: String, trackName: String): Boolean {
        return withContext(Dispatchers.IO) {
            var attempts = 0
            while (attempts < 3) {
                val response =
                    httpClient.put("https://api.spotify.com/v1/me/player/play?device_id=$deviceId") {
                        header("Authorization", "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody("""{"uris":["$trackUri"]}""")
                    }
                attempts++
                logger.log("Intento $attempts para $trackName - Response Status: ${response.status}")

                if (response.status !== HttpStatusCode.NotFound && (response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.OK)) {
                    return@withContext true
                } else {
                    logger.log("play spotifyApi fail: $trackUri - $trackName")
                }

                delay(2000)
            }

            return@withContext false
        }
    }

    suspend fun getRandomSongs(): List<Song>? {
        return withContext(Dispatchers.IO) {

            try {
                val domain = "https://shufflerionserver.onrender.com"
                val createSessionUrl = "/songs/random"
                logger.log("URL: $domain$createSessionUrl")
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

                val dummySong = Song(
                    "Dummy Title",
                    "Dummy Artist",
                    "spotify:track:243CX6U8LofX7SJbBewWRN",
                    "dummy_image_url",
                    disabled = false,
                    visible = false
                )
                if (songs.size >= 3) {
                    songs.add(3, dummySong)
                } else {
                    songs.add(dummySong)
                }
                logger.log("Canciones recibidas: $songs")
                songs
            } catch (e: Exception) {
                logger.logError("Excepción al obtener canciones aleatorias: ${e.message}")
                null
            }
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
                logger.logError("Error al obtener el estado del player: ${response.status}")
                null
            }
        }
    }

    suspend fun exchange(code: String): TokenResponse? {
        logger.log("Exchanging code for tokens...")
        return try {
            val clientId = "335ea7b32dd24009bd0529ba85f0f8cc"
            val clientSecret = "b482ee9f0aa4408da21b224d59c2d445"
            val credentials = "$clientId:$clientSecret".encodeBase64()

            val response = httpClient.post("https://accounts.spotify.com/api/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                header("Authorization", "Basic $credentials")
                setBody(FormDataContent(Parameters.build {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", "shufflerionApp://callback")
                }))
            }

            if (response.status == HttpStatusCode.OK) {
                val jsonResponse = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                val newAccessToken = jsonResponse["access_token"]?.jsonPrimitive?.content
                val newRefreshToken = jsonResponse["refresh_token"]?.jsonPrimitive?.content

                if (newAccessToken != null && newRefreshToken != null) {
                    logger.log("Access token and refresh token exchanged successfully.")
                    setAccessToken(newAccessToken)
                    setRefreshToken(newRefreshToken)
                    return TokenResponse(newAccessToken, newRefreshToken)
                }
            } else {
                logger.logError("Error exchanging code: ${response.status}")
            }
            null
        } catch (e: Exception) {
            logger.logError("Exception in exchange: ${e.message}")
            null
        }
    }

    suspend fun refreshAccessToken(): String? {
        return withContext(Dispatchers.IO) {
            logger.log("Lanzando API para refrescar tokens...")
            var newAccessToken: String?
            var newAccessToken2: String?

            try {
                val clientId = "335ea7b32dd24009bd0529ba85f0f8cc"
                val clientSecret = "b482ee9f0aa4408da21b224d59c2d445"
                val credentials = "$clientId:$clientSecret".encodeBase64()

                // First, try refreshing with refreshToken2
                val response2 = httpClient.post("https://accounts.spotify.com/api/token") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    header("Authorization", "Basic $credentials")
                    setBody(FormDataContent(Parameters.build {
                        append("grant_type", "refresh_token")
                        append("refresh_token", refreshToken2 ?: "")
                    }))
                }

                if (response2.status == HttpStatusCode.OK) {
                    val jsonResponse2 = Json.parseToJsonElement(response2.bodyAsText()).jsonObject
                    newAccessToken2 = jsonResponse2["access_token"]?.jsonPrimitive?.content

                    if (newAccessToken2 != null) {
                        logger.log("Nuevo accessToken2 recibido: $newAccessToken2")
                        setAccessToken2(newAccessToken2)
                    }
                }

                // If refreshing with refreshToken2 failed, try with refreshToken1
                val response1 = httpClient.post("https://accounts.spotify.com/api/token") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    header("Authorization", "Basic $credentials")
                    setBody(FormDataContent(Parameters.build {
                        append("grant_type", "refresh_token")
                        append("refresh_token", refreshToken ?: "")
                    }))
                }
                if (response1.status == HttpStatusCode.OK) {
                    val jsonResponse1 = Json.parseToJsonElement(response1.bodyAsText()).jsonObject
                    newAccessToken = jsonResponse1["access_token"]?.jsonPrimitive?.content

                    if (newAccessToken != null) {
                        logger.log("Nuevo accessToken recibido: $newAccessToken")
                        setAccessToken(newAccessToken)
                        return@withContext (newAccessToken)
                    }
                }
                logger.logError("Error al refrescar los tokens: ambos intentos fallaron.")
                null
            } catch (e: Exception) {
                logger.logError("Excepción al refrescar los tokens: ${e.message}")
                null
            }
        }
    }

}
