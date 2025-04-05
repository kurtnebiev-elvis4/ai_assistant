package com.mycelium.myapplication.data.recording

enum class RecordState {
    NONE,
    INITIALIZED,
    RECORDING,
    PAUSED,
    STOPPED
}

data class RecordInfo(
    val time: Long,
    val state: RecordState
)

data class Chunk(
    val sessionId: String,
    val index: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long = System.currentTimeMillis()
) {
    fun duration() = endTime - startTime
}

interface ChunkListener {
    fun onNewChunk(chunk: Chunk)
    fun onChunkFinished(chunk: Chunk)
}

interface AudioDataListener {
    fun onAudioDataReceived(data: ShortArray)
    fun recording(info: RecordInfo)
}

interface IAudioRecorder {
    var audioDataListener: AudioDataListener?
    var chunkListener: ChunkListener?
    fun startRecording(sessionId: String)
    fun pauseRecording()
    fun resumeRecording()
    fun stopRecording(): String?
    fun recordedTime(): Long
    fun state(): RecordState
}