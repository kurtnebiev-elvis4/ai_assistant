package com.mycelium.myapplication.ui.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mycelium.myapplication.data.model.Prompt
import common.provideAction
import common.provideUIState

@Composable
fun PromptScreen(
    viewModel: PromptViewModel = hiltViewModel(),
    action: (PromptViewModel.NavigationEvent) -> Unit,
) {
    val state by viewModel.provideUIState().collectAsState()
    LaunchedEffect(Unit) {
        viewModel.provideAction().collect {
            action(it)
        }
    }
    PromptScreen(
        state = state,
        callback = viewModel,
    )
}

@Composable
fun PromptScreen(
    state: PromptUiState,
    callback: PromptDialogCallback,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Custom Prompts",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (state.error.isNotEmpty()) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (state.isAddingPrompt) {
            AddPromptForm(
                label = state.newPromptLabel,
                message = state.newPromptMessage,
                onLabelChange = callback::onUpdateNewPromptLabel,
                onMessageChange = callback::onUpdateNewPromptMessage,
                onSave = callback::onSavePrompt,
                onCancel = callback::onCancelAddPrompt
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.prompts) { prompt ->
                        PromptItem(
                            prompt = prompt,
                            isSelected = state.selectedPrompts.contains(prompt),
                            onSelect = { selected ->
                                callback.onSelectPrompt(prompt, selected)
                            },
                            onDelete = { callback.onDeletePrompt(prompt) }
                        )
                    }
                }
                Button(onClick = {
                    callback.analyse()
                }, modifier = Modifier.align(Alignment.BottomCenter)) {
                    Text(text = "Analyse")
                }

                FloatingActionButton(
                    onClick = callback::onAddPrompt,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Prompt")
                }
            }
        }
    }
}

@Composable
fun AddPromptForm(
    label: String,
    message: String,
    onLabelChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Add New Prompt",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = label,
            onValueChange = onLabelChange,
            label = { Text("Label") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onSave
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun PromptItem(
    prompt: Prompt,
    isSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelect
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = prompt.label,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = prompt.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!prompt.isDefault) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Prompt",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // Default prompts can't be deleted
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Default",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}