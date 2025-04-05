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
import com.mycelium.myapplication.data.recording.Chunk
import com.mycelium.myapplication.data.recording.ChunkListener
import com.mycelium.myapplication.data.recording.IAudioRecorder
import com.mycelium.myapplication.data.recording.RecordInfo
import com.mycelium.myapplication.data.recording.RecordState
import com.mycelium.myapplication.data.recording.WavRecorder
import com.mycelium.myapplication.data.recording.getFile
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
    val micState: RecordState = RecordState.NONE,
    val time: String = "",
    val error: String = "",
    val shareIntent: android.content.Intent? = null,
    val isPlaying: Boolean = false,
    val currentPlayingSession: String? = null
)

sealed class PermissionState {
    object Unknown : PermissionState()
    object Granted : PermissionState()
    object Denied : PermissionState()
}


@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RecordingRepository,
    override val uiStateM: UIStateManager<RecordingState>
) : ViewModel(), WithUIStateManger<RecordingState>, RecordingScreenCallback {
    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Unknown)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val _waveform = MutableStateFlow<List<Short>>(emptyList())
    val waveform: StateFlow<List<Short>> = _waveform.asStateFlow()

    val recordings = repository.getAllRecordings()

    private var currentSession = RecordingSession()
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

    override fun shareRecordingChunks(recording: RecordingSession) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val chunks = repository.getChunksForSession(recording.id)
                if (chunks.isEmpty()) {
                    push(uiState.copy(error = "No chunks found for this recording"))
                    return@launch
                }
                
                // Create list of file URIs to share
                val filePaths = chunks.map { it.filePath }.filter { File(it).exists() }
                if (filePaths.isEmpty()) {
                    push(uiState.copy(error = "No valid chunk files found for this recording"))
                    return@launch
                }
                
                // The actual sharing will be handled by the UI layer
                // We just prepare the file paths and notify the UI
                val intent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(
                        android.content.Intent.EXTRA_STREAM,
                        ArrayList(filePaths.map { 
                            androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                File(it)
                            )
                        })
                    )
                    type = "audio/wav"
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                // This will be captured by the UI layer
                push(uiState.copy(shareIntent = intent))
            } catch (e: Exception) {
                push(uiState.copy(error = e.message ?: "Failed to share recording chunks"))
            }
        }
    }
    
    fun resetShareIntent() {
        push(uiState.copy(shareIntent = null))
    }
    
    // Store the current media player to be able to stop it
    private var currentMediaPlayer: android.media.MediaPlayer? = null
    
    override fun playRecording(recording: RecordingSession) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // If this recording is already playing, stop it
                if (uiState.isPlaying && uiState.currentPlayingSession == recording.id) {
                    stopPlayback()
                    return@launch
                }
                
                // Stop any existing playback
                stopPlayback()
                
                val chunks = repository.getChunksForSession(recording.id)
                if (chunks.isEmpty()) {
                    push(uiState.copy(error = "No chunks found for this recording"))
                    return@launch
                }
                
                // Create list of file URIs to play
                val filePaths = chunks.map { it.filePath }.filter { File(it).exists() }
                if (filePaths.isEmpty()) {
                    push(uiState.copy(error = "No valid chunk files found for this recording"))
                    return@launch
                }
                
                // Create and start a media player
                val mediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(filePaths.first())
                    prepare()
                    start()
                    
                    // Set up a listener for completion to play the next chunk if available
                    var currentIndex = 0
                    setOnCompletionListener {
                        currentIndex++
                        if (currentIndex < filePaths.size) {
                            // Play the next chunk
                            reset()
                            try {
                                setDataSource(filePaths[currentIndex])
                                prepare()
                                start()
                            } catch (e: Exception) {
                                Log.e("RecordingViewModel", "Error playing next chunk", e)
                                release()
                                currentMediaPlayer = null
                                push(uiState.copy(isPlaying = false, currentPlayingSession = null))
                            }
                        } else {
                            // All chunks played
                            release()
                            currentMediaPlayer = null
                            push(uiState.copy(isPlaying = false, currentPlayingSession = null))
                        }
                    }
                }
                
                // Store the media player for future reference
                currentMediaPlayer = mediaPlayer
                
                // Update UI state indicating playback is happening
                push(uiState.copy(isPlaying = true, currentPlayingSession = recording.id))
                
                // When the MediaPlayer is released, update the UI
                mediaPlayer.setOnErrorListener { _, _, _ ->
                    currentMediaPlayer = null
                    push(uiState.copy(isPlaying = false, currentPlayingSession = null))
                    true
                }
            } catch (e: Exception) {
                currentMediaPlayer = null
                push(uiState.copy(error = e.message ?: "Failed to play recording", isPlaying = false, currentPlayingSession = null))
            }
        }
    }
    
    private fun stopPlayback() {
        currentMediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                currentMediaPlayer = null
                push(uiState.copy(isPlaying = false, currentPlayingSession = null))
            } catch (e: Exception) {
                Log.e("RecordingViewModel", "Error stopping playback", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopPlayback()
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

