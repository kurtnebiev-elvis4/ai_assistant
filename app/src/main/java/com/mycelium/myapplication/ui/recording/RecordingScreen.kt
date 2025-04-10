package com.mycelium.myapplication.ui.recording

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mycelium.myapplication.data.model.RecordingSession
import com.mycelium.myapplication.data.recording.RecordState
import common.provideUIState

interface RecordingScreenCallback {
    fun stopRecording()
    fun pauseRecording()
    fun unpauseRecording()
    fun startRecording()
}

interface RecordListCallback {
    fun deleteRecording(session: RecordingSession)
    fun shareRecordingChunks(recording: RecordingSession)
    fun playRecording(recording: RecordingSession)
    fun toggleChunksView(recording: RecordingSession)
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
        recordingState = RecordingState(time = "10:00"),
        recordListState = RecordListState(),
        waveform = emptyList(),
        serverUiState = ServerUiState(),
        onRequestPermission = {},
        callback = object : RecordingScreenCallback {
            override fun stopRecording() {}
            override fun pauseRecording() {}
            override fun unpauseRecording() {}
            override fun startRecording() {}
        },
        listCallback = object : RecordListCallback {
            override fun deleteRecording(session: RecordingSession) {}
            override fun shareRecordingChunks(recording: RecordingSession) {}
            override fun playRecording(recording: RecordingSession) {}
            override fun toggleChunksView(recording: RecordingSession) {}
        },
        hideServerDialog = {},
        showAddServerDialog = {},
        onNavigateToResult = {})
}

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel(),
    listViewModel: RecordListViewModel = hiltViewModel(),
    serverViewModel: ServerViewModel = hiltViewModel(),
    onRequestPermission: () -> Unit,
    onNavigateToResult: (String) -> Unit
) {
    val recordingState by viewModel.provideUIState().collectAsState()
    val recordListState by listViewModel.provideUIState().collectAsState()
    val serverUiState by serverViewModel.provideUIState().collectAsState()
    val waveform by viewModel.waveform.collectAsState()

    val context = LocalContext.current

    val shareLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { listViewModel.resetShareIntent() }

    // Handle the share intent if available
    LaunchedEffect(recordListState.shareIntent) {
        recordListState.shareIntent?.let { intent ->
            shareLauncher.launch(Intent.createChooser(intent, "Share Recordings"))
        }
    }

    // Show errors as a toast if needed
    LaunchedEffect(recordingState.error) {
        if (recordingState.error.isNotEmpty()) {
            android.widget.Toast.makeText(
                context,
                recordingState.error,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Show server selection dialog
    if (serverUiState.isShowingDialog) {
        if (serverUiState.newServerName.isNotEmpty() || serverUiState.newServerRunpodId.isNotEmpty()) {
            // Show server form dialog
            ServerFormDialog(
                state = serverUiState,
                onDismiss = { serverViewModel.hideDialog() },
                onSave = { serverViewModel.saveServer() },
                onNameChange = { serverViewModel.updateNewServerName(it) },
                onRunpodIdChange = { serverViewModel.updateNewServerRunpodId(it) },
                onPortChange = { serverViewModel.updateNewServerPort(it) }
            )
        } else {
            // Show server list dialog
            ServerListDialog(
                state = serverUiState,
                onDismiss = { serverViewModel.hideDialog() },
                onSelectServer = { serverViewModel.selectServer(it) },
                onAddServer = { serverViewModel.showAddServerDialog() },
                onEditServer = { serverViewModel.showEditServerDialog(it) },
                onDeleteServer = { serverViewModel.deleteServer(it) },
                onRefreshHealth = { serverViewModel.refreshHealthStatus() }
            )
        }
    }

    RecordingScreen(
        recordingState,
        recordListState,
        serverUiState,
        waveform,
        onRequestPermission,
        viewModel,
        listViewModel,
        { serverViewModel.hideDialog() },
        { serverViewModel.showAddServerDialog() },
        onNavigateToResult
    )
}

@Composable
fun RecordingScreen(
    recordingState: RecordingState,
    recordListState: RecordListState,
    serverUiState: ServerUiState,
    waveform: List<Short>,
    onRequestPermission: () -> Unit,
    callback: RecordingScreenCallback,
    listCallback: RecordListCallback,
    hideServerDialog: () -> Unit,
    showAddServerDialog: () -> Unit,
    onNavigateToResult: (String) -> Unit
) {

    Scaffold(
        floatingActionButton = {
            ServerButton(
                modifier = Modifier.padding(bottom = 80.dp),
                onClick = {
                    hideServerDialog()
                    showAddServerDialog()
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            RecordingList(
                recordings = recordListState.records,
                onDeleteRecording = listCallback::deleteRecording,
                onPlayRecording = listCallback::playRecording,
                onShareRecording = { recording ->
                    listCallback.shareRecordingChunks(recording)
                },
                onViewResults = { recording ->
                    onNavigateToResult(recording.id)
                },
                onToggleChunksView = listCallback::toggleChunksView,
                currentPlayingSession = recordListState.currentPlayingSession,
                chunksMap = recordListState.chunksMap
            )

            if (recordingState.error.isNotEmpty()) {
                Text(
                    text = recordingState.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }
            if (recordingState.isMicGranted == false) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxHeight(0.5f)
                            .fillMaxWidth()
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
                RecordButton(
                    Modifier.align(Alignment.BottomCenter),
                    state = recordingState.micState,
                    recordingState.time,
                    waveform,
                    callback,
                    onRequestPermission = onRequestPermission,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            ) {
                Text(
                    text = "Build Version: 1.0.2",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        hideServerDialog()
                        showAddServerDialog()
                    }
                ) {
                    serverUiState.selectedServer?.let { selectedServer ->
                        ServerIcon(
                            serverEntry = selectedServer,
                            isSelected = true,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedServer.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordButton(
    modifier: Modifier = Modifier,
    state: RecordState,
    time: String,
    waveform: List<Short>,
    callback: RecordingScreenCallback,
    onRequestPermission: () -> Unit,
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
                        if (state == RecordState.RECORDING) {
                            it.scale(pulsation)
                        } else {
                            it
                        }
                    },
                onClick = {
                    onRequestPermission()
                    when (state) {
                        RecordState.RECORDING -> callback.pauseRecording()
                        RecordState.PAUSED -> callback.unpauseRecording()
                        else -> callback.startRecording()
                    }
                },
            ) {
                val image = when (state) {
                    RecordState.PAUSED -> Icons.Filled.PlayArrow
                    RecordState.RECORDING -> Icons.Filled.Pause
                    else -> Icons.Filled.Mic
                }
                Icon(
                    modifier = Modifier
                        .fillMaxSize(0.3f),
                    imageVector = image,
                    contentDescription = "Start Recording"
                )
            }
            if (state == RecordState.RECORDING) {
                WaveformDisplay(
                    waveform, Modifier
                        .align(Alignment.Center)
                )
            }
            if (state in arrayOf(RecordState.RECORDING, RecordState.PAUSED)) {
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
        if (state in arrayOf(RecordState.RECORDING, RecordState.PAUSED)) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = {
                    callback.stopRecording()
                }) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "Stop Recording"
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
            .clip(CircleShape)
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