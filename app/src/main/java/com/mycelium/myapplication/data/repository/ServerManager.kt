package com.mycelium.myapplication.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mycelium.myapplication.data.model.ServerEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_SERVER_LIST = "server_list"
private const val PREF_SELECTED_SERVER_ID = "selected_server_id"
private const val TAG = "ServerManager"
private const val HEALTH_CHECK_TIMEOUT = 10L // seconds

@Singleton
class ServerManager @Inject constructor(
    private val preferences: SharedPreferences
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val _serverList = MutableStateFlow<List<ServerEntry>>(emptyList())
    val serverList: StateFlow<List<ServerEntry>> = _serverList.asStateFlow()

    private val _selectedServer = MutableStateFlow<ServerEntry?>(null)
    val selectedServer: StateFlow<ServerEntry?> = _selectedServer.asStateFlow()
    
    // Keep a cached reference of the previously selected server
    // This is useful for when we need to recreate the API client
    private var cachedSelectedServer: ServerEntry? = null

    private val _isCheckingHealth = MutableStateFlow(false)
    val isCheckingHealth: StateFlow<Boolean> = _isCheckingHealth.asStateFlow()

    private var healthCheckJob: Job? = null

    init {
        loadServerList()
        loadSelectedServer()
    }

    private fun loadServerList() {
        val list = getServerListFromPreferences()
        _serverList.value = list
    }

    private fun loadSelectedServer() {
        val selectedServerId = preferences.getString(PREF_SELECTED_SERVER_ID, null)
        
        // Find the selected server by ID
        var server = _serverList.value.find { it.id == selectedServerId }
        
        // If we couldn't find it (possibly because the ID changed), try to use the first server
        if (server == null && _serverList.value.isNotEmpty()) {
            server = _serverList.value.first()
            // Save this selection so we use it next time
            selectServer(server.id)
        }
        
        _selectedServer.value = server
        cachedSelectedServer = server
    }

    fun selectServer(serverId: String) {
        preferences.edit().putString(PREF_SELECTED_SERVER_ID, serverId).apply()
        loadSelectedServer()
        
        // Log selection for debugging
        val selectedServer = _selectedServer.value
        if (selectedServer != null) {
            Log.d(TAG, "Selected server: ${selectedServer.name} (${selectedServer.runpodId})")
        } else {
            Log.e(TAG, "Failed to select server with ID: $serverId")
        }
    }

    fun addCustomServer(name: String, runpodId: String, port: Int = 8000): ServerEntry {
        // Check if a server with the same runpodId already exists
        val existingServer = _serverList.value.find { it.runpodId == runpodId && it.port == port }
        if (existingServer != null) {
            return existingServer
        }
        
        val newServer = ServerEntry(
            id = UUID.randomUUID().toString(),
            name = name,
            runpodId = runpodId,
            port = port,
            isCustom = true,
            isOnline = false,
            lastChecked = System.currentTimeMillis()
        )

        val updatedList = _serverList.value.toMutableList().apply {
            add(newServer)
        }

        saveServerList(updatedList)

        // Check health of the new server
        checkServerHealth(newServer.id)

        return newServer
    }

    fun updateServer(server: ServerEntry) {
        val updatedList = _serverList.value.toMutableList().apply {
            val index = indexOfFirst { it.id == server.id }
            if (index >= 0) {
                set(index, server)
            }
        }

        saveServerList(updatedList)
    }

    fun deleteServer(serverId: String) {
        val serverToDelete = _serverList.value.find { it.id == serverId } ?: return

        // Don't allow deleting the last server
        if (_serverList.value.size <= 1) {
            return
        }

        val updatedList = _serverList.value.filter { it.id != serverId }
        saveServerList(updatedList)

        // If the selected server was deleted, select the first one
        if (_selectedServer.value?.id == serverId) {
            selectServer(updatedList.first().id)
        }
    }

    fun checkAllServersHealth() {
        if (_isCheckingHealth.value) return

        healthCheckJob?.cancel()
        healthCheckJob = coroutineScope.launch {
            _isCheckingHealth.value = true

            try {
                val updatedServers = _serverList.value.map { server ->
                    checkServerHealthStatus(server)
                }

                withContext(Dispatchers.Main) {
                    saveServerList(updatedServers)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking server health: ${e.message}")
            } finally {
                _isCheckingHealth.value = false
            }
        }
    }

    fun checkServerHealth(serverId: String) {
        coroutineScope.launch {
            val server = _serverList.value.find { it.id == serverId } ?: return@launch

            try {
                val updatedServer = checkServerHealthStatus(server)

                val updatedList = _serverList.value.toMutableList().apply {
                    val index = indexOfFirst { it.id == serverId }
                    if (index >= 0) {
                        set(index, updatedServer)
                    }
                }

                withContext(Dispatchers.Main) {
                    saveServerList(updatedList)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking server health: ${e.message}")
            }
        }
    }

    private suspend fun checkServerHealthStatus(server: ServerEntry): ServerEntry {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(HEALTH_CHECK_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(HEALTH_CHECK_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(HEALTH_CHECK_TIMEOUT, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(server.serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            val api = retrofit.create(AssistantApi::class.java)

            val response = withContext(Dispatchers.IO) {
                try {
                    api.checkHealth().body()?.get("status") == "ok"
                } catch (e: Exception) {
                    Log.e(TAG, "Server ${server.name} health check failed: ${e.message}")
                    false
                }
            }

            server.copy(
                isOnline = response,
                lastChecked = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating API client for health check: ${e.message}")
            server.copy(
                isOnline = false,
                lastChecked = System.currentTimeMillis()
            )
        }
    }

    private fun saveServerList(serverList: List<ServerEntry>) {
        val jsonString = Gson().toJson(serverList)
        preferences.edit().putString(PREF_SERVER_LIST, jsonString).apply()
        _serverList.value = serverList
        
        Log.d(TAG, "Saved ${serverList.size} servers to preferences")
        
        // Make sure selected server is still valid after updating the list
        val selectedServerId = preferences.getString(PREF_SELECTED_SERVER_ID, null)
        if (selectedServerId != null && serverList.none { it.id == selectedServerId }) {
            // Selected server no longer exists in the list, select first one
            if (serverList.isNotEmpty()) {
                selectServer(serverList.first().id)
            }
        }
    }

    private fun getServerListFromPreferences(): List<ServerEntry> {
        val jsonString = preferences.getString(PREF_SERVER_LIST, null)
        val serverList = if (jsonString != null) {
            try {
                val type = object : TypeToken<List<ServerEntry>>() {}.type
                Gson().fromJson<List<ServerEntry>>(jsonString, type)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing server list from preferences", e)
                emptyList()
            }
        } else {
            emptyList()
        }
        
        Log.d(TAG, "Loaded ${serverList.size} servers from preferences")
        return serverList
    }
}