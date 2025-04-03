package com.mycelium.myapplication.ui.recording

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
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

@Preview(showBackground = true)
@Composable
fun RecordingScreenPreview() {
    RecordingScreen(
        RecordingState.Idle, PermissionState.Unknown,
        emptyList(), {}, object : RecordingScreenCallback {
            override fun stopRecording() {}
            override fun startRecording() {}
            override fun deleteRecording(session: RecordingSession) {}
        })
}

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel(),
    onRequestPermission: () -> Unit
) {
    val recordingState by viewModel.provideUIState().collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val recordings by viewModel.recordings.collectAsState(initial = emptyList())
    RecordingScreen(recordingState, permissionState, recordings, onRequestPermission, viewModel)
}

@Composable
fun RecordingScreen(
    recordingState: RecordingState,
    permissionState: PermissionState,
    recordings: List<RecordingSession>,
    onRequestPermission: () -> Unit,
    callback: RecordingScreenCallback
) {
    val isRecording = recordingState is RecordingState.Recording
    val scale by animateFloatAsState(if (isRecording) 1.2f else 1f)


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
                    onPlayRecording = { /* TODO: Implement playback */ }
                )
            }

            if (recordingState is RecordingState.Uploaded) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            if (recordingState is RecordingState.Error) {
                Text(
                    text = (recordingState as RecordingState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }

            RecordButton(
                Modifier.align(Alignment.BottomCenter),
                isRecording = isRecording, callback,
                onRequestPermission = onRequestPermission
            )
        }
    }
}

@Composable
fun RecordButton(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
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
        Button(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .aspectRatio(1f)
                .align(Alignment.Center).let {
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
    }
}