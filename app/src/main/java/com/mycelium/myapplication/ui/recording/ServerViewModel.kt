package com.mycelium.myapplication.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.myapplication.data.model.ServerEntry
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

data class ServerUiState(
    val servers: List<ServerEntry> = emptyList(),
    val selectedServer: ServerEntry? = null,
    val isShowingDialog: Boolean = false,
    val newServerName: String = "",
    val newServerRunpodId: String = "",
    val newServerPort: String = "8000",
    val isEditMode: Boolean = false,
    val editServerId: String = "",
    val errorMessage: String = "",
    val isCheckingHealth: Boolean = false
)

@HiltViewModel
class ServerViewModel @Inject constructor(
    private val serverManager: ServerManager,
    override val uiStateM: UIStateManager<ServerUiState>
) : ViewModel(), WithUIStateManger<ServerUiState> {

    init {
        viewModelScope.launch {
            serverManager.serverList.collectLatest { servers ->
                push(uiState.copy(servers = servers))
            }
        }

        viewModelScope.launch {
            serverManager.selectedServer.collectLatest { server ->
                push(uiState.copy(selectedServer = server))
            }
        }
        
        viewModelScope.launch {
            serverManager.isCheckingHealth.collectLatest { isChecking ->
                push(uiState.copy(isCheckingHealth = isChecking))
            }
        }
        
        // Initial health check
        checkAllServersHealth()
        
        // Periodic health checks
        viewModelScope.launch {
            while(true) {
                kotlinx.coroutines.delay(60000) // Check every minute
                checkAllServersHealth()
            }
        }
    }

    fun showAddServerDialog() {
        push(
            uiState.copy(
                isShowingDialog = true,
                isEditMode = false,
                newServerName = "",
                newServerRunpodId = "",
                newServerPort = "8000",
                errorMessage = ""
            )
        )
    }

    fun showEditServerDialog(server: ServerEntry) {
        push(
            uiState.copy(
                isShowingDialog = true,
                isEditMode = true,
                editServerId = server.id,
                newServerName = server.name,
                newServerRunpodId = server.runpodId,
                newServerPort = server.port.toString(),
                errorMessage = ""
            )
        )
    }

    fun hideDialog() {
        push(
            uiState.copy(
                isShowingDialog = false,
                errorMessage = ""
            )
        )
    }

    fun updateNewServerName(name: String) {
        push(uiState.copy(newServerName = name))
    }

    fun updateNewServerRunpodId(runpodId: String) {
        push(uiState.copy(newServerRunpodId = runpodId))
    }

    fun updateNewServerPort(port: String) {
        push(uiState.copy(newServerPort = port))
    }

    fun saveServer() {
        val state = uiState
        val name = state.newServerName.trim()
        val runpodId = state.newServerRunpodId.trim()
        val portString = state.newServerPort.trim()

        if (name.isEmpty() || runpodId.isEmpty()) {
            push(
                uiState.copy(
                    errorMessage = "Server name and RunPod ID are required"
                )
            )
            return
        }

        val port = try {
            portString.toInt()
        } catch (e: NumberFormatException) {
            push(
                uiState.copy(
                    errorMessage = "Port must be a number"
                )
            )
            return
        }

        if (state.isEditMode) {
            val serverToUpdate = state.servers.find { it.id == state.editServerId } ?: return
            val updatedServer = serverToUpdate.copy(
                name = name,
                runpodId = runpodId,
                port = port
            )
            serverManager.updateServer(updatedServer)
        } else {
            val newServer = serverManager.addCustomServer(name, runpodId, port)
            serverManager.selectServer(newServer.id)
        }

        hideDialog()
    }

    fun selectServer(server: ServerEntry) {
        serverManager.selectServer(server.id)
    }

    fun deleteServer(server: ServerEntry) {
        if (server.isCustom) {
            serverManager.deleteServer(server.id)
        }
    }
    
    fun checkServerHealth(serverId: String) {
        serverManager.checkServerHealth(serverId)
    }
    
    fun checkAllServersHealth() {
        serverManager.checkAllServersHealth()
    }
    
    fun refreshHealthStatus() {
        viewModelScope.launch {
            checkAllServersHealth()
        }
    }
}