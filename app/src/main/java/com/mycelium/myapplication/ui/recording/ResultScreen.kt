package com.mycelium.myapplication.ui.recording

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import common.provideUIState

@Preview
@Composable
fun ResultScreenPreview() {
    ResultScreen(recordingId = "123", onBackClick = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    recordingId: String,
    viewModel: ResultViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.provideUIState().collectAsState()

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

                uiState.resultText.isNotEmpty() -> {
                    val context = LocalContext.current
                    Button(
                        onClick = { viewModel.shareAllResults(context) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share All Results",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share All")
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
            if (uiState.resultText.isNotEmpty()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.resultText.toList()) { item ->
                        var expanded by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                        ) {
                            Card(
                                onClick = { expanded = !expanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.first,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val context = LocalContext.current
                                    IconButton(
                                        onClick = {
                                            val parts = item.second.split("</think>", limit = 2)
                                            val displayText = if (parts.size == 2) parts[1] else item.second
                                            viewModel.shareResultPart(context, item.first, displayText.trim())
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = "Share"
                                        )
                                    }
                                }
                            }

                            if (expanded) {
                                val parts = item.second.split("</think>", limit = 2)
                                val (collapsiblePart, visiblePart) = if (parts.size == 2) parts[0] to parts[1] else "" to item.second

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        if (collapsiblePart.isNotBlank()) {
                                            Text(
                                                text = collapsiblePart.trim(),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                            )
                                            HorizontalDivider(
                                                modifier = Modifier
                                                    .padding(vertical = 8.dp)
                                                    .fillMaxWidth()
                                            )
                                        }
                                        Text(
                                            text = visiblePart.trim(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}