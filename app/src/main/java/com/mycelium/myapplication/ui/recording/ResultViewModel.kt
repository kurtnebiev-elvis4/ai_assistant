package com.mycelium.myapplication.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.myapplication.data.repository.RecordingRepository
import common.UIStateManager
import common.WithUIStateManger
import common.push
import common.uiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultUiState(
    val isLoading: Boolean = false,
    val isProcessingComplete: Boolean = false,
    val resultText: List<Pair<String, String>> = emptyList(),
    val error: String = ""
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: RecordingRepository, override val uiStateM: UIStateManager<ResultUiState>
) : ViewModel(), WithUIStateManger<ResultUiState> {


    fun loadResultStatus(recordingId: String) {
        viewModelScope.launch {
            try {
                push(uiState.copy(isLoading = true, error = ""))

                val statusResponse = repository.getProcessingStatus(recordingId)
                if (statusResponse.any { it.value == true }) {
                    downloadResult(recordingId, statusResponse.filter { it.value }.keys.toList())
                }
                if (statusResponse.all { it.value }) {
                    push(
                        uiState.copy(
                            isLoading = false, isProcessingComplete = true
                        )

                    )
                } else {
                    push(
                        uiState.copy(
                            isLoading = false, isProcessingComplete = false
                        )
                    )
                }
            } catch (e: Exception) {
                push(
                    uiState.copy(
                        isLoading = false, error = "Error checking status: ${e.message ?: "Unknown error"}"
                    )
                )
            }
        }
    }

    fun downloadResult(
        recordingId: String, types: List<String> = listOf("transcript", "summary", "tasks", "decisions", "ready")
    ) {
        viewModelScope.launch {
            push(uiState.copy(isLoading = true, error = ""))
            repository.downloadResult(recordingId, types).catch {
                    push(
                        uiState.copy(
                            isLoading = false, error = "Error downloading result: ${it.message ?: "Unknown error"}"
                        )
                    )
                }.collect {
                    push(
                        uiState.copy(
                            isLoading = false, resultText = uiState.resultText + it
                        )
                    )
                }
        }
    }
}