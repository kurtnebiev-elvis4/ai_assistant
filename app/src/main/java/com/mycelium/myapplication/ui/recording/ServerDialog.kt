package com.mycelium.myapplication.ui.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mycelium.myapplication.data.model.ServerEntry
import com.mycelium.myapplication.data.model.ServerStatus

@Composable
fun ServerIcon(
    serverEntry: ServerEntry,
    serverStatus: ServerStatus,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    serverStatus.isOnline -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = serverEntry.name.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                serverStatus.isOnline -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onErrorContainer
            }
        )
    }
}

@Composable
fun ServerButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Server settings",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ChatButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = "Chat with AI",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ServerListDialog(
    state: ServerUiState,
    onDismiss: () -> Unit,
    onSelectServer: (ServerEntry) -> Unit,
    onAddServer: () -> Unit,
    onEditServer: (ServerEntry) -> Unit,
    onDeleteServer: (ServerEntry) -> Unit,
    onRefreshHealth: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Server",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row {
                        IconButton(
                            onClick = onRefreshHealth,
                            enabled = !state.isCheckingHealth
                        ) {
                            if (state.isCheckingHealth) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh server status"
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(state.servers.keys.toList()) { server ->
                        val isSelected = state.selectedServer?.serverUrl == server.serverUrl

                        ServerItem(
                            onSelectServer, server, state.servers[server] ?: ServerStatus(),
                            isSelected, onEditServer, onDeleteServer
                        )

                        Divider()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAddServer,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add server"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Server")
                }
            }
        }
    }
}

@Preview
@Composable
fun ServerItemPreview() {
    ServerItem(
        onSelectServer = { },
        server = ServerEntry(name = "name", runpodId = "url", port = 8000),
        status = ServerStatus(isOnline = true),
        isSelected = true,
        onEditServer = {},
        onDeleteServer = { }
    )
}

@Composable
private fun ServerItem(
    onSelectServer: (ServerEntry) -> Unit,
    server: ServerEntry,
    status: ServerStatus,
    isSelected: Boolean,
    onEditServer: (ServerEntry) -> Unit,
    onDeleteServer: (ServerEntry) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectServer(server) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                ServerIcon(
                    serverEntry = server,
                    serverStatus = status,
                    isSelected = isSelected
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = server.serverUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (status.isOnline) {
                                    Color.Green
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                shape = CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = if (status.isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.isOnline) {
                            Color.Green
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onEditServer(server) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit server"
                )
            }

            IconButton(onClick = { onDeleteServer(server) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete server"
                )
            }
        }
    }
}

@Composable
fun ServerFormDialog(
    state: ServerUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onRunpodIdChange: (String) -> Unit,
    onPortChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (state.isEditMode) "Edit Server" else "Add Server") },
        text = {
            AddServerItem(state, onNameChange, onRunpodIdChange, onPortChange)
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddServerItem(
    state: ServerUiState,
    onNameChange: (String) -> Unit,
    onRunpodIdChange: (String) -> Unit,
    onPortChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = state.newServerEntry?.name.orEmpty(),
            onValueChange = onNameChange,
            label = { Text("Server Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.newServerEntry?.runpodId.orEmpty(),
            onValueChange = onRunpodIdChange,
            label = { Text("RunPod ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.newServerEntry?.port?.toString().orEmpty(),
            onValueChange = onPortChange,
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth()
        )

        if (state.errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}