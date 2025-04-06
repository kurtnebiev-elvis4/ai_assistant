package com.mycelium.myapplication.ui.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.myapplication.data.model.RecordingSession
import com.mycelium.myapplication.data.recording.AudioDataListener
import com.mycelium.myapplication.data.recording.Chunk
import com.mycelium.myapplication.data.recording.ChunkListener
import com.mycelium.myapplication.data.recording.IAudioRecorder
import com.mycelium.myapplication.data.recording.RecordInfo
import com.mycelium.myapplication.data.recording.RecordState
import com.mycelium.myapplication.data.recording.WavRecorder
import com.mycelium.myapplication.data.repository.RecordingRepository
import common.UIStateManager
import common.WithUIStateManger
import common.push
import common.uiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordingState(
    val micState: RecordState = RecordState.NONE,
    val time: String = "",
    val error: String = "",
    val isMicGranted: Boolean = false
)

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RecordingRepository,
    override val uiStateM: UIStateManager<RecordingState>
) : ViewModel(), WithUIStateManger<RecordingState>, RecordingScreenCallback {

    private val _waveform = MutableStateFlow<List<Short>>(emptyList())
    val waveform: StateFlow<List<Short>> = _waveform.asStateFlow()

    private var currentSession = RecordingSession()
    private var audioRecorder: IAudioRecorder? = null

    init {
        checkPermission()
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

    fun checkPermission() =
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            push(RecordingState(isMicGranted = false, error = "Microphone permission not granted"))
            false
        } else {
            push(RecordingState(isMicGranted = true))
            true
        }


    override fun startRecording() {
        if (!checkPermission()) return
        viewModelScope.launch(Dispatchers.Default) {
            try {
                currentSession = RecordingSession(
                    startTime = System.currentTimeMillis(),
                )
                repository.insertRecording(currentSession)
                audioRecorder = WavRecorder(context).apply {
                    audioDataListener = object : AudioDataListener {
                        override fun onAudioDataReceived(data: ShortArray) {
                            _waveform.value = data.toList()
                        }

                        override fun recording(info: RecordInfo) {
                            push(
                                uiState.copy(
                                    micState = audioRecorder?.state() ?: RecordState.NONE,
                                    time = info.time.formatMilliseconds()
                                )
                            )
                        }
                    }
                    chunkListener = object : ChunkListener {
                        override fun onNewChunk(chunk: Chunk) {
                            push(uiState.copy(micState = audioRecorder?.state() ?: RecordState.NONE))
                        }

                        override fun onChunkFinished(chunk: Chunk) {
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    repository.uploadChunk(chunk, false)
                                } catch (e: Exception) {
                                    Log.e("RecordingViewModel", "Failed to queue chunk upload", e)
                                    // Even if there's an error adding to the queue, we'll retry later via the worker
                                }
                            }
                        }
                    }
                }
                audioRecorder?.startRecording(currentSession.id)
                push(
                    uiState.copy(
                        micState = audioRecorder?.state() ?: RecordState.NONE,
                        time = audioRecorder?.recordedTime()?.formatMilliseconds().orEmpty()
                    )
                )
            } catch (e: Exception) {
                push(
                    uiState.copy(
                        micState = audioRecorder?.state() ?: RecordState.NONE,
                        error = e.message ?: "Failed to start recording"
                    )
                )
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

    override fun unpauseRecording() {
        audioRecorder?.resumeRecording()
        push(uiState.copy(micState = audioRecorder?.state() ?: RecordState.NONE))
    }

    override fun pauseRecording() {
        audioRecorder?.pauseRecording()
        push(uiState.copy(micState = audioRecorder?.state() ?: RecordState.NONE))
    }

    override fun stopRecording() {
        viewModelScope.launch {
            currentSession.let { session ->
                try {
                    audioRecorder?.stopRecording()
                    delay(100)
                    push(uiState.copy(audioRecorder?.state() ?: RecordState.NONE))
                } catch (e: Exception) {
                    push(
                        uiState.copy(
                            audioRecorder?.state() ?: RecordState.NONE,
                            error = e.message ?: "Failed to stop recording"
                        )
                    )
                }
                try {
                    repository.finishSession(session.id)
                } catch (e: Exception) {
                    push(
                        uiState.copy(
                            audioRecorder?.state() ?: RecordState.NONE,
                            error = e.message ?: "Failed to finish session"
                        )
                    )
                }
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
        } catch (e: Exception) {
            push(uiState.copy(error = e.message ?: "Failed to upload recording"))
        }
    }
}

