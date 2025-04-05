package com.mycelium.myapplication.data.recording

interface ChunkListener {
    fun onNewChunk(chunk: Chunk)
    fun onChunkFinished(chunk: Chunk)
}

interface AudioDataListener {
    fun onAudioDataReceived(data: ShortArray)
}

interface IAudioRecorder {
    var audioDataListener: AudioDataListener?
    var chunkListener: ChunkListener?
    fun startRecording(sessionId: String)
    fun pauseRecording()
    fun resumeRecording()
    fun stopRecording(): String?
    fun recordedTime(): Long
    fun isRecording(): Boolean
}