package com.mycelium.myapplication.ui.recording

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.myapplication.data.model.RecordingSession
import com.mycelium.myapplication.data.repository.RecordingRepository
import common.UIStateManager
import common.WithUIStateManger
import common.push
import common.uiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RecordListState(
    val records: List<RecordingSession> = emptyList(),
    val isPlaying: Boolean = false,
    val shareIntent: android.content.Intent? = null,
    val currentPlayingSession: String? = null,
    val error: String = ""
)

@HiltViewModel
class RecordListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RecordingRepository,
    override val uiStateM: UIStateManager<RecordListState>
) : ViewModel(), WithUIStateManger<RecordListState>, RecordListCallback {

    private var currentMediaPlayer: android.media.MediaPlayer? = null


    init {
        viewModelScope.launch(Dispatchers.Default) {
            repository.getAllRecordings().collect {
                push(uiState.copy(records = it))
            }
        }
    }

    fun resetShareIntent() {
        push(uiState.copy(shareIntent = null))
    }


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
                push(
                    uiState.copy(
                        error = e.message ?: "Failed to play recording",
                        isPlaying = false,
                        currentPlayingSession = null
                    )
                )
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

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}