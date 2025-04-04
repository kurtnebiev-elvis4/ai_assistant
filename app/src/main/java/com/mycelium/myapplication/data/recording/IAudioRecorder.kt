package com.mycelium.myapplication.data.recording

interface IAudioRecorder {
    fun startRecording(sessionId: String)
    fun stopRecording(): String?
    fun recordedTime(): Long
}