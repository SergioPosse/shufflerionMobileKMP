package com.example.shufflerionmultiplatform

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seiko.imageloader.rememberImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun PlayerScreen(spotifyApi: SpotifyApi, spotifyAppRemote: SpotifyAppRemoteInterface) {
    var deviceId by remember { mutableStateOf<String?>(null) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var currentSongIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val latestSpotifyRemote = rememberUpdatedState(spotifyAppRemote)
    var isPlaying by remember { mutableStateOf<Boolean>(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isPausedByUser by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var isFetchingSongs by remember { mutableStateOf(false) }


    LaunchedEffect(currentSongIndex) {
        listState.animateScrollToItem(currentSongIndex)
    }



    LaunchedEffect(Unit) {
        deviceId = spotifyApi.getDeviceId()
        var attempts = 0
        while (attempts < 2) {
            val newSongs = spotifyApi.getRandomSongs()
            if (newSongs != null) {
                songs = newSongs
                songs[0].visible = true
                isLoading = false
                break
            } else {
                attempts++
                delay(2000)
            }
        }
        isLoading = false
    }

    suspend fun fetchMoreSongs() {
        if (isFetchingSongs) return // Evita múltiples fetches
        isFetchingSongs = true
        val newSongs = spotifyApi.getRandomSongs()
        if (newSongs != null) {
            songs = songs + newSongs
        } else {
            delay(2000) // Espera antes de volver a intentar
        }
        isFetchingSongs = false
    }

    fun markAsFailAndContinue(index: Int, direction: String?): Int {
        songs[index].disabled = true
        if (currentSongIndex > 0) {
            if (direction == "PREV") {
                currentSongIndex--
            } else {
                currentSongIndex++
            }
        }
        songs[currentSongIndex].visible = true
        return currentSongIndex
    }


    suspend fun playOnlyOneSong(direction: String?, index: Int?) {
        if (direction == "PREV") {
            currentSongIndex--
        }
        if (direction == "NEXT") {
            currentSongIndex++
        }
        if (index !== null) {
            currentSongIndex = index
        }
        songs[currentSongIndex].visible = true
        isPlaying = true
        if (currentSongIndex == 2) {
            val response =
                deviceId?.let {
                    spotifyApi.playSong(
                        it,
                        "spotify:track:243CX6U8LofX7SJbBewWRN"
                    )
                }
            println("current songindex: $currentSongIndex")
            println("response playsong: $response")
            if (response == false) {
                markAsFailAndContinue(currentSongIndex, direction)
                playOnlyOneSong(null, currentSongIndex)
            }
            else {
                delay(5000)
                val responsePlayerState = spotifyApi.getPlayerState()
                println("check response: $responsePlayerState")

                // Verificar si la canción está en reproducción, si no, continuar con la siguiente canción
                val jsonElement = responsePlayerState?.let { Json.parseToJsonElement(it) }
                val isPlayingPlayer = jsonElement?.jsonObject?.get("is_playing")?.jsonPrimitive?.booleanOrNull ?: false

                if (!isPlayingPlayer && !isPausedByUser && isPlaying) {
                    println("No hay reproducción activa. Reproduciendo la siguiente canción...")
                    markAsFailAndContinue(currentSongIndex, direction)
                    playOnlyOneSong(null, currentSongIndex)
                }
            }
        } else {
            val response = deviceId?.let { spotifyApi.playSong(it, songs[currentSongIndex].url) }
            println("current songindex: $currentSongIndex")
            println("response playsong: $response")
            if (response == false) {
                markAsFailAndContinue(currentSongIndex, direction)
                playOnlyOneSong(null, currentSongIndex)
            }
            else {
                delay(5000)
                val responsePlayerState = spotifyApi.getPlayerState()
                println("check response: $responsePlayerState")

                // Verificar si la canción está en reproducción, si no, continuar con la siguiente canción
                val jsonElement = responsePlayerState?.let { Json.parseToJsonElement(it) }
                val isPlayingPlayer = jsonElement?.jsonObject?.get("is_playing")?.jsonPrimitive?.booleanOrNull ?: false

                if (!isPlayingPlayer && !isPausedByUser && isPlaying) {
                    println("No hay reproducción activa. Reproduciendo la siguiente canción...")
                    markAsFailAndContinue(currentSongIndex, direction)
                    playOnlyOneSong(null, currentSongIndex)
                }
            }
        }
    }


    LaunchedEffect(Unit) {
        latestSpotifyRemote.value.subscribeToPlayerState { playerState ->
            val paused = playerState.isPaused
            val playbackPosition = playerState.playbackPosition
            val track = playerState.track.name
            println("paused: $paused")
            if (
                playbackPosition <= 0 &&
                isPlaying &&
                paused &&
                track != null
            ) {
                println("Finalizado!!!!!")
                coroutineScope.launch {
                    if (currentSongIndex < songs.size - 1) {
                        playOnlyOneSong("NEXT", null)
                    } else {
                        fetchMoreSongs()
                        delay(500)
                        playOnlyOneSong("NEXT", null)
                    }

                }
            }
        }
    }
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            songs.getOrNull(currentSongIndex)?.let { currentSong ->
                Image(
                    painter = rememberImagePainter(currentSong.image),
                    contentDescription = "Current song image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(currentSong.title, fontSize = 20.sp, color = Color.White)
                Text(currentSong.artist, fontSize = 16.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(songs.filter { it.visible }) { song ->
                    val isCurrentSong = songs.indexOf(song) == currentSongIndex
                    val backgroundGradient = Brush.horizontalGradient(
                        colors = if (song.disabled) {
                            listOf(Color(0xFFFF0000), Color(0xFFF43646))
                        } else if (isCurrentSong) {
                            listOf(Color(0xFFAF8E4C), Color(0xFFF46236))
                        } else {
                            listOf(Color(0x4D8000FF), Color(0x5C9C27B0)) // Normal
                        }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(backgroundGradient)
                            .clickable {
                                coroutineScope.launch {
                                    playOnlyOneSong(null, songs.indexOf(song))
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Text(
                            "${song.artist} - ${song.title}",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    if (currentSongIndex > 0) {
                        coroutineScope.launch {
                            playOnlyOneSong("PREV", null)
                        }
                    }
                }) { Text("Previous") }

                Button(onClick = {
                    coroutineScope.launch {
                        playOnlyOneSong(null, currentSongIndex)
                    }
                }) { Text("Play") }

                Button(onClick = {
                    coroutineScope.launch {
                        if (currentSongIndex < songs.size - 1) {
                            currentSongIndex++
                        } else {
                            spotifyApi.getRandomSongs()?.let { newSongs ->
                                songs = songs + newSongs
                                currentSongIndex = songs.size - newSongs.size
                            }
                        }
                        playOnlyOneSong(null, currentSongIndex)
                    }

                }) { Text("Next") }
            }
        }
    }
}
