package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DecibelGauge
import com.example.ui.components.NoiseLevelBadge
import com.example.ui.components.RecordSessionDialog
import com.example.ui.components.WaveformView
import com.example.ui.theme.AcousticAmber
import com.example.ui.theme.AcousticCyan
import com.example.ui.theme.AcousticGreen
import com.example.ui.theme.AcousticRed
import com.example.ui.theme.DarkCard
import com.example.viewmodel.MeterViewModel

@Composable
fun MeterScreen(
    viewModel: MeterViewModel,
    hasRecordPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meterData by viewModel.meterData.collectAsStateWithLifecycle()
    val weightingType by viewModel.weightingType.collectAsStateWithLifecycle()
    val responseMode by viewModel.responseMode.collectAsStateWithLifecycle()
    val pendingSessionResult by viewModel.pendingSessionResult.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    // Save session dialog
    pendingSessionResult?.let { result ->
        RecordSessionDialog(
            sessionResult = result,
            onSave = { title, tag, notes ->
                viewModel.saveSessionRecord(title, tag, notes)
            },
            onDismiss = {
                viewModel.dismissSessionDialog()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasRecordPermission) {
            // Permission Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Izin Mikrofon Diperlukan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aplikasi membutuhkan akses mikrofon untuk mengukur intensitas desibel (dB) suara secara akurat dan 100% offline.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AcousticCyan, contentColor = Color(0xFF0F172A)),
                        modifier = Modifier.testTag("request_mic_permission_button")
                    ) {
                        Text("Izinkan Mikrofon", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Top Status Bar (Weighting, Speed, Live State)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (meterData.isRecordingAudio) AcousticGreen else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (meterData.isRecordingAudio) "Mikrofon Aktif" else "Siaga",
                    fontSize = 11.sp,
                    color = if (meterData.isRecordingAudio) AcousticGreen else Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillLabel(weightingType)
                PillLabel(responseMode)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Decibel Dial Gauge
        DecibelGauge(
            currentDb = meterData.currentDb,
            peakDb = meterData.peakDb,
            weightingType = weightingType
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Statistics Bar (MIN, AVG, MAX, PEAK) + Reset Action
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(label = "MIN", value = "%.1f".format(meterData.minDb), color = AcousticGreen)
                StatItem(label = "AVG (Leq)", value = "%.1f".format(meterData.avgDb), color = AcousticCyan)
                StatItem(label = "MAX", value = "%.1f".format(meterData.maxDb), color = AcousticAmber)
                StatItem(label = "PEAK", value = "%.1f".format(meterData.peakDb), color = AcousticRed)

                IconButton(
                    onClick = { viewModel.resetStatistics() },
                    modifier = Modifier.size(36.dp).testTag("reset_stats_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Statistik",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Noise Level & Safe Exposure Badge
        NoiseLevelBadge(currentDb = meterData.currentDb)

        Spacer(modifier = Modifier.height(12.dp))

        // Live Waveform Oscilloscope
        WaveformView(waveformPoints = meterData.waveformPoints)

        Spacer(modifier = Modifier.height(16.dp))

        // Session Recording Action Button
        if (meterData.isSessionActive) {
            Button(
                onClick = { viewModel.stopSessionRecording() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcousticRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("stop_session_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(end = 6.dp)
                )
                val mins = meterData.sessionDurationSec / 60
                val secs = meterData.sessionDurationSec % 60
                Text(
                    text = "Berhenti & Simpan Rekaman (${String.format("%02d:%02d", mins, secs)})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        } else {
            Button(
                onClick = {
                    if (hasRecordPermission) {
                        viewModel.startSessionRecording()
                    } else {
                        onRequestPermission()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcousticCyan,
                    contentColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_session_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    tint = AcousticRed,
                    modifier = Modifier.size(20.dp).padding(end = 6.dp)
                )
                Text(
                    text = "Mulai Rekam Sesi Pengukuran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
private fun PillLabel(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1))
    }
}
