package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SoundMeasurementRecord
import com.example.ui.theme.AcousticCyan
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordDetailDialog(
    record: SoundMeasurementRecord,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(record.timestamp))

    // Parse samples for mini graph
    val samples = record.samplesJson.split(",")
        .mapNotNull { it.trim().toFloatOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "$dateStr • ${record.locationTag}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                IconButton(
                    onClick = {
                        val shareText = """
                            📊 Laporan Pengukuran Suara Decibel Meter
                            Judul: ${record.title}
                            Waktu: $dateStr
                            Lokasi: ${record.locationTag}
                            Durasi: ${record.durationSeconds} detik
                            Bobot: ${record.weightingType}
                            Status: ${record.riskLevel}
                            
                            Statistik Kebisingan:
                            • Rata-rata (Leq): ${"%.1f".format(record.avgDb)} dB
                            • Minimum: ${"%.1f".format(record.minDb)} dB
                            • Maksimum: ${"%.1f".format(record.maxDb)} dB
                            • Puncak: ${"%.1f".format(record.peakDb)} dB
                            • Frekuensi Dominan: ${record.dominantFreqHz} Hz
                            ${if (record.notes.isNotBlank()) "Catatan: ${record.notes}" else ""}
                            
                            Diukur menggunakan aplikasi Decibel Meter Offline.
                        """.trimIndent()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Bagikan Hasil Pengukuran")
                        context.startActivity(shareIntent)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Bagikan",
                        tint = AcousticCyan
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Key statistics grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Rata-rata", "%.1f".format(record.avgDb), AcousticCyan, Modifier.weight(1f))
                    StatCard("Maks", "%.1f".format(record.maxDb), Color(0xFFEF4444), Modifier.weight(1f))
                    StatCard("Min", "%.1f".format(record.minDb), Color(0xFF10B981), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Puncak", "%.1f dB".format(record.peakDb), Color(0xFFF59E0B), Modifier.weight(1f))
                    StatCard("Durasi", "${record.durationSeconds} dtk", Color.White, Modifier.weight(1f))
                    StatCard("Frekuensi", "${record.dominantFreqHz} Hz", Color(0xFF38BDF8), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mini graph if samples exist
                if (samples.isNotEmpty()) {
                    Text(
                        text = "Grafik Tren Kebisingan Sesi:",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCard)
                            .padding(6.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val minVal = (samples.minOrNull() ?: 20f) - 5f
                            val maxVal = (samples.maxOrNull() ?: 100f) + 5f

                            if (samples.size >= 2) {
                                val stepX = w / (samples.size - 1)
                                val path = Path()
                                samples.forEachIndexed { i, s ->
                                    val x = i * stepX
                                    val y = (1f - ((s - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)) * h
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(
                                    path = path,
                                    color = AcousticCyan,
                                    style = Stroke(width = 2f)
                                )
                            }
                        }
                    }
                }

                if (record.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Catatan: ${record.notes}",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcousticCyan,
                    contentColor = Color(0xFF0F172A)
                )
            ) {
                Text("Tutup", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDelete,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                modifier = Modifier.testTag("delete_record_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                )
                Text("Hapus")
            }
        }
    )
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 10.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
