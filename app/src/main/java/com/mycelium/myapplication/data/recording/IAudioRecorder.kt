package com.mycelium.myapplication.data.recording

import androidx.room.Index
import java.io.File

interface ChunkListener {
    fun onNewChunk(chunkIndex: Int, file: File)
    fun onChunkFinished(chunkIndex: Int, file: File)
}

interface AudioDataListener {
    fun onAudioDataReceived(data: ShortArray)
}

interface IAudioRecorder {
    var audioDataListener: AudioDataListener?
    var chunkListener: ChunkListener?
    fun startRecording(sessionId: String)
    fun stopRecording(): String?
    fun recordedTime(): Long
    fun isRecording(): Boolean
}