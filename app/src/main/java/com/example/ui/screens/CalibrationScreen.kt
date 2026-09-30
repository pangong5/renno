package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.AcousticAmber
import com.example.ui.theme.AcousticCyan
import com.example.ui.theme.AcousticGreen
import com.example.ui.theme.AcousticRed
import com.example.ui.theme.AcousticTeal
import com.example.ui.theme.DarkCard
import com.example.viewmodel.MeterViewModel

@Composable
fun CalibrationScreen(
    viewModel: MeterViewModel,
    modifier: Modifier = Modifier
) {
    val meterData by viewModel.meterData.collectAsStateWithLifecycle()
    val calibrationOffset by viewModel.calibrationOffset.collectAsStateWithLifecycle()
    val responseMode by viewModel.responseMode.collectAsStateWithLifecycle()
    val weightingType by viewModel.weightingType.collectAsStateWithLifecycle()
    val warningThreshold by viewModel.warningThreshold.collectAsStateWithLifecycle()
    val enableVibrate by viewModel.enableVibrate.collectAsStateWithLifecycle()

    var showTargetDialog by remember { mutableStateOf(false) }
    var targetDbInput by remember { mutableStateOf("60.0") }

    val scrollState = rememberScrollState()

    // Target Calibration Dialog
    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            containerColor = Color(0xFF131B2E),
            title = {
                Text(
                    text = "Kalibrasi ke Target Akurat",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Masukkan intensitas desibel (dB) yang Anda ketahui di ruangan saat ini (misal dari alat pengukur referensi atau standar kalibrator 94.0 dB):",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = targetDbInput,
                        onValueChange = { targetDbInput = it },
                        label = { Text("Target dB SPL") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AcousticCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("target_db_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = targetDbInput.toFloatOrNull()
                        if (parsed != null && parsed in 20.0f..120.0f) {
                            viewModel.calibrateToTarget(parsed)
                        }
                        showTargetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AcousticCyan, contentColor = Color(0xFF0F172A))
                ) {
                    Text("Terapkan Kalibrasi")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTargetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = AcousticCyan,
                modifier = Modifier.size(24.dp).padding(end = 6.dp)
            )
            Column {
                Text(
                    text = "Kalibrasi & Pengaturan",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Sesuaikan sensitivitas mikrofon perangkat Anda",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Calibration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bacaan Suara Saat Ini",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "%.1f dB".format(meterData.currentDb),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AcousticCyan
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Kompensasi Offset",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "%+.1f dB".format(calibrationOffset),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (calibrationOffset == 0.0f) Color.White else AcousticAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fine-tune buttons (-0.5 / +0.5) and slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setCalibrationOffset(calibrationOffset - 0.5f) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .testTag("offset_minus_button")
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurang 0.5 dB", tint = Color.White)
                    }

                    Slider(
                        value = calibrationOffset,
                        onValueChange = { viewModel.setCalibrationOffset(it) },
                        valueRange = -30.0f..30.0f,
                        steps = 59,
                        colors = SliderDefaults.colors(
                            thumbColor = AcousticCyan,
                            activeTrackColor = AcousticCyan,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp).testTag("calibration_slider")
                    )

                    IconButton(
                        onClick = { viewModel.setCalibrationOffset(calibrationOffset + 0.5f) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .testTag("offset_plus_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah 0.5 dB", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Presets
                Text(
                    text = "Profil Kalibrasi Cepat:",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetChip("Default (0 dB)", calibrationOffset == 0.0f, Modifier.weight(1f)) {
                        viewModel.applyPresetCalibration("default")
                    }
                    PresetChip("Kamar (~30)", false, Modifier.weight(1f)) {
                        viewModel.applyPresetCalibration("quiet_room")
                    }
                    PresetChip("Bicara (~60)", false, Modifier.weight(1f)) {
                        viewModel.applyPresetCalibration("normal_conversation")
                    }
                    PresetChip("Jalan (~75)", false, Modifier.weight(1f)) {
                        viewModel.applyPresetCalibration("traffic")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showTargetDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("target_calibrate_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AcousticCyan)
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kalibrasi ke Nilai Target Khusus...")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings Section
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Parameter Pengukuran",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Response Speed (FAST / SLOW)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Kecepatan Respons", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(
                            text = if (responseMode == "FAST") "FAST (125 ms) - Fluktuasi cepat" else "SLOW (1000 ms) - Standar lingkungan",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = responseMode == "FAST",
                            onClick = { viewModel.setResponseMode("FAST") },
                            label = { Text("FAST") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AcousticCyan,
                                selectedLabelColor = Color(0xFF0F172A)
                            )
                        )
                        FilterChip(
                            selected = responseMode == "SLOW",
                            onClick = { viewModel.setResponseMode("SLOW") },
                            label = { Text("SLOW") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AcousticCyan,
                                selectedLabelColor = Color(0xFF0F172A)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Weighting Type (dBA / dBZ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Pembobotan Frekuensi", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(
                            text = if (weightingType == "dBA") "dBA - Meniru kepekaan telinga manusia" else "dBZ - Respons frekuensi datar rata",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = weightingType == "dBA",
                            onClick = { viewModel.setWeightingType("dBA") },
                            label = { Text("dBA") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AcousticCyan,
                                selectedLabelColor = Color(0xFF0F172A)
                            )
                        )
                        FilterChip(
                            selected = weightingType == "dBZ",
                            onClick = { viewModel.setWeightingType("dBZ") },
                            label = { Text("dBZ") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AcousticCyan,
                                selectedLabelColor = Color(0xFF0F172A)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Vibration Alert Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = AcousticAmber,
                            modifier = Modifier.size(20.dp).padding(end = 6.dp)
                        )
                        Column {
                            Text("Peringatan Getar Kebisingan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Getar saat melebihi ${warningThreshold.toInt()} dB", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                    Switch(
                        checked = enableVibrate,
                        onCheckedChange = { viewModel.setEnableVibrate(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AcousticCyan,
                            checkedTrackColor = AcousticCyan.copy(alpha = 0.5f)
                        )
                    )
                }

                if (enableVibrate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ambang Batas Peringatan: ${warningThreshold.toInt()} dB", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                    Slider(
                        value = warningThreshold,
                        onValueChange = { viewModel.setWarningThreshold(it) },
                        valueRange = 60.0f..110.0f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = AcousticAmber,
                            activeTrackColor = AcousticAmber
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Noise Reference Table (WHO / OSHA Standards)
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tabel Panduan Kebisingan (WHO / OSHA)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Standar batas paparan kebisingan harian:",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(10.dp))

                NoiseRow("10 - 20 dB", "Gemerisik Daun, Studio Sunyi", "Aman Tanpa Batas", AcousticCyan)
                NoiseRow("30 - 45 dB", "Bisikan, Perpustakaan, Kamar Malam", "Aman Tanpa Batas", AcousticGreen)
                NoiseRow("50 - 65 dB", "Percakapan Santai, Kantor, AC Rumah", "Aman & Nyaman", Color(0xFF38BDF8))
                NoiseRow("70 - 80 dB", "Lalu Lintas Jalan, Restoran Ramai", "Mulai Mengganggu", AcousticAmber)
                NoiseRow("85 dB", "Lalu Lintas Padat, Blender Dapur", "Maks 8 Jam / Hari (OSHA)", Color(0xFFF97316))
                NoiseRow("95 dB", "Pemotong Rumput, Bor Listrik", "Maks 45 - 60 Menit", Color(0xFFEF4444))
                NoiseRow("105 dB", "Gergaji Mesin, Konser Rock", "Maks 4 - 5 Menit!", Color(0xFFDC2626))
                NoiseRow("120+ dB", "Sirene Dekat, Mesin Jet Pesawat", "Bahaya Kerusakan Permanen!", Color(0xFF991B1B))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PresetChip(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AcousticCyan else Color(0xFF1E293B))
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1)
        )
    }
}

@Composable
private fun NoiseRow(range: String, example: String, safeLimit: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = range, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                    Text(text = example, fontSize = 11.sp, color = Color(0xFFCBD5E1))
                }
            }
            Text(
                text = safeLimit,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
