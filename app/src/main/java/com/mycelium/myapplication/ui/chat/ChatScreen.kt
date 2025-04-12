package com.mycelium.myapplication.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mycelium.myapplication.data.model.ChatMessage
import com.mycelium.myapplication.data.model.ServerEntry
import com.mycelium.myapplication.ui.recording.ServerIcon
import com.mycelium.myapplication.ui.recording.ServerUiState
import com.mycelium.myapplication.ui.recording.ServerViewModel
import common.provideUIState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    serverViewModel: ServerViewModel = hiltViewModel(),
    showServerDialog: () -> Unit = { serverViewModel.showAddServerDialog() }
) {
    val chatUiState by viewModel.provideUIState().collectAsState()
    val serverUiState by serverViewModel.provideUIState().collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(chatUiState.messages.size) {
        if (chatUiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatUiState.messages.size - 1)
        }
    }

    // Show connection errors
    LaunchedEffect(chatUiState.error) {
        if (chatUiState.error.isNotEmpty()) {
            android.widget.Toast.makeText(
                android.app.Application(),
                chatUiState.error,
                android.widget.Toast.LENGTH_SHORT
            ).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            serverUiState.selectedServer?.let { selectedServer ->
                ServerInfo(
                    server = selectedServer,
                    isConnected = chatUiState.isConnected,
                    onClick = showServerDialog
                )
            }
        }
        // Message list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = listState
        ) {
            items(chatUiState.messages) { message ->
                MessageItem(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Bottom typing area
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = chatUiState.inputText,
                    onValueChange = { viewModel.updateInputText(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            viewModel.sendMessage(chatUiState.inputText)
                            keyboardController?.hide()
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        viewModel.sendMessage(chatUiState.inputText)
                        keyboardController?.hide()
                    },
                    enabled = chatUiState.inputText.isNotBlank() && !chatUiState.isLoading
                ) {
                    if (chatUiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServerInfo(server: ServerEntry, isConnected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .padding(end = 4.dp)
    ) {
        ServerIcon(
            serverEntry = server,
            isSelected = true,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = server.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Connection status indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isConnected) {
                        Color.Green
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun MessageItem(message: ChatMessage) {
    val alignment = if (message.isFromUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isFromUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (message.isFromUser)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                    )
                )
                .background(bgColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = textColor
            )
        }

        Text(
            text = formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}