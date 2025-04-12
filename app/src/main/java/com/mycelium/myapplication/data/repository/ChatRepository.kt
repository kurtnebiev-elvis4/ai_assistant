package com.mycelium.myapplication.data.repository

import com.mycelium.myapplication.data.model.ChatMessage
import com.mycelium.myapplication.data.model.ServerEntry
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import android.util.Log
import java.util.UUID

interface ChatRepository {
    val messages: StateFlow<List<ChatMessage>>
    val isConnected: StateFlow<Boolean>

    suspend fun connect(server: ServerEntry)
    suspend fun disconnect()
    suspend fun sendMessage(content: String)
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val client: OkHttpClient
) : ChatRepository {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var webSocket: WebSocket? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val chatId = UUID.randomUUID().toString()

    override suspend fun connect(server: ServerEntry) {
        Log.d("ChatRepository", "Attempting to connect to server: $server")
        if (_isConnected.value) {
            disconnect()
        }

        val wsUrl = "wss://${server.runpodId}-${server.port}.proxy.runpod.net/ws/${chatId}"

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("x-api-key", "test-api-key")
            .build()

        withContext(Dispatchers.IO) {
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("ChatRepository", "WebSocket connection opened")
                    coroutineScope.launch {
                        _isConnected.value = true
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d("ChatRepository", "Received message: $text")
                    coroutineScope.launch {
                        val newMessage = ChatMessage(
                            content = text,
                            isFromUser = false
                        )
                        _messages.value = _messages.value + newMessage
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("ChatRepository", "WebSocket is closing: code=$code, reason=$reason")
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("ChatRepository", "WebSocket closed: code=$code, reason=$reason")
                    coroutineScope.launch {
                        _isConnected.value = false
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("ChatRepository", "WebSocket failure", t)
                    coroutineScope.launch {
                        _isConnected.value = false
                        val errorMessage = ChatMessage(
                            content = "Connection error: ${t.message}",
                            isFromUser = false
                        )
                        _messages.value = _messages.value + errorMessage
                    }
                }
            })
        }
    }

    override suspend fun disconnect() {
        Log.d("ChatRepository", "Disconnecting from WebSocket")
        withContext(Dispatchers.IO) {
            webSocket?.close(1000, "User closed the connection")
            webSocket = null
            _isConnected.value = false
        }
    }

    override suspend fun sendMessage(content: String) {
        Log.d("ChatRepository", "Sending message: $content")
        val message = ChatMessage(
            content = content,
            isFromUser = true
        )

        _messages.value = _messages.value + message

        withContext(Dispatchers.IO) {
            if (webSocket != null && _isConnected.value) {
                webSocket?.send(content)
            } else {
                Log.w("ChatRepository", "Failed to send message. Not connected to server.")
                val errorMessage = ChatMessage(
                    content = "Not connected to server. Please reconnect.",
                    isFromUser = false
                )
                _messages.value = _messages.value + errorMessage
            }
        }
    }
}