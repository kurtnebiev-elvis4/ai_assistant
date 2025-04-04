package com.mycelium.myapplication.ui.recording

import android.os.Build
import android.util.Half.abs
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mycelium.ai_meet_assistant.R
import com.mycelium.myapplication.data.model.RecordingSession
import common.provideUIState

interface RecordingScreenCallback {
    fun stopRecording()
    fun startRecording()
    fun deleteRecording(session: RecordingSession)
}

fun movingAverage(data: List<Short>, windowSize: Int = 5): List<Short> {
    return data.mapIndexed { index, _ ->
        val from = maxOf(0, index - windowSize / 2)
        val to = minOf(data.size, index + windowSize / 2 + 1)
        (data.subList(from, to).sum() / (to - from)).toShort()
    }
}

@Preview(showBackground = true)
@Composable
fun RecordingScreenPreview() {
    RecordingScreen(
        RecordingState(time = "10:00"),
        PermissionState.Unknown,
        emptyList(),
        emptyList(), {}, object : RecordingScreenCallback {
            override fun stopRecording() {}
            override fun startRecording() {}
            override fun deleteRecording(session: RecordingSession) {}
        })
}

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel(),
    onRequestPermission: () -> Unit,
    onNavigateToResult: (String) -> Unit
) {
    val recordingState by viewModel.provideUIState().collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val recordings by viewModel.recordings.collectAsState(initial = emptyList())
    val waveform by viewModel.waveform.collectAsState()
    
    // Implement the navigation in the callback
    val callbackWithNavigation = object : RecordingViewModelCallback {
        override fun startRecording() = viewModel.startRecording()
        override fun stopRecording() = viewModel.stopRecording()
        override fun deleteRecording(session: RecordingSession) = viewModel.deleteRecording(session)
        override fun navigateToResultScreen(recordingId: String) = onNavigateToResult(recordingId)
    }
    
    RecordingScreen(recordingState, permissionState, waveform, recordings, onRequestPermission, callbackWithNavigation)
}

@Composable
fun RecordingScreen(
    recordingState: RecordingState,
    permissionState: PermissionState,
    waveform: List<Short>,
    recordings: List<RecordingSession>,
    onRequestPermission: () -> Unit,
    callback: RecordingScreenCallback
) {

    Scaffold(
        floatingActionButton = {

        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            if (permissionState == PermissionState.Denied) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Microphone permission is required to record audio",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = onRequestPermission) {
                        Text("Grant Permission")
                    }
                }
            } else {
                RecordingList(
                    recordings = recordings,
                    onDeleteRecording = callback::deleteRecording,
                    onPlayRecording = { /* TODO: Implement playback */ },
                    onViewResults = { recording -> 
                        if (callback is RecordingViewModelCallback) {
                            callback.navigateToResultScreen(recording.id)
                        }
                    }
                )
            }

//            if (recordingState is RecordingState.Uploaded) {
//                CircularProgressIndicator(
//                    modifier = Modifier.align(Alignment.TopCenter)
//                )
//            }

            if (recordingState.error.isNotEmpty()) {
                Text(
                    text = recordingState.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }

            RecordButton(
                Modifier.align(Alignment.BottomCenter),
                isRecording = recordingState.isRecording,
                recordingState.time,
                waveform,
                callback,
                onRequestPermission = onRequestPermission
            )
        }
    }
}

@Composable
fun RecordButton(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    time: String,
    waveform: List<Short>,
    callback: RecordingScreenCallback,
    onRequestPermission: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulsation by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight(0.5f)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .aspectRatio(1f)
                .align(Alignment.Center)
        ) {
            Button(
                modifier = Modifier
                    .fillMaxSize()
                    .let {
                        if (isRecording) {
                            it.scale(pulsation)
                        } else {
                            it
                        }
                    },
                onClick = {
                    onRequestPermission()
                    if (isRecording) {
                        callback.stopRecording()
                    } else {
                        callback.startRecording()
                    }
                },
            ) {
                val image = if (isRecording) {
                    ImageVector.vectorResource(R.drawable.ic_stop)
                } else {
                    ImageVector.vectorResource(R.drawable.ic_mic)
                }
                Icon(
                    modifier = Modifier.fillMaxSize(0.3f),
                    imageVector = image,
                    contentDescription = "Start Recording"
                )
            }
            if (isRecording) {
                WaveformDisplay(waveform, Modifier.align(Alignment.Center))
            }
            if (time.isNotEmpty()) {
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    text = time,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

        }
    }
}

@Composable
fun WaveformDisplay(
    waveform: List<Short>,
    modifier: Modifier = Modifier
) {
    val maxSamples = 20
    val waveformBuffer = remember { mutableStateListOf<Short>() }

    LaunchedEffect(waveform) {
        val cleaned = movingAverage(waveform)
        val noiseThreshold = 1000.toShort()
        val filtered = cleaned.map { if (kotlin.math.abs(it.toInt()) < noiseThreshold) 0.toShort() else it }

        val batchSize = 320
        val averaged = filtered.chunked(batchSize).map { chunk ->
            if (chunk.isNotEmpty()) (chunk.sum() /*/ chunk.size*/).toShort() else 0.toShort()
        }

        waveformBuffer.addAll(averaged)
        if (waveformBuffer.size > maxSamples) {
            waveformBuffer.removeRange(0, waveformBuffer.size - maxSamples)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxHeight(0.6f)
            .fillMaxWidth(0.6f)
    ) {
        val centerY = size.height / 2

        if (waveformBuffer.isEmpty()) return@Canvas

        val widthPerSample = size.width / waveformBuffer.size

        waveformBuffer.forEachIndexed { index, sample ->
            val normalized = sample / Short.MAX_VALUE.toFloat()
            val x = index * widthPerSample

            if (kotlin.math.abs(normalized) < 0.02f) {
                drawCircle(
                    color = Color(0x44CCCCCC),
                    center = Offset(x, centerY),
                    radius = 3f
                )
            } else {
                val y = (normalized * centerY)
                drawLine(
                    color = Color(0x88DDDDDD),
                    start = Offset(x, centerY - y),
                    end = Offset(x, centerY + y),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}