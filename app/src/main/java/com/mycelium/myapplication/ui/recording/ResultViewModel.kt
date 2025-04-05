package com.mycelium.myapplication.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.myapplication.data.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class ResultUiState(
    val isLoading: Boolean = false,
    val isProcessingComplete: Boolean = false,
    val resultText: String = "",
    val error: String = ""
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: RecordingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun loadResultStatus(recordingId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = "") }
                
                val statusResponse = repository.getProcessingStatus(recordingId)
                if (statusResponse) {
                    _uiState.update { it.copy(
                        isLoading = false,
                        isProcessingComplete = true
                    ) }
                } else {
                    _uiState.update { it.copy(
                        isLoading = false,
                        isProcessingComplete = false
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Error checking status: ${e.message ?: "Unknown error"}"
                ) }
            }
        }
    }

    fun downloadResult(recordingId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = "") }
                
                val resultText = repository.downloadResult(recordingId)
                _uiState.update { it.copy(
                    isLoading = false,
                    resultText = resultText
                ) }
            } catch (e: IOException) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Network error: ${e.message ?: "Unable to download result"}"
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Error downloading result: ${e.message ?: "Unknown error"}"
                ) }
            }
        }
    }
}