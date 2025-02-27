package com.example.shufflerionmultiplatform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigation(modifier: Modifier = Modifier, spotifyAuth : SpotifyAuth, spotifyApi: SpotifyApi, spotifyAppRemote: SpotifyAppRemoteInterface) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val clipboardManager = getClipboardManager()

    NavHost(navController = navController, startDestination = "main" ) {
        composable("main"){
            MainContent(spotifyAuth, spotifyApi, goToPlayer = {
                navController.navigate("player")
            }, clipboardManager, spotifyAppRemote)
        }
        composable("player"){
            PlayerScreen(spotifyApi, spotifyAppRemote)
        }
    }
}
