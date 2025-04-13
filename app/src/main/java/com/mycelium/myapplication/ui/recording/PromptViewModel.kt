package com.mycelium.myapplication.ui.recording

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.myapplication.data.model.Prompt
import com.mycelium.myapplication.data.repository.PromptRepository
import com.mycelium.myapplication.data.repository.RecordingRepository
import common.ActionManager
import common.UIStateManager
import common.WithActionManger
import common.WithUIStateManger
import common.push
import common.send
import common.uiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PromptUiState(
    val prompts: List<Prompt> = emptyList(),
    val isAddingPrompt: Boolean = false,
    val newPromptLabel: String = "",
    val newPromptMessage: String = "",
    val selectedPrompts: List<Prompt> = emptyList(),
    val error: String = ""
)

interface PromptDialogCallback {
    fun onAddPrompt()
    fun onDeletePrompt(prompt: Prompt)
    fun onSelectPrompt(prompt: Prompt, selected: Boolean)
    fun onSavePrompt()
    fun onCancelAddPrompt()
    fun onUpdateNewPromptLabel(label: String)
    fun onUpdateNewPromptMessage(message: String)
    fun analyse()
}

@HiltViewModel
class PromptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RecordingRepository,
    private val promptRepository: PromptRepository,
    override val uiStateM: UIStateManager<PromptUiState>,
    override val actionM: ActionManager<NavigationEvent>
) : ViewModel(), WithUIStateManger<PromptUiState>,
    WithActionManger<PromptViewModel.NavigationEvent>, PromptDialogCallback {
    sealed class NavigationEvent {
        object Finish : NavigationEvent()
    }

    private val recordingId: String = savedStateHandle["recordingId"]!!

    init {
        loadPrompts()
    }

    private fun loadPrompts() {
        viewModelScope.launch {
            promptRepository.getAllPrompts().collectLatest { prompts ->
                push(uiState.copy(prompts = prompts))
            }
        }
    }

    override fun onAddPrompt() {
        push(uiState.copy(isAddingPrompt = true))
    }

    override fun onDeletePrompt(prompt: Prompt) {
        viewModelScope.launch {
            try {
                promptRepository.deletePrompt(prompt)
                // Also remove from selected prompts if it was selected
                val updatedSelected = uiState.selectedPrompts.filter { it.id != prompt.id }
                push(uiState.copy(selectedPrompts = updatedSelected))
            } catch (e: Exception) {
                push(uiState.copy(error = "Failed to delete prompt: ${e.message}"))
            }
        }
    }

    override fun onSelectPrompt(prompt: Prompt, selected: Boolean) {
        val updatedSelection = if (selected) {
            uiState.selectedPrompts + prompt
        } else {
            uiState.selectedPrompts.filter { it.id != prompt.id }
        }
        push(uiState.copy(selectedPrompts = updatedSelection))
    }

    override fun onSavePrompt() {
        if (uiState.newPromptLabel.isBlank() || uiState.newPromptMessage.isBlank()) {
            push(uiState.copy(error = "Label and message are required"))
            return
        }

        viewModelScope.launch {
            try {
                val newPrompt = Prompt(
                    label = uiState.newPromptLabel.trim(),
                    message = uiState.newPromptMessage.trim()
                )
                promptRepository.insertPrompt(newPrompt)

                // Reset the form
                push(
                    uiState.copy(
                        isAddingPrompt = false,
                        newPromptLabel = "",
                        newPromptMessage = "",
                        error = ""
                    )
                )
            } catch (e: Exception) {
                push(uiState.copy(error = "Failed to save prompt: ${e.message}"))
            }
        }
    }

    override fun onCancelAddPrompt() {
        push(
            uiState.copy(
                isAddingPrompt = false,
                newPromptLabel = "",
                newPromptMessage = "",
                error = ""
            )
        )
    }

    override fun onUpdateNewPromptLabel(label: String) {
        push(uiState.copy(newPromptLabel = label))
    }

    override fun onUpdateNewPromptMessage(message: String) {
        push(uiState.copy(newPromptMessage = message))
    }

    override fun analyse() {
        viewModelScope.launch {
            try {
                repository.finishSession(
                    recordingId,
                    getSelectedPromptsMap()
                )
            } catch (e: Exception) {
                push(
                    uiState.copy(
                        error = e.message ?: "Failed to finish session with prompts"
                    )
                )
            }
            send(NavigationEvent.Finish)
        }
    }

    fun clearSelections() {
        push(uiState.copy(selectedPrompts = emptyList()))
    }

    fun getSelectedPromptsMap(): Map<String, String> {
        return uiState.selectedPrompts.associate { it.label to it.message }
    }
}