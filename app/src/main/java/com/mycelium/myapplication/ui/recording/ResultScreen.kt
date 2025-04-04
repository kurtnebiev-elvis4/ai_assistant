package com.mycelium.myapplication.ui.recording

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    recordingId: String,
    viewModel: ResultViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(recordingId) {
        viewModel.loadResultStatus(recordingId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        // You'll need to import the icon: Icons.Default.ArrowBack
                        // Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        Text("<")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
                    Text(
                        text = "Checking status...",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                uiState.error.isNotBlank() -> {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Button(
                        onClick = { viewModel.loadResultStatus(recordingId) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }
                !uiState.isProcessingComplete -> {
                    Text(
                        text = "Processing in progress...",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 16.dp)
                    )
                    Button(
                        onClick = { viewModel.loadResultStatus(recordingId) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Refresh Status")
                    }
                }
                uiState.resultText.isNotBlank() -> {
                    Text(
                        text = "Result",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.resultText,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        )
                    }
                }
                else -> {
                    Text(
                        text = "Processing complete",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Button(
                        onClick = { viewModel.downloadResult(recordingId) },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Download Result")
                    }
                }
            }
        }
    }
}