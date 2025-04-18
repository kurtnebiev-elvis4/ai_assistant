package com.mycelium.myapplication.data.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) : IAudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    override var audioDataListener: AudioDataListener? = null
    override var chunkListener: ChunkListener? = null


    override fun startRecording(sessionId: String) {
        val outputFile = File(context.cacheDir, "recording_$sessionId.mp3")
        currentFile = outputFile

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            setAudioSamplingRate(16000)
            setAudioEncodingBitRate(128000)
            prepare()
            start()
        }
    }

    override fun pauseRecording() {
        TODO("Not yet implemented")
    }

    override fun resumeRecording() {
        TODO("Not yet implemented")
    }

    override fun stopRecording(): String? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            currentFile?.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun recordedTime(): Long = 0
    override fun state(): RecordState {
        TODO("Not yet implemented")
    }

}