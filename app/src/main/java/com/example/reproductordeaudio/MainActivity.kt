package com.example.reproductordeaudio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.Coil
import coil.ImageLoader
import com.example.reproductordeaudio.data.local.EmbeddedArtworkFetcher
import com.example.reproductordeaudio.presentation.home.HomeScreen
import com.example.reproductordeaudio.presentation.home.HomeViewModel
import com.example.reproductordeaudio.presentation.player.PlayerScreen
import com.example.reproductordeaudio.ui.theme.ReproductorDeAudioTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            homeViewModel.syncLibrary()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(EmbeddedArtworkFetcher.Factory(applicationContext))
            }
            .build()
        Coil.setImageLoader(imageLoader)

        checkAndRequestPermissions()

        val shouldOpenPlayer = intent?.getBooleanExtra("open_player", false) ?: false
        val startDestination = if (shouldOpenPlayer) "player" else "home"

        setContent {
            ReproductorDeAudioTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("home") {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onSongClick = { navController.navigate("player") }
                        )
                    }
                    composable("player") {
                        PlayerScreen(
                            viewModel = homeViewModel,
                            onBackClick = {
                                homeViewModel.triggerScrollToCurrentSong()
                                if (navController.previousBackStackEntry != null) {
                                    navController.popBackStack()
                                } else {
                                    navController.navigate("home") {
                                        popUpTo("player") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(storagePermission)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            homeViewModel.syncLibrary()
        }
    }
}
