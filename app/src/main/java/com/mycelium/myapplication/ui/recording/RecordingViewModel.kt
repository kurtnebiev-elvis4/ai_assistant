package com.mycelium.myapplication.ui.recording

import common.UIStateManager
import common.WithUIStateManger
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.myapplication.data.model.RecordingSession
import com.mycelium.myapplication.data.recording.AudioDataListener
import com.mycelium.myapplication.data.recording.AudioRecorder
import com.mycelium.myapplication.data.recording.ChunkListener
import com.mycelium.myapplication.data.recording.IAudioRecorder
import com.mycelium.myapplication.data.recording.WavRecorder
import com.mycelium.myapplication.data.repository.RecordingRepository
import common.push
import common.uiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RecordingState(
    val isRecording: Boolean = false,
    val time: String = "",
    val error: String = ""
)

sealed class PermissionState {
    object Unknown : PermissionState()
    object Granted : PermissionState()
    object Denied : PermissionState()
}

interface RecordingViewModelCallback : RecordingScreenCallback {
    fun navigateToResultScreen(recordingId: String)
}

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RecordingRepository,
    override val uiStateM: UIStateManager<RecordingState>
) : ViewModel(), WithUIStateManger<RecordingState>, RecordingViewModelCallback {
    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Unknown)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val _waveform = MutableStateFlow<List<Short>>(emptyList())
    val waveform: StateFlow<List<Short>> = _waveform.asStateFlow()

    val recordings = repository.getAllRecordings()

    private var currentSession: RecordingSession? = null
    private var audioRecorder: IAudioRecorder? = null

//    fun updatePermissionState(granted: Boolean) {
//        _permissionState.value = if (granted) PermissionState.Granted else PermissionState.Denied
//    }

    init {
        viewModelScope.launch {
            try {
                repository.health()
                // Start the upload queue to process any pending chunks
                repository.startUploadQueue()
                // Retry any failed uploads
                repository.retryFailedUploads()
            } catch (e: Exception) {
                Log.e("RecordingViewModel", "Error during initialization", e)
            }
        }
    }

    override fun startRecording() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
//                if (_permissionState.value != PermissionState.Granted) {
//                    push(RecordingState.Error("Microphone permission not granted"))
//                    return@launch
//                }
                currentSession = RecordingSession()
                repository.insertRecording(currentSession!!)
                audioRecorder = WavRecorder(context).apply {
                    audioDataListener = object : AudioDataListener {
                        override fun onAudioDataReceived(data: ShortArray) {
                            _waveform.value = data.toList()
                        }
                    }
                    chunkListener = object : ChunkListener {
                        override fun onNewChunk(chunkIndex: Int, file: File) {
                            // Could be used for real-time progress updates if needed
                        }

                        override fun onChunkFinished(chunkIndex: Int, file: File) {
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    currentSession?.let { session ->
                                        // Add to queue and attempt to upload
                                        // The isLastChunk parameter will be set to false here as this is a chunk
                                        repository.uploadChunk(session.id, chunkIndex, false, file)
                                    }
                                } catch (e: Exception) {
                                    Log.e("RecordingViewModel", "Failed to queue chunk upload", e)
                                    // Even if there's an error adding to the queue, we'll retry later via the worker
                                }
                            }
                        }
                    }
                }
                audioRecorder?.startRecording(currentSession!!.id)
                push(
                    uiState.copy(
                        isRecording = true,
                        time = audioRecorder?.recordedTime()?.formatMilliseconds().orEmpty()
                    )
                )
            } catch (e: Exception) {
                push(uiState.copy(error = e.message ?: "Failed to start recording"))
            }
            while (audioRecorder?.isRecording() == true) {
                delay(1000)
                push(uiState.copy(time = audioRecorder?.recordedTime()?.formatMilliseconds().orEmpty()))
            }
        }
    }

    private fun Long.formatMilliseconds(): String {
        val totalSeconds = this / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        return if (hours > 0)
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else
            String.format("%02d:%02d", minutes, seconds)
    }

    override fun stopRecording() {
        viewModelScope.launch {
            currentSession?.let { session ->
                try {
                    audioRecorder?.stopRecording()
                    delay(100)
                    repository.finishSession(session.id)
//                        ?.let { filePath ->
//                        session.audioFilePath = filePath
//                        session.endTime = System.currentTimeMillis()
//                        // Set file size
//                        File(filePath).let { file ->
//                            if (file.exists()) {
//                                session.fileSize = file.length()
//                            }
//                        }
//                        repository.updateRecording(session)
//                        uploadRecording(session)
//                    }

                    push(uiState.copy(isRecording = false))
                } catch (e: Exception) {
                    push(uiState.copy(error = e.message ?: "Failed to stop recording"))
                }
            }
        }
    }

    override fun deleteRecording(recording: RecordingSession) {
        viewModelScope.launch {
            try {
                repository.deleteRecording(recording)
                recording.audioFilePath?.let { filePath ->
                    File(filePath).delete()
                }
            } catch (e: Exception) {
                push(uiState.copy(error = e.message ?: "Failed to delete recording"))
            }
        }
    }
    
    override fun navigateToResultScreen(recordingId: String) {
        // This method will be implemented in the UI layer
    }

    private suspend fun uploadRecording(session: RecordingSession) {
        // TODO: Implement actual upload logic
        // This is a stub implementation
        try {
            // Simulate network delay
            delay(1000)
            session.isUploaded = true
            repository.updateRecording(session)
        } catch (e: Exception) {
            push(uiState.copy(error = e.message ?: "Failed to upload recording"))
        }
    }
}

