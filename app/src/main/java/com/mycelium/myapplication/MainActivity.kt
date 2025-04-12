package com.mycelium.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mycelium.myapplication.data.repository.UploadChunkWorker
import com.mycelium.myapplication.ui.chat.ChatScreen
import com.mycelium.myapplication.ui.recording.RecordListScreen
import com.mycelium.myapplication.ui.recording.RecordingScreen
import com.mycelium.myapplication.ui.recording.ResultScreen
import com.mycelium.myapplication.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

@Composable
fun MainScreen(requestAudioPermission: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val navController = rememberNavController()
    Scaffold(
        topBar = {

        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Mic, contentDescription = "recording") },
                    label = { Text("Recording") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        navController.navigate("recording") {
                            popUpTo("recording") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "RecordList") },
                    label = { Text("Record List") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        navController.navigate("record_list") {
                            popUpTo("record_list") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Chat") },
                    label = { Text("Chat") },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        navController.navigate("chat") {
                            popUpTo("chat") { inclusive = true }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            NavHost(navController = navController, startDestination = "recording") {

                composable("recording") {
                    RecordingScreen(
                        onRequestPermission = { requestAudioPermission() },
                    )
                }
                composable("record_list") {
                    RecordListScreen(
                        onNavigateToResult = { recordingId ->
                            navController.navigate("result/$recordingId")
                        },
                    )
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
                        showServerDialog = {}
                    )
                }
            }
        }
    }
}

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
                MainScreen(this::requestAudioPermission)
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