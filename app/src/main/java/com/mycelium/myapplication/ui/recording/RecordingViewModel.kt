package com.mycelium.myapplication.ui.recording

import common.UIStateManager
import common.WithUIStateManger
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.myapplication.data.model.RecordingSession
import com.mycelium.myapplication.data.recording.AudioRecorder
import com.mycelium.myapplication.data.repository.RecordingRepository
import common.push
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RecordingRepository,
    override val uiStateM: UIStateManager<RecordingState>
) : ViewModel(), WithUIStateManger<RecordingState>, RecordingScreenCallback {
    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Unknown)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    val recordings = repository.getAllRecordings()

    private var currentSession: RecordingSession? = null
    private var audioRecorder: AudioRecorder? = null

//    fun updatePermissionState(granted: Boolean) {
//        _permissionState.value = if (granted) PermissionState.Granted else PermissionState.Denied
//    }

    override fun startRecording() {
        viewModelScope.launch {
            try {
//                if (_permissionState.value != PermissionState.Granted) {
//                    push(RecordingState.Error("Microphone permission not granted"))
//                    return@launch
//                }
                currentSession = RecordingSession()
                repository.insertRecording(currentSession!!)
                audioRecorder = AudioRecorder(context)
                audioRecorder?.startRecording(currentSession!!.id)
                push(RecordingState.Recording)
            } catch (e: Exception) {
                push(RecordingState.Error(e.message ?: "Failed to start recording"))
            }
        }
    }

    override fun stopRecording() {
        viewModelScope.launch {
            try {
                currentSession?.let { session ->
                    audioRecorder?.stopRecording()?.let { filePath ->
                        session.audioFilePath = filePath
                        session.endTime = System.currentTimeMillis()
                        // Set file size
                        File(filePath).let { file ->
                            if (file.exists()) {
                                session.fileSize = file.length()
                            }
                        }
                        repository.updateRecording(session)
                        uploadRecording(session)
                    }
                }
                push(RecordingState.Idle)
            } catch (e: Exception) {
                push(RecordingState.Error(e.message ?: "Failed to stop recording"))
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
                push(RecordingState.Error(e.message ?: "Failed to delete recording"))
            }
        }
    }

    private suspend fun uploadRecording(session: RecordingSession) {
        // TODO: Implement actual upload logic
        // This is a stub implementation
        try {
            // Simulate network delay
            delay(1000)
            session.isUploaded = true
            repository.updateRecording(session)
            push(RecordingState.Uploaded(session))
        } catch (e: Exception) {
            push(RecordingState.Error(e.message ?: "Failed to upload recording"))
        }
    }
}

sealed class RecordingState {
    object Idle : RecordingState()
    object Recording : RecordingState()
    data class Uploaded(val session: RecordingSession) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

sealed class PermissionState {
    object Unknown : PermissionState()
    object Granted : PermissionState()
    object Denied : PermissionState()
} 