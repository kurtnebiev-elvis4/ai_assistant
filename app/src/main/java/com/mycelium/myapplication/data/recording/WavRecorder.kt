package com.mycelium.myapplication.data.recording

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton


fun Chunk.getFile(context: Context) = File(context.cacheDir, "recording_${sessionId}_$index.wav")
const val MaxChunkDuration = 5 * 60_000L

@Singleton
class WavRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) : IAudioRecorder {
    private var currentFile: File? = null
    override var audioDataListener: AudioDataListener? = null
    override var chunkListener: ChunkListener? = null

    val sampleRate = 16000
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    var audioRecord: AudioRecord? = null
    var state: RecordState = RecordState.NONE
    var startTime: Long = 0L
    var endTime: Long = 0L

    override fun recordedTime(): Long =
        if (endTime == 0L && startTime == 0L) 0L
        else if (endTime == 0L) System.currentTimeMillis() - startTime
        else endTime - startTime

    override fun state(): RecordState = state


    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecording(sessionId: String) {
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        this.audioRecord = audioRecord
        val buffer = ByteArray(bufferSize)
        chunkList.clear()
        state = RecordState.INITIALIZED

        GlobalScope.launch(Dispatchers.IO) {
            audioRecord.startRecording()
            state = RecordState.RECORDING
            var chunk: Chunk? = null
            var outputStream: OutputStream? = null
            do {
                callOnTimePro(1000) {
                    async { audioDataListener?.recording(RecordInfo(chunkList.sumOf { it.duration() }, state)) }
                }
                if (state == RecordState.PAUSED) {
                    if (chunk != null) {
                        chunkFinished(chunk, outputStream!!)
                        chunk = null
                    }
                    delay(10)
                    continue
                } else if (state == RecordState.RECORDING) {
                    if (chunk == null || System.currentTimeMillis() - chunk.startTime >= MaxChunkDuration) {
                        if (chunk != null) {
                            chunkFinished(chunk, outputStream!!)
                        }
                        val nextIndex = chunkList.lastOrNull()?.index?.let { it + 1 } ?: 0
                        createChunk(sessionId, nextIndex).let {
                            chunk = it.first
                            outputStream = it.second
                        }
                    }
                    val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        outputStream?.write(buffer, 0, bytesRead)
                        async {
                            val shortBuffer = ShortArray(bytesRead / 2)
                            ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer)
                            audioDataListener?.onAudioDataReceived(shortBuffer)
                        }
                    }
                    chunk?.endTime = System.currentTimeMillis()
                }
            } while (state in arrayOf(RecordState.RECORDING, RecordState.PAUSED))
            if (chunk != null && outputStream != null) {
                chunkFinished(chunk, outputStream)
            }
            endTime = System.currentTimeMillis()
            audioRecord.release()
        }
    }

    private val chunkList = mutableListOf<Chunk>()
    private fun createChunk(sessionId: String, index: Int = 0): Pair<Chunk, FileOutputStream> {
        val chunk = Chunk(sessionId, index, System.currentTimeMillis())
        chunkList.add(chunk)
        return chunk to chunk.getFile(context).let {
            currentFile = it
            chunkListener?.onNewChunk(chunk)
            FileOutputStream(it).also {
                it.write(ByteArray(44)) // WAV header placeholder
            }
        }
    }

    override fun resumeRecording() {
        audioRecord?.startRecording()
        state = RecordState.RECORDING
    }

    override fun pauseRecording() {
        state = RecordState.PAUSED
        audioRecord?.stop()
    }

    override fun stopRecording(): String? {
        return try {
            state = RecordState.STOPPED
            audioRecord?.stop()
            audioRecord = null
            currentFile?.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun chunkFinished(
        chunk: Chunk,
        outputStream: OutputStream,
    ) {
        chunk.endTime = System.currentTimeMillis()
        val outputFile = chunk.getFile(context)
        val totalAudioLen = chunk.getFile(context).length() - 44
        writeWavHeader(outputFile, totalAudioLen, sampleRate)
        outputStream.close()
        chunkListener?.onChunkFinished(chunk)
    }

    fun writeWavHeader(file: File, totalAudioLen: Long, sampleRate: Int) {
        val channels = 1
        val byteRate = 16 * sampleRate * channels / 8
        val totalDataLen = totalAudioLen + 36

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        writeInt(header, 4, totalDataLen.toInt()) // file size - 8
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeInt(header, 16, 16) // Subchunk1Size
        writeShort(header, 20, 1) // PCM
        writeShort(header, 22, channels.toShort())
        writeInt(header, 24, sampleRate)
        writeInt(header, 28, byteRate)
        writeShort(header, 32, (channels * 16 / 8).toShort()) // block align
        writeShort(header, 34, 16) // bits per sample
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeInt(header, 40, totalAudioLen.toInt())

        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)
        raf.write(header)
        raf.close()
    }

    fun writeInt(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xff).toByte()
        data[offset + 1] = ((value shr 8) and 0xff).toByte()
        data[offset + 2] = ((value shr 16) and 0xff).toByte()
        data[offset + 3] = ((value shr 24) and 0xff).toByte()
    }

    fun writeShort(data: ByteArray, offset: Int, value: Short) {
        data[offset] = (value.toInt() and 0xff).toByte()
        data[offset + 1] = ((value.toInt() shr 8) and 0xff).toByte()
    }

    private var lastCallTime = 0L
    private suspend fun callOnTimePro(period: Long = 1000L, action: suspend () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCallTime >= period) {
            action()
            lastCallTime = currentTime
        }
    }
}

