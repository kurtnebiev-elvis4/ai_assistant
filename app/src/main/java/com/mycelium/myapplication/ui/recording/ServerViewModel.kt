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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerUiState(
    val servers: List<ServerEntry> = emptyList(),
    val selectedServer: ServerEntry? = null,
    val isShowingDialog: Boolean = false,
    val newServerEntry: ServerEntry? = null,
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
            serverManager.serverSet.collectLatest { servers ->
                push(uiState.copy(servers = servers.toList()))
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
            while (true) {
                delay(60000) // Check every minute
                checkAllServersHealth()
            }
        }
    }

    fun showAddServerDialog() {
        push(
            uiState.copy(
                isShowingDialog = true,
                isEditMode = false,
                newServerEntry = ServerEntry("", "", 8000),
                errorMessage = ""
            )
        )
    }

    fun showEditServerDialog(server: ServerEntry) {
        push(
            uiState.copy(
                isShowingDialog = true,
                isEditMode = true,
                editServerId = server.serverUrl,
                newServerEntry = server.copy(),
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
        push(uiState.copy(newServerEntry = uiState.newServerEntry?.copy(name = name)))
    }

    fun updateNewServerRunpodId(runpodId: String) {
        push(uiState.copy(newServerEntry = uiState.newServerEntry?.copy(runpodId = runpodId)))
    }

    fun updateNewServerPort(port: String) {
        push(uiState.copy(newServerEntry = uiState.newServerEntry?.copy(port = port.toInt())))
    }

    fun saveServer() {
        val state = uiState
        val name = state.newServerEntry?.name?.trim()
        val runpodId = state.newServerEntry?.runpodId?.trim()
        val port = state.newServerEntry?.port

        if (name?.isEmpty() != false || runpodId?.isEmpty() != false || port == null) {
            push(uiState.copy(errorMessage = "Server name and RunPod ID and port are required"))
            return
        }

        if (state.isEditMode) {
            val serverToUpdate = state.servers.find { it.serverUrl == state.editServerId } ?: return
            val updatedServer = serverToUpdate.copy(
                name = name,
                runpodId = runpodId,
                port = port
            )
            serverManager.updateServer(updatedServer)
        } else {
            val newServer = serverManager.addCustomServer(name, runpodId, port)
            serverManager.selectServer(newServer.serverUrl)
        }

        hideDialog()
    }

    fun selectServer(server: ServerEntry) {
        serverManager.selectServer(server.serverUrl)
    }

    fun deleteServer(server: ServerEntry) {
        if (server.isCustom) {
            serverManager.deleteServer(server.serverUrl)
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