package com.example.shufflerionmultiplatform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigation(
    modifier: Modifier = Modifier,
    logger: Logger,
    spotifyAuth: SpotifyAuth,
    spotifyApi: SpotifyApi,
    spotifyAppRemote: SpotifyAppRemoteInterface
) {
    val navController = rememberNavController()
    val clipboardManager = getClipboardManager()

    logger.log("newrelic message: navigation initialized")

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainContent(spotifyAuth, spotifyApi, goToPlayer = {
                navController.navigate("player")
            }, clipboardManager, spotifyAppRemote, logger)
        }
        composable("player") {
            PlayerScreen(spotifyApi, spotifyAppRemote, logger)
        }
    }
}
