package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import com.example.ui.components.SpectrumVisualizer
import com.example.ui.theme.AcousticAmber
import com.example.ui.theme.AcousticCyan
import com.example.ui.theme.AcousticGreen
import com.example.ui.theme.AcousticPurple
import com.example.ui.theme.AcousticTeal
import com.example.ui.theme.AcousticYellow
import com.example.ui.theme.DarkCard
import com.example.viewmodel.MeterViewModel

@Composable
fun SpectrumScreen(
    viewModel: MeterViewModel,
    modifier: Modifier = Modifier
) {
    val meterData by viewModel.meterData.collectAsStateWithLifecycle()
    val weightingType by viewModel.weightingType.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with Weighting selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Analisis Spektrum Frekuensi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "FFT 1024-Point • Real-Time 44.1 kHz",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Weighting filter chip
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = weightingType == "dBA",
                    onClick = { viewModel.setWeightingType("dBA") },
                    label = { Text("dBA (Telinga)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AcousticCyan,
                        selectedLabelColor = Color(0xFF0F172A),
                        containerColor = DarkCard,
                        labelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("filter_dba")
                )
                FilterChip(
                    selected = weightingType == "dBZ",
                    onClick = { viewModel.setWeightingType("dBZ") },
                    label = { Text("dBZ (Datar)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AcousticCyan,
                        selectedLabelColor = Color(0xFF0F172A),
                        containerColor = DarkCard,
                        labelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("filter_dbz")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Spectrum Visualizer
        SpectrumVisualizer(
            bands = meterData.bands,
            dominantFreqHz = meterData.dominantFreqHz,
            dominantNote = meterData.dominantNote,
            dominantBand = meterData.dominantBand
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Dominant Frequency Feature Card
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
                            text = "Frekuensi Puncak Dominan",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (meterData.dominantFreqHz > 20f) "%.1f Hz".format(meterData.dominantFreqHz) else "- Hz",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = AcousticCyan
                        )
                    }

                    // Pitch note circle
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = AcousticAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = meterData.dominantNote,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcousticAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Klasifikasi Rentang: ${meterData.dominantBand}",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Frequency Band Explanations
        Text(
            text = "Panduan Rentang Spektrum Audio",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        BandGuideCard("Sub-Bass (20 - 60 Hz)", "Getaran rendah, gemuruh mesin berat, subwoofer", AcousticCyan)
        BandGuideCard("Bass (60 - 250 Hz)", "Ketukan drum, bassline musik, dengungan knalpot", AcousticTeal)
        BandGuideCard("Low Mid (250 - 500 Hz)", "Kehangatan vokal pria, nada dasar piano, bodi instrumen", AcousticGreen)
        BandGuideCard("Mid (500 - 2000 Hz)", "Suara percakapan manusia, klakson, kejelasan suara", AcousticYellow)
        BandGuideCard("High Mid (2k - 4k Hz)", "Sensitivitas tertinggi telinga manusia, gesekan, tangisan bayi", AcousticAmber)
        BandGuideCard("Presence (4k - 6k Hz)", "Kecerahan suara, desisan artikulasi huruf S", AcousticPurple)
        BandGuideCard("Brilliance (> 6k Hz)", "Simbal drum, gemerisik udara, detail frekuensi tinggi", Color(0xFFF43F5E))

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun BandGuideCard(title: String, desc: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Column {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(text = desc, fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}
