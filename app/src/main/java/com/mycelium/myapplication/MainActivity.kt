package com.mycelium.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.internal.GsonBuildConfig
import com.mycelium.ai_meet_assistant.BuildConfig
import com.mycelium.myapplication.data.repository.UploadChunkWorker
import com.mycelium.myapplication.ui.chat.ChatScreen
import com.mycelium.myapplication.ui.recording.RecordingScreen
import com.mycelium.myapplication.ui.recording.ResultScreen
import com.mycelium.myapplication.ui.recording.ServerViewModel
import com.mycelium.myapplication.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "recording") {
                    composable("recording") {
                        val recordingScreen = RecordingScreen(
                            onRequestPermission = { requestAudioPermission() },
                            onNavigateToResult = { recordingId ->
                                navController.navigate("result/$recordingId")
                            },
                            onNavigateToChat = {
                                navController.navigate("chat")
                            }
                        )

                        recordingScreen
                    }

                    composable(
                        route = "result/{recordingId}",
                        arguments = listOf(
                            navArgument("recordingId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val recordingId = backStackEntry.arguments?.getString("recordingId") ?: ""
                        ResultScreen(
                            recordingId = recordingId,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    
                    composable("chat") {
                        ChatScreen(
                            onNavigateUp = { navController.popBackStack() },
                            showServerDialog = {
                            }
                        )
                    }
                }

            }
        }
        UploadChunkWorker.enqueueOneTimeUploadWorker(this)
    }

    private fun requestAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {

            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}