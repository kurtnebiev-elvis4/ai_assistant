package com.mycelium.myapplication.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.myapplication.data.model.ChatMessage
import com.mycelium.myapplication.data.repository.ChatRepository
import com.mycelium.myapplication.data.repository.ServerManager
import common.UIStateManager
import common.WithUIStateManger
import common.push
import common.uiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val inputText: String = "",
    val error: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val serverManager: ServerManager,
    override val uiStateM: UIStateManager<ChatUiState>
) : ViewModel(), WithUIStateManger<ChatUiState> {

    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.messages.collectLatest { messages ->
                push(uiState.copy(messages = messages))
            }
        }

        viewModelScope.launch {
            chatRepository.isConnected.collectLatest { isConnected ->
                _connectionStatus.value = isConnected
                push(uiState.copy(isConnected = isConnected))
            }
        }

        viewModelScope.launch {
            serverManager.selectedServer.collectLatest { server ->
                if (server != null && uiState.messages.isEmpty()) {
                    connect()
                }
            }
        }
    }

    fun connect() {
        viewModelScope.launch {
            try {
                serverManager.selectedServer.value?.let { server ->
                    push(uiState.copy(isLoading = true, error = ""))
                    chatRepository.connect(server)
                }
            } catch (e: Exception) {
                push(uiState.copy(error = "Failed to connect: ${e.message}"))
            } finally {
                push(uiState.copy(isLoading = false))
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            chatRepository.disconnect()
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            try {
                push(uiState.copy(isLoading = true, inputText = ""))
                chatRepository.sendMessage(message)
            } catch (e: Exception) {
                push(uiState.copy(error = "Failed to send message: ${e.message}"))
            } finally {
                push(uiState.copy(isLoading = false))
            }
        }
    }

    fun updateInputText(text: String) {
        push(uiState.copy(inputText = text))
    }

    fun clearError() {
        push(uiState.copy(error = ""))
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}