package com.example.shufflerionmultiplatform

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@SuppressLint("SuspiciousIndentation")
@Composable
fun Navigation(
    modifier: Modifier = Modifier,
    logger: Logger,
    playerService: PlayerService,
    spotifyAuth: SpotifyAuth,
    spotifyApi: SpotifyApi,
    spotifyAppRemote: SpotifyAppRemoteInterface
) {
    val navController = rememberNavController()
    val clipboardManager = getClipboardManager()
        NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainContent(spotifyAuth, spotifyApi, goToPlayer = {
                navController.navigate("player")
            }, clipboardManager, spotifyAppRemote, logger)
        }
        composable("player") {
            PlayerScreen(playerService, spotifyAppRemote, logger)
        }
    }
}
