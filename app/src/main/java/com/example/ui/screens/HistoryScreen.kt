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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Timer
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
import com.example.data.SoundMeasurementRecord
import com.example.ui.components.RecordDetailDialog
import com.example.ui.theme.AcousticAmber
import com.example.ui.theme.AcousticCyan
import com.example.ui.theme.AcousticGreen
import com.example.ui.theme.AcousticRed
import com.example.ui.theme.DarkCard
import com.example.viewmodel.MeterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MeterViewModel,
    modifier: Modifier = Modifier
) {
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val recordCount by viewModel.recordCount.collectAsStateWithLifecycle()
    val maxDbEver by viewModel.maxDbEver.collectAsStateWithLifecycle()
    val selectedFilterTag by viewModel.selectedFilterTag.collectAsStateWithLifecycle()
    val selectedRecordDetail by viewModel.selectedRecordDetail.collectAsStateWithLifecycle()

    var showClearConfirm by remember { mutableStateOf(false) }

    val tags = listOf("Semua", "Kamar", "Kantor", "Jalan", "Industri", "Musik", "Studio", "Lainnya")

    val filteredRecords = if (selectedFilterTag == "Semua") {
        allRecords
    } else {
        allRecords.filter { it.locationTag.equals(selectedFilterTag, ignoreCase = true) }
    }

    // Detail dialog
    selectedRecordDetail?.let { record ->
        RecordDetailDialog(
            record = record,
            onDelete = {
                viewModel.deleteRecord(record)
            },
            onDismiss = {
                viewModel.showRecordDetail(null)
            }
        )
    }

    // Clear all confirm dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = Color(0xFF131B2E),
            title = {
                Text("Hapus Semua Riwayat?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Semua catatan rekaman sesi kebisingan akan dihapus secara permanen.",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AcousticRed)
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Riwayat Pengukuran Suara",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Tersimpan di perangkat (100% Offline)",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                if (allRecords.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.testTag("clear_all_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Hapus Semua",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }

        item {
            // Overview Banner Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OverviewCard(
                    title = "Total Rekaman",
                    value = "$recordCount",
                    color = AcousticCyan,
                    modifier = Modifier.weight(1f)
                )
                OverviewCard(
                    title = "Puncak Tertinggi",
                    value = if (maxDbEver != null) "%.1f dB".format(maxDbEver) else "-",
                    color = AcousticRed,
                    modifier = Modifier.weight(1f)
                )
                val avgOfAll = if (allRecords.isNotEmpty()) allRecords.map { it.avgDb }.average().toFloat() else 0f
                OverviewCard(
                    title = "Rata-rata dB",
                    value = if (allRecords.isNotEmpty()) "%.1f dB".format(avgOfAll) else "-",
                    color = AcousticAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // Tag Filter Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                items(tags) { tag ->
                    val isSelected = tag == selectedFilterTag
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilterTag(tag) },
                        label = { Text(tag) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AcousticCyan,
                            selectedLabelColor = Color(0xFF0F172A),
                            containerColor = DarkCard,
                            labelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("filter_tag_$tag")
                    )
                }
            }
        }

        if (filteredRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (allRecords.isEmpty()) "Belum ada riwayat rekaman" else "Tidak ada rekaman untuk kategori '$selectedFilterTag'",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gunakan tombol 'Mulai Rekam' di layar utama untuk merekam sesi.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                HistoryItemCard(
                    record = record,
                    onClick = { viewModel.showRecordDetail(record) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OverviewCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 10.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = color
            )
        }
    }
}

@Composable
private fun HistoryItemCard(
    record: SoundMeasurementRecord,
    onClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(record.timestamp))

    val riskColor = when {
        record.avgDb < 30f -> AcousticCyan
        record.avgDb < 50f -> AcousticGreen
        record.avgDb < 70f -> Color(0xFF38BDF8)
        record.avgDb < 85f -> AcousticAmber
        else -> AcousticRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${record.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${record.locationTag} • $dateStr",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Risk badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(riskColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = record.riskLevel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Rata-rata", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(
                        "%.1f dB".format(record.avgDb),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = AcousticCyan
                    )
                }
                Column {
                    Text("Maks", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(
                        "%.1f dB".format(record.maxDb),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = AcousticRed
                    )
                }
                Column {
                    Text("Puncak", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(
                        "%.1f dB".format(record.peakDb),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = AcousticAmber
                    )
                }
                Column {
                    Text("Durasi", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            "${record.durationSeconds}s",
                            fontSize = 13.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }
    }
}
