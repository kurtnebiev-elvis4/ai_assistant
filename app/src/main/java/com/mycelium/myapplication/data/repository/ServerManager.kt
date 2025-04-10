package com.mycelium.myapplication.data.repository

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mycelium.myapplication.data.model.ServerEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_SERVER_LIST = "server_list"
private const val PREF_SELECTED_SERVER_ID = "selected_server_id"

@Singleton
class ServerManager @Inject constructor(
    private val preferences: SharedPreferences
) {
    private val _serverList = MutableStateFlow<List<ServerEntry>>(emptyList())
    val serverList: StateFlow<List<ServerEntry>> = _serverList.asStateFlow()
    
    private val _selectedServer = MutableStateFlow<ServerEntry?>(null)
    val selectedServer: StateFlow<ServerEntry?> = _selectedServer.asStateFlow()
    
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
        _selectedServer.value = _serverList.value.find { it.id == selectedServerId } ?: _serverList.value.firstOrNull()
    }
    
    fun selectServer(serverId: String) {
        preferences.edit().putString(PREF_SELECTED_SERVER_ID, serverId).apply()
        loadSelectedServer()
    }
    
    fun addCustomServer(name: String, runpodId: String, port: Int = 8000): ServerEntry {
        val newServer = ServerEntry(
            id = UUID.randomUUID().toString(),
            name = name,
            runpodId = runpodId,
            port = port,
            isCustom = true
        )
        
        val updatedList = _serverList.value.toMutableList().apply {
            add(newServer)
        }
        
        saveServerList(updatedList)
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
    
    private fun saveServerList(serverList: List<ServerEntry>) {
        val jsonString = Gson().toJson(serverList)
        preferences.edit().putString(PREF_SERVER_LIST, jsonString).apply()
        _serverList.value = serverList
    }
    
    private fun getServerListFromPreferences(): List<ServerEntry> {
        val jsonString = preferences.getString(PREF_SERVER_LIST, null)
        return if (jsonString != null) {
            try {
                val type = object : TypeToken<List<ServerEntry>>() {}.type
                Gson().fromJson(jsonString, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}