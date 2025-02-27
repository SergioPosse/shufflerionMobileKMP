package com.example.shufflerionmultiplatform

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(spotifyApi: SpotifyApi, spotifyAppRemote: SpotifyAppRemoteInterface) {
    var deviceId by remember { mutableStateOf<String?>(null) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var currentSongIndex by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val latestSpotifyRemote = rememberUpdatedState(spotifyAppRemote)


    LaunchedEffect(Unit) {
        deviceId = spotifyApi.getDeviceId()
        coroutineScope.launch {
            spotifyApi.getRandomSongs()?.let { newSongs ->
                songs = newSongs
            }
        }
    }

    LaunchedEffect(Unit) {
        latestSpotifyRemote.value.subscribeToPlayerState { playerState ->
            println("player state: $playerState") // Ahora se imprimirá siempre

            val trackDuration = playerState.track?.duration ?: 0
            val playbackPosition = playerState.playbackPosition

            if (!playerState.isPaused && trackDuration > 0 && playbackPosition >= trackDuration - 1000) {
                if (currentSongIndex < songs.size - 1) {
                    currentSongIndex++
                } else {
                    coroutineScope.launch {
                        spotifyApi.getRandomSongs()?.let { newSongs ->
                            songs = songs + newSongs
                            currentSongIndex = songs.size - newSongs.size
                        }
                    }
                }
                deviceId?.let { spotifyApi.playSong(it, songs[currentSongIndex].url) }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        songs.getOrNull(currentSongIndex)?.let { currentSong ->
            Image(
                painter = rememberImagePainter(currentSong.image),
                contentDescription = "Current song image",
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(currentSong.title, fontSize = 20.sp, color = Color.White)
            Text(currentSong.artist, fontSize = 16.sp, color = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs) { song ->
                val isCurrentSong = songs.indexOf(song) == currentSongIndex
                val backgroundGradient = Brush.horizontalGradient(
                    colors = if (isCurrentSong) {
                        listOf(Color(0xABFF00F2), Color(0xC9E91E7D))
                    } else {
                        listOf(Color(0xFF8000FF), Color(0xFFFFA500)) // Normal
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(backgroundGradient)
                        .clickable {
                            currentSongIndex = songs.indexOf(song)
                            deviceId?.let { spotifyApi.playSong(it, song.url) }
                        }
                        .padding(12.dp)
                ) {
                    Text(
                        song.title,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                if (currentSongIndex > 0) {
                    currentSongIndex--
                    deviceId?.let { spotifyApi.playSong(it, songs[currentSongIndex].url) }
                }
            }) { Text("Previous") }

            Button(onClick = {
                deviceId?.let { spotifyApi.playSong(it, songs[currentSongIndex].url) }
            }) { Text("Play") }

            Button(onClick = {
                if (currentSongIndex < songs.size - 1) {
                    currentSongIndex++
                } else {
                    coroutineScope.launch {
                        spotifyApi.getRandomSongs()?.let { newSongs ->
                            songs = songs + newSongs
                            currentSongIndex = songs.size - newSongs.size
                        }
                    }
                }
                deviceId?.let { spotifyApi.playSong(it, songs[currentSongIndex].url) }
            }) { Text("Next") }
        }
    }
}
